package com.appletv.launcher;

import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

public class TvInputHelper {
    private static final String TAG = "TvInputHelper";

    public static final String INPUT_HDMI_1 = "com.tcl.tvinput/.TvPassThroughService/HW15";
    public static final String INPUT_HDMI_2 = "com.tcl.tvinput/.TvPassThroughService/HW16";
    public static final String INPUT_HDMI_3 = "com.tcl.tvinput/.TvPassThroughService/HW17";
    public static final String INPUT_TUNER = "com.tcl.tvinput/.TunerInputService/HW2";
    public static final String INPUT_AV = "com.tcl.tvinput/.TvPassThroughService/HW3";

    public static void switchToInput(Context context, String inputId) {
        try {
            Uri uri;
            if (INPUT_TUNER.equals(inputId)) {
                Intent tclTvIntent = context.getPackageManager().getLaunchIntentForPackage("com.tcl.tv");
                if (tclTvIntent != null) {
                    context.startActivity(tclTvIntent);
                    return;
                }
                uri = TvContract.buildChannelsUriForInput(inputId);
            } else {
                uri = TvContract.buildChannelUriForPassthroughInput(inputId);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to switch input to " + inputId, e);
            try {
                Intent tclTvIntent = context.getPackageManager().getLaunchIntentForPackage("com.tcl.tv");
                if (tclTvIntent != null) {
                    context.startActivity(tclTvIntent);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
