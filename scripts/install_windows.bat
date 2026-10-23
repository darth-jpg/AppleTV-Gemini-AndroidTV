@echo off
chcp 65001 >nul
echo ========================================================
echo   Apple TV Launcher for Android TV - Universal Installer
echo ========================================================
echo.
echo Este script instala o Apple TV Launcher na sua televisão ou box
echo e desativa em segurança a publicidade e telemetria de fábrica
echo (Google TV, TCL, Sony Bravia, Xiaomi Mi TV, Philips, Chromecast).
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

for /f "tokens=*" %%a in ('adb shell getprop ro.product.manufacturer 2^>nul') do set TV_BRAND=%%a
for /f "tokens=*" %%b in ('adb shell getprop ro.product.model 2^>nul') do set TV_MODEL=%%b
if not "%TV_BRAND%"=="" (
    echo Dispositivo detetado: %TV_BRAND% %TV_MODEL%
)

echo.
echo [1/4] A desativar publicidade, telemetria e bloatware de fábrica...

REM --- Universal Google TV & Android TV Baseline (Todas as marcas) ---
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.apps.tv.dreamx >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.tvrecommendations >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.leanbacklauncher.recommendations >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.feedback >nul 2>&1
adb shell pm disable-user --user 0 com.android.tv.feedbackconsent >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.tv.remotecontrol.logging >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.apps.tv.netoscope >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.adservices.api >nul 2>&1
adb shell pm disable-user --user 0 com.google.android.ext.adservices.api >nul 2>&1
adb shell pm disable-user --user 0 tv.samba.ssm >nul 2>&1
adb shell pm disable-user --user 0 tv.samba.ssm.ui >nul 2>&1

REM --- TCL Smart TVs ---
adb shell pm disable-user --user 0 com.tcl.tv.tclhome_passive >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.partnercustomizer >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.channelplus >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.guard >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.logkit >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.waterfall.overseas >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.useragreement >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.miracast >nul 2>&1
adb shell pm disable-user --user 0 com.tcl.esticker >nul 2>&1

REM --- Sony Bravia Smart TVs ---
adb shell pm disable-user --user 0 com.sony.dtv.sonyselect >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.promos >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.discovery >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.bivlinfo >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.tvlaunchercustomizer >nul 2>&1
adb shell pm disable-user --user 0 android.autoinstalls.config.sony.bravia >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.demomode >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.da.service >nul 2>&1
adb shell pm disable-user --user 0 com.qterics.da.product >nul 2>&1
adb shell pm disable-user --user 0 com.sony.dtv.b2b.vendorprotocol >nul 2>&1

REM --- Xiaomi / Mi TV / Mi Box ---
adb shell pm disable-user --user 0 com.miui.tv.analytics >nul 2>&1
adb shell pm disable-user --user 0 com.xiaomi.statistic >nul 2>&1
adb shell pm disable-user --user 0 com.mitv.tvhome.atv >nul 2>&1
adb shell pm disable-user --user 0 com.mitv.tvhome.michannel >nul 2>&1
adb shell pm disable-user --user 0 com.mitv.tvhome.mitvplus >nul 2>&1
adb shell pm disable-user --user 0 com.xm.webcontent >nul 2>&1
adb shell pm disable-user --user 0 com.xiaomi.mitv.tvmanager >nul 2>&1
adb shell pm disable-user --user 0 com.xiaomi.mitv.smartshare >nul 2>&1
adb shell pm disable-user --user 0 com.mitv.milinkservice >nul 2>&1
adb shell pm disable-user --user 0 android.autoinstalls.config.xiaomi.amelie >nul 2>&1

REM --- Philips Smart TVs ---
adb shell pm disable-user --user 0 org.droidtv.nettvrecommender >nul 2>&1
adb shell pm disable-user --user 0 org.droidtv.channels >nul 2>&1
adb shell pm disable-user --user 0 org.droidtv.demomode >nul 2>&1

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
