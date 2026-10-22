# Universal Build Script for AppleTV Launcher
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$rootDir = Split-Path -Parent $scriptDir
$appDir = Join-Path $rootDir "src\app"
$releaseDir = Join-Path $rootDir "releases"

New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Building Apple TV Launcher for Android TV" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# Gradle build if wrapper or system gradle exists
if (Test-Path (Join-Path $appDir "gradlew.bat")) {
    Push-Location $appDir
    & .\gradlew.bat assembleRelease
    Pop-Location
} else {
    Write-Host "Open this project in Android Studio or compile using Gradle." -ForegroundColor Yellow
}
