#!/usr/bin/env bash
# ========================================================
#   Apple TV Launcher for Android TV - Universal Installer
#   Compatible with macOS and Linux (Ubuntu, Debian, Fedora, Arch)
# ========================================================

set -e

echo "========================================================"
echo "  Apple TV Launcher for Android TV - Universal Installer"
echo "========================================================"
echo ""
echo "Este script instala o Apple TV Launcher na sua televisão ou box"
echo "e desativa em segurança a publicidade e telemetria de fábrica"
echo "(Google TV, TCL, Sony Bravia, Xiaomi Mi TV, Philips, Chromecast)."
echo ""

# Verify ADB is installed
if ! command -v adb &> /dev/null; then
    echo "[ERRO] O comando 'adb' não foi encontrado no seu sistema."
    echo "Instale o Android Debug Bridge (ADB):"
    echo "  - macOS: brew install android-platform-tools"
    echo "  - Ubuntu/Debian: sudo apt install adb"
    echo "  - Fedora: sudo dnf install android-tools"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
APK_PATH="${ROOT_DIR}/releases/appletv_launcher.apk"

read -p "Introduza o endereço IP da sua TV (ex: 192.168.0.50): " TV_IP
if [ -z "$TV_IP" ]; then
    echo "[ERRO] Endereço IP não especificado. A cancelar."
    exit 1
fi

echo ""
echo "A ligar ao dispositivo ${TV_IP}:5555..."
adb connect "${TV_IP}:5555"

TV_BRAND=$(adb shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r' || true)
TV_MODEL=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)
if [ -n "$TV_BRAND" ]; then
    echo "Dispositivo detetado: ${TV_BRAND} ${TV_MODEL}"
fi

echo ""
echo "[1/4] A desativar publicidade, telemetria e bloatware de fábrica..."

# --- Universal Google TV & Android TV Baseline (Todas as marcas) ---
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.apps.tv.dreamx >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.tvrecommendations >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.leanbacklauncher.recommendations >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.feedback >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.android.tv.feedbackconsent >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.tv.remotecontrol.logging >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.apps.tv.netoscope >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.adservices.api >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.google.android.ext.adservices.api >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 tv.samba.ssm >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 tv.samba.ssm.ui >/dev/null 2>&1 || true

# --- TCL Smart TVs ---
adb shell pm disable-user --user 0 com.tcl.tv.tclhome_passive >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.partnercustomizer >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.channelplus >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.guard >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.logkit >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.waterfall.overseas >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.useragreement >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.miracast >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.tcl.esticker >/dev/null 2>&1 || true

# --- Sony Bravia Smart TVs ---
adb shell pm disable-user --user 0 com.sony.dtv.sonyselect >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.promos >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.discovery >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.bivlinfo >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.tvlaunchercustomizer >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 android.autoinstalls.config.sony.bravia >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.demomode >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.da.service >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.qterics.da.product >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.sony.dtv.b2b.vendorprotocol >/dev/null 2>&1 || true

# --- Xiaomi / Mi TV / Mi Box ---
adb shell pm disable-user --user 0 com.miui.tv.analytics >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.xiaomi.statistic >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.mitv.tvhome.atv >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.mitv.tvhome.michannel >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.mitv.tvhome.mitvplus >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.xm.webcontent >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.xiaomi.mitv.tvmanager >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.xiaomi.mitv.smartshare >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 com.mitv.milinkservice >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 android.autoinstalls.config.xiaomi.amelie >/dev/null 2>&1 || true

# --- Philips Smart TVs ---
adb shell pm disable-user --user 0 org.droidtv.nettvrecommender >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 org.droidtv.channels >/dev/null 2>&1 || true
adb shell pm disable-user --user 0 org.droidtv.demomode >/dev/null 2>&1 || true

echo ""
echo "[2/4] A instalar o Apple TV Launcher..."
if [ -f "$APK_PATH" ]; then
    adb install -r -d "$APK_PATH"
else
    echo "[AVISO] Ficheiro APK não encontrado em releases/appletv_launcher.apk."
fi

echo ""
echo "[3/4] A configurar permissões de áudio, assistente e proteção de ecrã..."
adb shell pm grant com.appletv.launcher android.permission.RECORD_AUDIO >/dev/null 2>&1 || true
adb shell settings put secure enabled_accessibility_services com.appletv.launcher/com.appletv.launcher.RemoteKeyService >/dev/null 2>&1 || true
adb shell settings put secure accessibility_enabled 1 >/dev/null 2>&1 || true
adb shell settings put secure assistant com.appletv.launcher/.MainActivity >/dev/null 2>&1 || true
adb shell settings delete secure voice_interaction_service >/dev/null 2>&1 || true
adb shell settings put secure screensaver_components com.appletv.launcher/.AppleTvDreamService >/dev/null 2>&1 || true
adb shell settings put secure screensaver_enabled 1 >/dev/null 2>&1 || true

echo ""
echo "[4/4] A iniciar o Apple TV Launcher..."
adb shell am start -n com.appletv.launcher/.MainActivity >/dev/null 2>&1 || true
adb shell input keyevent 3 >/dev/null 2>&1 || true

echo ""
echo "========================================================"
echo "  Instalação concluída com sucesso!"
echo "  O Apple TV Launcher está agora ativo na sua televisão."
echo "========================================================"
echo ""
