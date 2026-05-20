#!/bin/bash
# ── v4 PART B: MainActivity, Settings (working), Music fixes ──
# Run from ~/FountainPlay
set -e
P="app/src/main/java/com/fountainpdl/fountainplay"

echo "════════════════ v4 Part B ════════════════"

# ════════════════════════════════════════════════════════════
# 1. AppPreferences — add accent/primary stored as hex string
# ════════════════════════════════════════════════════════════
cat > $P/util/AppPreferences.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    private static final String PREF = "fp_prefs";

    public static final String KEY_THEME           = "theme";
    public static final String KEY_PRIMARY_COLOR   = "primary_color";
    public static final String KEY_ACCENT_COLOR    = "accent_color";
    public static final String KEY_AUDIO_FOLDERS   = "audio_folders";
    public static final String KEY_VIDEO_FOLDERS   = "video_folders";
    public static final String KEY_PLAYBACK_SPEED  = "playback_speed";
    public static final String KEY_SKIP_INTERVAL   = "skip_interval";
    public static final String KEY_RESUME_PLAYBACK = "resume_playback";
    public static final String KEY_GESTURES        = "gestures_enabled";
    public static final String KEY_HW_ACCEL        = "hw_acceleration";
    public static final String KEY_REMEMBER_POS    = "remember_position";
    public static final String KEY_LAST_URI        = "last_uri";
    public static final String KEY_LAST_POS        = "last_position";
    public static final String KEY_RECENTLY_PLAYED = "recently_played";

    private final SharedPreferences p;

    public AppPreferences(Context ctx) {
        p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String  getTheme()              { return p.getString(KEY_THEME, "dark"); }
    public void    setTheme(String t)      { p.edit().putString(KEY_THEME, t).apply(); }

    // Colors stored as int (ARGB)
    public int     getPrimaryColor()       { return p.getInt(KEY_PRIMARY_COLOR, 0xFF7B2FBE); }
    public void    setPrimaryColor(int c)  { p.edit().putInt(KEY_PRIMARY_COLOR, c).apply(); }
    public int     getAccentColor()        { return p.getInt(KEY_ACCENT_COLOR, 0xFFE53935); }
    public void    setAccentColor(int c)   { p.edit().putInt(KEY_ACCENT_COLOR, c).apply(); }

    public Set<String> getAudioFolders()           { return p.getStringSet(KEY_AUDIO_FOLDERS, new HashSet<>()); }
    public void        setAudioFolders(Set<String> f) { p.edit().putStringSet(KEY_AUDIO_FOLDERS, f).apply(); }
    public Set<String> getVideoFolders()           { return p.getStringSet(KEY_VIDEO_FOLDERS, new HashSet<>()); }
    public void        setVideoFolders(Set<String> f) { p.edit().putStringSet(KEY_VIDEO_FOLDERS, f).apply(); }

    public float   getPlaybackSpeed()      { return p.getFloat(KEY_PLAYBACK_SPEED, 1.0f); }
    public void    setPlaybackSpeed(float s){ p.edit().putFloat(KEY_PLAYBACK_SPEED, s).apply(); }

    public int     getSkipInterval()       { return p.getInt(KEY_SKIP_INTERVAL, 10); }
    public void    setSkipInterval(int s)  { p.edit().putInt(KEY_SKIP_INTERVAL, s).apply(); }

    public boolean getResumePlayback()     { return p.getBoolean(KEY_RESUME_PLAYBACK, true); }
    public boolean getGesturesEnabled()    { return p.getBoolean(KEY_GESTURES, true); }
    public boolean getHwAcceleration()     { return p.getBoolean(KEY_HW_ACCEL, true); }
    public boolean getRememberPosition()   { return p.getBoolean(KEY_REMEMBER_POS, true); }

    public void    setLastUri(String u)    { p.edit().putString(KEY_LAST_URI, u).apply(); }
    public String  getLastUri()            { return p.getString(KEY_LAST_URI, null); }
    public void    setLastPos(long pos)    { p.edit().putLong(KEY_LAST_POS, pos).apply(); }
    public long    getLastPos()            { return p.getLong(KEY_LAST_POS, 0); }

    public boolean getBoolean(String k, boolean def) { return p.getBoolean(k, def); }
    public void    setBoolean(String k, boolean v)   { p.edit().putBoolean(k, v).apply(); }
}
EOF

# ════════════════════════════════════════════════════════════
# 2. SettingsFragment — all options actually working
# ════════════════════════════════════════════════════════════
cat > $P/ui/settings/SettingsFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.*;

public class SettingsFragment extends Fragment {

    private AppPreferences prefs;

    // Preset color palettes  [name, primaryColor(int), accentColor(int)]
    private static final Object[][] COLOR_PRESETS = {
        {"Purple + Red (Default)", 0xFF7B2FBE, 0xFFE53935},
        {"Blue + Orange",          0xFF1565C0, 0xFFFF6F00},
        {"Teal + Pink",            0xFF00695C, 0xFFE91E63},
        {"Deep Purple + Amber",    0xFF4527A0, 0xFFFFB300},
        {"Indigo + Green",         0xFF283593, 0xFF2E7D32},
        {"Red + Blue",             0xFFC62828, 0xFF1565C0},
        {"Dark Green + Gold",      0xFF1B5E20, 0xFFFFC107},
        {"Monochrome",             0xFF424242, 0xFF757575},
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        setupTheme(view);
        setupColorScheme(view);
        setupPlayback(view);
        setupVideo(view);
        setupFolders(view);
        setupData(view);
    }

    // ── THEME ────────────────────────────────────────────────
    private void setupTheme(View v) {
        TextView tvVal = v.findViewById(R.id.tv_theme_value);
        tvVal.setText(themeLabel(prefs.getTheme()));

        v.findViewById(R.id.pref_theme).setOnClickListener(__ -> {
            String[] labels = {"Dark", "Light", "AMOLED", "Follow System"};
            String[] values = {"dark", "light", "amoled", "system"};
            int cur = indexOf(values, prefs.getTheme());
            new AlertDialog.Builder(requireContext())
                .setTitle("Theme")
                .setSingleChoiceItems(labels, cur, (d, i) -> {
                    prefs.setTheme(values[i]);
                    tvVal.setText(labels[i]);
                    // Apply night mode immediately
                    FountainApp.applyNightMode(values[i]);
                    d.dismiss();
                    // Recreate so per-activity theme (AMOLED) kicks in
                    requireActivity().recreate();
                }).show();
        });
    }

    // ── COLOR SCHEME ─────────────────────────────────────────
    private void setupColorScheme(View v) {
        View primaryDot = v.findViewById(R.id.color_preview_primary);
        View accentDot  = v.findViewById(R.id.color_preview_accent);
        updateDot(primaryDot, prefs.getPrimaryColor());
        updateDot(accentDot, prefs.getAccentColor());

        v.findViewById(R.id.pref_primary_color).setOnClickListener(__ -> {
            String[] names = new String[COLOR_PRESETS.length];
            for (int i = 0; i < COLOR_PRESETS.length; i++) names[i] = (String) COLOR_PRESETS[i][0];
            new AlertDialog.Builder(requireContext())
                .setTitle("Color Scheme")
                .setItems(names, (d, i) -> {
                    int primary = (int) COLOR_PRESETS[i][1];
                    int accent  = (int) COLOR_PRESETS[i][2];
                    prefs.setPrimaryColor(primary);
                    prefs.setAccentColor(accent);
                    updateDot(primaryDot, primary);
                    updateDot(accentDot, accent);
                    Toast.makeText(requireContext(),
                        "Color scheme applied — restart app to see all changes",
                        Toast.LENGTH_SHORT).show();
                }).show();
        });
    }

    private void updateDot(View dot, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        dot.setBackground(gd);
    }

    // ── PLAYBACK ─────────────────────────────────────────────
    private void setupPlayback(View v) {
        TextView tvSpeed = v.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        v.findViewById(R.id.pref_speed).setOnClickListener(__ -> {
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","3.0×"};
            float[]  vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
            new AlertDialog.Builder(requireContext()).setTitle("Default Speed")
                .setItems(opts, (d, i) -> {
                    prefs.setPlaybackSpeed(vals[i]);
                    tvSpeed.setText(opts[i]);
                }).show();
        });

        TextView tvSkip = v.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + "s");
        v.findViewById(R.id.pref_skip).setOnClickListener(__ -> {
            String[] opts = {"5s","10s","15s","30s","60s"};
            int[]    vals = {5, 10, 15, 30, 60};
            new AlertDialog.Builder(requireContext()).setTitle("Skip Interval")
                .setItems(opts, (d, i) -> {
                    prefs.setSkipInterval(vals[i]);
                    tvSkip.setText(opts[i]);
                }).show();
        });

        bindSwitch(v, R.id.sw_resume, AppPreferences.KEY_RESUME_PLAYBACK, prefs.getResumePlayback());
    }

    // ── VIDEO ────────────────────────────────────────────────
    private void setupVideo(View v) {
        bindSwitch(v, R.id.sw_hw_accel,    AppPreferences.KEY_HW_ACCEL, prefs.getHwAcceleration());
        bindSwitch(v, R.id.sw_gestures,    AppPreferences.KEY_GESTURES, prefs.getGesturesEnabled());
        bindSwitch(v, R.id.sw_remember_pos,AppPreferences.KEY_REMEMBER_POS, prefs.getRememberPosition());
    }

    // ── FOLDERS ──────────────────────────────────────────────
    private void setupFolders(View v) {
        setupFolderPref(v, true);   // music
        setupFolderPref(v, false);  // video
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId   = isAudio ? R.id.tv_audio_folders  : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> saved = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
        tv.setText(saved.isEmpty() ? "All folders" : saved.size() + " selected");

        view.findViewById(prefId).setOnClickListener(vv ->
            new Thread(() -> {
                List<String> folders = isAudio
                    ? MediaScanner.getAudioFolders(requireContext())
                    : MediaScanner.getVideoFolders(requireContext());
                requireActivity().runOnUiThread(() -> {
                    if (folders.isEmpty()) {
                        Toast.makeText(requireContext(), "No folders found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] items = folders.toArray(new String[0]);
                    boolean[] checked = new boolean[items.length];
                    for (int i = 0; i < items.length; i++) checked[i] = saved.contains(items[i]);
                    new AlertDialog.Builder(requireContext())
                        .setTitle(isAudio ? "Music Folders" : "Video Folders")
                        .setMultiChoiceItems(items, checked, (d, i, c) -> {
                            if (c) saved.add(items[i]); else saved.remove(items[i]);
                        })
                        .setPositiveButton("Apply", (d, i) -> {
                            if (isAudio) prefs.setAudioFolders(saved);
                            else prefs.setVideoFolders(saved);
                            tv.setText(saved.isEmpty() ? "All folders" : saved.size() + " selected");
                        })
                        .setNegativeButton("Cancel", null).show();
                });
            }).start()
        );
    }

    // ── DATA ─────────────────────────────────────────────────
    private void setupData(View v) {
        v.findViewById(R.id.btn_clear_history).setOnClickListener(__ ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear History?")
                .setMessage("This will erase all play history.")
                .setPositiveButton("Clear", (d, i) ->
                    new Thread(() ->
                        com.fountainpdl.fountainplay.db.AppDatabase
                            .get(requireContext()).historyDao().clearAll()
                    ).start()
                )
                .setNegativeButton("Cancel", null).show()
        );
    }

    // ── Helpers ───────────────────────────────────────────────
    private void bindSwitch(View root, int swId, String key, boolean current) {
        SwitchCompat sw = root.findViewById(swId);
        if (sw == null) return;
        sw.setChecked(current);
        sw.setOnCheckedChangeListener((b, c) -> prefs.setBoolean(key, c));
    }

    private String themeLabel(String val) {
        switch (val) {
            case "light":  return "Light";
            case "amoled": return "AMOLED";
            case "system": return "Follow System";
            default:       return "Dark";
        }
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return 0;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 3. MusicFragment — fixed delete (MediaStore), fixed mini-player
#    button, context menu fully wired
# ════════════════════════════════════════════════════════════
cat > $P/ui/music/MusicFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.music;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class MusicFragment extends Fragment {

    private MediaAdapter adapter;
    private final List<MediaItem> songs = new ArrayList<>();
    private String currentSort = "name";
    private boolean sortAscending = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_music, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv    = view.findViewById(R.id.rv_songs);
        TextView tvCount   = view.findViewById(R.id.tv_song_count);
        SearchView sv      = view.findViewById(R.id.search_view);

        adapter = new MediaAdapter(songs, 0);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // ── Tap: open player ──────────────────────────────────
        adapter.setOnItemClickListener((item, pos) -> openPlayer(item));

        // ── Long-press: full context menu ─────────────────────
        adapter.setOnItemLongListener((item, pos, anchor) ->
            showSongMenu(item, pos));

        // ── Shuffle All ───────────────────────────────────────
        view.findViewById(R.id.btn_shuffle_all).setOnClickListener(v -> {
            if (songs.isEmpty()) return;
            List<MediaItem> shuffled = new ArrayList<>(songs);
            Collections.shuffle(shuffled);
            PlayQueue.get().setQueue(shuffled, 0);
            PlayQueue.get().setShuffle(true);
            openPlayer(shuffled.get(0));
        });

        // ── Sort ─────────────────────────────────────────────
        view.findViewById(R.id.btn_sort).setOnClickListener(v -> showSortDialog());

        // ── Refresh ───────────────────────────────────────────
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> loadSongs(tvCount));

        // ── Search ────────────────────────────────────────────
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadSongs(tvCount);
    }

    private void loadSongs(TextView tvCount) {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders  = prefs.getAudioFolders();
            List<MediaItem> result = MediaScanner.scanAudio(
                requireContext(), folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                songs.clear();
                songs.addAll(result);
                adapter.updateAll(result);
                adapter.sort(currentSort);
                tvCount.setText(songs.size() + " songs");
            });
        }).start();
    }

    private void openPlayer(MediaItem item) {
        int idx = songs.indexOf(item);
        PlayQueue.get().setQueue(songs, idx < 0 ? 0 : idx);
        Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
        i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
        i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
        i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
        i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        startActivity(i);
    }

    private void showSortDialog() {
        String[] labels = {"Name ↑","Name ↓","Artist","Date Added","Duration","Size","Folder"};
        String[] keys   = {"name_asc","name_desc","artist","date","duration","size","folder"};
        new AlertDialog.Builder(requireContext()).setTitle("Sort by")
            .setItems(labels, (d, i) -> {
                currentSort = keys[i];
                if (currentSort.endsWith("_desc")) {
                    adapter.sort(currentSort.replace("_desc",""));
                    adapter.reverse();
                } else {
                    adapter.sort(currentSort);
                }
            }).show();
    }

    private void showSongMenu(MediaItem item, int pos) {
        String[] opts = {
            "▶  Play",
            "⏭  Play Next",
            "➕  Add to Queue",
            "📋  Add to Playlist",
            "⭐  Add to Favourites",
            "↗  Share",
            "ℹ  Info",
            "🗑  Delete"
        };
        new AlertDialog.Builder(requireContext())
            .setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0: openPlayer(item); break;
                    case 1:
                        PlayQueue.get().addNext(item);
                        toast("Will play next");
                        break;
                    case 2:
                        PlayQueue.get().addToQueue(item);
                        toast("Added to queue");
                        break;
                    case 3: showAddToPlaylistDialog(item); break;
                    case 4: addToFavourites(item); break;
                    case 5: shareItem(item); break;
                    case 6: showInfo(item); break;
                    case 7: confirmDelete(item, pos); break;
                }
            }).show();
    }

    private void showAddToPlaylistDialog(MediaItem item) {
        new Thread(() -> {
            List<PlaylistEntity> playlists =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) {
                    toast("No playlists — create one in Library");
                    return;
                }
                String[] names = new String[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;
                new AlertDialog.Builder(requireContext()).setTitle("Add to Playlist")
                    .setItems(names, (d, i) -> addToPlaylist(item, playlists.get(i)))
                    .show();
            });
        }).start();
    }

    private void addToPlaylist(MediaItem item, PlaylistEntity playlist) {
        new Thread(() -> {
            PlaylistSong s = new PlaylistSong();
            s.playlistId = playlist.id;
            s.path = item.getPath(); s.title = item.getTitle();
            s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
            s.duration = item.getDuration();
            AppDatabase.get(requireContext()).playlistDao().insertSong(s);
            AppDatabase.get(requireContext()).playlistDao().updateCount(playlist.id);
            requireActivity().runOnUiThread(() -> toast("Added to " + playlist.name));
        }).start();
    }

    private void addToFavourites(MediaItem item) {
        // Create/find a "Favourites" playlist and add to it
        new Thread(() -> {
            List<PlaylistEntity> all =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            PlaylistEntity fav = null;
            for (PlaylistEntity p : all) {
                if ("Favourites".equals(p.name)) { fav = p; break; }
            }
            if (fav == null) {
                PlaylistEntity newFav = new PlaylistEntity();
                newFav.name = "Favourites";
                newFav.createdAt = System.currentTimeMillis();
                long id = AppDatabase.get(requireContext()).playlistDao().insertPlaylist(newFav);
                newFav.id = (int) id;
                fav = newFav;
            }
            PlaylistSong s = new PlaylistSong();
            s.playlistId = fav.id;
            s.path = item.getPath(); s.title = item.getTitle();
            s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
            s.duration = item.getDuration();
            AppDatabase.get(requireContext()).playlistDao().insertSong(s);
            AppDatabase.get(requireContext()).playlistDao().updateCount(fav.id);
            requireActivity().runOnUiThread(() -> toast("Added to Favourites ⭐"));
        }).start();
    }

    private void shareItem(MediaItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
        share.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " - " + item.getArtist());
        startActivity(Intent.createChooser(share, "Share"));
    }

    private void showInfo(MediaItem item) {
        String info = "Title:    " + item.getTitle()
            + "\nArtist:   " + item.getArtist()
            + "\nAlbum:    " + item.getAlbum()
            + "\nDuration: " + item.getFormattedDuration()
            + "\nSize:     " + (item.getSize() / 1024 / 1024) + " MB"
            + "\nFolder:   " + item.getFolder()
            + "\nPath:     " + item.getPath();
        new AlertDialog.Builder(requireContext())
            .setTitle("File Info")
            .setMessage(info)
            .setPositiveButton("OK", null).show();
    }

    private void confirmDelete(MediaItem item, int pos) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete file?")
            .setMessage("\"" + item.getTitle() + "\" will be permanently deleted.")
            .setPositiveButton("Delete", (d, i) -> deleteFile(item, pos))
            .setNegativeButton("Cancel", null).show();
    }

    private void deleteFile(MediaItem item, int pos) {
        new Thread(() -> {
            boolean deleted = false;
            // Try MediaStore first (works on Android 10+)
            try {
                Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                int rows = requireContext().getContentResolver().delete(
                    collection,
                    MediaStore.Audio.Media.DATA + "=?",
                    new String[]{item.getPath()});
                deleted = rows > 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Fallback: direct file delete
            if (!deleted) {
                java.io.File f = new java.io.File(item.getPath());
                deleted = f.exists() && f.delete();
            }
            final boolean success = deleted;
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    songs.remove(item);
                    adapter.updateAll(new ArrayList<>(songs));
                    toast("Deleted");
                } else {
                    toast("Could not delete — try a file manager");
                }
            });
        }).start();
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 4. MediaAdapter — add reverse() method
# ════════════════════════════════════════════════════════════
# Append reverse() to the existing sort() block
python3 << 'PYEOF'
with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","r") as f:
    c = f.read()
if "public void reverse()" not in c:
    c = c.replace(
        "        notifyDataSetChanged();\n    }\n\n    // ── Selection",
        "        notifyDataSetChanged();\n    }\n\n    public void reverse() { Collections.reverse(items); notifyDataSetChanged(); }\n\n    // ── Selection"
    )
    with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","w") as f:
        f.write(c)
    print("✅ reverse() added to MediaAdapter")
else:
    print("✅ reverse() already present")
PYEOF

# ════════════════════════════════════════════════════════════
# 5. MainActivity — extend BaseActivity, fix mini player
# ════════════════════════════════════════════════════════════
cat > $P/MainActivity.java << 'EOF'
package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class MainActivity extends BaseActivity implements PlaybackState.Listener {

    private NavController navController;
    private static final int PERM = 100;

    private PlaybackService service;
    private boolean bound = false;

    private CardView miniCard;
    private ImageView miniArt;
    private TextView  miniTitle, miniArtist;
    private ImageButton miniPlay, miniPrev, miniNext, miniClose;
    private ProgressBar miniProgress;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound   = true;
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) showMiniPlayer(cur);
        }
        @Override public void onServiceDisconnected(ComponentName n) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment host = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = host.getNavController();
        NavigationUI.setupWithNavController(
            findViewById(R.id.bottom_nav), navController);

        setupMiniPlayer();
        requestPermissions();
        PlaybackState.get().addListener(this);

        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, conn, BIND_AUTO_CREATE);
    }

    private void setupMiniPlayer() {
        miniCard     = findViewById(R.id.mini_player);
        miniArt      = miniCard.findViewById(R.id.mini_album_art);
        miniTitle    = miniCard.findViewById(R.id.mini_title);
        miniArtist   = miniCard.findViewById(R.id.mini_artist);
        miniPlay     = miniCard.findViewById(R.id.mini_play_pause);
        miniPrev     = miniCard.findViewById(R.id.mini_prev);
        miniNext     = miniCard.findViewById(R.id.mini_next);
        miniClose    = miniCard.findViewById(R.id.mini_close);
        miniProgress = miniCard.findViewById(R.id.mini_progress);

        miniCard.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            Intent i = new Intent(this, AudioPlayerActivity.class);
            i.putExtra(AudioPlayerActivity.EXTRA_URI, cur.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, cur.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, cur.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, cur.getAlbumArtUri());
            i.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
            startActivity(i);
        });

        miniPlay.setOnClickListener(v -> {
            if (bound) { service.playPause(); pulse(miniPlay); }
        });
        miniPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });
        miniNext.setOnClickListener(v -> { if (bound) service.skipNext(); });
        miniClose.setOnClickListener(v -> {
            if (bound) service.getPlayer().pause();
            PlaybackState.get().setCurrentItem(null);
            miniCard.setVisibility(View.GONE);
        });
    }

    public void showMiniPlayer(MediaItem item) {
        if (item == null || !item.isAudio()) return;
        miniTitle.setText(item.getTitle());
        miniArtist.setText(item.getArtist());
        if (item.getAlbumArtUri() != null)
            Glide.with(this).load(item.getAlbumArtUri()).centerCrop()
                .placeholder(R.drawable.bg_play_button).into(miniArt);
        if (miniCard.getVisibility() != View.VISIBLE) {
            miniCard.setAlpha(0f);
            miniCard.setTranslationY(40f);
            miniCard.setVisibility(View.VISIBLE);
            miniCard.animate().alpha(1f).translationY(0f).setDuration(220).start();
        }
    }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f,1.2f,1f,1.2f,
            Animation.RELATIVE_TO_SELF,.5f, Animation.RELATIVE_TO_SELF,.5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> { if (item != null && item.isAudio()) showMiniPlayer(item); });
    }
    @Override public void onPlayStateChanged(boolean playing) {
        runOnUiThread(() -> miniPlay.setImageResource(
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
    }
    @Override public void onPositionChanged(long pos, long dur) {
        if (dur > 0)
            runOnUiThread(() -> miniProgress.setProgress((int)(pos * 1000 / dur)));
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] p = {Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
            boolean ok = true;
            for (String s : p) if (ContextCompat.checkSelfPermission(this,s)!=PackageManager.PERMISSION_GRANTED){ok=false;break;}
            if (!ok) ActivityCompat.requestPermissions(this, p, PERM);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM);
        }
    }

    @Override public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        PlaybackState.get().removeListener(this);
        if (bound) { unbindService(conn); bound = false; }
    }
}
EOF

echo ""
echo "✅ PART B DONE — run fp_v4C.sh next"
