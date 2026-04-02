package com.l4dar.nativeapp.render;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal GLB (glTF 2.0 binary) loader for car models.
 * Extracts vertex positions, normals, and indices from GLB files.
 */
public final class GLBLoader {
    private static final String TAG = "GLBLoader";
    private static final int MAGIC = 0x46546C67; // "glTF"
    private static final int VERSION = 2;
    private static final int CHUNK_TYPE_JSON = 0x4E4F534A; // "JSON"
    private static final int CHUNK_TYPE_BIN = 0x004E4942; // "BIN\0"

    /**
     * Parsed GLB mesh data ready for GPU rendering.
     */
    public static class GLBMesh {
        public float[] positions;  // x,y,z interleaved
        public float[] normals;    // x,y,z interleaved
        public float[] colors;     // r,g,b,a interleaved
        public int[] indices;      // triangle indices
        public int vertexCount;
        public int indexCount;
        public boolean hasTransparency;
    }

    /**
     * Load a GLB model from assets and extract mesh data.
     * @param context Android context for asset access
     * @param assetPath Path to .glb file in assets (e.g., "octane.glb")
     * @return Parsed GLBMesh or null on error
     */
    public static GLBMesh loadFromAssets(Context context, String assetPath) {
        try (InputStream is = new BufferedInputStream(context.getAssets().open(assetPath))) {
            return loadFromStream(is, assetPath);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load GLB: " + assetPath, e);
            return null;
        }
    }

    private static GLBMesh loadFromStream(InputStream is, String assetPath) throws IOException {
        byte[] headerBytes = new byte[12];
        if (is.read(headerBytes) != 12) {
            throw new IOException("Invalid GLB header");
        }

        ByteBuffer headerBuf = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
        int magic = headerBuf.getInt();
        int version = headerBuf.getInt();
        int length = headerBuf.getInt();

        if (magic != MAGIC || version != VERSION) {
            throw new IOException("Invalid GLB format");
        }

        // Read JSON chunk header
        byte[] jsonChunkHeader = new byte[8];
        if (is.read(jsonChunkHeader) != 8) {
            throw new IOException("Invalid JSON chunk header");
        }

        ByteBuffer jsonHeaderBuf = ByteBuffer.wrap(jsonChunkHeader).order(ByteOrder.LITTLE_ENDIAN);
        int jsonLength = jsonHeaderBuf.getInt();
        int jsonType = jsonHeaderBuf.getInt();

        if (jsonType != CHUNK_TYPE_JSON) {
            throw new IOException("Expected JSON chunk");
        }

        byte[] jsonBytes = new byte[jsonLength];
        if (is.read(jsonBytes) != jsonLength) {
            throw new IOException("Failed to read JSON chunk");
        }

        String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
        JSONObject gltf;
        try {
            gltf = new JSONObject(jsonStr);
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON chunk", e);
        }

        // Read BIN chunk header
        byte[] binChunkHeader = new byte[8];
        if (is.read(binChunkHeader) != 8) {
            throw new IOException("Invalid BIN chunk header");
        }

        ByteBuffer binHeaderBuf = ByteBuffer.wrap(binChunkHeader).order(ByteOrder.LITTLE_ENDIAN);
        int binLength = binHeaderBuf.getInt();
        int binType = binHeaderBuf.getInt();

        if (binType != CHUNK_TYPE_BIN) {
            throw new IOException("Expected BIN chunk");
        }

        byte[] binData = new byte[binLength];
        if (is.read(binData) != binLength) {
            throw new IOException("Failed to read BIN chunk");
        }

        ByteBuffer binBuf = ByteBuffer.wrap(binData).order(ByteOrder.LITTLE_ENDIAN);

        // Extract the full scene mesh hierarchy.
        return extractMesh(gltf, binBuf, assetPath);
    }

    private static GLBMesh extractMesh(JSONObject gltf, ByteBuffer binData, String assetPath) {
        try {
            JSONArray nodes = gltf.getJSONArray("nodes");
            JSONArray meshes = gltf.getJSONArray("meshes");
            JSONArray accessors = gltf.getJSONArray("accessors");
            JSONArray bufferViews = gltf.getJSONArray("bufferViews");
            JSONArray materials = gltf.optJSONArray("materials");

            if (nodes.length() == 0 || meshes.length() == 0) {
                Log.w(TAG, "GLB missing nodes or meshes");
                return null;
            }

            java.util.ArrayList<Float> positionList = new java.util.ArrayList<>();
            java.util.ArrayList<Float> normalList = new java.util.ArrayList<>();
            java.util.ArrayList<Float> colorList = new java.util.ArrayList<>();
            java.util.ArrayList<Integer> indexList = new java.util.ArrayList<>();

            java.util.List<Integer> roots = findRootNodes(gltf, nodes);
            if (roots.isEmpty()) {
                for (int i = 0; i < nodes.length(); i++) {
                    roots.add(i);
                }
            }

            float[] identity = new float[16];
            Matrix.setIdentityM(identity, 0);
            boolean[] visited = new boolean[nodes.length()];
            MeshAccumulator accumulator = new MeshAccumulator(positionList, normalList, colorList, indexList);
            boolean isDominusAsset = assetPath != null && assetPath.toLowerCase().contains("dominus");

            for (int rootNode : roots) {
                traverseNode(rootNode, identity, nodes, meshes, accessors, bufferViews, materials, binData, accumulator, visited, isDominusAsset, 0);
            }

            if (positionList.isEmpty() || indexList.isEmpty()) {
                Log.w(TAG, "No mesh geometry extracted from GLB scene");
                return null;
            }

            GLBMesh meshData = new GLBMesh();
            meshData.positions = toFloatArray(positionList);
            meshData.normals = toFloatArray(normalList);
            meshData.colors = toFloatArray(colorList);
            meshData.indices = toIntArray(indexList);
            meshData.vertexCount = meshData.positions.length / 3;
            meshData.indexCount = meshData.indices.length;
            meshData.hasTransparency = accumulator.hasTransparency;

            return meshData;

        } catch (Exception e) {
            Log.e(TAG, "Failed to extract mesh", e);
            return null;
        }
    }

    private static java.util.List<Integer> findRootNodes(JSONObject gltf, JSONArray nodes) throws Exception {
        java.util.ArrayList<Integer> roots = new java.util.ArrayList<>();
        JSONArray scenes = gltf.optJSONArray("scenes");
        if (scenes != null && scenes.length() > 0) {
            int sceneIndex = gltf.optInt("scene", 0);
            if (sceneIndex >= 0 && sceneIndex < scenes.length()) {
                JSONArray rootNodes = scenes.getJSONObject(sceneIndex).optJSONArray("nodes");
                if (rootNodes != null) {
                    for (int i = 0; i < rootNodes.length(); i++) {
                        roots.add(rootNodes.getInt(i));
                    }
                }
            }
        }

        if (!roots.isEmpty()) {
            return roots;
        }

        boolean[] referenced = new boolean[nodes.length()];
        for (int i = 0; i < nodes.length(); i++) {
            JSONArray children = nodes.getJSONObject(i).optJSONArray("children");
            if (children == null) {
                continue;
            }
            for (int j = 0; j < children.length(); j++) {
                int child = children.getInt(j);
                if (child >= 0 && child < referenced.length) {
                    referenced[child] = true;
                }
            }
        }
        for (int i = 0; i < referenced.length; i++) {
            if (!referenced[i]) {
                roots.add(i);
            }
        }
        return roots;
    }

    private static void traverseNode(int nodeIndex, float[] parentMatrix, JSONArray nodes,
                                     JSONArray meshes, JSONArray accessors, JSONArray bufferViews,
                                     JSONArray materials, ByteBuffer binData, MeshAccumulator accumulator,
                                     boolean[] visited, boolean isDominusAsset, int depth) throws Exception {
        if (nodeIndex < 0 || nodeIndex >= nodes.length()) {
            return;
        }
        if (visited[nodeIndex]) {
            return;
        }

        visited[nodeIndex] = true;

        JSONObject node = nodes.getJSONObject(nodeIndex);
        float[] localMatrix = buildNodeMatrix(node, isDominusAsset, depth);
        float[] worldMatrix = new float[16];
        Matrix.multiplyMM(worldMatrix, 0, parentMatrix, 0, localMatrix, 0);

        if (node.has("mesh") && shouldIncludeNode(node, isDominusAsset)) {
            appendMesh(node.getInt("mesh"), worldMatrix, meshes, accessors, bufferViews, materials, binData, accumulator);
        }
        JSONArray children = node.optJSONArray("children");
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.length(); i++) {
            traverseNode(children.getInt(i), worldMatrix, nodes, meshes, accessors, bufferViews, materials, binData, accumulator, visited, isDominusAsset, depth + 1);
        }
    }

    private static float[] buildNodeMatrix(JSONObject node, boolean isDominusAsset, int depth) throws Exception {
        float[] matrix = new float[16];

        if (node.has("matrix")) {
            JSONArray src = node.getJSONArray("matrix");
            for (int i = 0; i < 16; i++) {
                matrix[i] = (float) src.getDouble(i);
            }
            return matrix;
        }

        float[] translation = new float[] {0f, 0f, 0f};
        float[] rotation = new float[] {0f, 0f, 0f, 1f};
        float[] scale = new float[] {1f, 1f, 1f};

        JSONArray t = node.optJSONArray("translation");
        if (t != null) {
            for (int i = 0; i < 3; i++) {
                translation[i] = (float) t.getDouble(i);
            }
        }
        JSONArray r = node.optJSONArray("rotation");
        if (r != null) {
            for (int i = 0; i < 4; i++) {
                rotation[i] = (float) r.getDouble(i);
            }
        }
        JSONArray s = node.optJSONArray("scale");
        if (s != null) {
            for (int i = 0; i < 3; i++) {
                scale[i] = (float) s.getDouble(i);
            }
        }

        float[] translationMatrix = new float[16];
        float[] rotationMatrix = new float[16];
        float[] scaleMatrix = new float[16];
        float[] temp = new float[16];

        Matrix.setIdentityM(translationMatrix, 0);
        Matrix.translateM(translationMatrix, 0, translation[0], translation[1], translation[2]);
        buildQuaternionMatrix(rotation, rotationMatrix);
        Matrix.setIdentityM(scaleMatrix, 0);
        Matrix.scaleM(scaleMatrix, 0, scale[0], scale[1], scale[2]);

        Matrix.multiplyMM(temp, 0, rotationMatrix, 0, scaleMatrix, 0);
        Matrix.multiplyMM(matrix, 0, translationMatrix, 0, temp, 0);
        return matrix;
    }

    private static boolean shouldIncludeNode(JSONObject node, boolean isDominusAsset) {
        // Dominus exports use inconsistent naming across variants; include all mesh nodes and
        // rely on transform safeguards plus normalization instead of name-based filtering.
        return true;
    }

    private static void buildQuaternionMatrix(float[] q, float[] matrix) {
        float x = q[0];
        float y = q[1];
        float z = q[2];
        float w = q[3];
        float x2 = x + x;
        float y2 = y + y;
        float z2 = z + z;
        float xx = x * x2;
        float xy = x * y2;
        float xz = x * z2;
        float yy = y * y2;
        float yz = y * z2;
        float zz = z * z2;
        float wx = w * x2;
        float wy = w * y2;
        float wz = w * z2;

        Matrix.setIdentityM(matrix, 0);
        matrix[0] = 1f - (yy + zz);
        matrix[1] = xy + wz;
        matrix[2] = xz - wy;
        matrix[4] = xy - wz;
        matrix[5] = 1f - (xx + zz);
        matrix[6] = yz + wx;
        matrix[8] = xz + wy;
        matrix[9] = yz - wx;
        matrix[10] = 1f - (xx + yy);
    }

    private static void appendMesh(int meshIndex, float[] worldMatrix, JSONArray meshes,
                                   JSONArray accessors, JSONArray bufferViews, JSONArray materials,
                                   ByteBuffer binData, MeshAccumulator accumulator) throws Exception {
        if (meshIndex < 0 || meshIndex >= meshes.length()) {
            return;
        }

        JSONArray primitives = meshes.getJSONObject(meshIndex).optJSONArray("primitives");
        if (primitives == null) {
            return;
        }

        for (int primitiveIndex = 0; primitiveIndex < primitives.length(); primitiveIndex++) {
            try {
                JSONObject primitive = primitives.getJSONObject(primitiveIndex);
                JSONObject attributes = primitive.optJSONObject("attributes");
                if (attributes == null || !attributes.has("POSITION") || !primitive.has("indices")) {
                    continue;
                }

                int[] indices = extractIndexData(accessors, bufferViews, binData, primitive.getInt("indices"));
                float[] positions = extractAccessorData(accessors, bufferViews, binData, attributes.getInt("POSITION"));
                float[] normals = attributes.has("NORMAL")
                    ? extractAccessorData(accessors, bufferViews, binData, attributes.getInt("NORMAL"))
                    : generateNormals(positions, indices);

                int localVertexCount = positions.length / 3;
                boolean hasInvalidIndex = false;
                for (int index : indices) {
                    if (index < 0 || index >= localVertexCount) {
                        hasInvalidIndex = true;
                        break;
                    }
                }
                if (hasInvalidIndex) {
                    Log.w(TAG, "Skipping primitive with invalid indices mesh=" + meshIndex + " primitive=" + primitiveIndex);
                    continue;
                }

                if (normals.length < positions.length) {
                    normals = generateNormals(positions, indices);
                }

                float[] color = extractMaterialColor(materials, primitive.optInt("material", -1));
                if (color[3] < 0.999f) {
                    accumulator.hasTransparency = true;
                }

                int vertexOffset = accumulator.positions.size() / 3;
                for (int i = 0; i < positions.length; i += 3) {
                    float[] transformedPosition = transformPosition(worldMatrix, positions[i], positions[i + 1], positions[i + 2]);
                    float[] transformedNormal = transformNormal(worldMatrix, normals[i], normals[i + 1], normals[i + 2]);

                    accumulator.positions.add(transformedPosition[0]);
                    accumulator.positions.add(transformedPosition[1]);
                    accumulator.positions.add(transformedPosition[2]);
                    accumulator.normals.add(transformedNormal[0]);
                    accumulator.normals.add(transformedNormal[1]);
                    accumulator.normals.add(transformedNormal[2]);
                    accumulator.colors.add(color[0]);
                    accumulator.colors.add(color[1]);
                    accumulator.colors.add(color[2]);
                    accumulator.colors.add(color[3]);
                }

                for (int index : indices) {
                    accumulator.indices.add(vertexOffset + index);
                }
            } catch (Exception primitiveError) {
                Log.w(TAG, "Skipping invalid primitive mesh=" + meshIndex + " primitive=" + primitiveIndex, primitiveError);
            }
        }
    }

    private static float[] extractMaterialColor(JSONArray materials, int materialIndex) throws Exception {
        float[] color = new float[] {0.18f, 0.46f, 0.85f, 1f};
        if (materials == null || materialIndex < 0 || materialIndex >= materials.length()) {
            return color;
        }

        JSONObject material = materials.getJSONObject(materialIndex);
        JSONObject pbr = material.optJSONObject("pbrMetallicRoughness");
        if (pbr != null) {
            JSONArray baseColorFactor = pbr.optJSONArray("baseColorFactor");
            if (baseColorFactor != null && baseColorFactor.length() >= 4) {
                color[0] = (float) baseColorFactor.getDouble(0);
                color[1] = (float) baseColorFactor.getDouble(1);
                color[2] = (float) baseColorFactor.getDouble(2);
                color[3] = (float) baseColorFactor.getDouble(3);
            }
        }
        String alphaMode = material.optString("alphaMode", "OPAQUE");
        if ("OPAQUE".equals(alphaMode)) {
            color[3] = 1f;
        }
        return color;
    }

    private static float[] transformPosition(float[] matrix, float x, float y, float z) {
        float[] in = new float[] {x, y, z, 1f};
        float[] out = new float[4];
        Matrix.multiplyMV(out, 0, matrix, 0, in, 0);
        return new float[] {out[0], out[1], out[2]};
    }

    private static float[] transformNormal(float[] matrix, float x, float y, float z) {
        float m00 = matrix[0];
        float m01 = matrix[4];
        float m02 = matrix[8];
        float m10 = matrix[1];
        float m11 = matrix[5];
        float m12 = matrix[9];
        float m20 = matrix[2];
        float m21 = matrix[6];
        float m22 = matrix[10];

        float c00 = m11 * m22 - m12 * m21;
        float c01 = m12 * m20 - m10 * m22;
        float c02 = m10 * m21 - m11 * m20;
        float c10 = m02 * m21 - m01 * m22;
        float c11 = m00 * m22 - m02 * m20;
        float c12 = m01 * m20 - m00 * m21;
        float c20 = m01 * m12 - m02 * m11;
        float c21 = m02 * m10 - m00 * m12;
        float c22 = m00 * m11 - m01 * m10;

        float det = m00 * c00 + m01 * c01 + m02 * c02;

        float nx;
        float ny;
        float nz;
        if (Math.abs(det) > 1e-8f) {
            float invDet = 1f / det;
            // inverse-transpose(M) * n = cofactor(M) / det * n
            nx = (c00 * x + c01 * y + c02 * z) * invDet;
            ny = (c10 * x + c11 * y + c12 * z) * invDet;
            nz = (c20 * x + c21 * y + c22 * z) * invDet;
        } else {
            nx = m00 * x + m01 * y + m02 * z;
            ny = m10 * x + m11 * y + m12 * z;
            nz = m20 * x + m21 * y + m22 * z;
        }

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 1e-6f) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        return new float[] {nx, ny, nz};
    }

    private static float[] toFloatArray(java.util.ArrayList<Float> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static int[] toIntArray(java.util.ArrayList<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static final class MeshAccumulator {
        final java.util.ArrayList<Float> positions;
        final java.util.ArrayList<Float> normals;
        final java.util.ArrayList<Float> colors;
        final java.util.ArrayList<Integer> indices;
        boolean hasTransparency;

        MeshAccumulator(java.util.ArrayList<Float> positions,
                        java.util.ArrayList<Float> normals,
                        java.util.ArrayList<Float> colors,
                        java.util.ArrayList<Integer> indices) {
            this.positions = positions;
            this.normals = normals;
            this.colors = colors;
            this.indices = indices;
        }
    }

    private static float[] extractAccessorData(JSONArray accessors, JSONArray bufferViews,
                                               ByteBuffer binData, int accessorIdx) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIdx);
        int bufferViewIdx = accessor.getInt("bufferView");
        int offset = accessor.optInt("byteOffset", 0);
        int count = accessor.getInt("count");

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIdx);
        int bufferOffset = bufferView.optInt("byteOffset", 0);
        int byteStride = bufferView.optInt("byteStride", 12); // Default 3 floats

        float[] data = new float[count * 3];
        ByteBuffer buf = binData.duplicate();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(bufferOffset + offset);

        for (int i = 0; i < count; i++) {
            data[i * 3] = buf.getFloat();
            data[i * 3 + 1] = buf.getFloat();
            data[i * 3 + 2] = buf.getFloat();

            if (byteStride > 12) {
                buf.position(buf.position() + (byteStride - 12));
            }
        }

        return data;
    }

    private static int[] extractIndexData(JSONArray accessors, JSONArray bufferViews,
                                          ByteBuffer binData, int accessorIdx) throws Exception {
        JSONObject accessor = accessors.getJSONObject(accessorIdx);
        int bufferViewIdx = accessor.getInt("bufferView");
        int offset = accessor.optInt("byteOffset", 0);
        int count = accessor.getInt("count");
        int componentType = accessor.getInt("componentType"); // 5121=u8, 5123=u16, 5125=u32

        JSONObject bufferView = bufferViews.getJSONObject(bufferViewIdx);
        int bufferOffset = bufferView.optInt("byteOffset", 0);

        int[] data = new int[count];
        ByteBuffer buf = binData.duplicate();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(bufferOffset + offset);

        for (int i = 0; i < count; i++) {
            switch (componentType) {
                case 5121: // UNSIGNED_BYTE
                    data[i] = buf.get() & 0xFF;
                    break;
                case 5123: // UNSIGNED_SHORT
                    data[i] = buf.getShort() & 0xFFFF;
                    break;
                case 5125: // UNSIGNED_INT
                    data[i] = buf.getInt();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported index componentType: " + componentType);
            }
        }

        return data;
    }

    private static float[] generateNormals(float[] positions, int[] indices) {
        float[] normals = new float[positions.length];

        // Simple flat shading: average face normals per vertex
        for (int i = 0; i < positions.length; i++) {
            normals[i] = 0f;
        }

        // For each triangle, compute face normal and accumulate on indexed vertices.
        for (int i = 0; i + 2 < indices.length; i += 3) {
            int i0 = indices[i] * 3;
            int i1 = indices[i + 1] * 3;
            int i2 = indices[i + 2] * 3;

            if (i0 < 0 || i1 < 0 || i2 < 0
                    || i0 + 2 >= positions.length
                    || i1 + 2 >= positions.length
                    || i2 + 2 >= positions.length) {
                continue;
            }

            float x0 = positions[i0], y0 = positions[i0 + 1], z0 = positions[i0 + 2];
            float x1 = positions[i1], y1 = positions[i1 + 1], z1 = positions[i1 + 2];
            float x2 = positions[i2], y2 = positions[i2 + 1], z2 = positions[i2 + 2];

            // Edge vectors
            float e1x = x1 - x0, e1y = y1 - y0, e1z = z1 - z0;
            float e2x = x2 - x0, e2y = y2 - y0, e2z = z2 - z0;

            // Cross product (face normal)
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            normals[i0] += nx; normals[i0 + 1] += ny; normals[i0 + 2] += nz;
            normals[i1] += nx; normals[i1 + 1] += ny; normals[i1 + 2] += nz;
            normals[i2] += nx; normals[i2 + 1] += ny; normals[i2 + 2] += nz;
        }

        // Normalize
        for (int i = 0; i < normals.length; i += 3) {
            float x = normals[i], y = normals[i + 1], z = normals[i + 2];
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            if (len > 0.0001f) {
                normals[i] /= len;
                normals[i + 1] /= len;
                normals[i + 2] /= len;
            }
        }

        return normals;
    }
}
