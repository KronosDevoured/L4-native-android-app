package com.l4dar.nativeapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;

import com.l4dar.nativeapp.core.input.InputSnapshot;
import com.l4dar.nativeapp.core.math.Quat;
import com.l4dar.nativeapp.core.math.Vec3;
import com.l4dar.nativeapp.core.physics.L4PhysicsEngine;
import com.l4dar.nativeapp.core.settings.SettingsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Physics Integration Tests - Validates Android L4PhysicsEngine against CSV reference data
 * from web physics.js.
 * 
 * These tests require an Android device or emulator and validate actual physics parity.
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class PhysicsIntegrationTest {

    private static final float DEFAULT_DT = 1f / 60f; // 60 Hz fallback timestep
    private static final float RELATIVE_TOLERANCE = 0.01f;      // 1% relative per-component tolerance
    private static final float ABSOLUTE_TOLERANCE = 0.01f;      // 0.01 rad/s minimum per-component tolerance
    private static final float MAGNITUDE_TOLERANCE = 0.015f;    // Tight absolute magnitude tolerance
    private static final float FINAL_STATE_TOLERANCE = 0.02f;   // Final-frame confidence check

    // Confidence gate thresholds (stronger than previous 5% diverged-frame policy)
    private static final float MAX_DIVERGED_FRAME_RATIO = 0.02f; // 2% max diverged frames
    private static final int MAX_CONSECUTIVE_DIVERGED_FRAMES = 2;
    private static final float MAX_AXIS_MAE = 0.02f;             // Mean absolute error per axis
    private static final float MAX_AXIS_RMSE = 0.03f;            // RMS error per axis

    // CSV integrity thresholds
    private static final double CSV_DT_TOLERANCE = 1e-6;
    private static final double CSV_MAG_TOLERANCE = 1e-6;

    private L4PhysicsEngine physicsEngine;
    private Quat carQuaternion;
    private Context context;

    /**
     * Test data row from CSV reference files
     */
    private static class TestDataRow {
        int frame;
        double time;
        double wxRef, wyRef, wzRef;
        double magRef;
        double inputPitch, inputYaw, inputRoll;
        boolean darActive;

        static TestDataRow parse(String line) {
            String[] parts = line.split(",");
            TestDataRow row = new TestDataRow();
            row.frame = Integer.parseInt(parts[0]);
            row.time = Double.parseDouble(parts[1]);
            row.wxRef = Double.parseDouble(parts[2]);
            row.wyRef = Double.parseDouble(parts[3]);
            row.wzRef = Double.parseDouble(parts[4]);
            row.magRef = Double.parseDouble(parts[5]);
            row.inputPitch = Double.parseDouble(parts[6]);
            row.inputYaw = Double.parseDouble(parts[7]);
            row.inputRoll = Double.parseDouble(parts[8]);
            row.darActive = Boolean.parseBoolean(parts[9]);
            return row;
        }
    }

    private static class AxisStats {
        double absErrorSum = 0.0;
        double sqErrorSum = 0.0;
        double maxAbsError = 0.0;

        void add(float actual, float expected) {
            double err = Math.abs(actual - expected);
            absErrorSum += err;
            sqErrorSum += err * err;
            if (err > maxAbsError) {
                maxAbsError = err;
            }
        }

        double mae(int sampleCount) {
            return sampleCount > 0 ? absErrorSum / sampleCount : 0.0;
        }

        double rmse(int sampleCount) {
            return sampleCount > 0 ? Math.sqrt(sqErrorSum / sampleCount) : 0.0;
        }
    }

    @Before
    public void setUp() {
        // Get Android context from instrumentation
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Create SettingsManager with real Android context
        SettingsManager settings = new SettingsManager(context);
        settings.load();
        
        // Create physics engine
        physicsEngine = new L4PhysicsEngine(settings);
        
        // Create identity quaternion
        carQuaternion = new Quat();
        carQuaternion.set(0, 0, 0, 1);
    }

    /**
     * Load test data from assets CSV file
     */
    private List<TestDataRow> loadTestData(String filename) throws IOException {
        List<TestDataRow> data = new ArrayList<>();

        try (InputStream inputStream = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                data.add(TestDataRow.parse(line));
            }
        }

        return data;
    }

    /**
     * Validate the reference CSV structure so harness failures are not caused by bad assets.
     */
    private void validateCsvIntegrity(String filename, List<TestDataRow> data) {
        assertTrue("CSV must contain at least 2 rows for " + filename, data.size() >= 2);

        for (int i = 0; i < data.size(); i++) {
            TestDataRow row = data.get(i);

            assertEquals("Frame index mismatch in " + filename + " at row " + i,
                i, row.frame);

            if (i > 0) {
                TestDataRow prev = data.get(i - 1);
                double dt = row.time - prev.time;
                assertTrue("Non-positive dt in " + filename + " at frame " + row.frame,
                    dt > 0.0);
                assertEquals("Unexpected dt spacing in " + filename + " at frame " + row.frame,
                    DEFAULT_DT, dt, CSV_DT_TOLERANCE);
            }

            double magFromComponents = Math.sqrt(
                row.wxRef * row.wxRef + row.wyRef * row.wyRef + row.wzRef * row.wzRef);
            assertEquals("Reference magnitude mismatch in " + filename + " at frame " + row.frame,
                row.magRef, magFromComponents, CSV_MAG_TOLERANCE);
        }
    }

    /**
     * Check if actual value is within tolerance of expected
     */
    private boolean withinTolerance(float actual, float expected) {
        float absExpected = Math.abs(expected);
        float relativeTol = absExpected * RELATIVE_TOLERANCE;
        float tol = Math.max(relativeTol, ABSOLUTE_TOLERANCE);
        return Math.abs(actual - expected) <= tol;
    }

    /**
     * Validate physics parity for a single scenario
     */
    private void validateScenario(String filename) throws IOException {
        System.out.println("\n===== Physics Parity Validation: " + filename + " =====");
        
        List<TestDataRow> testData = loadTestData(filename);
        assertTrue("Test data should not be empty for " + filename, !testData.isEmpty());
        validateCsvIntegrity(filename, testData);

        // Continuous trajectory validation: start from identity once and integrate row by row.
        physicsEngine.reset();
        carQuaternion.set(0, 0, 0, 1);

        int passed = 0;
        int failed = 0;
        int consecutiveFailed = 0;
        int maxConsecutiveFailed = 0;

        AxisStats wxStats = new AxisStats();
        AxisStats wyStats = new AxisStats();
        AxisStats wzStats = new AxisStats();
        AxisStats magStats = new AxisStats();

        for (int i = 0; i < testData.size(); i++) {
            TestDataRow row = testData.get(i);

            float dt = DEFAULT_DT;
            if (i > 0) {
                dt = (float) (row.time - testData.get(i - 1).time);
            }

            int airRollDirection = 0;
            float airRollIntensity = 1.0f;
            if (row.darActive) {
                if (Math.abs(row.inputRoll) > 1e-6) {
                    airRollDirection = (int) Math.signum(row.inputRoll);
                    airRollIntensity = (float) Math.min(1.0, Math.abs(row.inputRoll));
                } else {
                    // Existing CSV assets encode DAR activity without directional roll value.
                    airRollDirection = 1;
                    airRollIntensity = 1.0f;
                }
            }

            // Create input snapshot from CSV
            InputSnapshot input = new InputSnapshot(
                (float) row.inputYaw,
                (float) row.inputPitch,
                1.0f,
                0.0f,
                0.0f,
                0.0f,
                airRollDirection,
                airRollIntensity,
                row.darActive
            );

            // Step physics
            physicsEngine.performStep(input, dt, carQuaternion);

            // Get output angular velocity
            Vec3 w = physicsEngine.getAngularVelocity();
            float wMag = (float) Math.sqrt(w.x * w.x + w.y * w.y + w.z * w.z);

            wxStats.add(w.x, (float) row.wxRef);
            wyStats.add(w.y, (float) row.wyRef);
            wzStats.add(w.z, (float) row.wzRef);
            magStats.add(wMag, (float) row.magRef);

            // Compare with reference
            boolean wxMatch = withinTolerance(w.x, (float) row.wxRef);
            boolean wyMatch = withinTolerance(w.y, (float) row.wyRef);
            boolean wzMatch = withinTolerance(w.z, (float) row.wzRef);
            boolean magMatch = Math.abs(wMag - (float) row.magRef) <= MAGNITUDE_TOLERANCE;

            if (wxMatch && wyMatch && wzMatch && magMatch) {
                passed++;
                consecutiveFailed = 0;
            } else {
                failed++;
                consecutiveFailed++;
                if (consecutiveFailed > maxConsecutiveFailed) {
                    maxConsecutiveFailed = consecutiveFailed;
                }
                if (failed <= 3) {
                    System.out.printf(
                        "Frame %d: got (%.4f, %.4f, %.4f | mag %.4f), expected (%.4f, %.4f, %.4f | mag %.4f)\n",
                        row.frame, w.x, w.y, w.z, wMag, row.wxRef, row.wyRef, row.wzRef, row.magRef);
                }
            }
        }

        TestDataRow last = testData.get(testData.size() - 1);
        Vec3 finalW = physicsEngine.getAngularVelocity();
        float finalMag = (float) Math.sqrt(finalW.x * finalW.x + finalW.y * finalW.y + finalW.z * finalW.z);

        double wxMae = wxStats.mae(testData.size());
        double wyMae = wyStats.mae(testData.size());
        double wzMae = wzStats.mae(testData.size());

        double wxRmse = wxStats.rmse(testData.size());
        double wyRmse = wyStats.rmse(testData.size());
        double wzRmse = wzStats.rmse(testData.size());

        double divergedRatio = (double) failed / (double) testData.size();

        System.out.printf("Results: %d/%d frames match (%.1f%% parity)\n",
            passed, testData.size(), (100.0 * passed / testData.size()));
        System.out.printf(
            "Stats: divergedRatio=%.3f, maxConsecutiveDiverged=%d, MAE(wx/wy/wz)=%.4f/%.4f/%.4f, RMSE(wx/wy/wz)=%.4f/%.4f/%.4f, maxAbsErr(wx/wy/wz)=%.4f/%.4f/%.4f\n",
            divergedRatio,
            maxConsecutiveFailed,
            wxMae, wyMae, wzMae,
            wxRmse, wyRmse, wzRmse,
            wxStats.maxAbsError, wyStats.maxAbsError, wzStats.maxAbsError
        );

        assertTrue("Too many diverged frames for " + filename + ": ratio=" + divergedRatio,
            divergedRatio <= MAX_DIVERGED_FRAME_RATIO);
        assertTrue("Too many consecutive diverged frames for " + filename + ": max=" + maxConsecutiveFailed,
            maxConsecutiveFailed <= MAX_CONSECUTIVE_DIVERGED_FRAMES);

        assertTrue("wx MAE too high for " + filename + ": " + wxMae, wxMae <= MAX_AXIS_MAE);
        assertTrue("wy MAE too high for " + filename + ": " + wyMae, wyMae <= MAX_AXIS_MAE);
        assertTrue("wz MAE too high for " + filename + ": " + wzMae, wzMae <= MAX_AXIS_MAE);

        assertTrue("wx RMSE too high for " + filename + ": " + wxRmse, wxRmse <= MAX_AXIS_RMSE);
        assertTrue("wy RMSE too high for " + filename + ": " + wyRmse, wyRmse <= MAX_AXIS_RMSE);
        assertTrue("wz RMSE too high for " + filename + ": " + wzRmse, wzRmse <= MAX_AXIS_RMSE);

        assertTrue("Magnitude MAE too high for " + filename + ": " + magStats.mae(testData.size()),
            magStats.mae(testData.size()) <= MAGNITUDE_TOLERANCE);

        assertEquals("Final wx mismatch for " + filename, (float) last.wxRef, finalW.x, FINAL_STATE_TOLERANCE);
        assertEquals("Final wy mismatch for " + filename, (float) last.wyRef, finalW.y, FINAL_STATE_TOLERANCE);
        assertEquals("Final wz mismatch for " + filename, (float) last.wzRef, finalW.z, FINAL_STATE_TOLERANCE);
        assertEquals("Final magnitude mismatch for " + filename, (float) last.magRef, finalMag, FINAL_STATE_TOLERANCE);

    }

    @Test
    public void testPhysicsParity_PitchNoDar() throws IOException {
        validateScenario("L4_pitch_nodar_test.csv");
    }

    @Test
    public void testPhysicsParity_YawNoDar() throws IOException {
        validateScenario("L4_yaw_nodar_test.csv");
    }

    @Test
    public void testPhysicsParity_RollDar() throws IOException {
        validateScenario("L4_roll_dar_test.csv");
    }

    @Test
    public void testPhysicsParity_PitchDar() throws IOException {
        validateScenario("L4_pitch_dar_test.csv");
    }

    @Test
    public void testPhysicsParity_YawDar() throws IOException {
        validateScenario("L4_yaw_dar_test.csv");
    }

    @Test
    public void testPhysicsParity_Diagonal22Dar() throws IOException {
        validateScenario("L4_diagonal_22_dar_test.csv");
    }

    @Test
    public void testPhysicsParity_Diagonal45Dar() throws IOException {
        validateScenario("L4_diagonal_45_dar_test.csv");
    }

    @Test
    public void testPhysicsParity_Diagonal67Dar() throws IOException {
        validateScenario("L4_diagonal_67_dar_test.csv");
    }
}
