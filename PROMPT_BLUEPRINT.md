# 🧠 Universal AI Agent Master Blueprint (Prompt)

> **Copy and paste this prompt into your favorite AI Coding Agent (Antigravity, Cursor, Claude Code, or Windsurf) to autonomously transform your Smart TV into an Apple TV (tvOS) with Google Gemini AI.**

---

```markdown
You are an expert Android TV Systems Engineer and UI/UX Designer.
Your objective is to transform an Android TV / Google TV device into a blazing-fast (<40MB RAM), ad-free Apple TV (tvOS) experience with an on-device Google Gemini Voice Assistant and zero-latency game controller routing.

Follow this systematic 6-phase engineering workflow:

### Phase 1: Hardware Discovery & Environmental Audit
1. Connect to the TV over wireless ADB:
   `adb connect <TV_IP>:5555`
2. Audit the system properties:
   - Manufacturer: `adb shell getprop ro.product.manufacturer`
   - Model: `adb shell getprop ro.product.model`
   - Android Version: `adb shell getprop ro.build.version.release`
   - Display density & window manager metrics: `adb shell wm size; adb shell wm density`
3. Map Bluetooth and input hardware:
   - Identify remote controls: `adb shell dumpsys input`
   - Inspect bonded controllers: `adb shell dumpsys bluetooth_manager`

### Phase 2: Safe Manufacturer Debloating Matrix
1. Disable universal Google TV adware and telemetry:
   - `com.google.android.apps.tv.launcherx` (Stock ad-heavy launcher)
   - `com.google.android.apps.tv.dreamx` (Stock screensaver)
   - `com.google.android.tungsten.setupwraith`
   - `tv.samba.ssm.ui` (ACR automatic content recognition)
2. Apply manufacturer-specific service disabling (do NOT uninstall system packages, only `disable-user --user 0` to preserve factory reset safety):
   - IF TCL: disable `com.tcl.channelplus`, `com.tcl.guard`, `com.tcl.tv.tclhome_passive`, `com.tcl.logkit`, `com.tcl.waterfall.overseas`
   - IF Xiaomi: disable `com.xiaomi.mitv.upgrade`, `com.xiaomi.mitv.advert`
   - IF Sony: disable `com.sony.dtv.b2b.core` (preserve Bravia picture calibration services)

### Phase 3: tvOS Launcher Architecture (Native Leanback)
1. Build an Android TV launcher application (`com.appletv.launcher`) with `android.intent.category.HOME`:
   - Dynamic floating dock with Apple TV glassmorphism (radial gradient borders, blur layer, elevation scaling on focus).
   - Focused card animation: smooth 1.08x scale with 150ms interpolator.
   - Categorized horizontal scrolling grids for Apps, Cloud/PC Gaming (Steam Link, Luna, Moonlight), and System Tools.
   - Dynamic top bar with live clock, Open-Meteo weather widget (no API key required), and minimal action pills: [Sleep Timer] [Inputs] [Wi-Fi] [Settings].
2. Implement 4K Apple Aerial Screensavers using Android `DreamService` (`AppleTvDreamService`):
   - Multi-scene photographic landscapes with gentle Ken Burns pan & zoom animation.
   - Wakes on any remote or gamepad input.

### Phase 4: Gemini AI Voice Assistant Integration
1. Integrate the Google Gemini Flash REST API with structured JSON output:
   - Endpoint: `https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=<GEMINI_API_KEY>`
   - System Instruction enforcing strict JSON: `{"speech": "...", "action": "launch_app | sleep_timer | switch_hdmi | none", "param": "...", "badge": "..."}`
2. Route voice interactions through native `SpeechRecognizer` and Portuguese/English `TextToSpeech`.
3. Intercept the hardware remote microphone button by registering intent filters for `android.intent.action.ASSIST`, `android.intent.action.VOICE_ASSIST`, and `android.intent.action.SEARCH`.
4. Render an Apple Intelligence pulsating gradient orb inside a frosted glass floating card overlay.

### Phase 5: Hardware Game Controller Protection (Zero Latency)
1. Implement an `AccessibilityService` (`RemoteKeyService`) with `FLAG_REQUEST_FILTER_KEY_EVENTS` to intercept OEM remote shortcut keys (Netflix, Prime, Disney).
2. CRITICAL: Add a strict controller filter to NEVER intercept gamepad inputs:
   - Check `KeyEvent.isGamepadButton(keyCode)` -> return `super.onKeyEvent(event)` immediately.
   - In `isGamepadOrController(event)`, distinguish gamepads (DualSense 5, Xbox, Switch Pro) from TV remotes (exclude names containing "remote", "rc833", "consumer control").
   - Ensure A/Cross, B/Circle, X/Square, Y/Triangle pass with 0ms delay to Steam Link and cloud gaming services.

### Phase 6: Deployment, Permissions & Screenshot Verification
1. Build and sign the APK with Android SDK build-tools.
2. Deploy via ADB:
   - `adb install -r -d appletv_launcher.apk`
   - Grant microphone: `adb shell pm grant com.appletv.launcher android.permission.RECORD_AUDIO`
   - Enable accessibility: `adb shell settings put secure enabled_accessibility_services com.appletv.launcher/.RemoteKeyService`
   - Set assistant: `adb shell settings put secure assistant com.appletv.launcher/.MainActivity`
   - Set screensaver: `adb shell settings put secure screensaver_components com.appletv.launcher/.AppleTvDreamService`
3. Capture screen verification:
   - `adb shell screencap -p /sdcard/verify.png && adb pull /sdcard/verify.png`
   - Verify visual hierarchy, contrast ratios, and dock alignment.
```
