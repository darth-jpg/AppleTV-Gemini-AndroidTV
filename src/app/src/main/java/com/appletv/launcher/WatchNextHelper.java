package com.appletv.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.LruCache;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Watch Next (Continuar a Ver / Up Next) helper for Apple TV Launcher.
 * Reads unfinished media cards directly from Android TV's system provider (TvContract).
 * Supports Plex, Prime Video, HBO Max, Disney+, Stremio, and any app publishing to Watch Next.
 */
public final class WatchNextHelper {
    public static final String PERMISSION = "android.permission.READ_TV_LISTINGS";
    public static final int MAX_ITEMS = 6;
    public static final int MAX_PER_APP = 2;

    // Small 4MB memory cache for thumbnails
    private static final LruCache<String, Bitmap> sThumbCache = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public static class Item {
        public String pkg;
        public String title;
        public String episode;
        public String appName;
        public long duration;
        public long position;
        public long engaged;
        public String posterUri;
        public String thumbnailUri;
        public String intentUri;

        public float getProgress() {
            if (duration <= 0 || position < 1000 || position >= duration) return -1f;
            return (float) position / duration;
        }

        public String getBestImageUri() {
            return thumbnailUri != null && !thumbnailUri.isEmpty() ? thumbnailUri : posterUri;
        }
    }

    public static boolean hasPermission(Context context) {
        try {
            return context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Loads the active Continue Watching items (runs off main thread).
     */
    public static List<Item> loadWatchNext(Context context) {
        if (!hasPermission(context)) return Collections.emptyList();

        List<Item> all = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        String[] projection = new String[] {
            TvContract.WatchNextPrograms._ID,
            TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
            TvContract.WatchNextPrograms.COLUMN_TITLE,
            TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
            TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
            TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE,
            TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
            TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
            TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
            TvContract.WatchNextPrograms.COLUMN_BROWSABLE
        };

        try (Cursor c = cr.query(TvContract.WatchNextPrograms.CONTENT_URI, projection, null, null, null)) {
            if (c != null) {
                int colPkg = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME);
                int colTitle = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_TITLE);
                int colSeason = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER);
                int colEpisode = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER);
                int colEpTitle = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_EPISODE_TITLE);
                int colDur = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS);
                int colPos = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
                int colPoster = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI);
                int colThumb = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI);
                int colIntent = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_INTENT_URI);
                int colEngaged = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS);
                int colType = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE);
                int colBrowsable = c.getColumnIndex(TvContract.WatchNextPrograms.COLUMN_BROWSABLE);

                while (c.moveToNext()) {
                    if (colBrowsable >= 0 && "0".equals(c.getString(colBrowsable))) continue;
                    // Ignore static watchlists
                    if (colType >= 0 && c.getInt(colType) == TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST) continue;

                    String pkg = colPkg >= 0 ? c.getString(colPkg) : null;
                    String title = colTitle >= 0 ? c.getString(colTitle) : null;
                    if (pkg == null || title == null) continue;

                    Item item = new Item();
                    item.pkg = pkg;
                    item.title = title;

                    String season = colSeason >= 0 ? c.getString(colSeason) : null;
                    String episode = colEpisode >= 0 ? c.getString(colEpisode) : null;
                    if (season != null && episode != null) {
                        item.episode = "T" + season + " E" + episode;
                    } else if (colEpTitle >= 0) {
                        item.episode = c.getString(colEpTitle);
                    }

                    item.duration = colDur >= 0 ? c.getLong(colDur) : 0;
                    item.position = colPos >= 0 ? c.getLong(colPos) : 0;
                    item.engaged = colEngaged >= 0 ? c.getLong(colEngaged) : 0;
                    item.posterUri = colPoster >= 0 ? c.getString(colPoster) : null;
                    item.thumbnailUri = colThumb >= 0 ? c.getString(colThumb) : null;
                    item.intentUri = colIntent >= 0 ? c.getString(colIntent) : null;

                    all.add(item);
                }
            }
        } catch (Throwable t) {
            // Safe fallback if TvContract query is not permitted on current firmware
        }

        if (all.isEmpty()) return Collections.emptyList();

        // Sort newest engagement first
        Collections.sort(all, (a, b) -> Long.compare(b.engaged, a.engaged));

        PackageManager pm = context.getPackageManager();
        List<Item> result = new ArrayList<>();
        Map<String, Integer> countPerApp = new HashMap<>();
        Set<String> seen = new HashSet<>();

        for (Item item : all) {
            String dedupeKey = item.pkg + "|" + (item.intentUri != null ? item.intentUri : item.title);
            if (!seen.add(dedupeKey)) continue;

            int count = countPerApp.containsKey(item.pkg) ? countPerApp.get(item.pkg) : 0;
            if (count >= MAX_PER_APP) continue;
            countPerApp.put(item.pkg, count + 1);

            try {
                ApplicationInfo info = pm.getApplicationInfo(item.pkg, 0);
                item.appName = pm.getApplicationLabel(info).toString();
            } catch (Throwable t) {
                item.appName = item.pkg;
            }

            result.add(item);
            if (result.size() >= MAX_ITEMS) break;
        }

        return result;
    }

    /**
     * Loads and caches a scaled thumbnail for the card.
     */
    public static Bitmap loadThumbnail(Context context, String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) return null;

        Bitmap cached = sThumbCache.get(uriStr);
        if (cached != null && !cached.isRecycled()) return cached;

        try {
            Uri uri = Uri.parse(uriStr);
            InputStream is = null;
            if ("content".equalsIgnoreCase(uri.getScheme()) || "android.resource".equalsIgnoreCase(uri.getScheme())) {
                is = context.getContentResolver().openInputStream(uri);
            } else if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                HttpURLConnection conn = (HttpURLConnection) new URL(uriStr).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(4000);
                is = conn.getInputStream();
            }

            if (is != null) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // Saves 50% RAM compared to ARGB_8888
                Bitmap full = BitmapFactory.decodeStream(is, null, opts);
                is.close();

                if (full != null) {
                    // Downscale to 320x180 (16:9 thumbnail)
                    Bitmap scaled = Bitmap.createScaledBitmap(full, 320, 180, true);
                    if (scaled != full) full.recycle();
                    sThumbCache.put(uriStr, scaled);
                    return scaled;
                }
            }
        } catch (Throwable t) {
            // Ignore load failure
        }
        return null;
    }

    /**
     * Resumes playback of this item in its target app.
     */
    public static void launchItem(Context context, Item item) {
        if (item == null) return;
        Intent launchIntent = null;

        if (item.intentUri != null && !item.intentUri.isEmpty()) {
            try {
                launchIntent = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME);
            } catch (Throwable t) {
                // Fallback to launcher intent
            }
        }

        if (launchIntent == null && item.pkg != null) {
            launchIntent = context.getPackageManager().getLaunchIntentForPackage(item.pkg);
        }

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(launchIntent);
            } catch (Throwable t) {
                // Ignore launch error
            }
        }
    }
}
