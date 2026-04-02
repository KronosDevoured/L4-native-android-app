param(
    [string]$GradleVersion = "8.9"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    Write-Error "Gradle is not installed or not on PATH. Install Gradle first, then rerun this script."
}

Write-Host "Generating Gradle wrapper (version $GradleVersion)..."
gradle wrapper --gradle-version $GradleVersion
Write-Host "Wrapper generated. Build with .\\gradlew.bat assembleDebug"
