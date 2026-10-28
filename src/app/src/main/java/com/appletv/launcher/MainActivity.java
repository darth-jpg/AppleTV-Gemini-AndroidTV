package com.appletv.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String TAG = "AppleTvLauncher";

    private static final String PREFS_NAME = "apple_tv_prefs";
    private static final String KEY_DOCK_PREFIX = "dock_slot_";
    private static final String KEY_DOCK_LIST = "dock_packages_list";
    private static final String KEY_GRID_LIST = "custom_grid_packages_list";
    private static final String KEY_HIDDEN_PACKAGES = "hidden_packages_set";
    private static final String KEY_HIDDEN_INPUTS = "hidden_inputs_set";

    // Reorder Mode ("Hold OK to Move")
    private boolean isReorderMode = false;
    private boolean isReorderingDock = false;
    private int reorderIndex = -1;
    private static final String KEY_SCREENSAVER_TIMEOUT = "screensaver_timeout_mins";
    private static final String KEY_SOUND_EFFECTS = "sound_effects_enabled";
    private static final String KEY_SOUND_MODE = "sound_mode_setting";

    // Navigation Sound Modes
    public static final int SOUND_MODE_OFF = 0;       // Desativado (Silencioso)
    public static final int SOUND_MODE_APPLE = 1;     // Som Apple TV (Personalizado)
    public static final int SOUND_MODE_SYSTEM = 2;    // Som do Sistema (TCL Padrão)

    private static final String[] DEFAULT_DOCK = new String[]{
        "org.smarttube.stable",
        "com.plexapp.android",
        "com.amazon.amazonvideo.livingroom",
        "com.wbd.stream"
    };

    private ImageView ivWallpaper;
    private ScrollView mainScroll;
    private TextView tvGreeting;
    private TextView tvClock;
    private TextView tvDate;
    private TextView tvWeather;
    private Button btnSleep;
    private Button btnInputs;
    private Button btnWifi;
    private Button btnSettings;
    private Button btnManageApps;
    private View viewAmbientGlow;
    private View viewGridDimmer;
    private TextView tvActiveDockTitle;
    private TextView tvActiveDockSubtitle;
    private LinearLayout dockContainer;
    private GridLayout appsGrid;
    private LinearLayout dropdownInputs;
    private Button btnRestoreInputs;

    // Siri Voice Assistant Views and Logic
    private Button btnSiri;
    private FrameLayout siriOverlayContainer;
    private LinearLayout siriCard;
    private ImageView siriOrb;
    private TextView tvSiriTitle;
    private TextView tvSiriQuery;
    private TextView tvSiriResponse;
    private TextView tvSiriBadge;
    private LinearLayout siriSuggestionsLayout;
    private Button btnSiriChip1, btnSiriChip2, btnSiriChip3, btnSiriChip4;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private ValueAnimator siriOrbAnimator;
    private final Handler siriDismissHandler = new Handler(Looper.getMainLooper());

    // Apple TV Aerial Screensaver Fields
    private static final long SCREENSAVER_TIMEOUT_MS = 180000; // 3 minutes
    private static final long AERIAL_TRANSITION_INTERVAL_MS = 14000; // 14 seconds per scene
    private FrameLayout screensaverOverlay;
    private ImageView ivScreensaver1;
    private ImageView ivScreensaver2;
    private TextView tvScreensaverTitle;
    private TextView tvScreensaverSubtitle;
    private TextView tvScreensaverClock;
    private TextView tvScreensaverDate;
    private TextView tvScreensaverHint;
    private boolean isScreensaverActive = false;
    private int currentAerialIndex = 0;
    private boolean useScreensaver1 = true;
    private final List<AerialScene> aerialScenes = new ArrayList<>();

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

    private final Runnable screensaverInactivityRunnable = new Runnable() {
        @Override
        public void run() {
            startScreensaver();
        }
    };

    private final Runnable screensaverCycleRunnable = new Runnable() {
        @Override
        public void run() {
            transitionToNextAerialScene();
        }
    };

    // Navigation Sound Effects (Apple TV Click vs System TCL vs Off)
    private SoundPool soundPool;
    private int navClickSoundId = 0;
    private int soundMode = SOUND_MODE_APPLE;

    private Typeface outfitRegular;
    private Typeface outfitLight;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    private final List<AppItem> dockItems = new ArrayList<>();
    private final List<AppItem> gridItems = new ArrayList<>();
    private final List<AppItem> allInstalledApps = new ArrayList<>();

    private int lastFocusedDockIndex = 0;
    private int currentWallpaperResId = 0;
    private long lastWeatherFetchTime = 0;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Package broadcast event: " + (intent != null ? intent.getAction() : "null"));
            reloadAll();
        }
    };

    private final BroadcastReceiver siriReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSiriOverlay();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSoundEffects();
        initViews();
        applySoundMode(soundMode);
        loadFonts();
        setupClockAndGreeting();
        setupPills();
        setupInputsDropdown();
        reloadAll();

        // Register package events receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        // Register Siri trigger receiver
        IntentFilter siriFilter = new IntentFilter("com.appletv.launcher.ACTION_TRIGGER_SIRI");
        registerReceiver(siriReceiver, siriFilter);

        Intent startIntent = getIntent();
        if (startIntent != null && (Intent.ACTION_ASSIST.equals(startIntent.getAction()) ||
                "android.intent.action.VOICE_ASSIST".equals(startIntent.getAction()) ||
                Intent.ACTION_SEARCH.equals(startIntent.getAction()) ||
                startIntent.getBooleanExtra("TRIGGER_SIRI", false))) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showSiriOverlay();
                }
            }, 400);
        }

        // Focus first dock item and ensure clean scroll at top by default
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mainScroll != null) mainScroll.scrollTo(0, 0);
                View firstDock = dockContainer != null && dockContainer.getChildCount() > 0 ? dockContainer.getChildAt(0) : null;
                if (firstDock != null) {
                    firstDock.requestFocus();
                }
            }
        }, 300);

        resetInactivityTimer();
    }

    private void initViews() {
        ivWallpaper = findViewById(R.id.iv_wallpaper);
        mainScroll = findViewById(R.id.main_scroll);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvClock = findViewById(R.id.tv_clock);
        tvDate = findViewById(R.id.tv_date);
        tvWeather = findViewById(R.id.tv_weather);
        btnSleep = findViewById(R.id.btn_sleep);
        btnInputs = findViewById(R.id.btn_inputs);
        btnWifi = findViewById(R.id.btn_wifi);
        btnSettings = findViewById(R.id.btn_settings);
        btnManageApps = findViewById(R.id.btn_manage_apps);
        if (btnManageApps != null) {
            btnManageApps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showManageAppsDialog();
                }
            });
        }
        viewAmbientGlow = findViewById(R.id.view_ambient_glow);
        viewGridDimmer = findViewById(R.id.view_grid_dimmer);
        tvActiveDockTitle = findViewById(R.id.tv_active_dock_title);
        tvActiveDockSubtitle = findViewById(R.id.tv_active_dock_subtitle);
        dockContainer = findViewById(R.id.dock_container);
        appsGrid = findViewById(R.id.apps_grid);
        dropdownInputs = findViewById(R.id.dropdown_inputs);
        btnRestoreInputs = findViewById(R.id.item_restore_inputs);

        // Screensaver views
        screensaverOverlay = findViewById(R.id.screensaver_overlay);
        ivScreensaver1 = findViewById(R.id.iv_screensaver_1);
        ivScreensaver2 = findViewById(R.id.iv_screensaver_2);
        tvScreensaverTitle = findViewById(R.id.tv_screensaver_title);
        tvScreensaverSubtitle = findViewById(R.id.tv_screensaver_subtitle);
        tvScreensaverClock = findViewById(R.id.tv_screensaver_clock);
        tvScreensaverDate = findViewById(R.id.tv_screensaver_date);
        tvScreensaverHint = findViewById(R.id.tv_screensaver_hint);

        // Siri Assistant Views
        siriOverlayContainer = findViewById(R.id.siri_overlay_container);
        siriCard = findViewById(R.id.siri_card);
        siriOrb = findViewById(R.id.siri_orb);
        tvSiriTitle = findViewById(R.id.tv_siri_title);
        tvSiriQuery = findViewById(R.id.tv_siri_query);
        tvSiriResponse = findViewById(R.id.tv_siri_response);
        tvSiriBadge = findViewById(R.id.tv_siri_badge);
        siriSuggestionsLayout = findViewById(R.id.siri_suggestions_layout);
        btnSiriChip1 = findViewById(R.id.btn_siri_chip1);
        btnSiriChip2 = findViewById(R.id.btn_siri_chip2);
        btnSiriChip3 = findViewById(R.id.btn_siri_chip3);
        btnSiriChip4 = findViewById(R.id.btn_siri_chip4);

        initSiriSuggestions();
        initSiriTts();

        initScreensaverScenes();
    }

    private void loadFonts() {
        try {
            outfitRegular = Typeface.createFromAsset(getAssets(), "fonts/Outfit-Regular.ttf");
            outfitLight = Typeface.createFromAsset(getAssets(), "fonts/Outfit-Light.ttf");

            tvGreeting.setTypeface(outfitRegular);
            tvClock.setTypeface(outfitLight);
            tvDate.setTypeface(outfitRegular);
            if (tvWeather != null) tvWeather.setTypeface(outfitRegular);
            tvActiveDockTitle.setTypeface(outfitRegular);
            if (tvActiveDockSubtitle != null) tvActiveDockSubtitle.setTypeface(outfitRegular);

            if (btnSiri != null) btnSiri.setTypeface(outfitRegular);
            if (btnSleep != null) btnSleep.setTypeface(outfitRegular);
            btnInputs.setTypeface(outfitRegular);
            btnWifi.setTypeface(outfitRegular);
            btnSettings.setTypeface(outfitRegular);
            if (btnManageApps != null) btnManageApps.setTypeface(outfitRegular);

            if (tvScreensaverTitle != null) tvScreensaverTitle.setTypeface(outfitRegular);
            if (tvScreensaverSubtitle != null) tvScreensaverSubtitle.setTypeface(outfitRegular);
            if (tvScreensaverClock != null) tvScreensaverClock.setTypeface(outfitLight);
            if (tvScreensaverDate != null) tvScreensaverDate.setTypeface(outfitRegular);
            if (tvScreensaverHint != null) tvScreensaverHint.setTypeface(outfitRegular);

            TextView moreApps = findViewById(R.id.tv_more_apps_title);
            if (moreApps != null) moreApps.setTypeface(outfitRegular);
        } catch (Exception e) {
            Log.e(TAG, "Error loading fonts", e);
        }
    }

    private void setupClockAndGreeting() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateClock();
                handler.postDelayed(this, 10000);
            }
        };
        updateClock();
        handler.post(clockRunnable);
    }

    private void updateClock() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            tvGreeting.setText(getString(R.string.greeting_morning).toUpperCase());
        } else if (hour >= 12 && hour < 20) {
            tvGreeting.setText(getString(R.string.greeting_afternoon).toUpperCase());
        } else {
            tvGreeting.setText(getString(R.string.greeting_evening).toUpperCase());
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvClock.setText(timeFormat.format(new Date()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM", new Locale("pt", "PT"));
        String dateStr = dateFormat.format(new Date()).toUpperCase();
        tvDate.setText(dateStr);

        updateDynamicWallpaper();
        updateSleepPillText();
        fetchWeatherAsync();
    }

    private void updateDynamicWallpaper() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        int resId;
        if (hour >= 8 && hour < 18) {
            resId = R.drawable.wallpaper_day;
        } else if (hour >= 18 && hour < 21) {
            resId = R.drawable.wallpaper_sunset;
        } else {
            resId = R.drawable.wallpaper_night;
        }

        if (currentWallpaperResId != resId) {
            currentWallpaperResId = resId;
            if (ivWallpaper != null) {
                ivWallpaper.setImageResource(resId);
            }
        }
    }

    private void fetchWeatherAsync() {
        fetchWeatherAsync(false);
    }

    private void fetchWeatherAsync(final boolean force) {
        // Show cached weather immediately
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cached = prefs.getString("cached_weather", null);
        if (cached != null && tvWeather != null) {
            tvWeather.setText(cached);
        }

        long now = System.currentTimeMillis();
        if (!force && (now - lastWeatherFetchTime < 20 * 60 * 1000L)) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String cityName = prefs.getString("weather_city_name", "LISBOA");
                    String latStr = prefs.getString("weather_lat", "38.7223");
                    String lonStr = prefs.getString("weather_lon", "-9.1393");
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + latStr + "&longitude=" + lonStr + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject root = new JSONObject(sb.toString());
                        JSONObject current = root.getJSONObject("current");
                        double temp = current.getDouble("temperature_2m");
                        double feelsLike = current.optDouble("apparent_temperature", temp);
                        int humidity = current.optInt("relative_humidity_2m", 0);
                        double wind = current.optDouble("wind_speed_10m", 0.0);
                        int code = current.getInt("weather_code");

                        double max = temp;
                        double min = temp;
                        if (root.has("daily")) {
                            JSONObject daily = root.getJSONObject("daily");
                            if (daily.has("temperature_2m_max") && daily.getJSONArray("temperature_2m_max").length() > 0) {
                                max = daily.getJSONArray("temperature_2m_max").getDouble(0);
                            }
                            if (daily.has("temperature_2m_min") && daily.getJSONArray("temperature_2m_min").length() > 0) {
                                min = daily.getJSONArray("temperature_2m_min").getDouble(0);
                            }
                        }

                        String icon = getWeatherIcon(code);
                        final String weatherText = String.format(Locale.getDefault(), "%s  %.0f°C  %s", icon, temp, cityName);

                        lastWeatherFetchTime = System.currentTimeMillis();
                        prefs.edit()
                            .putString("cached_weather", weatherText)
                            .putFloat("weather_temp", (float) temp)
                            .putFloat("weather_feels", (float) feelsLike)
                            .putInt("weather_humidity", humidity)
                            .putFloat("weather_wind", (float) wind)
                            .putInt("weather_code", code)
                            .putFloat("weather_max", (float) max)
                            .putFloat("weather_min", (float) min)
                            .apply();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (tvWeather != null) {
                                    tvWeather.setText(weatherText);
                                }
                            }
                        });
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Weather fetch error", e);
                }
            }
        }).start();
    }

    private String getWeatherDescription(int code) {
        switch (code) {
            case 0: return "Céu limpo";
            case 1: return "Predominantemente limpo";
            case 2: return "Parcialmente nublado";
            case 3: return "Nublado";
            case 45: case 48: return "Nevoeiro";
            case 51: case 53: case 55: return "Chuviscos";
            case 61: case 63: case 65: return "Chuva moderada";
            case 80: case 81: case 82: return "Aguaceiros";
            case 71: case 73: case 75: return "Neve";
            case 95: case 96: case 99: return "Trovoada";
            default: return "Tempo estável";
        }
    }

    private void showWeatherDetailsDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cityName = prefs.getString("weather_city_name", "LISBOA");
        float temp = prefs.getFloat("weather_temp", -99f);
        float feels = prefs.getFloat("weather_feels", -99f);
        int humidity = prefs.getInt("weather_humidity", -1);
        float wind = prefs.getFloat("weather_wind", -1f);
        int code = prefs.getInt("weather_code", 0);
        float max = prefs.getFloat("weather_max", -99f);
        float min = prefs.getFloat("weather_min", -99f);

        String icon = getWeatherIcon(code);
        String desc = getWeatherDescription(code);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("🌤️ Apple Weather · " + cityName);

        StringBuilder sb = new StringBuilder();
        if (temp != -99f) {
            sb.append(String.format(Locale.getDefault(), "%s %s (%.1fºC)\n\n", icon, desc, temp));
            if (feels != -99f) {
                sb.append(String.format(Locale.getDefault(), "🌡️ Sensação Térmica: %.1fºC\n", feels));
            }
            if (max != -99f && min != -99f) {
                sb.append(String.format(Locale.getDefault(), "📈 Máxima: %.0fºC  ·  📉 Mínima: %.0fºC\n", max, min));
            }
            if (humidity >= 0) {
                sb.append(String.format(Locale.getDefault(), "💧 Humidade do Ar: %d%%\n", humidity));
            }
            if (wind >= 0) {
                sb.append(String.format(Locale.getDefault(), "💨 Velocidade do Vento: %.1f km/h\n", wind));
            }
        } else {
            sb.append("A carregar previsões meteorológicas em tempo real...");
        }

        builder.setMessage(sb.toString().trim());

        builder.setPositiveButton("🔄 Atualizar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "A atualizar meteorologia...", Toast.LENGTH_SHORT).show();
                fetchWeatherAsync(true);
            }
        });

        builder.setNeutralButton("📍 Mudar Cidade", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showChangeCityDialog();
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showChangeCityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Selecionar Localização");

        final String[] cities = new String[]{
            "Lisboa", "Porto", "Braga", "Coimbra", "Faro",
            "Funchal (Madeira)", "Ponta Delgada (Açores)",
            "Madrid", "Paris", "Londres", "São Paulo"
        };
        final String[] lats = new String[]{
            "38.7223", "41.1579", "41.5454", "40.2033", "37.0194",
            "32.6669", "37.7412",
            "40.4168", "48.8566", "51.5074", "-23.5505"
        };
        final String[] lons = new String[]{
            "-9.1393", "-8.6291", "-8.4265", "-8.4103", "-7.9322",
            "-16.9241", "-25.6756",
            "-3.7038", "2.3522", "-0.1278", "-46.6333"
        };

        builder.setItems(cities, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < cities.length) {
                    String chosenCity = cities[which].split(" ")[0].toUpperCase();
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit()
                        .putString("weather_city_name", chosenCity)
                        .putString("weather_lat", lats[which])
                        .putString("weather_lon", lons[which])
                        .apply();
                    Toast.makeText(MainActivity.this, "Cidade alterada para " + cities[which], Toast.LENGTH_SHORT).show();
                    fetchWeatherAsync(true);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getWeatherIcon(int code) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour < 7 || hour >= 21);

        switch (code) {
            case 0:
                return isNight ? "🌙" : "☀️";
            case 1:
            case 2:
                return isNight ? "☁️" : "🌤️";
            case 3:
                return "☁️";
            case 45:
            case 48:
                return "🌫️";
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 65:
            case 80:
            case 81:
            case 82:
                return "🌧️";
            case 71:
            case 73:
            case 75:
            case 85:
            case 86:
                return "🌨️";
            case 95:
            case 96:
            case 99:
                return "⛈️";
            default:
                return isNight ? "🌙" : "🌤️";
        }
    }

    private void initScreensaverScenes() {
        aerialScenes.clear();
        aerialScenes.add(new AerialScene(R.drawable.screensaver_hawaii, "Costa Na Pali", "Kauai • Havai"));
        aerialScenes.add(new AerialScene(R.drawable.screensaver_fjord, "Fjords Árticos", "Tromsø • Noruega"));
        aerialScenes.add(new AerialScene(R.drawable.wallpaper_day, "Alpes Suíços", "Matterhorn • Zermatt"));
        aerialScenes.add(new AerialScene(R.drawable.wallpaper_sunset, "Dolomitas", "Val di Funes • Itália"));
        aerialScenes.add(new AerialScene(R.drawable.wallpaper_night, "Lago Eufórico", "Nova Zelândia"));
    }

    private void initSoundEffects() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.contains(KEY_SOUND_MODE)) {
            soundMode = prefs.getInt(KEY_SOUND_MODE, SOUND_MODE_APPLE);
        } else if (prefs.contains(KEY_SOUND_EFFECTS)) {
            soundMode = prefs.getBoolean(KEY_SOUND_EFFECTS, true) ? SOUND_MODE_APPLE : SOUND_MODE_OFF;
        } else {
            soundMode = SOUND_MODE_APPLE;
        }

        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();
            navClickSoundId = soundPool.load(this, R.raw.nav_click, 1);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SoundPool", e);
        }
    }

    private void applySoundMode(int mode) {
        this.soundMode = mode;
        boolean enableSystemFx = (mode == SOUND_MODE_SYSTEM);
        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().setSoundEffectsEnabled(enableSystemFx);
            applySoundEffectsRecursively(getWindow().getDecorView(), enableSystemFx);
        }
    }

    private void applySoundEffectsRecursively(View v, boolean enabled) {
        if (v == null) return;
        v.setSoundEffectsEnabled(enabled);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applySoundEffectsRecursively(vg.getChildAt(i), enabled);
            }
        }
    }

    private void setSoundMode(int mode) {
        this.soundMode = mode;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putInt(KEY_SOUND_MODE, mode)
            .putBoolean(KEY_SOUND_EFFECTS, mode != SOUND_MODE_OFF)
            .apply();
        applySoundMode(mode);
    }

    private void playNavSound() {
        if (soundMode != SOUND_MODE_APPLE || soundPool == null || navClickSoundId == 0) return;
        try {
            soundPool.play(navClickSoundId, 0.45f, 0.45f, 1, 0, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private void showSoundOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Efeitos Sonoros de Navegação");

        final String[] options = new String[]{
            "Som Apple TV (Personalizado)" + (soundMode == SOUND_MODE_APPLE ? "  ✓" : ""),
            "Som do Sistema (TCL Padrão)" + (soundMode == SOUND_MODE_SYSTEM ? "  ✓" : ""),
            "Desativado (Silencioso)" + (soundMode == SOUND_MODE_OFF ? "  ✓" : "")
        };

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int newMode;
                if (which == 0) {
                    newMode = SOUND_MODE_APPLE;
                } else if (which == 1) {
                    newMode = SOUND_MODE_SYSTEM;
                } else {
                    newMode = SOUND_MODE_OFF;
                }
                setSoundMode(newMode);
                String msg = (newMode == SOUND_MODE_APPLE) ? "Som Apple TV ativado" :
                             (newMode == SOUND_MODE_SYSTEM) ? "Som do Sistema TCL ativado" :
                             "Sons de navegação desativados";
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if (newMode == SOUND_MODE_APPLE) {
                    playNavSound();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null && dialog.getWindow().getDecorView() != null) {
            dialog.getWindow().getDecorView().setSoundEffectsEnabled(soundMode == SOUND_MODE_SYSTEM);
        }
        dialog.show();
    }

    private int getScreensaverTimeoutMinutes() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_SCREENSAVER_TIMEOUT, 3);
    }

    private void setScreensaverTimeoutMinutes(int minutes) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_SCREENSAVER_TIMEOUT, minutes).apply();
        resetInactivityTimer();
    }

    private void showScreensaverTimeoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Tempo de Espera do Screensaver");

        final int currentMins = getScreensaverTimeoutMinutes();
        final String[] options = new String[]{
            "1 minuto" + (currentMins == 1 ? "  ✓" : ""),
            "2 minutos" + (currentMins == 2 ? "  ✓" : ""),
            "3 minutos (Recomendado)" + (currentMins == 3 ? "  ✓" : ""),
            "5 minutos" + (currentMins == 5 ? "  ✓" : ""),
            "10 minutos" + (currentMins == 10 ? "  ✓" : ""),
            "Desativar Screensaver Automático" + (currentMins == 0 ? "  ✓" : "")
        };
        final int[] values = new int[]{1, 2, 3, 5, 10, 0};

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < values.length) {
                    int val = values[which];
                    setScreensaverTimeoutMinutes(val);
                    String msg = val > 0 ? "Screensaver definido para " + val + " minutos" : "Screensaver automático desativado";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void resetInactivityTimer() {
        handler.removeCallbacks(screensaverInactivityRunnable);
        int mins = getScreensaverTimeoutMinutes();
        if (!isScreensaverActive && mins > 0) {
            handler.postDelayed(screensaverInactivityRunnable, mins * 60 * 1000L);
        }
    }

    private void startScreensaver() {
        if (isScreensaverActive || screensaverOverlay == null) return;
        isScreensaverActive = true;

        if (aerialScenes.isEmpty()) {
            initScreensaverScenes();
        }

        currentAerialIndex = 0;
        AerialScene scene = aerialScenes.get(0);
        useScreensaver1 = true;
        ivScreensaver1.setImageResource(scene.drawableRes);
        ivScreensaver1.setAlpha(1.0f);
        ivScreensaver1.setScaleX(1.0f);
        ivScreensaver1.setScaleY(1.0f);
        ivScreensaver1.setTranslationX(0f);
        ivScreensaver1.setTranslationY(0f);

        ivScreensaver2.setAlpha(0.0f);

        tvScreensaverTitle.setText(scene.title);
        tvScreensaverSubtitle.setText(scene.subtitle);
        updateScreensaverClock();

        screensaverOverlay.setAlpha(0.0f);
        screensaverOverlay.setVisibility(View.VISIBLE);
        screensaverOverlay.animate().alpha(1.0f).setDuration(1000).withEndAction(new Runnable() {
            @Override
            public void run() {
                startKenBurns(ivScreensaver1);
                handler.postDelayed(screensaverCycleRunnable, AERIAL_TRANSITION_INTERVAL_MS);
            }
        }).start();
    }

    private void stopScreensaver() {
        if (!isScreensaverActive || screensaverOverlay == null) return;
        isScreensaverActive = false;

        handler.removeCallbacks(screensaverCycleRunnable);

        ivScreensaver1.animate().cancel();
        ivScreensaver2.animate().cancel();

        screensaverOverlay.animate().alpha(0.0f).setDuration(400).withEndAction(new Runnable() {
            @Override
            public void run() {
                screensaverOverlay.setVisibility(View.GONE);
                View dockView = findViewById(500 + lastFocusedDockIndex);
                if (dockView == null && dockContainer != null && dockContainer.getChildCount() > 0) {
                    dockView = dockContainer.getChildAt(0);
                }
                if (dockView != null) dockView.requestFocus();
            }
        }).start();

        resetInactivityTimer();
    }

    private void startKenBurns(ImageView iv) {
        iv.setScaleX(1.0f);
        iv.setScaleY(1.0f);
        iv.setTranslationX(0f);
        iv.setTranslationY(0f);
        iv.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .translationX(-18f)
            .translationY(-10f)
            .setDuration(AERIAL_TRANSITION_INTERVAL_MS + 2000)
            .start();
    }

    private void transitionToNextAerialScene() {
        if (!isScreensaverActive || aerialScenes.isEmpty()) return;

        currentAerialIndex = (currentAerialIndex + 1) % aerialScenes.size();
        final AerialScene nextScene = aerialScenes.get(currentAerialIndex);

        final ImageView activeIv = useScreensaver1 ? ivScreensaver1 : ivScreensaver2;
        final ImageView nextIv = useScreensaver1 ? ivScreensaver2 : ivScreensaver1;
        useScreensaver1 = !useScreensaver1;

        nextIv.setImageResource(nextScene.drawableRes);
        nextIv.setAlpha(0.0f);
        startKenBurns(nextIv);

        nextIv.animate().alpha(1.0f).setDuration(2000).start();
        activeIv.animate().alpha(0.0f).setDuration(2000).start();

        tvScreensaverTitle.animate().alpha(0.0f).setDuration(400).withEndAction(new Runnable() {
            @Override
            public void run() {
                tvScreensaverTitle.setText(nextScene.title);
                tvScreensaverSubtitle.setText(nextScene.subtitle);
                tvScreensaverTitle.animate().alpha(1.0f).setDuration(600).start();
            }
        }).start();

        updateScreensaverClock();
        handler.postDelayed(screensaverCycleRunnable, AERIAL_TRANSITION_INTERVAL_MS);
    }

    private void updateScreensaverClock() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE d MMMM", new Locale("pt", "PT"));
        if (tvScreensaverClock != null) tvScreensaverClock.setText(timeFormat.format(new Date()));
        if (tvScreensaverDate != null) tvScreensaverDate.setText(dateFormat.format(new Date()).toUpperCase());
    }

    private void updateSleepPillText() {
        if (btnSleep == null) return;
        long remaining = RemoteKeyService.getRemainingSleepMinutes();
        if (remaining > 0) {
            btnSleep.setText(remaining + "m");
        } else {
            btnSleep.setText("Desligar");
        }
    }

    private void showSleepTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Temporizador de Desligar");

        long remaining = RemoteKeyService.getRemainingSleepMinutes();
        String cancelOption = remaining > 0 ? "Desativar Temporizador (restam " + remaining + "m)" : "Desativar Temporizador";

        final String[] options = new String[]{
            "Desligar em 15 minutos",
            "Desligar em 30 minutos",
            "Desligar em 45 minutos",
            "Desligar em 60 minutos",
            "Desligar em 90 minutos",
            cancelOption
        };

        final int[] minutes = new int[]{15, 30, 45, 60, 90, 0};

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int mins = minutes[which];
                RemoteKeyService.setSleepTimer(mins);
                updateSleepPillText();
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupPills() {
        // Weather Interactive Pill
        if (tvWeather != null) {
            tvWeather.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showWeatherDetailsDialog();
                }
            });
            tvWeather.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        playNavSound();
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start();
                        if (viewGridDimmer != null) {
                            viewGridDimmer.animate().alpha(0.0f).setDuration(250).start();
                        }
                    } else {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    }
                }
            });
        }

        // Sleep Timer Pill
        if (btnSleep != null) {
            updateSleepPillText();
            btnSleep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSleepTimerDialog();
                }
            });
        }

        // Wi-Fi Pill
        updateWifiStatus();
        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } catch (Exception e) {
                    openSettingsFallback();
                }
            }
        });

        // Settings Pill
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsControlMenu();
            }
        });

        // Inputs Pill
        btnInputs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleInputsDropdown();
            }
        });

        View.OnFocusChangeListener soundFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    playNavSound();
                    if (viewGridDimmer != null) {
                        if (v == btnManageApps) {
                            viewGridDimmer.animate().alpha(0.85f).setDuration(250).start();
                        } else {
                            viewGridDimmer.animate().alpha(0.0f).setDuration(250).start();
                        }
                    }
                }
            }
        };
        if (btnSiri != null) btnSiri.setOnFocusChangeListener(soundFocusListener);
        if (btnSleep != null) btnSleep.setOnFocusChangeListener(soundFocusListener);
        btnInputs.setOnFocusChangeListener(soundFocusListener);
        btnWifi.setOnFocusChangeListener(soundFocusListener);
        btnSettings.setOnFocusChangeListener(soundFocusListener);
        if (btnManageApps != null) btnManageApps.setOnFocusChangeListener(soundFocusListener);
    }

    private void updateWifiStatus() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                WifiInfo info = wifi.getConnectionInfo();
                if (info != null && info.getSSID() != null) {
                    String ssid = info.getSSID().replace("\"", "");
                    if (!ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                        btnWifi.setText(ssid);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        btnWifi.setText("Wi-Fi");
    }

    private void openBluetoothSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new android.content.ComponentName("com.android.tv.settings", "com.android.tv.settings.connectivity.BluetoothSettingsActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e2) {
                try {
                    Intent intent = new Intent("android.settings.BLUETOOTH_SETTINGS");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e3) {
                    openSettingsFallback();
                }
            }
        }
    }

    private void openSettingsFallback() {
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

    private void showSettingsControlMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Centro de Controlo & Definições");

        final List<String> options = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        options.add("⚙️ Definições do Sistema Android TV");
        actions.add(new Runnable() {
            @Override
            public void run() {
                openSettingsFallback();
            }
        });

        options.add("⏱️ Temporizador de Desligar (Sleep Timer)");
        actions.add(new Runnable() {
            @Override
            public void run() {
                showSleepTimerDialog();
            }
        });

        options.add("🖼️ Protetor de Ecrã Apple TV Aerials");
        actions.add(new Runnable() {
            @Override
            public void run() {
                startScreensaver();
            }
        });

        options.add("🎧 Auscultadores & Bluetooth (Comandos / Áudio)");
        actions.add(new Runnable() {
            @Override
            public void run() {
                openBluetoothSettings();
            }
        });

        options.add("👁️ Gerir Aplicações Ocultadas (" + getHiddenPackages().size() + ")");
        actions.add(new Runnable() {
            @Override
            public void run() {
                showHiddenAppsDialog();
            }
        });

        options.add("🔊 Efeitos Sonoros de Navegação");
        actions.add(new Runnable() {
            @Override
            public void run() {
                showSoundOptionsDialog();
            }
        });

        options.add("🔄 Reiniciar Televisão");
        actions.add(new Runnable() {
            @Override
            public void run() {
                confirmRestartTv();
            }
        });

        builder.setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < actions.size()) {
                    actions.get(which).run();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void confirmRestartTv() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Reiniciar Televisão");
        builder.setMessage("Tem a certeza que deseja reiniciar o sistema agora?");
        builder.setPositiveButton("Reiniciar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartDevice();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void restartDevice() {
        if (RemoteKeyService.instance != null) {
            try {
                boolean success = RemoteKeyService.instance.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
                if (success) return;
            } catch (Exception ignored) {
            }
        }
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                pm.reboot(null);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            Intent intent = new Intent("android.intent.action.REBOOT");
            intent.putExtra("nowait", 1);
            intent.putExtra("interval", 1);
            intent.putExtra("window", 0);
            sendBroadcast(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível reiniciar automaticamente. Use o comando da TV.", Toast.LENGTH_SHORT).show();
        }
    }

    private static final String KEY_INPUT_NAME_PREFIX = "input_name_";

    private String getInputDisplayName(String key, String fallback) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_INPUT_NAME_PREFIX + key, fallback);
    }

    private void saveInputDisplayName(String key, String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_INPUT_NAME_PREFIX + key, name).apply();
    }

    private void updateInputLabels() {
        Button btn1 = findViewById(R.id.item_input_hdmi1);
        if (btn1 != null) btn1.setText(getInputDisplayName("hdmi1", "HDMI 1"));
        Button btn2 = findViewById(R.id.item_input_hdmi2);
        if (btn2 != null) btn2.setText(getInputDisplayName("hdmi2", "HDMI 2"));
        Button btn3 = findViewById(R.id.item_input_hdmi3);
        if (btn3 != null) btn3.setText(getInputDisplayName("hdmi3", "HDMI 3"));
    }

    private void showRenameInputDialog(final String key, final String fallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Personalizar Entrada (" + fallback + ")");

        final String[] presets = new String[]{
            "🎮 PlayStation 5",
            "🎮 Xbox Series X/S",
            "🎮 Nintendo Switch",
            "💻 PC Gamer / Steam",
            "🍏 Apple TV 4K",
            "📺 Box TV / Operadora",
            "🔄 Repor Nome de Fábrica (" + fallback + ")"
        };

        builder.setItems(presets, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == presets.length - 1) {
                    saveInputDisplayName(key, fallback);
                } else {
                    saveInputDisplayName(key, presets[which]);
                }
                updateInputLabels();
                Toast.makeText(MainActivity.this, "Nome da entrada atualizado", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupInputsDropdown() {
        View.OnFocusChangeListener soundFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    playNavSound();
                }
            }
        };

        int[] inputIds = new int[]{
            R.id.item_input_hdmi1,
            R.id.item_input_hdmi2,
            R.id.item_input_hdmi3,
            R.id.item_input_tv,
            R.id.item_input_av
        };
        for (int id : inputIds) {
            View view = findViewById(id);
            if (view != null) {
                view.setOnFocusChangeListener(soundFocusListener);
            }
        }

        updateInputLabels();

        final View.OnLongClickListener renameListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int id = v.getId();
                if (id == R.id.item_input_hdmi1) showRenameInputDialog("hdmi1", "HDMI 1");
                else if (id == R.id.item_input_hdmi2) showRenameInputDialog("hdmi2", "HDMI 2");
                else if (id == R.id.item_input_hdmi3) showRenameInputDialog("hdmi3", "HDMI 3");
                return true;
            }
        };

        findViewById(R.id.item_input_hdmi1).setOnLongClickListener(renameListener);
        findViewById(R.id.item_input_hdmi2).setOnLongClickListener(renameListener);
        findViewById(R.id.item_input_hdmi3).setOnLongClickListener(renameListener);

        findViewById(R.id.item_input_hdmi1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvInputHelper.switchToInput(MainActivity.this, TvInputHelper.INPUT_HDMI_1);
                hideInputsDropdown();
            }
        });
        findViewById(R.id.item_input_hdmi2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvInputHelper.switchToInput(MainActivity.this, TvInputHelper.INPUT_HDMI_2);
                hideInputsDropdown();
            }
        });
        findViewById(R.id.item_input_hdmi3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvInputHelper.switchToInput(MainActivity.this, TvInputHelper.INPUT_HDMI_3);
                hideInputsDropdown();
            }
        });
        findViewById(R.id.item_input_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvInputHelper.switchToInput(MainActivity.this, TvInputHelper.INPUT_TUNER);
                hideInputsDropdown();
            }
        });
        findViewById(R.id.item_input_av).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TvInputHelper.switchToInput(MainActivity.this, TvInputHelper.INPUT_AV);
                hideInputsDropdown();
            }
        });
    }

    private void toggleInputsDropdown() {
        if (dropdownInputs.getVisibility() == View.VISIBLE) {
            hideInputsDropdown();
        } else {
            updateInputLabels();
            dropdownInputs.setVisibility(View.VISIBLE);
            View first = findViewById(R.id.item_input_hdmi1);
            if (first != null) first.requestFocus();
        }
    }

    private void hideInputsDropdown() {
        dropdownInputs.setVisibility(View.GONE);
        btnInputs.requestFocus();
    }

    private List<String> getDockPackageList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        List<String> list = new ArrayList<>();
        String saved = prefs.getString(KEY_DOCK_LIST, null);
        if (saved != null && !saved.trim().isEmpty()) {
            String[] parts = saved.split(",");
            for (String p : parts) {
                p = p.trim();
                if (!p.isEmpty() && !list.contains(p)) {
                    list.add(p);
                }
            }
        }
        if (list.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                String pkg = prefs.getString(KEY_DOCK_PREFIX + i, DEFAULT_DOCK[i]);
                if (pkg != null && !pkg.trim().isEmpty() && !list.contains(pkg)) {
                    list.add(pkg);
                }
            }
        }
        if (list.isEmpty()) {
            Collections.addAll(list, DEFAULT_DOCK);
        }
        return list;
    }

    private void saveDockPackageList(List<String> list) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
            if (i < 4) {
                prefs.edit().putString(KEY_DOCK_PREFIX + i, list.get(i)).apply();
            }
        }
        prefs.edit().putString(KEY_DOCK_LIST, sb.toString()).apply();
    }

    private void addDockPackage(String pkg) {
        List<String> list = getDockPackageList();
        if (!list.contains(pkg)) {
            if (list.size() >= 6) {
                Toast.makeText(this, "A Dock permite no máximo 6 aplicações", Toast.LENGTH_SHORT).show();
                return;
            }
            list.add(pkg);
            saveDockPackageList(list);
            reloadAll();
        }
    }

    private void removeDockPackage(int index) {
        List<String> list = getDockPackageList();
        if (index >= 0 && index < list.size()) {
            if (list.size() <= 1) {
                Toast.makeText(this, "A Dock deve manter pelo menos 1 aplicação", Toast.LENGTH_SHORT).show();
                return;
            }
            list.remove(index);
            saveDockPackageList(list);
            if (lastFocusedDockIndex >= list.size()) {
                lastFocusedDockIndex = Math.max(0, list.size() - 1);
            }
            reloadAll();
        }
    }

    private void moveDockPackage(int fromIndex, int toIndex) {
        List<String> list = getDockPackageList();
        if (fromIndex >= 0 && fromIndex < list.size() && toIndex >= 0 && toIndex < list.size()) {
            String item = list.remove(fromIndex);
            list.add(toIndex, item);
            saveDockPackageList(list);
            lastFocusedDockIndex = toIndex;
            reloadAll();
        }
    }

    private void replaceDockPackage(int slotIndex, String newPackage) {
        List<String> list = getDockPackageList();
        if (slotIndex >= 0 && slotIndex < list.size() && newPackage != null) {
            int existing = list.indexOf(newPackage);
            if (existing != -1 && existing != slotIndex) {
                list.set(existing, list.get(slotIndex));
            }
            list.set(slotIndex, newPackage);
            saveDockPackageList(list);
            reloadAll();
        }
    }

    private Set<String> getHiddenPackages() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_HIDDEN_PACKAGES, null);
        return set != null ? new HashSet<>(set) : new HashSet<String>();
    }

    private void hidePackage(String pkg) {
        Set<String> hidden = getHiddenPackages();
        hidden.add(pkg);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putStringSet(KEY_HIDDEN_PACKAGES, hidden).apply();
        reloadAll();
    }

    private void unhidePackage(String pkg) {
        Set<String> hidden = getHiddenPackages();
        hidden.remove(pkg);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putStringSet(KEY_HIDDEN_PACKAGES, hidden).apply();
        reloadAll();
    }

    private void openAppDetails(String pkg) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + pkg));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open app details for " + pkg, e);
        }
    }

    private void uninstallApp(String pkg) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + pkg));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot uninstall " + pkg, e);
        }
    }

    private String[] getDockPackages() {
        List<String> list = getDockPackageList();
        return list.toArray(new String[0]);
    }

    private void setDockPackage(int slotIndex, String newPackage) {
        replaceDockPackage(slotIndex, newPackage);
    }

    private Drawable getCustomAppBanner(String packageName) {
        if (packageName == null) return null;
        try {
            switch (packageName) {
                case "com.limelight":
                    return getDrawable(R.drawable.banner_moonlight);
                case "org.smarttube.stable":
                    return getDrawable(R.drawable.banner_smarttube);
                case "com.netflix.ninja":
                    return getDrawable(R.drawable.banner_netflix);
                case "com.plexapp.android":
                    return getDrawable(R.drawable.banner_plex);
                case "com.amazon.amazonvideo.livingroom":
                    return getDrawable(R.drawable.banner_prime);
                case "com.wbd.stream":
                    return getDrawable(R.drawable.banner_max);
                case "com.android.vending":
                    return getDrawable(R.drawable.banner_playstore);
                case "com.google.android.youtube.tv":
                    return getDrawable(R.drawable.banner_youtube);
                case "com.stremio.one":
                    return getDrawable(R.drawable.banner_stremio);
                case "com.spotify.tv.android":
                case "com.spotify.music":
                    return getDrawable(R.drawable.banner_spotify);
                default:
                    return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getAppGlowColor(String packageName) {
        if (packageName == null) return Color.parseColor("#3B82F6");
        switch (packageName) {
            case "org.smarttube.stable":
            case "com.google.android.youtube.tv":
            case "com.netflix.ninja":
                return Color.parseColor("#E50914");
            case "com.plexapp.android":
                return Color.parseColor("#E5A00D");
            case "com.amazon.amazonvideo.livingroom":
                return Color.parseColor("#00A8E1");
            case "com.wbd.stream":
                return Color.parseColor("#002BE7");
            case "com.disney.disneyplus":
                return Color.parseColor("#113CCF");
            case "com.spotify.tv.android":
            case "com.spotify.music":
                return Color.parseColor("#1DB954");
            case "com.android.vending":
                return Color.parseColor("#00875A");
            case "com.limelight":
                return Color.parseColor("#008080");
            case "org.xbmc.kodi":
                return Color.parseColor("#17B2E7");
            case "com.stremio.one":
                return Color.parseColor("#8B5CF6");
            case "com.apple.atve.androidtv.appletv":
                return Color.parseColor("#FFFFFF");
            default:
                return Color.parseColor("#2563EB");
        }
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        dockItems.clear();
        gridItems.clear();
        allInstalledApps.clear();

        Map<String, ResolveInfo> appMap = new HashMap<>();

        // 1. Gather all Leanback apps
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> lbActivities = pm.queryIntentActivities(leanbackIntent, 0);
        if (lbActivities != null) {
            for (ResolveInfo ri : lbActivities) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    appMap.put(ri.activityInfo.packageName, ri);
                }
            }
        }

        // 2. Gather all standard launcher apps (sideloaded / utilities)
        Intent standardIntent = new Intent(Intent.ACTION_MAIN);
        standardIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> stdActivities = pm.queryIntentActivities(standardIntent, 0);
        if (stdActivities != null) {
            for (ResolveInfo ri : stdActivities) {
                if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                    if (!appMap.containsKey(ri.activityInfo.packageName)) {
                        appMap.put(ri.activityInfo.packageName, ri);
                    }
                }
            }
        }

        // System bloatware blacklist
        Set<String> blacklistedPackages = new HashSet<>();
        blacklistedPackages.add("com.android.tv.settings");
        blacklistedPackages.add("com.tcl.tv");
        blacklistedPackages.add("com.tcl.dashboard");
        blacklistedPackages.add("com.tcl.tv.tclhome_passive");
        blacklistedPackages.add("com.tcl.ui_mediaCenter");
        blacklistedPackages.add("com.tcl.gallery");
        blacklistedPackages.add("com.tcl.browser");
        blacklistedPackages.add("com.tcl.miracast");
        blacklistedPackages.add("com.google.android.play.games");
        blacklistedPackages.add("com.tcl.partnercustomizer");
        blacklistedPackages.add("com.tcl.guard");
        blacklistedPackages.add(getPackageName()); // Launcher itself

        for (Map.Entry<String, ResolveInfo> entry : appMap.entrySet()) {
            String pkg = entry.getKey();
            if (!blacklistedPackages.contains(pkg)) {
                ResolveInfo ri = entry.getValue();
                String label = ri.loadLabel(pm).toString();

                Drawable banner = getCustomAppBanner(pkg);
                if (banner == null) {
                    banner = ri.activityInfo.loadBanner(pm);
                }
                if (banner == null) {
                    banner = ri.activityInfo.loadIcon(pm);
                }
                allInstalledApps.add(new AppItem(pkg, ri.activityInfo.name, label, banner, getAppGlowColor(pkg)));
            }
        }

        // Sort all installed apps alphabetically by label
        Collections.sort(allInstalledApps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });

        // 3. Populate Dock items from SharedPreferences
        List<String> dockPkgs = getDockPackageList();
        Set<String> dockSet = new HashSet<>();
        for (String pkg : dockPkgs) {
            dockSet.add(pkg);
            dockItems.add(createAppItem(pm, pkg));
        }

        // 4. Populate Grid items from all installed apps not in dock and not hidden
        Set<String> hiddenSet = getHiddenPackages();
        List<AppItem> unplaced = new ArrayList<>();
        for (AppItem app : allInstalledApps) {
            if (!dockSet.contains(app.packageName) && !hiddenSet.contains(app.packageName)) {
                unplaced.add(app);
            }
        }

        // Support persistent custom grid ordering
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedGrid = prefs.getString(KEY_GRID_LIST, null);
        if (savedGrid != null && !savedGrid.trim().isEmpty()) {
            String[] savedOrder = savedGrid.split(",");
            Map<String, AppItem> unplacedMap = new HashMap<>();
            for (AppItem item : unplaced) {
                unplacedMap.put(item.packageName, item);
            }
            for (String pkg : savedOrder) {
                AppItem itm = unplacedMap.remove(pkg);
                if (itm != null) {
                    gridItems.add(itm);
                }
            }
            // Append any new apps installed after saved order
            for (AppItem item : unplaced) {
                if (unplacedMap.containsKey(item.packageName)) {
                    gridItems.add(item);
                }
            }
        } else {
            gridItems.addAll(unplaced);
        }
    }

    private AppItem createAppItem(PackageManager pm, String packageName) {
        for (AppItem item : allInstalledApps) {
            if (item.packageName.equals(packageName)) {
                return item;
            }
        }
        String label = packageName;
        Drawable banner = getCustomAppBanner(packageName);
        try {
            label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
            if (banner == null) {
                banner = pm.getApplicationBanner(packageName);
            }
            if (banner == null) {
                banner = pm.getApplicationIcon(packageName);
            }
        } catch (Exception ignored) {
        }
        return new AppItem(packageName, null, label, banner, getAppGlowColor(packageName));
    }

    private void reloadAll() {
        loadApps();
        setupDock();
        setupGrid();
        if (!dockItems.isEmpty()) {
            if (lastFocusedDockIndex >= dockItems.size()) lastFocusedDockIndex = 0;
            updateDockTitleAndGlow(dockItems.get(lastFocusedDockIndex));
        }
    }

    private void checkAndRefreshAppsIfChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    PackageManager pm = getPackageManager();
                    Intent lb = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    List<ResolveInfo> lbList = pm.queryIntentActivities(lb, 0);
                    Intent std = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> stdList = pm.queryIntentActivities(std, 0);

                    Set<String> currentPkgs = new HashSet<>();
                    if (lbList != null) {
                        for (ResolveInfo ri : lbList) {
                            if (ri.activityInfo != null) currentPkgs.add(ri.activityInfo.packageName);
                        }
                    }
                    if (stdList != null) {
                        for (ResolveInfo ri : stdList) {
                            if (ri.activityInfo != null) currentPkgs.add(ri.activityInfo.packageName);
                        }
                    }

                    Set<String> knownPkgs = new HashSet<>();
                    for (AppItem item : allInstalledApps) {
                        knownPkgs.add(item.packageName);
                    }

                    if (!currentPkgs.equals(knownPkgs)) {
                        Log.i(TAG, "Installed packages changed, reloading UI...");
                        reloadAll();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking apps changed", e);
                }
            }
        });
    }

    private void setupDock() {
        dockContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int[] topPillIds = new int[]{R.id.btn_sleep, R.id.btn_inputs, R.id.btn_wifi, R.id.btn_settings};

        for (int i = 0; i < dockItems.size(); i++) {
            final int index = i;
            final AppItem item = dockItems.get(i);
            View dockTile = inflater.inflate(R.layout.item_dock_tile, dockContainer, false);
            final FrameLayout frame = dockTile.findViewById(R.id.dock_item_frame);
            ImageView img = dockTile.findViewById(R.id.dock_item_img);

            frame.setId(500 + i);
            frame.setClipToOutline(true);
            img.setClipToOutline(true);

            if (item.banner != null) {
                img.setImageDrawable(item.banner);
                int w = item.banner.getIntrinsicWidth();
                int h = item.banner.getIntrinsicHeight();
                if (w > 0 && h > 0 && ((float) w / (float) h) < 1.35f) {
                    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    int pad = (int) (8 * getResources().getDisplayMetrics().density);
                    img.setPadding(pad, pad, pad, pad);
                } else {
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setPadding(0, 0, 0, 0);
                }
            } else {
                img.setImageResource(android.R.drawable.sym_def_app_icon);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int pad = (int) (10 * getResources().getDisplayMetrics().density);
                img.setPadding(pad, pad, pad, pad);
            }

            int pillIndex = Math.min(i, topPillIds.length - 1);
            frame.setNextFocusUpId(topPillIds[pillIndex]);

            // Connect top pills down to dock items
            if (i == 0 && btnSleep != null) btnSleep.setNextFocusDownId(500);
            if (i == 1 && btnInputs != null) btnInputs.setNextFocusDownId(501);
            if (i == 2 && btnWifi != null) btnWifi.setNextFocusDownId(502);
            if (i == 3 && btnSettings != null) btnSettings.setNextFocusDownId(503);

            // Link down to corresponding grid item if available
            if (!gridItems.isEmpty()) {
                int targetGridIndex = Math.min(i, gridItems.size() - 1);
                frame.setNextFocusDownId(1000 + targetGridIndex);
            } else {
                frame.setNextFocusDownId(R.id.btn_manage_apps);
            }

            frame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        playNavSound();
                        lastFocusedDockIndex = index;
                        frame.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180).start();
                        updateDockTitleAndGlow(item);
                        // Return completely to clean 100% scenic landscape at (0, 0)
                        mainScroll.smoothScrollTo(0, 0);
                        if (viewGridDimmer != null) {
                            viewGridDimmer.animate().alpha(0.0f).setDuration(250).start();
                        }
                    } else {
                        frame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                }
            });

            frame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPackage(item.packageName);
                }
            });

            // Long press OK to customize this dock slot
            frame.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDockItemOptionsDialog(index, item);
                    return true;
                }
            });

            dockContainer.addView(dockTile);
        }

        // Add "+" button if dock has less than 6 items
        if (dockItems.size() < 6) {
            View addTile = inflater.inflate(R.layout.item_dock_add, dockContainer, false);
            final FrameLayout addFrame = addTile.findViewById(R.id.dock_add_frame);
            addFrame.setId(R.id.dock_item_add);
            addFrame.setClipToOutline(true);
            addFrame.setNextFocusUpId(R.id.btn_settings);
            addFrame.setNextFocusDownId(R.id.btn_manage_apps);

            addFrame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        playNavSound();
                        addFrame.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180).start();
                        tvActiveDockTitle.setText("Adicionar à Dock");
                        if (tvActiveDockSubtitle != null) {
                            tvActiveDockSubtitle.setText("PERSONALIZAR ATALHOS");
                        }

                        // Gentle soft ambient glow for add button
                        GradientDrawable glow = new GradientDrawable();
                        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
                        glow.setGradientRadius(1190f);
                        glow.setGradientCenter(0.32f, 0.78f);
                        glow.setColors(new int[]{
                            Color.argb(20, 255, 255, 255),
                            Color.argb(6, 255, 255, 255),
                            Color.TRANSPARENT
                        });
                        viewAmbientGlow.setBackground(glow);
                        mainScroll.smoothScrollTo(0, 0);
                        if (viewGridDimmer != null) {
                            viewGridDimmer.animate().alpha(0.0f).setDuration(250).start();
                        }
                    } else {
                        addFrame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                }
            });

            addFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddAppToDockDialog();
                }
            });

            dockContainer.addView(addTile);
        }

        // Initialize glow with currently selected or first item
        if (!dockItems.isEmpty()) {
            if (lastFocusedDockIndex >= dockItems.size()) lastFocusedDockIndex = 0;
            updateDockTitleAndGlow(dockItems.get(lastFocusedDockIndex));
        }
        applySoundMode(soundMode);
    }

    private void startReorderMode(boolean inDock, int index) {
        isReorderMode = true;
        isReorderingDock = inDock;
        reorderIndex = index;
        playNavSound();
        Toast.makeText(this, "🖐️ Mover Aplicação: Usa as setas do comando • Pressiona OK ou Voltar para fixar", Toast.LENGTH_LONG).show();
        updateReorderVisuals();
    }

    private void stopReorderMode() {
        if (!isReorderMode) return;
        isReorderMode = false;
        playNavSound();
        Toast.makeText(this, "✓ Posição guardada", Toast.LENGTH_SHORT).show();

        if (isReorderingDock) {
            saveDockPackageList(getDockPackageList());
            reloadAll();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    View v = findViewById(500 + reorderIndex);
                    if (v != null) v.requestFocus();
                }
            }, 80);
        } else {
            saveGridPackageList();
            reloadAll();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    View v = findViewById(1000 + reorderIndex);
                    if (v != null) v.requestFocus();
                }
            }, 80);
        }
    }

    private void moveReorderDock(int delta) {
        int newIndex = reorderIndex + delta;
        List<String> list = getDockPackageList();
        if (newIndex >= 0 && newIndex < list.size() && newIndex < dockItems.size()) {
            String pkg = list.remove(reorderIndex);
            list.add(newIndex, pkg);
            saveDockPackageList(list);

            AppItem item = dockItems.remove(reorderIndex);
            dockItems.add(newIndex, item);

            reorderIndex = newIndex;
            lastFocusedDockIndex = newIndex;
            playNavSound();
            setupDock();
            updateReorderVisuals();
        }
    }

    private void moveReorderGrid(int delta) {
        int newIndex = reorderIndex + delta;
        if (newIndex >= 0 && newIndex < gridItems.size()) {
            AppItem item = gridItems.remove(reorderIndex);
            gridItems.add(newIndex, item);
            reorderIndex = newIndex;
            saveGridPackageList();
            playNavSound();
            setupGrid();
            updateReorderVisuals();
        }
    }

    private void updateReorderVisuals() {
        if (!isReorderMode) return;
        if (isReorderingDock) {
            for (int i = 0; i < dockContainer.getChildCount(); i++) {
                View child = dockContainer.getChildAt(i);
                if (child != null) {
                    FrameLayout frame = child.findViewById(R.id.dock_item_frame);
                    if (frame != null) {
                        if (i == reorderIndex) {
                            frame.animate().scaleX(1.15f).scaleY(1.15f).alpha(1.0f).setDuration(120).start();
                            frame.requestFocus();
                        } else {
                            frame.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.5f).setDuration(120).start();
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < appsGrid.getChildCount(); i++) {
                View child = appsGrid.getChildAt(i);
                if (child != null) {
                    FrameLayout frame = child.findViewById(R.id.tile_frame);
                    if (frame != null) {
                        if (i == reorderIndex) {
                            frame.animate().scaleX(1.15f).scaleY(1.15f).alpha(1.0f).setDuration(120).start();
                            frame.requestFocus();
                        } else {
                            frame.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.5f).setDuration(120).start();
                        }
                    }
                }
            }
        }
    }

    private void saveGridPackageList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gridItems.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(gridItems.get(i).packageName);
        }
        prefs.edit().putString(KEY_GRID_LIST, sb.toString()).apply();
    }

    private void showDockItemOptionsDialog(final int slotIndex, final AppItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle(item.label + " (Dock)");

        final List<String> options = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        options.add("🖐️ Mover com as setas do comando");
        actions.add(new Runnable() {
            @Override
            public void run() {
                startReorderMode(true, slotIndex);
            }
        });

        options.add("🔁 Substituir aplicação...");
        actions.add(new Runnable() {
            @Override
            public void run() {
                showChooseAppForDockDialog(slotIndex);
            }
        });

        if (slotIndex > 0) {
            options.add("⬅️ Mover para a esquerda");
            actions.add(new Runnable() {
                @Override
                public void run() {
                    moveDockPackage(slotIndex, slotIndex - 1);
                }
            });
        }

        if (slotIndex < dockItems.size() - 1) {
            options.add("➡️ Mover para a direita");
            actions.add(new Runnable() {
                @Override
                public void run() {
                    moveDockPackage(slotIndex, slotIndex + 1);
                }
            });
        }

        if (dockItems.size() > 1) {
            options.add("❌ Remover da Dock");
            actions.add(new Runnable() {
                @Override
                public void run() {
                    removeDockPackage(slotIndex);
                    Toast.makeText(MainActivity.this, item.label + " removida da Dock", Toast.LENGTH_SHORT).show();
                }
            });
        }

        options.add("ℹ️ Detalhes da Aplicação");
        actions.add(new Runnable() {
            @Override
            public void run() {
                openAppDetails(item.packageName);
            }
        });

        builder.setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < actions.size()) {
                    actions.get(which).run();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAddAppToDockDialog() {
        final List<AppItem> candidates = new ArrayList<>();
        List<String> dockPkgs = getDockPackageList();
        for (AppItem app : allInstalledApps) {
            if (!dockPkgs.contains(app.packageName)) {
                candidates.add(app);
            }
        }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "Todas as aplicações instaladas já estão na Dock", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Adicionar Aplicação à Dock");

        final ListAdapter adapter = new ArrayAdapter<AppItem>(this, android.R.layout.select_dialog_item, android.R.id.text1, candidates) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(17f);
                AppItem candidate = candidates.get(position);
                tv.setText(candidate.label);
                if (candidate.banner != null) {
                    try {
                        Drawable icon = candidate.banner.getConstantState() != null ? candidate.banner.getConstantState().newDrawable().mutate() : candidate.banner;
                        icon.setBounds(0, 0, 56, 56);
                        tv.setCompoundDrawables(icon, null, null, null);
                        tv.setCompoundDrawablePadding(20);
                    } catch (Exception ignored) {
                    }
                }
                return view;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < candidates.size()) {
                    AppItem selected = candidates.get(which);
                    addDockPackage(selected.packageName);
                    Toast.makeText(MainActivity.this, selected.label + " adicionada à Dock", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showChooseAppForDockDialog(final int slotIndex) {
        final List<AppItem> candidates = new ArrayList<>();
        List<String> dockPkgs = getDockPackageList();
        for (AppItem app : allInstalledApps) {
            if (!dockPkgs.contains(app.packageName)) {
                candidates.add(app);
            }
        }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "Nenhuma outra aplicação disponível para substituir", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Substituir Posição " + (slotIndex + 1) + " da Dock");

        final ListAdapter adapter = new ArrayAdapter<AppItem>(this, android.R.layout.select_dialog_item, android.R.id.text1, candidates) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(17f);
                AppItem item = candidates.get(position);
                tv.setText(item.label);
                if (item.banner != null) {
                    try {
                        Drawable icon = item.banner.getConstantState() != null ? item.banner.getConstantState().newDrawable().mutate() : item.banner;
                        icon.setBounds(0, 0, 56, 56);
                        tv.setCompoundDrawables(icon, null, null, null);
                        tv.setCompoundDrawablePadding(20);
                    } catch (Exception ignored) {
                    }
                }
                return view;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < candidates.size()) {
                    AppItem selected = candidates.get(which);
                    replaceDockPackage(slotIndex, selected.packageName);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPinToDockDialog(final AppItem item) {
        if (dockItems.size() < 6) {
            addDockPackage(item.packageName);
            Toast.makeText(this, item.label + " adicionada à Dock", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Substituir na Dock por \"" + item.label + "\"");

        String[] options = new String[dockItems.size()];
        for (int i = 0; i < dockItems.size(); i++) {
            options[i] = "Posição " + (i + 1) + " (atualmente: " + dockItems.get(i).label + ")";
        }

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                replaceDockPackage(which, item.packageName);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getAppCategorySubtitle(String pkg) {
        if (pkg == null) return "APLICAÇÃO";
        String lower = pkg.toLowerCase();
        if (lower.contains("netflix") || lower.contains("plex") || lower.contains("wbd") ||
            lower.contains("amazonvideo") || lower.contains("stremio") || lower.contains("disney") ||
            lower.contains("hbo") || lower.contains("max") || lower.contains("apple")) {
            return "STREAMING & SÉRIES";
        }
        if (lower.contains("youtube") || lower.contains("smarttube") || lower.contains("spotify") ||
            lower.contains("tidal") || lower.contains("music") || lower.contains("audio")) {
            return "VÍDEO & MÚSICA";
        }
        if (lower.contains("steam") || lower.contains("moonlight") || lower.contains("luna") ||
            lower.contains("geforce") || lower.contains("game") || lower.contains("retroarch")) {
            return "CLOUD GAMING & JOGOS";
        }
        if (lower.contains("browser") || lower.contains("chrome") || lower.contains("internet")) {
            return "NAVEGADOR & WEB";
        }
        if (lower.contains("settings") || lower.contains("vending") || lower.contains("playstore")) {
            return "SISTEMA & UTILITÁRIOS";
        }
        return "APLICAÇÃO";
    }

    private void updateDockTitleAndGlow(AppItem item) {
        tvActiveDockTitle.setText(item.label);
        if (tvActiveDockSubtitle != null) {
            tvActiveDockSubtitle.setText(getAppCategorySubtitle(item.packageName));
        }

        // Full-screen seamless radial ambient glow centered on lowered dock position
        int color = item.glowColor;
        GradientDrawable glow = new GradientDrawable();
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(1200f);
        glow.setGradientCenter(0.32f, 0.78f);
        glow.setColors(new int[]{
            Color.argb(26, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(8, Color.red(color), Color.green(color), Color.blue(color)),
            Color.TRANSPARENT
        });
        viewAmbientGlow.setBackground(glow);
    }

    private void setupGrid() {
        appsGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < gridItems.size(); i++) {
            final AppItem item = gridItems.get(i);
            View tileView = inflater.inflate(R.layout.item_app_tile, appsGrid, false);

            final FrameLayout frame = tileView.findViewById(R.id.tile_frame);
            ImageView icon = tileView.findViewById(R.id.tile_icon);
            final TextView label = tileView.findViewById(R.id.tile_label);

            if (outfitRegular != null) label.setTypeface(outfitRegular);
            label.setText(item.label);
            if (item.banner != null) {
                icon.setImageDrawable(item.banner);
                int w = item.banner.getIntrinsicWidth();
                int h = item.banner.getIntrinsicHeight();
                if (w > 0 && h > 0 && ((float) w / (float) h) < 1.35f) {
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    int pad = (int) (8 * getResources().getDisplayMetrics().density);
                    icon.setPadding(pad, pad, pad, pad);
                } else {
                    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    icon.setPadding(0, 0, 0, 0);
                }
            } else {
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int pad = (int) (10 * getResources().getDisplayMetrics().density);
                icon.setPadding(pad, pad, pad, pad);
            }

            frame.setId(1000 + i);

            // Connect Up focus between first row of grid and dock / manage button
            if (i < 4) {
                if (i == 3 && btnManageApps != null) {
                    frame.setNextFocusUpId(R.id.btn_manage_apps);
                } else if (i < dockItems.size()) {
                    frame.setNextFocusUpId(500 + i);
                } else {
                    frame.setNextFocusUpId(R.id.btn_manage_apps);
                }
            }

            frame.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        playNavSound();
                        frame.animate().scaleX(1.06f).scaleY(1.06f).setDuration(180).start();
                        label.animate().alpha(1.0f).setDuration(180).start();
                        if (viewGridDimmer != null) {
                            viewGridDimmer.animate().alpha(0.85f).setDuration(250).start();
                        }

                        // Smoothly scroll grid into viewing range
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        if (location[1] > 650) {
                            mainScroll.smoothScrollBy(0, location[1] - 480);
                        } else if (location[1] < 160) {
                            mainScroll.smoothScrollBy(0, location[1] - 260);
                        }
                    } else {
                        frame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                        label.animate().alpha(0.0f).setDuration(120).start();
                    }
                }
            });

            frame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchPackage(item.packageName);
                }
            });

            // Long press OK to customize or manage this app
            frame.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showGridAppOptionsDialog(item);
                    return true;
                }
            });

            appsGrid.addView(tileView);
        }

        // Setup btnManageApps focus links
        if (btnManageApps != null) {
            btnManageApps.setNextFocusUpId(!dockItems.isEmpty() ? 500 + dockItems.size() - 1 : R.id.btn_settings);
            if (!gridItems.isEmpty()) {
                int targetCol = Math.min(3, gridItems.size() - 1);
                btnManageApps.setNextFocusDownId(1000 + targetCol);
            }
        }
        applySoundMode(soundMode);
    }

    private void showGridAppOptionsDialog(final AppItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle(item.label);

        final List<String> options = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        options.add("🖐️ Mover com as setas do comando");
        actions.add(new Runnable() {
            @Override
            public void run() {
                int idx = -1;
                for (int i = 0; i < gridItems.size(); i++) {
                    if (gridItems.get(i).packageName.equals(item.packageName)) {
                        idx = i;
                        break;
                    }
                }
                if (idx != -1) {
                    startReorderMode(false, idx);
                }
            }
        });

        if (dockItems.size() < 6) {
            options.add("⭐ Adicionar à Dock");
            actions.add(new Runnable() {
                @Override
                public void run() {
                    addDockPackage(item.packageName);
                    Toast.makeText(MainActivity.this, item.label + " adicionada à Dock", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            options.add("⭐ Fixar na Dock (substituir)");
            actions.add(new Runnable() {
                @Override
                public void run() {
                    showPinToDockDialog(item);
                }
            });
        }

        options.add("👁️ Ocultar da Lista");
        actions.add(new Runnable() {
            @Override
            public void run() {
                hidePackage(item.packageName);
                Toast.makeText(MainActivity.this, item.label + " ocultada da lista", Toast.LENGTH_SHORT).show();
            }
        });

        options.add("🗑️ Desinstalar Aplicação");
        actions.add(new Runnable() {
            @Override
            public void run() {
                uninstallApp(item.packageName);
            }
        });

        options.add("ℹ️ Detalhes da Aplicação");
        actions.add(new Runnable() {
            @Override
            public void run() {
                openAppDetails(item.packageName);
            }
        });

        builder.setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < actions.size()) {
                    actions.get(which).run();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showManageAppsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Gestão de Aplicações");

        final Set<String> hidden = getHiddenPackages();
        String hiddenOption = "👁️ Aplicações Ocultadas (" + hidden.size() + " ocultas)";
        int currentTimeout = getScreensaverTimeoutMinutes();
        String timeoutText = currentTimeout > 0 ? currentTimeout + " min" : "Desativado";

        String soundModeSummary;
        if (soundMode == SOUND_MODE_APPLE) {
            soundModeSummary = "Apple TV (Personalizado)";
        } else if (soundMode == SOUND_MODE_SYSTEM) {
            soundModeSummary = "Sistema TCL (Padrão)";
        } else {
            soundModeSummary = "Desativado";
        }

        final String[] options = new String[]{
            hiddenOption,
            "🎬 Iniciar Proteção de Ecrã (Apple TV Aerial)",
            "⏱️ Tempo do Screensaver (" + timeoutText + ")",
            "🔊 Sons de Navegação (" + soundModeSummary + ")",
            "🛒 Abrir Google Play Store",
            "⭐ Adicionar Aplicação à Dock",
            "💡 Dica: Mantém o botão OK premido em qualquer app para opções rápidas"
        };

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showHiddenAppsDialog();
                } else if (which == 1) {
                    startScreensaver();
                } else if (which == 2) {
                    showScreensaverTimeoutDialog();
                } else if (which == 3) {
                    showSoundOptionsDialog();
                } else if (which == 4) {
                    launchPackage("com.android.vending");
                } else if (which == 5) {
                    showAddAppToDockDialog();
                }
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHiddenAppsDialog() {
        final Set<String> hidden = getHiddenPackages();
        if (hidden.isEmpty()) {
            Toast.makeText(this, "Não existem aplicações ocultadas", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<AppItem> hiddenList = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String pkg : hidden) {
            hiddenList.add(createAppItem(pm, pkg));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Restaurar Aplicações Ocultadas");

        final String[] names = new String[hiddenList.size()];
        for (int i = 0; i < hiddenList.size(); i++) {
            names[i] = "👁️ Mostrar \"" + hiddenList.get(i).label + "\"";
        }

        builder.setItems(names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which >= 0 && which < hiddenList.size()) {
                    AppItem restored = hiddenList.get(which);
                    unhidePackage(restored.packageName);
                    Toast.makeText(MainActivity.this, restored.label + " restaurada", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setPositiveButton("Restaurar Todas", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .remove(KEY_HIDDEN_PACKAGES).apply();
                reloadAll();
                Toast.makeText(MainActivity.this, "Todas as aplicações foram restauradas", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Fechar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchPackage(String pkg) {
        try {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (intent == null && "org.smarttube.stable".equals(pkg)) {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("org.smarttube.stable", "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity");
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
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch " + pkg, e);
        }
    }

    private boolean handleMenuButtonOnFocusedView() {
        View focused = getCurrentFocus();
        if (focused == null) return false;

        for (int i = 0; i < dockItems.size(); i++) {
            if (focused.getId() == 500 + i) {
                showDockItemOptionsDialog(i, dockItems.get(i));
                return true;
            }
        }

        if (focused.getId() == R.id.dock_item_add) {
            showAddAppToDockDialog();
            return true;
        }

        if (focused.getId() == R.id.btn_manage_apps) {
            showManageAppsDialog();
            return true;
        }

        int focusedId = focused.getId();
        if (focusedId >= 1000 && focusedId < 1000 + gridItems.size()) {
            int gridIndex = focusedId - 1000;
            showGridAppOptionsDialog(gridItems.get(gridIndex));
            return true;
        }

        return false;
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
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Any key interaction resets inactivity timer
        resetInactivityTimer();

        // If screensaver is active, ANY button dismisses it gracefully
        if (isScreensaverActive) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                stopScreensaver();
            }
            return true;
        }

        // If event is from a game controller, pass it directly to system/view hierarchy
        // so that A/Cross clicks focused items and B/O/X/Y behave naturally without launching apps
        if (isGamepadOrController(event)) {
            return super.dispatchKeyEvent(event);
        }

        // Reorder Mode key interception (Move app with D-Pad, confirm with OK or Back)
        if (isReorderMode) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (isReorderingDock) moveReorderDock(-1);
                    else moveReorderGrid(-1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (isReorderingDock) moveReorderDock(1);
                    else moveReorderGrid(1);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (!isReorderingDock) moveReorderGrid(-4);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!isReorderingDock) moveReorderGrid(4);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BACK) {
                    stopReorderMode();
                    return true;
                }
            }
            return true;
        }

        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        int action = event.getAction();
        Log.i(TAG, "dispatchKeyEvent: action=" + action + " keyCode=" + keyCode + " scanCode=" + scanCode);

        // If soundMode is not SYSTEM (e.g. Apple TV or Off), handle DPAD navigation directly
        // to prevent Android's ViewRootImpl from playing the duplicate framework navigation sound
        if (soundMode != SOUND_MODE_SYSTEM) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {

                if (action == KeyEvent.ACTION_DOWN) {
                    int direction;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        direction = View.FOCUS_LEFT;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        direction = View.FOCUS_RIGHT;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        direction = View.FOCUS_UP;
                    } else {
                        direction = View.FOCUS_DOWN;
                    }

                    View current = getCurrentFocus();
                    if (current != null) {
                        View next = current.focusSearch(direction);
                        if (next != null && next != current) {
                            next.requestFocus(direction);
                            return true;
                        }
                    }
                    return true;
                } else if (action == KeyEvent.ACTION_UP) {
                    return true;
                }
            }
        }

        if (action == KeyEvent.ACTION_UP) {
            // Context menu / Dock customization on MENU button (82)
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == 82) {
                if (handleMenuButtonOnFocusedView()) {
                    return true;
                }
                showSettingsControlMenu();
                return true;
            }

            // Settings button (scanCode 233/234 or keyCodes)
            if (keyCode == KeyEvent.KEYCODE_SETTINGS ||
                keyCode == 176 || keyCode == 312 || keyCode == 4514 || keyCode == 4137 ||
                scanCode == 233 || scanCode == 234) {
                showSettingsControlMenu();
                return true;
            }

            // Inputs button
            if (keyCode == KeyEvent.KEYCODE_TV_INPUT || keyCode == KeyEvent.KEYCODE_GUIDE || 
                keyCode == 178 || keyCode == 172 || (keyCode >= 4506 && keyCode <= 4509)) {
                toggleInputsDropdown();
                return true;
            }

            // Prime Video button (scancode 752 / keycode 4099)
            if (keyCode == 4099 || scanCode == 752) {
                launchPackage("com.amazon.amazonvideo.livingroom");
                return true;
            }

            // Netflix button (scancode 566 / keycode 4062)
            if (keyCode == 4062 || scanCode == 566) {
                launchPackage("com.netflix.ninja");
                return true;
            }

            // Disney+ button remapped to MAX (scancode 762 / keycode 4141)
            if (keyCode == 4141 || scanCode == 762) {
                launchPackage("com.wbd.stream");
                return true;
            }

            // TCL Channel button remapped to Plex (scancode 456 / keycode 4020, 4184)
            if (keyCode == 4020 || keyCode == 4184 || scanCode == 456) {
                launchPackage("com.plexapp.android");
                return true;
            }

            // YouTube button remapped to SmartTube (keycode 4065)
            if (keyCode == 4065) {
                launchPackage("org.smarttube.stable");
                return true;
            }

            // Mic / Siri button
            if (keyCode == KeyEvent.KEYCODE_VOICE_ASSIST || keyCode == KeyEvent.KEYCODE_SEARCH || 
                keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == 231 || keyCode == 219 || keyCode == 84 || 
                scanCode == 582 || scanCode == 217 || scanCode == 570) {
                showSiriOverlay();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (siriOverlayContainer != null && siriOverlayContainer.getVisibility() == View.VISIBLE) {
                hideSiriOverlay();
                return true;
            }
            if (dropdownInputs.getVisibility() == View.VISIBLE) {
                hideInputsDropdown();
                return true;
            }
            // If in apps grid, scroll back up to 0,0 and focus dock
            if (mainScroll.getScrollY() > 50) {
                mainScroll.smoothScrollTo(0, 0);
                if (viewGridDimmer != null) {
                    viewGridDimmer.animate().alpha(0.0f).setDuration(250).start();
                }
                View dockView = findViewById(500 + lastFocusedDockIndex);
                if (dockView == null && dockContainer != null && dockContainer.getChildCount() > 0) {
                    dockView = dockContainer.getChildAt(0);
                }
                if (dockView != null) dockView.requestFocus();
                return true;
            }
            return true; // Consume BACK on home
        }

        if (keyCode == KeyEvent.KEYCODE_TV_INPUT || keyCode == KeyEvent.KEYCODE_GUIDE || keyCode == 178 || keyCode == 172 ||
            (keyCode >= 4506 && keyCode <= 4509)) {
            toggleInputsDropdown();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU || keyCode == 176 || keyCode == 82 || keyCode == 312 ||
            keyCode == 4514 || keyCode == 4137) {
            return true;
        }

        // Remote shortcut buttons (consume on DOWN so ACTION_UP is handled cleanly by dispatchKeyEvent)
        boolean isMicDown = (keyCode == KeyEvent.KEYCODE_VOICE_ASSIST || keyCode == KeyEvent.KEYCODE_SEARCH || 
                             keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == 231 || keyCode == 219 || keyCode == 84 || 
                             keyCode == 582 || keyCode == 217 || keyCode == 570);
        if (!isGamepadOrController(event) && (isMicDown || keyCode == 4062 || keyCode == 4020 || keyCode == 4184 || keyCode == 4141 || keyCode == 4099 || keyCode == 4065)) {
            return true;
        }

        Log.i(TAG, "onKeyDown unhandled keycode: " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("ACTION_OPEN_INPUTS", false)) {
            toggleInputsDropdown();
        }
        if (intent != null && intent.getBooleanExtra("ACTION_START_SCREENSAVER", false)) {
            startScreensaver();
        }
        if (intent != null && (Intent.ACTION_ASSIST.equals(intent.getAction()) ||
                "android.intent.action.VOICE_ASSIST".equals(intent.getAction()) ||
                Intent.ACTION_SEARCH.equals(intent.getAction()) ||
                intent.getBooleanExtra("TRIGGER_SIRI", false))) {
            showSiriOverlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(screensaverInactivityRunnable);
        handler.removeCallbacks(screensaverCycleRunnable);
        if (isScreensaverActive && screensaverOverlay != null) {
            screensaverOverlay.setVisibility(View.GONE);
            isScreensaverActive = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySoundMode(soundMode);
        updateClock();
        updateWifiStatus();
        if (dropdownInputs != null) {
            dropdownInputs.setVisibility(View.GONE);
        }
        checkAndRefreshAppsIfChanged();
        resetInactivityTimer();
    }

    // -------------------------------------------------------------
    // Siri / AI Voice Assistant Implementation
    // -------------------------------------------------------------
    private void initSiriTts() {
        try {
            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int res = textToSpeech.setLanguage(new Locale("pt", "PT"));
                        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                            textToSpeech.setLanguage(new Locale("pt", "BR"));
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TTS", e);
        }
    }

    private void initSiriSuggestions() {
        if (btnSiriChip1 != null) {
            btnSiriChip1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeSiriAction("sleep_timer", "30");
                    tvSiriResponse.setText("A programar o temporizador para desligar em 30 minutos.");
                    tvSiriResponse.setVisibility(View.VISIBLE);
                    tvSiriBadge.setText("⏱️ Temporizador: 30 min");
                    tvSiriBadge.setVisibility(View.VISIBLE);
                    speakText("A programar o temporizador para desligar em 30 minutos.");
                    scheduleSiriDismiss(3500);
                }
            });
        }
        if (btnSiriChip2 != null) {
            btnSiriChip2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeSiriAction("launch_app", "plex");
                    tvSiriResponse.setText("A abrir o Plex.");
                    tvSiriResponse.setVisibility(View.VISIBLE);
                    speakText("A abrir o Plex.");
                    scheduleSiriDismiss(2000);
                }
            });
        }
        if (btnSiriChip3 != null) {
            btnSiriChip3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeSiriAction("launch_app", "steamlink");
                    tvSiriResponse.setText("A abrir a Steam Link.");
                    tvSiriResponse.setVisibility(View.VISIBLE);
                    speakText("A abrir a Steam Link.");
                    scheduleSiriDismiss(2000);
                }
            });
        }
        if (btnSiriChip4 != null) {
            btnSiriChip4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeSiriAction("switch_hdmi", "1");
                    tvSiriResponse.setText("A mudar para HDMI 1.");
                    tvSiriResponse.setVisibility(View.VISIBLE);
                    speakText("A mudar para HDMI 1.");
                    scheduleSiriDismiss(2000);
                }
            });
        }
    }

    private void speakText(String text) {
        if (textToSpeech != null && text != null && !text.isEmpty()) {
            try {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "siri_tts_id");
            } catch (Exception e) {
                Log.e(TAG, "TTS speak failed", e);
            }
        }
    }

    private void scheduleSiriDismiss(long delayMs) {
        siriDismissHandler.removeCallbacksAndMessages(null);
        siriDismissHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSiriOverlay();
            }
        }, delayMs);
    }

    public void showSiriOverlay() {
        if (siriOverlayContainer == null) return;

        if (isScreensaverActive) {
            stopScreensaver();
        }

        siriOverlayContainer.setVisibility(View.VISIBLE);
        siriOverlayContainer.setAlpha(0f);
        siriOverlayContainer.animate().alpha(1f).setDuration(250).start();

        if (siriCard != null) {
            siriCard.setTranslationY(80f);
            siriCard.animate().translationY(0f).setDuration(280).start();
        }

        tvSiriTitle.setText("Gemini");
        tvSiriQuery.setText("A ouvir... Podes falar.");
        tvSiriResponse.setVisibility(View.GONE);
        tvSiriBadge.setVisibility(View.GONE);
        siriSuggestionsLayout.setVisibility(View.GONE);

        startSiriOrbPulse();
        startListening();
    }

    public void hideSiriOverlay() {
        if (siriOverlayContainer == null || siriOverlayContainer.getVisibility() != View.VISIBLE) return;

        stopSiriOrbPulse();
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
            } catch (Exception ignored) {}
        }
        siriDismissHandler.removeCallbacksAndMessages(null);

        siriOverlayContainer.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
            @Override
            public void run() {
                siriOverlayContainer.setVisibility(View.GONE);
                siriOverlayContainer.setAlpha(1f);
                if (dockContainer != null && dockContainer.getChildCount() > 0) {
                    View dockTile = dockContainer.getChildAt(lastFocusedDockIndex >= 0 ? Math.min(lastFocusedDockIndex, dockContainer.getChildCount() - 1) : 0);
                    if (dockTile != null) dockTile.requestFocus();
                }
            }
        }).start();
    }

    private void startSiriOrbPulse() {
        stopSiriOrbPulse();
        siriOrbAnimator = ValueAnimator.ofFloat(0.85f, 1.25f);
        siriOrbAnimator.setDuration(700);
        siriOrbAnimator.setRepeatCount(ValueAnimator.INFINITE);
        siriOrbAnimator.setRepeatMode(ValueAnimator.REVERSE);
        siriOrbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                if (siriOrb != null) {
                    siriOrb.setScaleX(val);
                    siriOrb.setScaleY(val);
                    siriOrb.setAlpha(0.65f + (val - 0.85f) * 0.875f);
                }
            }
        });
        siriOrbAnimator.start();
    }

    private void stopSiriOrbPulse() {
        if (siriOrbAnimator != null) {
            siriOrbAnimator.cancel();
            siriOrbAnimator = null;
        }
        if (siriOrb != null) {
            siriOrb.setScaleX(1f);
            siriOrb.setScaleY(1f);
            siriOrb.setAlpha(1f);
        }
    }

    private void startListening() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-PT");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    tvSiriQuery.setText("A ouvir... Podes falar agora.");
                }

                @Override
                public void onBeginningOfSpeech() {
                    tvSiriQuery.setText("A ouvir...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    if (siriOrb != null) {
                        float scale = 1.0f + Math.max(0f, rmsdB) / 22f;
                        siriOrb.setScaleX(scale);
                        siriOrb.setScaleY(scale);
                    }
                }

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    tvSiriQuery.setText("A pensar...");
                    startSiriOrbPulse();
                }

                @Override
                public void onError(int error) {
                    Log.w(TAG, "SpeechRecognizer error: " + error);
                    handleSiriError(error);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results != null ? results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        onUserVoiceCommand(query);
                    } else {
                        handleSiriError(-1);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partials = partialResults != null ? partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) : null;
                    if (partials != null && !partials.isEmpty()) {
                        tvSiriQuery.setText("«" + partials.get(0) + "»");
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start speech recognition", e);
            handleSiriError(-2);
        }
    }

    private void onUserVoiceCommand(String query) {
        tvSiriQuery.setText("«" + query + "»");
        tvSiriResponse.setVisibility(View.VISIBLE);
        tvSiriResponse.setText("A pensar...");
        tvSiriBadge.setVisibility(View.GONE);

        GeminiClient.ask(query, new GeminiClient.GeminiCallback() {
            @Override
            public void onSuccess(final String speech, final String action, final String param, final String badge) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvSiriResponse.setText(speech);

                        if (badge != null && !badge.trim().isEmpty()) {
                            tvSiriBadge.setText(badge);
                            tvSiriBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvSiriBadge.setVisibility(View.GONE);
                        }

                        stopSiriOrbPulse();
                        speakText(speech);
                        executeSiriAction(action, param);
                        scheduleSiriDismiss(6000);
                    }
                });
            }

            @Override
            public void onError(final String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopSiriOrbPulse();
                        tvSiriResponse.setText("Desculpa, não consegui obter resposta: " + errorMessage);
                        showSiriSuggestions();
                    }
                });
            }
        });
    }

    private void executeSiriAction(String action, String param) {
        if (action == null || action.isEmpty() || "none".equalsIgnoreCase(action)) return;

        Log.i(TAG, "Executing Siri action: " + action + " (" + param + ")");
        try {
            String act = action.toLowerCase();
            if ("launch_app".equals(act)) {
                String pkg = null;
                String p = param != null ? param.toLowerCase() : "";
                if (p.contains("plex")) pkg = "com.plexapp.android";
                else if (p.contains("netflix")) pkg = "com.netflix.ninja";
                else if (p.contains("smarttube") || p.contains("youtube")) pkg = "org.smarttube.stable";
                else if (p.contains("prime")) pkg = "com.amazon.amazonvideo.livingroom";
                else if (p.contains("max") || p.contains("hbo")) pkg = "com.wbd.stream";
                else if (p.contains("steam")) pkg = "com.valvesoftware.steamlink";
                else if (p.contains("luna")) pkg = "com.amazon.spiderpork";
                else if (p.contains("setting") || p.contains("definiç")) {
                    openSettingsFallback();
                    return;
                }
                if (pkg != null) {
                    launchPackage(pkg);
                }
            } else if ("sleep_timer".equals(act)) {
                int minutes = 30;
                try {
                    minutes = Integer.parseInt(param);
                } catch (Exception ignored) {}
                RemoteKeyService.setSleepTimer(minutes);
                updateSleepPillText();
            } else if ("switch_hdmi".equals(act)) {
                int port = 1;
                try {
                    port = Integer.parseInt(param);
                } catch (Exception ignored) {}
                String inputId = TvInputHelper.INPUT_HDMI_1;
                if (port == 2) inputId = TvInputHelper.INPUT_HDMI_2;
                else if (port == 3) inputId = TvInputHelper.INPUT_HDMI_3;
                TvInputHelper.switchToInput(this, inputId);
            } else if ("set_wallpaper".equals(act)) {
                String p = param != null ? param.toLowerCase() : "";
                if (p.contains("day") || p.contains("dia")) {
                    ivWallpaper.setImageResource(R.drawable.wallpaper_day);
                } else if (p.contains("sunset") || p.contains("tarde") || p.contains("sol")) {
                    ivWallpaper.setImageResource(R.drawable.wallpaper);
                } else if (p.contains("night") || p.contains("noite")) {
                    ivWallpaper.setImageResource(R.drawable.wallpaper_night);
                }
            } else if ("search_youtube".equals(act)) {
                try {
                    Intent yt = new Intent(Intent.ACTION_SEARCH);
                    yt.setPackage("org.smarttube.stable");
                    yt.putExtra("query", param);
                    yt.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(yt);
                } catch (Exception e) {
                    try {
                        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(param)));
                        web.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(web);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in executeSiriAction", e);
        }
    }

    private void handleSiriError(int errorCode) {
        stopSiriOrbPulse();
        tvSiriQuery.setText("Não consegui ouvir. Podes escolher uma ação rápida:");
        showSiriSuggestions();
    }

    private void showSiriSuggestions() {
        if (siriSuggestionsLayout != null) {
            siriSuggestionsLayout.setVisibility(View.VISIBLE);
            if (btnSiriChip1 != null) btnSiriChip1.requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(clockRunnable);
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {
            }
            soundPool = null;
        }
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(siriReceiver);
        } catch (Exception ignored) {
        }
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
            } catch (Exception ignored) {}
            textToSpeech = null;
        }
        stopSiriOrbPulse();
        siriDismissHandler.removeCallbacksAndMessages(null);
    }
}
