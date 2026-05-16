package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.databinding.ActivityMainBinding;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class MainActivity extends AppCompatActivity implements PlaybackState.Listener {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final int PERMISSION_REQUEST = 100;

    // Mini player views
    private View miniPlayer;
    private ImageView miniArt;
    private TextView miniTitle, miniArtist;
    private ImageButton miniPlayPause, miniPrev, miniNext, miniClose;
    private ProgressBar miniProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen + keep screen on for video
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupMiniPlayer();
        requestStoragePermissions();

        PlaybackState.get().addListener(this);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }

    private void setupMiniPlayer() {
        miniPlayer = binding.miniPlayer;
        miniArt = miniPlayer.findViewById(R.id.mini_album_art);
        miniTitle = miniPlayer.findViewById(R.id.mini_title);
        miniArtist = miniPlayer.findViewById(R.id.mini_artist);
        miniPlayPause = miniPlayer.findViewById(R.id.mini_play_pause);
        miniPrev = miniPlayer.findViewById(R.id.mini_prev);
        miniNext = miniPlayer.findViewById(R.id.mini_next);
        miniClose = miniPlayer.findViewById(R.id.mini_close);
        miniProgress = miniPlayer.findViewById(R.id.mini_progress);

        // Tap mini player → reopen audio player
        miniPlayer.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.isAudio()) {
                Intent i = new Intent(this, AudioPlayerActivity.class);
                i.putExtra(AudioPlayerActivity.EXTRA_URI, cur.getPath());
                i.putExtra(AudioPlayerActivity.EXTRA_TITLE, cur.getTitle());
                i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, cur.getArtist());
                i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, cur.getAlbumArtUri());
                i.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
                startActivity(i);
            }
        });

        miniClose.setOnClickListener(v -> {
            miniPlayer.setVisibility(View.GONE);
            PlaybackState.get().setCurrentItem(null);
        });

        // Play/pause, prev, next wired via PlaybackState broadcast
        miniPlayPause.setOnClickListener(v -> {
            // Signal the active player via broadcast
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.TOGGLE_PLAY"));
        });
        miniNext.setOnClickListener(v ->
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.NEXT")));
        miniPrev.setOnClickListener(v ->
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.PREV")));
    }

    public void showMiniPlayer(MediaItem item) {
        if (item == null) return;
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(item.getTitle());
        miniArtist.setText(item.getArtist());
        if (item.getAlbumArtUri() != null) {
            Glide.with(this).load(item.getAlbumArtUri()).centerCrop().into(miniArt);
        } else {
            miniArt.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void updateMiniProgress(int progress) {
        miniProgress.setProgress(progress);
    }

    public void hideMiniPlayer() {
        miniPlayer.setVisibility(View.GONE);
    }

    @Override
    public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> {
            if (item != null) showMiniPlayer(item);
            else hideMiniPlayer();
        });
    }

    @Override
    public void onPlayStateChanged(boolean playing) {
        runOnUiThread(() -> miniPlayPause.setImageResource(
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
    }

    @Override
    public void onPositionChanged(long pos, long duration) {
        if (duration > 0) runOnUiThread(() ->
            updateMiniProgress((int)(pos * 1000 / duration)));
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] perms = {Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
            boolean allGranted = true;
            for (String p : perms)
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            if (!allGranted) ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        } else {
            String p = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{p}, PERMISSION_REQUEST);
        }
    }

    @Override public boolean onSupportNavigateUp() { return navController.navigateUp() || super.onSupportNavigateUp(); }
    @Override protected void onDestroy() { super.onDestroy(); PlaybackState.get().removeListener(this); }
}
