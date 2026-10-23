@echo off
chcp 65001 >nul
echo ========================================================
echo   Restaurar Google TV / Interface Original de Fábrica
echo ========================================================
echo.
echo Este script restaura o launcher original da sua televisão
echo e reativa todos os serviços e aplicações de fábrica.
echo.

set /p TV_IP="Introduza o endereço IP da sua TV (ex: 192.168.0.50): "
if "%TV_IP%"=="" (
    echo [ERRO] Endereço IP não especificado. A cancelar.
    pause
    exit /b 1
)

echo.
echo A ligar a %TV_IP%:5555...
adb connect %TV_IP%:5555

echo.
echo A reativar os serviços e launcher de fábrica...

REM --- Universal Google TV & Android TV Baseline ---
adb shell pm enable com.google.android.apps.tv.launcherx >nul 2>&1
adb shell pm enable com.google.android.apps.tv.dreamx >nul 2>&1
adb shell pm enable com.google.android.tungsten.setupwraith >nul 2>&1
adb shell pm enable com.google.android.tvrecommendations >nul 2>&1
adb shell pm enable com.google.android.leanbacklauncher.recommendations >nul 2>&1
adb shell pm enable com.google.android.feedback >nul 2>&1
adb shell pm enable com.android.tv.feedbackconsent >nul 2>&1
adb shell pm enable com.google.android.tv.remotecontrol.logging >nul 2>&1
adb shell pm enable com.google.android.apps.tv.netoscope >nul 2>&1
adb shell pm enable com.google.android.adservices.api >nul 2>&1
adb shell pm enable com.google.android.ext.adservices.api >nul 2>&1
adb shell pm enable tv.samba.ssm >nul 2>&1
adb shell pm enable tv.samba.ssm.ui >nul 2>&1

REM --- TCL Smart TVs ---
adb shell pm enable com.tcl.tv.tclhome_passive >nul 2>&1
adb shell pm enable com.tcl.partnercustomizer >nul 2>&1
adb shell pm enable com.tcl.channelplus >nul 2>&1
adb shell pm enable com.tcl.guard >nul 2>&1
adb shell pm enable com.tcl.logkit >nul 2>&1
adb shell pm enable com.tcl.waterfall.overseas >nul 2>&1
adb shell pm enable com.tcl.useragreement >nul 2>&1
adb shell pm enable com.tcl.miracast >nul 2>&1
adb shell pm enable com.tcl.esticker >nul 2>&1

REM --- Sony Bravia Smart TVs ---
adb shell pm enable com.sony.dtv.sonyselect >nul 2>&1
adb shell pm enable com.sony.dtv.promos >nul 2>&1
adb shell pm enable com.sony.dtv.discovery >nul 2>&1
adb shell pm enable com.sony.dtv.bivlinfo >nul 2>&1
adb shell pm enable com.sony.dtv.tvlaunchercustomizer >nul 2>&1
adb shell pm enable android.autoinstalls.config.sony.bravia >nul 2>&1
adb shell pm enable com.sony.dtv.demomode >nul 2>&1
adb shell pm enable com.sony.dtv.da.service >nul 2>&1
adb shell pm enable com.qterics.da.product >nul 2>&1
adb shell pm enable com.sony.dtv.b2b.vendorprotocol >nul 2>&1

REM --- Xiaomi / Mi TV / Mi Box ---
adb shell pm enable com.miui.tv.analytics >nul 2>&1
adb shell pm enable com.xiaomi.statistic >nul 2>&1
adb shell pm enable com.mitv.tvhome.atv >nul 2>&1
adb shell pm enable com.mitv.tvhome.michannel >nul 2>&1
adb shell pm enable com.mitv.tvhome.mitvplus >nul 2>&1
adb shell pm enable com.xm.webcontent >nul 2>&1
adb shell pm enable com.xiaomi.mitv.tvmanager >nul 2>&1
adb shell pm enable com.xiaomi.mitv.smartshare >nul 2>&1
adb shell pm enable com.mitv.milinkservice >nul 2>&1
adb shell pm enable android.autoinstalls.config.xiaomi.amelie >nul 2>&1

REM --- Philips Smart TVs ---
adb shell pm enable org.droidtv.nettvrecommender >nul 2>&1
adb shell pm enable org.droidtv.channels >nul 2>&1
adb shell pm enable org.droidtv.demomode >nul 2>&1

REM Restaurar o launcher original como Home
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity >nul 2>&1
adb shell input keyevent 3

echo.
echo ========================================================
echo   Sistema restaurado com sucesso para a interface original!
echo ========================================================
echo.
pause
