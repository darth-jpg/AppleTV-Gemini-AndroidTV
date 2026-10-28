package com.appletv.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver to ensure Apple TV Launcher starts cleanly when the TV powers on,
 * preventing factory TV interfaces or TV tuners from taking over after a cold boot.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "AppleTvBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "Boot event received: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            try {
                Intent launchIntent = new Intent(context, MainActivity.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch MainActivity on boot", e);
            }
        }
    }
}
