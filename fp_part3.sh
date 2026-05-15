#!/bin/bash
# ── PART 3: Java Source Files ──
# Run from inside ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

cat > $P/MainActivity.java << 'EOF'
package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.fountainpdl.fountainplay.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private static final int PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavigation();
        requestStoragePermissions();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] perms = { Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO };
            boolean allGranted = true;
            for (String p : perms) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false; break;
                }
            }
            if (!allGranted) ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        } else {
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
EOF

cat > $P/model/MediaItem.java << 'EOF'
package com.fountainpdl.fountainplay.model;

public class MediaItem {
    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_VIDEO = 1;

    private long id;
    private String title, artist, album, path, albumArtUri;
    private long duration, size;
    private int type;

    public MediaItem() {}
    public MediaItem(long id, String title, String artist, String path, long duration, int type) {
        this.id = id; this.title = title; this.artist = artist;
        this.path = path; this.duration = duration; this.type = type;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getArtist() { return artist; }
    public void setArtist(String a) { this.artist = a; }
    public String getAlbum() { return album; }
    public void setAlbum(String a) { this.album = a; }
    public String getPath() { return path; }
    public void setPath(String p) { this.path = p; }
    public long getDuration() { return duration; }
    public void setDuration(long d) { this.duration = d; }
    public long getSize() { return size; }
    public void setSize(long s) { this.size = s; }
    public int getType() { return type; }
    public void setType(int t) { this.type = t; }
    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String u) { this.albumArtUri = u; }
    public boolean isAudio() { return type == TYPE_AUDIO; }
    public boolean isVideo() { return type == TYPE_VIDEO; }

    public String getFormattedDuration() {
        long s = duration / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
}
EOF

cat > $P/util/MediaScanner.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

public class MediaScanner {

    public static List<MediaItem> scanAudio(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.ALBUM_ID
        };
        try (Cursor c = cr.query(uri, proj, null, null, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            while (c.moveToNext()) {
                long dur = c.getLong(iDur);
                if (dur < 1000) continue;
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    c.getString(iArtist), c.getString(iPath), dur, MediaItem.TYPE_AUDIO);
                item.setAlbum(c.getString(iAlbum));
                item.setSize(c.getLong(iSize));
                item.setAlbumArtUri(Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    String.valueOf(c.getLong(iAlbumId))).toString());
                items.add(item);
            }
        }
        return items;
    }

    public static List<MediaItem> scanVideo(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE
        };
        try (Cursor c = cr.query(uri, proj, null, null, MediaStore.Video.Media.DATE_MODIFIED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            while (c.moveToNext()) {
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    "", c.getString(iPath), c.getLong(iDur), MediaItem.TYPE_VIDEO);
                item.setSize(c.getLong(iSize));
                items.add(item);
            }
        }
        return items;
    }
}
EOF

cat > $P/service/PlaybackService.java << 'EOF'
package com.fountainpdl.fountainplay.service;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession;
    private ExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA).build();
        player = new ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true).build();
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo info) { return mediaSession; }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
EOF

cat > $P/ui/home/HomeFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.home;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.databinding.FragmentHomeBinding;
import java.util.Calendar;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = h < 12 ? "Good morning ☀️" : h < 17 ? "Good afternoon 🎵" : "Good evening 🌙";
        binding.tvGreeting.setText(greet);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
EOF

cat > $P/ui/music/MusicFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.music;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;

public class MusicFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Music Library — scanning...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        new Thread(() -> {
            int count = MediaScanner.scanAudio(requireContext()).size();
            requireActivity().runOnUiThread(() -> tv.setText("🎵 " + count + " audio files found"));
        }).start();
        return tv;
    }
}
EOF

cat > $P/ui/video/VideoFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.video;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;

public class VideoFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Video Library — scanning...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        new Thread(() -> {
            int count = MediaScanner.scanVideo(requireContext()).size();
            requireActivity().runOnUiThread(() -> tv.setText("🎬 " + count + " video files found"));
        }).start();
        return tv;
    }
}
EOF

cat > $P/ui/library/LibraryFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

public class LibraryFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("📚 Library — Playlists, History, Downloads");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        return tv;
    }
}
EOF

cat > $P/player/VideoPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.R;

public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "media_uri";
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) { finish(); return; }

        PlayerView playerView = findViewById(R.id.player_view);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onPause() { super.onPause(); player.pause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (player != null) { player.release(); player = null; } }
}
EOF

cat > $P/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.fountainpdl.fountainplay.R;

public class AudioPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) { finish(); return; }
        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onDestroy() { super.onDestroy(); if (player != null) { player.release(); player = null; } }
}
EOF

echo ""
echo "✅ ALL PARTS DONE!"
echo ""
echo "Now push to GitHub:"
echo "  git add ."
echo "  git commit -m 'feat: full project scaffold'"
echo "  git push"
echo ""
echo "Then check GitHub → Actions tab for your APK build 🎉"
