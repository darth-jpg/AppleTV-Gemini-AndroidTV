#!/usr/bin/env bash
# ========================================================
#   Restaurar Google TV / Interface Original de Fábrica
#   Compatible with macOS and Linux
# ========================================================

set -e

echo "========================================================"
echo "  Restaurar Google TV / Interface Original de Fábrica"
echo "========================================================"
echo ""
echo "Este script restaura o launcher original da sua televisão"
echo "e reativa todos os serviços e aplicações de fábrica."
echo ""

# Verify ADB is installed
if ! command -v adb &> /dev/null; then
    echo "[ERRO] O comando 'adb' não foi encontrado no seu sistema."
    exit 1
fi

read -p "Introduza o endereço IP da sua TV (ex: 192.168.0.50): " TV_IP
if [ -z "$TV_IP" ]; then
    echo "[ERRO] Endereço IP não especificado. A cancelar."
    exit 1
fi

echo ""
echo "A ligar a ${TV_IP}:5555..."
adb connect "${TV_IP}:5555"

echo ""
echo "A reativar os serviços e launcher de fábrica..."

# --- Universal Google TV & Android TV Baseline ---
adb shell pm enable com.google.android.apps.tv.launcherx >/dev/null 2>&1 || true
adb shell pm enable com.google.android.apps.tv.dreamx >/dev/null 2>&1 || true
adb shell pm enable com.google.android.tungsten.setupwraith >/dev/null 2>&1 || true
adb shell pm enable com.google.android.tvrecommendations >/dev/null 2>&1 || true
adb shell pm enable com.google.android.leanbacklauncher.recommendations >/dev/null 2>&1 || true
adb shell pm enable com.google.android.feedback >/dev/null 2>&1 || true
adb shell pm enable com.android.tv.feedbackconsent >/dev/null 2>&1 || true
adb shell pm enable com.google.android.tv.remotecontrol.logging >/dev/null 2>&1 || true
adb shell pm enable com.google.android.apps.tv.netoscope >/dev/null 2>&1 || true
adb shell pm enable com.google.android.adservices.api >/dev/null 2>&1 || true
adb shell pm enable com.google.android.ext.adservices.api >/dev/null 2>&1 || true
adb shell pm enable tv.samba.ssm >/dev/null 2>&1 || true
adb shell pm enable tv.samba.ssm.ui >/dev/null 2>&1 || true

# --- TCL Smart TVs ---
adb shell pm enable com.tcl.tv.tclhome_passive >/dev/null 2>&1 || true
adb shell pm enable com.tcl.partnercustomizer >/dev/null 2>&1 || true
adb shell pm enable com.tcl.channelplus >/dev/null 2>&1 || true
adb shell pm enable com.tcl.guard >/dev/null 2>&1 || true
adb shell pm enable com.tcl.logkit >/dev/null 2>&1 || true
adb shell pm enable com.tcl.waterfall.overseas >/dev/null 2>&1 || true
adb shell pm enable com.tcl.useragreement >/dev/null 2>&1 || true
adb shell pm enable com.tcl.miracast >/dev/null 2>&1 || true
adb shell pm enable com.tcl.esticker >/dev/null 2>&1 || true

# --- Sony Bravia Smart TVs ---
adb shell pm enable com.sony.dtv.sonyselect >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.promos >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.discovery >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.bivlinfo >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.tvlaunchercustomizer >/dev/null 2>&1 || true
adb shell pm enable android.autoinstalls.config.sony.bravia >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.demomode >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.da.service >/dev/null 2>&1 || true
adb shell pm enable com.qterics.da.product >/dev/null 2>&1 || true
adb shell pm enable com.sony.dtv.b2b.vendorprotocol >/dev/null 2>&1 || true

# --- Xiaomi / Mi TV / Mi Box ---
adb shell pm enable com.miui.tv.analytics >/dev/null 2>&1 || true
adb shell pm enable com.xiaomi.statistic >/dev/null 2>&1 || true
adb shell pm enable com.mitv.tvhome.atv >/dev/null 2>&1 || true
adb shell pm enable com.mitv.tvhome.michannel >/dev/null 2>&1 || true
adb shell pm enable com.mitv.tvhome.mitvplus >/dev/null 2>&1 || true
adb shell pm enable com.xm.webcontent >/dev/null 2>&1 || true
adb shell pm enable com.xiaomi.mitv.tvmanager >/dev/null 2>&1 || true
adb shell pm enable com.xiaomi.mitv.smartshare >/dev/null 2>&1 || true
adb shell pm enable com.mitv.milinkservice >/dev/null 2>&1 || true
adb shell pm enable android.autoinstalls.config.xiaomi.amelie >/dev/null 2>&1 || true

# --- Philips Smart TVs ---
adb shell pm enable org.droidtv.nettvrecommender >/dev/null 2>&1 || true
adb shell pm enable org.droidtv.channels >/dev/null 2>&1 || true
adb shell pm enable org.droidtv.demomode >/dev/null 2>&1 || true

# Restaurar o launcher original como Home
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity >/dev/null 2>&1 || true
adb shell input keyevent 3 >/dev/null 2>&1 || true

echo ""
echo "========================================================"
echo "  Sistema restaurado com sucesso para a interface original!"
echo "========================================================"
echo ""
