@echo off
chcp 65001 >nul
echo ========================================================
echo   Restaurar Google TV / Interface Original de Fábrica
echo ========================================================
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
adb shell pm enable com.google.android.apps.tv.launcherx >nul 2>&1
adb shell pm enable com.google.android.apps.tv.dreamx >nul 2>&1
adb shell pm enable com.google.android.tungsten.setupwraith >nul 2>&1

adb shell pm enable com.tcl.tv.tclhome_passive >nul 2>&1
adb shell pm enable com.tcl.partnercustomizer >nul 2>&1
adb shell pm enable com.tcl.channelplus >nul 2>&1
adb shell pm enable com.tcl.guard >nul 2>&1
adb shell pm enable com.tcl.logkit >nul 2>&1
adb shell pm enable com.tcl.waterfall.overseas >nul 2>&1
adb shell pm enable com.tcl.useragreement >nul 2>&1
adb shell pm enable com.tcl.miracast >nul 2>&1
adb shell pm enable com.tcl.esticker >nul 2>&1
adb shell pm enable tv.samba.ssm.ui >nul 2>&1

adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity >nul 2>&1
adb shell input keyevent 3

echo.
echo ========================================================
echo   Sistema restaurado com sucesso para a interface original!
echo ========================================================
echo.
pause
