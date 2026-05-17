package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.databinding.ActivityMainBinding;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class MainActivity extends AppCompatActivity implements PlaybackState.Listener {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final int PERM_REQUEST = 100;

    private PlaybackService service;
    private boolean bound = false;

    // Mini player views
    private CardView miniPlayerCard;
    private ImageView miniArt;
    private TextView miniTitle, miniArtist;
    private ImageButton miniPlayPause, miniPrev, miniNext, miniClose;
    private ProgressBar miniProgress;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound = true;
            // Restore mini player if something is playing
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) showMiniPlayer(cur);
        }
        @Override public void onServiceDisconnected(ComponentName n) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupMiniPlayer();
        requestPermissions();

        PlaybackState.get().addListener(this);

        // Bind to persistent service
        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, conn, BIND_AUTO_CREATE);
    }

    private void setupNavigation() {
        NavHostFragment host = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }

    private void setupMiniPlayer() {
        miniPlayerCard = findViewById(R.id.mini_player);
        miniArt        = miniPlayerCard.findViewById(R.id.mini_album_art);
        miniTitle      = miniPlayerCard.findViewById(R.id.mini_title);
        miniArtist     = miniPlayerCard.findViewById(R.id.mini_artist);
        miniPlayPause  = miniPlayerCard.findViewById(R.id.mini_play_pause);
        miniPrev       = miniPlayerCard.findViewById(R.id.mini_prev);
        miniNext       = miniPlayerCard.findViewById(R.id.mini_next);
        miniClose      = miniPlayerCard.findViewById(R.id.mini_close);
        miniProgress   = miniPlayerCard.findViewById(R.id.mini_progress);

        // Tap mini player → open full audio player
        miniPlayerCard.setOnClickListener(v -> {
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

        miniPlayPause.setOnClickListener(v -> {
            if (bound) {
                service.playPause();
                pulse(miniPlayPause);
            }
        });

        miniPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });
        miniNext.setOnClickListener(v -> { if (bound) service.skipNext(); });

        miniClose.setOnClickListener(v -> {
            if (bound) { service.getPlayer().stop(); }
            PlaybackState.get().setCurrentItem(null);
            miniPlayerCard.setVisibility(View.GONE);
        });
    }

    public void showMiniPlayer(MediaItem item) {
        if (item == null || !item.isAudio()) return;
        miniPlayerCard.setVisibility(View.VISIBLE);
        miniTitle.setText(item.getTitle());
        miniArtist.setText(item.getArtist());
        if (item.getAlbumArtUri() != null)
            Glide.with(this).load(item.getAlbumArtUri()).centerCrop()
                .placeholder(R.drawable.bg_play_button).into(miniArt);
        else miniArt.setImageResource(R.drawable.bg_play_button);

        // Animate in
        miniPlayerCard.setAlpha(0f);
        miniPlayerCard.setTranslationY(40f);
        miniPlayerCard.animate().alpha(1f).translationY(0f).setDuration(250).start();
    }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f,1.25f,1f,1.25f,
            Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> { if (item != null && item.isAudio()) showMiniPlayer(item); });
    }

    @Override public void onPlayStateChanged(boolean playing) {
        runOnUiThread(() -> miniPlayPause.setImageResource(
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
    }

    @Override public void onPositionChanged(long pos, long dur) {
        if (dur > 0) runOnUiThread(() ->
            miniProgress.setProgress((int)(pos * 1000 / dur)));
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] p = {Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
            boolean ok = true;
            for (String s : p) if (ContextCompat.checkSelfPermission(this,s)!=PackageManager.PERMISSION_GRANTED){ok=false;break;}
            if (!ok) ActivityCompat.requestPermissions(this, p, PERM_REQUEST);
        } else {
            String p = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this,p)!=PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,new String[]{p},PERM_REQUEST);
        }
    }

    @Override public boolean onSupportNavigateUp() { return navController.navigateUp()||super.onSupportNavigateUp(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        PlaybackState.get().removeListener(this);
        if (bound) { unbindService(conn); bound = false; }
    }
}
