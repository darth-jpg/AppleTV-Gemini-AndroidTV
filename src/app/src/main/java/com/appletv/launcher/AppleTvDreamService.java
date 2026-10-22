package com.appletv.launcher;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppleTvDreamService extends DreamService {
    private static final long AERIAL_INTERVAL_MS = 14000;

    private ImageView iv1;
    private ImageView iv2;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvClock;
    private TextView tvDate;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean useIv1 = true;
    private int currentSceneIndex = 0;
    private final List<AerialScene> scenes = new ArrayList<>();

    private static class AerialScene {
        final int drawableRes;
        final String title;
        final String subtitle;

        AerialScene(int drawableRes, String title, String subtitle) {
            this.drawableRes = drawableRes;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private final Runnable cycleRunnable = new Runnable() {
        @Override
        public void run() {
            nextScene();
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        setContentView(R.layout.layout_screensaver);

        iv1 = findViewById(R.id.iv_screensaver_1);
        iv2 = findViewById(R.id.iv_screensaver_2);
        tvTitle = findViewById(R.id.tv_screensaver_title);
        tvSubtitle = findViewById(R.id.tv_screensaver_subtitle);
        tvClock = findViewById(R.id.tv_screensaver_clock);
        tvDate = findViewById(R.id.tv_screensaver_date);

        try {
            Typeface regular = Typeface.createFromAsset(getAssets(), "fonts/Outfit-Regular.ttf");
            Typeface light = Typeface.createFromAsset(getAssets(), "fonts/Outfit-Light.ttf");
            if (tvTitle != null) tvTitle.setTypeface(regular);
            if (tvSubtitle != null) tvSubtitle.setTypeface(regular);
            if (tvClock != null) tvClock.setTypeface(light);
            if (tvDate != null) tvDate.setTypeface(regular);
        } catch (Exception ignored) {
        }

        scenes.clear();
        scenes.add(new AerialScene(R.drawable.screensaver_hawaii, "Costa Na Pali", "Kauai • Havai"));
        scenes.add(new AerialScene(R.drawable.screensaver_fjord, "Fjords Árticos", "Tromsø • Noruega"));
        scenes.add(new AerialScene(R.drawable.wallpaper_day, "Alpes Suíços", "Matterhorn • Zermatt"));
        scenes.add(new AerialScene(R.drawable.wallpaper_sunset, "Dolomitas", "Val di Funes • Itália"));
        scenes.add(new AerialScene(R.drawable.wallpaper_night, "Lago Eufórico", "Nova Zelândia"));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (scenes.isEmpty()) return;

        currentSceneIndex = 0;
        useIv1 = true;
        AerialScene scene = scenes.get(0);

        if (iv1 != null) {
            iv1.setImageResource(scene.drawableRes);
            iv1.setAlpha(1.0f);
            startKenBurns(iv1);
        }
        if (iv2 != null) {
            iv2.setAlpha(0.0f);
        }

        if (tvTitle != null) tvTitle.setText(scene.title);
        if (tvSubtitle != null) tvSubtitle.setText(scene.subtitle);
        updateClock();

        handler.postDelayed(cycleRunnable, AERIAL_INTERVAL_MS);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        handler.removeCallbacks(cycleRunnable);
        if (iv1 != null) iv1.animate().cancel();
        if (iv2 != null) iv2.animate().cancel();
    }

    private void startKenBurns(ImageView iv) {
        if (iv == null) return;
        iv.setScaleX(1.0f);
        iv.setScaleY(1.0f);
        iv.setTranslationX(0f);
        iv.setTranslationY(0f);
        iv.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .translationX(-18f)
            .translationY(-10f)
            .setDuration(AERIAL_INTERVAL_MS + 2000)
            .start();
    }

    private void nextScene() {
        if (scenes.isEmpty()) return;
        currentSceneIndex = (currentSceneIndex + 1) % scenes.size();
        final AerialScene scene = scenes.get(currentSceneIndex);

        final ImageView activeIv = useIv1 ? iv1 : iv2;
        final ImageView nextIv = useIv1 ? iv2 : iv1;
        useIv1 = !useIv1;

        if (nextIv != null) {
            nextIv.setImageResource(scene.drawableRes);
            nextIv.setAlpha(0.0f);
            startKenBurns(nextIv);
            nextIv.animate().alpha(1.0f).setDuration(2000).start();
        }
        if (activeIv != null) {
            activeIv.animate().alpha(0.0f).setDuration(2000).start();
        }

        if (tvTitle != null) {
            tvTitle.animate().alpha(0.0f).setDuration(400).withEndAction(new Runnable() {
                @Override
                public void run() {
                    tvTitle.setText(scene.title);
                    if (tvSubtitle != null) tvSubtitle.setText(scene.subtitle);
                    tvTitle.animate().alpha(1.0f).setDuration(600).start();
                }
            }).start();
        }

        updateClock();
        handler.postDelayed(cycleRunnable, AERIAL_INTERVAL_MS);
    }

    private void updateClock() {
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM", new Locale("pt", "PT"));
        if (tvClock != null) tvClock.setText(tf.format(new Date()));
        if (tvDate != null) tvDate.setText(df.format(new Date()).toUpperCase());
    }
}
