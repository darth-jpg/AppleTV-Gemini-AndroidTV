@echo off
chcp 65001 >nul
echo ========================================================
echo   Apple TV Launcher for Android TV - Universal Installer
echo ========================================================
echo.
echo Este script instala o Apple TV Launcher na sua televisão ou box
echo e configura os serviços de acessibilidade e controlo de comandos.
echo.

set /p TV_IP="Introduza o endereço IP da sua TV (ex: 192.168.0.50): "
if "%TV_IP%"=="" (
    echo [ERRO] Endereço IP não especificado. A cancelar.
    pause
    exit /b 1
)

echo.
echo A ligar ao dispositivo %TV_IP%:5555...
adb connect %TV_IP%:5555

echo.
echo [1/4] A desativar launcher padrão pesado com publicidade...
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.apps.tv.dreamx >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith >nul 2>&1
adb shell pm disable-user --user 0 tv.samba.ssm.ui >nul 2>&1

REM Otimizações opcionais para televisões TCL
adb shell pm disable-user --user 0 com.tcl.tv.tclhome_passive >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.partnercustomizer >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.channelplus >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.guard >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.logkit >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.waterfall.overseas >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.useragreement >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.miracast >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.esticker >nul 2>&1

echo.
echo [2/4] A instalar o Apple TV Launcher...
if exist "%~dp0..\releases\appletv_launcher.apk" (
    adb install -r -d "%~dp0..\releases\appletv_launcher.apk"
) else (
    echo [AVISO] Ficheiro APK não encontrado na pasta releases.
)

echo.
echo [3/4] A configurar permissões de áudio, assistente e proteção de ecrã...
adb shell pm grant com.appletv.launcher android.permission.RECORD_AUDIO >nul 2>&1
adb shell settings put secure enabled_accessibility_services com.appletv.launcher/com.appletv.launcher.RemoteKeyService >nul 2>&1
adb shell settings put secure accessibility_enabled 1 >nul 2>&1
adb shell settings put secure assistant com.appletv.launcher/.MainActivity >nul 2>&1
adb shell settings delete secure voice_interaction_service >nul 2>&1
adb shell settings put secure screensaver_components com.appletv.launcher/.AppleTvDreamService >nul 2>&1
adb shell settings put secure screensaver_enabled 1 >nul 2>&1

echo.
echo [4/4] A iniciar o Apple TV Launcher...
adb shell am start -n com.appletv.launcher/.MainActivity >nul 2>&1
adb shell input keyevent 3 >nul 2>&1

echo.
echo ========================================================
echo   Instalação concluída com sucesso!
echo   O Apple TV Launcher está agora ativo na sua televisão.
echo ========================================================
echo.
pause
