package com.appletv.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

public class GlobalKeyReceiver extends BroadcastReceiver {
    private static final String TAG = "GlobalKeyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "GlobalKey received action: " + action);

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event == null) return;

        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int keyAction = event.getAction();
        Log.i(TAG, "GlobalKey keyCode=" + keyCode + " scanCode=" + scanCode + " action=" + keyAction);

        if (keyAction == KeyEvent.ACTION_UP) {
            handleKey(context, keyCode, scanCode);
        }
    }

    private void handleKey(Context context, int keyCode, int scanCode) {
        // Settings key
        if (keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU || 
            keyCode == 312 || keyCode == 176 || keyCode == 82 || keyCode == 4514 || keyCode == 4137 ||
            scanCode == 233 || scanCode == 234) {
            openSettings(context);
            return;
        }

        // Inputs key
        if (keyCode == KeyEvent.KEYCODE_TV_INPUT || keyCode == KeyEvent.KEYCODE_GUIDE ||
            keyCode == 178 || keyCode == 172 || (keyCode >= 4506 && keyCode <= 4509)) {
            openInputs(context);
            return;
        }

        // Prime Video button (scancode 752 / keycode 4099)
        if (keyCode == 4099 || scanCode == 752 || keyCode == KeyEvent.KEYCODE_BUTTON_4 || keyCode == KeyEvent.KEYCODE_BUTTON_X || 
            keyCode == KeyEvent.KEYCODE_BUTTON_12 || keyCode == 191 || keyCode == 199) {
            launchApp(context, "com.amazon.amazonvideo.livingroom");
            return;
        }

        // Netflix button (scancode 566 / keycode 4062)
        if (keyCode == 4062 || scanCode == 566 || keyCode == KeyEvent.KEYCODE_BUTTON_3 || keyCode == KeyEvent.KEYCODE_BUTTON_B || 
            keyCode == KeyEvent.KEYCODE_BUTTON_11 || keyCode == 190 || keyCode == 198) {
            launchApp(context, "com.netflix.ninja");
            return;
        }

        // Disney+ button remapped to MAX (scancode 762 / keycode 4141)
        if (keyCode == 4141 || scanCode == 762 || keyCode == KeyEvent.KEYCODE_BUTTON_2 || keyCode == KeyEvent.KEYCODE_BUTTON_Y || 
            keyCode == KeyEvent.KEYCODE_BUTTON_10 || keyCode == 189 || keyCode == 197) {
            launchApp(context, "com.wbd.stream");
            return;
        }

        // TCL Channel button remapped to Plex (scancode 456 / keycode 4020, 4184)
        if (keyCode == 4020 || keyCode == 4184 || scanCode == 456) {
            launchApp(context, "com.plexapp.android");
            return;
        }

        // YouTube button (keycode 4065)
        if (keyCode == 4065 || keyCode == KeyEvent.KEYCODE_BUTTON_1 || keyCode == KeyEvent.KEYCODE_BUTTON_A || 
            keyCode == KeyEvent.KEYCODE_BUTTON_9 || keyCode == 188 || keyCode == 196) {
            launchApp(context, "com.google.android.youtube.tv");
            return;
        }
    }

    private void launchApp(Context context, String pkg) {
        try {
            Intent intent = context.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
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
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed launching " + pkg, e);
        }
    }

    private void openSettings(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    private void openInputs(Context context) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("ACTION_OPEN_INPUTS", true);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
