package com.appletv.launcher;

import android.view.animation.Interpolator;

/**
 * Apple tvOS Damped Spring Interpolator.
 * Pure math: zero allocations during animation, 60fps silky smooth organic bounce.
 * Exactly mimics Apple's tvOS mass-spring-damper physics for icon focus and scale.
 */
public class AppleSpringInterpolator implements Interpolator {
    private final float factor;

    public AppleSpringInterpolator() {
        this(0.42f);
    }

    public AppleSpringInterpolator(float factor) {
        this.factor = factor;
    }

    @Override
    public float getInterpolation(float input) {
        if (input <= 0f) return 0f;
        if (input >= 1f) return 1f;
        return (float) (Math.pow(2.0, -10.0 * input) * Math.sin((input - factor / 4.0) * (2.0 * Math.PI) / factor) + 1.0);
    }
}
