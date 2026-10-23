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

# Check if Gradle wrapper exists
if (Test-Path (Join-Path $appDir "gradlew.bat")) {
    Push-Location $appDir
    & .\gradlew.bat assembleRelease
    Pop-Location
} else {
    # Direct SDK toolchain build
    $javaBin = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin"
    if (-not (Test-Path $javaBin)) {
        $javaBin = Split-Path (Get-Command java.exe -ErrorAction SilentlyContinue).Source
    }

    $java = Join-Path $javaBin "java.exe"
    $javac = Join-Path $javaBin "javac.exe"
    $jar = Join-Path $javaBin "jar.exe"

    $sdkDir = "C:\Android\sdk"
    if (-not (Test-Path $sdkDir)) {
        $sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
    }
    $buildToolsDir = Join-Path $sdkDir "build-tools\31.0.0"
    $aapt2 = Join-Path $buildToolsDir "aapt2.exe"
    $zipalign = Join-Path $buildToolsDir "zipalign.exe"
    $d8Jar = Join-Path $buildToolsDir "lib\d8.jar"
    $apksignerJar = Join-Path $buildToolsDir "lib\apksigner.jar"
    $androidJar = Join-Path $sdkDir "platforms\android-31\android.jar"
    $keystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
    if (-not (Test-Path $keystore)) {
        $keystore = Join-Path $rootDir "debug.keystore"
    }

    if ((Test-Path $aapt2) -and (Test-Path $javac) -and (Test-Path $keystore)) {
        Write-Host "Building via Android SDK Toolchain (aapt2 + javac + d8)..." -ForegroundColor Yellow
        Remove-Item "$appDir\build" -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Path "$appDir\build\obj" -Force | Out-Null

        & $aapt2 compile --dir "$appDir\src\main\res" -o "$appDir\build\res.zip"
        & $aapt2 link -I $androidJar --manifest "$appDir\src\main\AndroidManifest.xml" -o "$appDir\build\unaligned.apk" "$appDir\build\res.zip" --java "$appDir\src\main\java" -A "$appDir\src\main\assets"

        $javaFiles = Get-ChildItem -Path "$appDir\src\main\java\com\appletv\launcher\*.java" | Select-Object -ExpandProperty FullName
        & $javac -encoding UTF-8 -source 8 -target 8 -cp $androidJar -d "$appDir\build\obj" $javaFiles

        $classFiles = Get-ChildItem -Path "$appDir\build\obj\com\appletv\launcher\*.class" | Select-Object -ExpandProperty FullName
        & $java -cp $d8Jar com.android.tools.r8.D8 --lib $androidJar --output "$appDir\build" $classFiles

        Push-Location "$appDir\build"
        & $jar -uf "$appDir\build\unaligned.apk" classes.dex
        & $zipalign -f 4 "$appDir\build\unaligned.apk" "$appDir\build\appletv_launcher.apk"
        & $java -jar $apksignerJar sign --ks $keystore --ks-pass pass:android --key-pass pass:android "$appDir\build\appletv_launcher.apk"
        Pop-Location

        Copy-Item "$appDir\build\appletv_launcher.apk" -Destination (Join-Path $releaseDir "appletv_launcher.apk") -Force
        Remove-Item "$appDir\src\main\java\com\appletv\launcher\R.java" -Force -ErrorAction SilentlyContinue

        Write-Host "Build complete! Saved to releases/appletv_launcher.apk" -ForegroundColor Green
    } else {
        Write-Host "Open this project in Android Studio or compile using Gradle." -ForegroundColor Yellow
    }
}
