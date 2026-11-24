package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements PlaybackState.Listener {

    private static final int PERM = 100;
    private static final String PREF_ASKED_NOTIFICATIONS = "asked_post_notifications";

    private NavController navController;
    private BottomNavigationView bottomNav;
    private boolean mediaGrantedAtStart;

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
            setPlayIcon(service.isPlaying());
        }
        @Override public void onServiceDisconnected(ComponentName n) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupEdgeToEdge();

        NavHostFragment host = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = host.getNavController();
        bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        setupBackNavigation();
        setupMiniPlayer();
        mediaGrantedAtStart = hasMediaPermission();
        if (mediaGrantedAtStart) LibraryRepo.get(this).ensureLoaded();
        requestPermissions();
        PlaybackState.get().addListener(this);

        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, conn, BIND_AUTO_CREATE);
    }

    /** Draw behind the system bars, then pad the content so nothing is hidden under them. */
    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true);
        boolean light = ColorUtils.calculateLuminance(tv.data) > 0.5;
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(light);
        c.setAppearanceLightNavigationBars(light);

        final View root = findViewById(R.id.main_root);
        final BottomNavigationView nav = findViewById(R.id.bottom_nav);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            root.setPadding(sb.left, sb.top, sb.right, ime > 0 ? ime : 0);
            nav.setPadding(0, 0, 0, ime > 0 ? 0 : sb.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Back: any tab goes to Home first, Home exits. */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                int dest = navController.getCurrentDestination() == null
                    ? R.id.nav_home : navController.getCurrentDestination().getId();
                if (dest == R.id.nav_history || dest == R.id.nav_manage_folders) {
                    navController.popBackStack();                 // sub-screens go back to where they came from
                } else if (dest != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);   // tabs go Home first
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
            i.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
            startActivity(i);
        });

        miniPlay.setOnClickListener(v -> { if (bound) { service.playPause(); pulse(miniPlay); } });
        miniPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });
        miniNext.setOnClickListener(v -> { if (bound) service.skipNext(); });
        miniClose.setOnClickListener(v -> {
            if (bound) service.stopPlayback();
            miniCard.setVisibility(View.GONE);
        });
    }

    public void showMiniPlayer(MediaItem item) {
        if (item == null || !item.isAudio()) return;
        miniTitle.setText(item.getTitle());
        miniArtist.setText(item.getArtist());
        if (item.getAlbumArtUri() != null)
            Glide.with(this).load(item.getAlbumArtUri()).centerCrop()
                .placeholder(R.drawable.bg_art_placeholder).into(miniArt);
        if (miniCard.getVisibility() != View.VISIBLE) {
            miniCard.setAlpha(0f);
            miniCard.setTranslationY(40f);
            miniCard.setVisibility(View.VISIBLE);
            miniCard.animate().alpha(1f).translationY(0f).setDuration(220).start();
        }
    }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    private void setPlayIcon(boolean playing) {
        miniPlay.setImageResource(playing
            ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> {
            if (item == null) miniCard.setVisibility(View.GONE);
            else if (item.isAudio()) showMiniPlayer(item);
        });
    }
    @Override public void onPlayStateChanged(boolean playing) { runOnUiThread(() -> setPlayIcon(playing)); }
    @Override public void onPositionChanged(long pos, long dur) {
        if (dur > 0) runOnUiThread(() -> miniProgress.setProgress((int) (pos * 1000 / dur)));
    }

    // ── Permissions ─────────────────────────────────────────────────────────────

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return granted(Manifest.permission.READ_MEDIA_AUDIO)
                && granted(Manifest.permission.READ_MEDIA_VIDEO);
        }
        return granted(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private boolean granted(String p) {
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
    }

    /** Media access, plus the notification permission (Android 13+), which upgrade installs never had. */
    private void requestPermissions() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!granted(Manifest.permission.READ_MEDIA_AUDIO)) need.add(Manifest.permission.READ_MEDIA_AUDIO);
            if (!granted(Manifest.permission.READ_MEDIA_VIDEO)) need.add(Manifest.permission.READ_MEDIA_VIDEO);
            AppPreferences prefs = new AppPreferences(this);
            int asks = prefs.getInt("notification_permission_asks", 0);
            if (!granted(Manifest.permission.POST_NOTIFICATIONS) && asks < 3) {
                need.add(Manifest.permission.POST_NOTIFICATIONS);
                prefs.setInt("notification_permission_asks", asks + 1);
            }
        } else if (!granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Lists were scanned before access existed: scan again now that access is granted.
        if (requestCode == PERM && !mediaGrantedAtStart && hasMediaPermission()) {
            mediaGrantedAtStart = true;
            LibraryRepo.get(this).refresh();
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
