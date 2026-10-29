package com.appletv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class RemoteKeyService extends AccessibilityService {
    private static final String TAG = "RemoteKeyService";

    public static RemoteKeyService instance;

    private static final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private static Runnable sleepRunnable;
    private static long sleepTargetTime = 0;

    public static void setSleepTimer(int minutes) {
        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
        }
        if (minutes <= 0) {
            sleepTargetTime = 0;
            Log.i(TAG, "Sleep timer cancelled");
            return;
        }

        sleepTargetTime = System.currentTimeMillis() + (minutes * 60L * 1000L);
        Log.i(TAG, "Sleep timer set for " + minutes + " minutes (target=" + sleepTargetTime + ")");

        sleepRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = sleepTargetTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    putTvToSleep();
                    sleepTargetTime = 0;
                    sleepRunnable = null;
                } else {
                    sleepHandler.postDelayed(this, Math.min(remaining, 5000L));
                }
            }
        };
        sleepHandler.postDelayed(sleepRunnable, Math.min(minutes * 60L * 1000L, 5000L));
    }

    public static long getRemainingSleepMinutes() {
        if (sleepTargetTime <= 0) return 0;
        long diff = sleepTargetTime - System.currentTimeMillis();
        if (diff <= 0) return 0;
        return (diff + 59999L) / 60000L;
    }

    public static void putTvToSleep() {
        Log.i(TAG, "Triggering TV sleep...");
        if (instance != null) {
            boolean success = instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            Log.i(TAG, "performGlobalAction GLOBAL_ACTION_LOCK_SCREEN result=" + success);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "RemoteKeyService connected!");
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) {
                info = new AccessibilityServiceInfo();
            }
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        } catch (Exception e) {
            Log.e(TAG, "Error in onServiceConnected", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private boolean isGamepadOrController(KeyEvent event) {
        if (event == null) return false;
        int keyCode = event.getKeyCode();
        if (KeyEvent.isGamepadButton(keyCode)) {
            return true;
        }
        InputDevice device = event.getDevice();
        if (device != null) {
            String name = device.getName();
            if (name != null) {
                String lower = name.toLowerCase();
                // Never treat the TV remote as a gamepad
                if (lower.contains("rc833") || lower.contains("consumer control") || 
                    lower.contains("remote") || lower.contains("venus_ir") || lower.contains("tcl")) {
                    return false;
                }
                if (lower.contains("dualsense") || lower.contains("wireless controller") ||
                    lower.contains("playstation") || lower.contains("xbox") || 
                    lower.contains("gamepad") || lower.contains("joystick")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // NEVER intercept inputs from game controllers (DualSense, Xbox, etc.)
        if (isGamepadOrController(event)) {
            return super.onKeyEvent(event);
        }

        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int action = event.getAction();
        Log.i(TAG, "onKeyEvent: action=" + action + " keyCode=" + keyCode + " scanCode=" + scanCode);

        boolean isMicKey = (keyCode == KeyEvent.KEYCODE_VOICE_ASSIST ||
            keyCode == KeyEvent.KEYCODE_SEARCH ||
            keyCode == KeyEvent.KEYCODE_ASSIST ||
            keyCode == 231 || keyCode == 219 || keyCode == 84 ||
            scanCode == 582 || scanCode == 217 || scanCode == 570);

        boolean isOurKey = isMicKey || (keyCode == 4062 || scanCode == 566 ||
            keyCode == 4020 || keyCode == 4184 || scanCode == 456 ||
            keyCode == 4141 || scanCode == 762 ||
            keyCode == 4099 || scanCode == 752 ||
            keyCode == 4065 || keyCode == 288 || keyCode == 188 ||
            keyCode == 4514 || keyCode == 4137 || scanCode == 233 || scanCode == 234 ||
            keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU ||
            keyCode == 312 || keyCode == 176 || keyCode == 82 ||
            keyCode == KeyEvent.KEYCODE_TV_INPUT || keyCode == KeyEvent.KEYCODE_GUIDE ||
            keyCode == 178 || keyCode == 172 || (keyCode >= 4506 && keyCode <= 4509));

        if (action == KeyEvent.ACTION_UP) {
            if (isMicKey) {
                triggerSiri();
                return true;
            }
            // Settings button (scanCode 233/234 or keyCodes)
            if (keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU || 
                keyCode == 312 || keyCode == 176 || keyCode == 82 || keyCode == 4514 || keyCode == 4137 ||
                scanCode == 233 || scanCode == 234) {
                openSettings();
                return true;
            }

            // Inputs button
            if (keyCode == KeyEvent.KEYCODE_TV_INPUT || keyCode == KeyEvent.KEYCODE_GUIDE || 
                keyCode == 178 || keyCode == 172 || (keyCode >= 4506 && keyCode <= 4509)) {
                openInputs();
                return true;
            }

            // Prime Video button (scancode 752 / keycode 4099)
            if (keyCode == 4099 || scanCode == 752) {
                launchApp("com.amazon.amazonvideo.livingroom");
                return true;
            }

            // Netflix button (scancode 566 / keycode 4062)
            if (keyCode == 4062 || scanCode == 566) {
                launchApp("com.netflix.ninja");
                return true;
            }

            // Disney+ button remapped to MAX (scancode 762 / keycode 4141)
            if (keyCode == 4141 || scanCode == 762) {
                launchApp("com.wbd.stream");
                return true;
            }

            // TCL Channel button remapped to Plex (scancode 456 / keycode 4020, 4184)
            if (keyCode == 4020 || keyCode == 4184 || scanCode == 456) {
                launchApp("com.plexapp.android");
                return true;
            }

            // YouTube button remapped to SmartTube (keycode 4065 / 288 / 188)
            if (keyCode == 4065 || keyCode == 288 || keyCode == 188) {
                launchApp("org.smarttube.stable");
                return true;
            }
        }

        if (action == KeyEvent.ACTION_DOWN && isOurKey) {
            return true;
        }

        return super.onKeyEvent(event);
    }

    private void launchApp(String pkg) {
        try {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (intent == null && "com.amazon.amazonvideo.livingroom".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.amazon.amazonvideo.livingroom", "com.amazon.ignition.IgnitionActivity");
            }
            if (intent == null && "com.netflix.ninja".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.netflix.ninja", "com.netflix.ninja.MainActivity");
            }
            if (intent == null && "com.plexapp.android".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.plexapp.android", "tv.plex.app.MainActivity");
            }
            if (intent == null && "com.wbd.stream".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.wbd.stream", "com.wbd.beam.BeamActivity");
            }
            if (intent == null && "org.smarttube.stable".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("org.smarttube.stable", "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity");
            }
            if (intent == null && "org.smarttube.stable".equals(pkg)) {
                intent = getPackageManager().getLeanbackLaunchIntentForPackage("com.google.android.youtube.tv");
            }
            if (intent == null && "com.google.android.youtube.tv".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.google.android.youtube.tv", "com.google.android.apps.youtube.tv.activity.ShellActivity");
            }
            if (intent == null && "com.android.tv.settings".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings");
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching " + pkg, e);
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private void openInputs() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("ACTION_OPEN_INPUTS", true);
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void triggerSiri() {
        try {
            Log.i(TAG, "Triggering Siri from RemoteKeyService");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("TRIGGER_SIRI", true);
            startActivity(intent);

            Intent broadcast = new Intent("com.appletv.launcher.ACTION_TRIGGER_SIRI");
            broadcast.setPackage(getPackageName());
            sendBroadcast(broadcast);
        } catch (Exception e) {
            Log.e(TAG, "Error triggering Siri", e);
        }
    }
}
