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
