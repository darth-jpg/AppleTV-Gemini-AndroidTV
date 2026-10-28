# 🍏 AppleTV-Gemini-AndroidTV

> **A blazing-fast (<40MB RAM), ad-free Apple TV (tvOS) experience for Android TV & Google TV, powered by a native on-device Google Gemini Voice Assistant and zero-latency game controller routing.**

[![Android TV](https://img.shields.io/badge/Android%20TV-9%20to%2014-green.svg)](https://developer.android.com/tv)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![RAM Usage](https://img.shields.io/badge/RAM%20Footprint-%3C40MB-brightgreen.svg)]()
[![Gemini AI](https://img.shields.io/badge/AI%20Assistant-Google%20Gemini%20Flash-blue.svg)](https://aistudio.google.com/)
[![Gaming](https://img.shields.io/badge/Controller-DualSense%205%20%7C%20Xbox-purple.svg)]()

---

## 📸 Screenshots (Real TV Running Android 12)

| Home Screen & tvOS Dock | Native Gemini AI Assistant Overlay |
| :---: | :---: |
| ![Home Screen](docs/images/home_screen.png) | ![Gemini Voice Assistant](docs/images/gemini_assistant.png) |

| Categorized Apps & Cloud Gaming Grid | 4K Apple Aerial Screensaver |
| :---: | :---: |
| ![Apps Grid](docs/images/apps_grid.png) | ![Aerial Screensaver](docs/images/screensaver.png) |

---

## ⚡ The Problem vs The Solution

| Feature | Stock Google TV / Factory OS | AppleTV-Gemini-AndroidTV |
| :--- | :--- | :--- |
| **RAM Footprint** | ~500MB – 650MB RAM | **< 40MB RAM** (92% reduction) |
| **Ads & Promoted Content** | Full-screen unskippable video ads & trailers | **Zero ads**, pure clean interface |
| **App Organization** | Fixed manufacturer rows & forced suggestions | **Interactive "Hold OK to Move"** with remote D-Pad |
| **Voice Assistant** | Slow, generic recommendations | **Google Gemini Flash** (Fast conversational TV control) |
| **Screensaver** | Generic Google ambient mode | **Apple TV 4K Aerials** (Ken Burns effect + live clock) |
| **Gaming Controllers** | OEM remote shortcuts hijack gamepads | **Zero-latency bypass** for DualSense 5 / Xbox |
| **Manufacturer Telemetry** | Active tracking & background bloat | **Universal Safe Debloat** (Google TV, TCL, Sony, Xiaomi, Philips) |

---

## ✨ Key Features

1. **Pure tvOS Aesthetics & Fluid Physics:**
   - **Frosted Glass Dock Band:** Translucent frosted glass capsule anchoring the dock to the bottom with subtle specular edge sheen.
   - **Cinematic Top Shelf:** Dynamic typography with large title, category badges (*Streaming & Séries, Cloud Gaming, Vídeo & Música*), and ambient radial glow.
   - **Specular Focus Sheen:** Refined 3D liquid glass focus highlight with smooth 1.06x elevation on active apps.
   - **Uniform Squircle Icon Balancing:** Automatic aspect-ratio centering for 1:1 square app icons inside glass cards.
   - **Interactive "Hold OK to Move" App Reordering:** Hold the remote OK button on any Dock or Grid tile to enter reorder mode. Use remote D-pad arrows to freely reposition apps horizontally and vertically; press OK or Back to lock in place. Order is permanently remembered.
   - **tvOS Parallax Scrim Dimming:** Smooth dynamic background dimming when scrolling down into the apps grid to maximize icon contrast and readability, automatically fading back to scenic landscape on the Dock.
   - **Interactive Apple Weather Card:** Frosted glass forecast modal with real-time temperature, apparent temperature (feels like), humidity %, wind speed, and location switcher.
   - **Bluetooth Audio & Accessories Shortcut:** Direct 1-click access to TV Bluetooth settings in Control Center for late-night headphones or pairing gamepads.
   - **Smart Input Folding & Custom HDMI Inputs:** Hide unused ports and customize active inputs (*PlayStation 5, Xbox, Nintendo Switch, PC Gamer, Apple TV*).
   - **App Hiding (Debloat UI):** Long-press any app in the grid to hide unwanted factory bloatware apps, with 1-click restore in Settings.
   - **Auto-Boot Guarantee (`BOOT_COMPLETED`):** Instant native launch on TV power-on, preventing factory tuners from taking over.
   - **tvOS Control Center & Quick Restart:** Integrated system menu on the Settings pill with 1-click TV reboot, sleep timer, screensaver trigger, and sound mode switcher.
   - Dynamic top bar with Sleep Timer, HDMI Inputs switcher, Wi-Fi status, and Settings.
   - Dynamic real-time weather widget (powered by Open-Meteo, zero API keys required).

2. **Gemini AI Voice Assistant (300ms latency):**
   - Direct remote control microphone button remapping via Android system `ACTION_ASSIST`.
   - Apple Intelligence-style pulsating animated orb in frosted glass card.
   - Structured JSON response actions: launches apps (*Plex, Netflix, SmartTube, Steam Link, Luna*), sets native TV sleep timers (*15, 30, 45, 60 mins*), switches HDMI inputs, changes wallpapers, or answers open general knowledge questions.

3. **Hardware Game Controller Protection:**
   - Dedicated low-level `AccessibilityService` (`RemoteKeyService`) with hardware input filtering.
   - Gamepads (PlayStation DualSense 5, Xbox Wireless Controller, Switch Pro) pass through with **0 ms latency** to Steam Link, Moonlight, or Luna, while preserving physical remote TV shortcut buttons.

4. **Cinematic Apple Aerial Screensavers:**
   - Built on native Android `DreamService` with smooth Ken Burns cinematic panning and 4K aerial photography.
   - Wakes instantly on any remote or gamepad button press.

---

## 🤖 Universal AI Agent Prompt (`PROMPT_BLUEPRINT.md`)

Want to adapt this project to your specific TV model (TCL, Sony, Xiaomi, Philips, Chromecast) using an AI agent (Cursor, Claude Code, Antigravity)?

👉 Check out [PROMPT_BLUEPRINT.md](PROMPT_BLUEPRINT.md) for the complete master prompt that autonomously inspects your hardware over ADB, cleans bloatware, and builds this experience.

---

## 🚀 Quick Installation (1-Click Wi-Fi Setup)

### Prerequisites:
1. Enable **Developer Options** and **USB Debugging** on your TV (*Settings -> System -> About -> Click Build 7 times -> Developer Options -> USB Debugging*).
2. Note your TV's IP address (*Settings -> Network & Internet*).

### Option A: Windows
1. Run `scripts/install_windows.bat`.
2. Enter your TV's IP address when prompted. The script automatically connects over Wi-Fi, safely debloats telemetry and ads, installs the APK, and sets up permissions.

### Option B: macOS & Linux
1. Open terminal and run:
   ```bash
   chmod +x scripts/install_linux_mac.sh
   ./scripts/install_linux_mac.sh
   ```
2. Enter your TV's IP address when prompted.

### Restore Stock Google TV:
Changed your mind? Restore factory settings and original launcher anytime in 1 click:
- **Windows:** `scripts/restore_stock_google_tv.bat`
- **macOS / Linux:** `chmod +x scripts/restore_linux_mac.sh && ./scripts/restore_linux_mac.sh`

---

## 📺 What if your TV runs Samsung Tizen, LG webOS, or Roku?

This launcher is built natively for **Android TV & Google TV** (TCL, Sony, Xiaomi, Philips, Hisense, Chromecast, Nvidia Shield, Fire TV). 

If you own a TV running a closed proprietary OS (**Samsung Tizen**, **LG webOS**, or **Roku**), you cannot install Android APKs directly, but you can achieve a clean, ad-free experience using these settings:

### 1. Samsung Smart TV (Tizen OS):
- **Disable ACR & Tracking:** *Settings -> General -> Privacy & Terms -> Turn off "Viewing Information Services" and "Interest-Based Advertising"*.
- **Hide Samsung TV Plus:** *Hover over the TV Plus icon in the home bar -> Press Down -> Select "Remove" -> Disable*.
- **Router DNS Ad-Block (NextDNS / Pi-hole):** Block `ads.samsung.com` and `samsungadhub.com`.

### 2. LG Smart TV (webOS):
- **Disable Screen Tracking (ACR):** *Settings -> General -> System -> Additional Settings -> Turn off "Live Plus"*.
- **Turn off Home Ads & Autoplay:** *Settings -> General -> System -> Additional Settings -> Home Settings -> Turn off "Home Promotion" and "Home Auto Launch"*.
- **Router DNS Ad-Block:** Block `ad.lgsmartad.com` and `ngfts.lge.com`.

### 3. Roku TV:
- **Limit Tracking:** *Settings -> Privacy -> Advertising -> Check "Limit ad tracking"*.

---

## 🛠️ Building from Source

### Configuration:
1. Open `src/app/src/main/java/com/appletv/launcher/GeminiClient.java`.
2. Add your free Google Gemini API key:
   ```java
   private static final String API_KEY = "YOUR_GEMINI_API_KEY_HERE";
   ```
   *(Get your free key with 1 click at [Google AI Studio](https://aistudio.google.com/))*.

3. (Optional) Customize your weather city in `MainActivity.java`:
   ```java
   prefs.getString("weather_city_name", "YOUR_CITY");
   ```

### Compile with Android SDK:
```bash
cd src/app
# Build with standard Gradle
./gradlew assembleRelease
```

---

## ⚖️ License
Released under the [MIT License](LICENSE).
Apple TV, tvOS, and Apple Intelligence are registered trademarks of Apple Inc. Google Gemini, Android, and Google TV are registered trademarks of Google LLC. This is an independent, non-commercial open-source project.
