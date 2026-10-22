package com.appletv.launcher;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final String packageName;
    public final String activityName;
    public final String label;
    public final Drawable banner;
    public final int glowColor;

    public AppItem(String packageName, String activityName, String label, Drawable banner, int glowColor) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.banner = banner;
        this.glowColor = glowColor;
    }
}
