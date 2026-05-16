package com.fountainpdl.fountainplay.player;

import android.app.PictureInPictureParams;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";

    private ExoPlayer player;
    private PlayerView playerView;
    private AppPreferences prefs;
    private AudioManager audioManager;

    // Gesture
    private float touchStartX, touchStartY;
    private long seekStartPos;
    private int startVolume, startBrightness;
    private boolean isLocked = false;
    private boolean gesturesEnabled = true;

    // Overlay views
    private View overlayLeft, overlayRight;
    private TextView tvVolume, tvBrightness, tvSeekIndicator;
    private ImageButton btnLock, btnPip;
    private Handler hideHandler = new Handler(Looper.getMainLooper());

    private static final int GESTURE_SEEK = 0, GESTURE_VOLUME = 1, GESTURE_BRIGHTNESS = 2;
    private int activeGesture = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        prefs = new AppPreferences(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        playerView    = findViewById(R.id.player_view);
        overlayLeft   = findViewById(R.id.overlay_left);
        overlayRight  = findViewById(R.id.overlay_right);
        tvVolume      = findViewById(R.id.tv_volume);
        tvBrightness  = findViewById(R.id.tv_brightness);
        tvSeekIndicator = findViewById(R.id.tv_seek_indicator);
        btnLock       = findViewById(R.id.btn_lock);
        btnPip        = findViewById(R.id.btn_pip);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        Uri uri;
        if (uriStr != null) uri = Uri.parse(uriStr);
        else if (getIntent().getData() != null) uri = getIntent().getData();
        else { finish(); return; }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
        player.setPlaybackSpeed(prefs.getPlaybackSpeed());

        setupGestures();

        btnPip.setOnClickListener(v -> enterPiP());

        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnLock.setImageResource(isLocked
                ? android.R.drawable.ic_lock_lock
                : android.R.drawable.ic_lock_idle_lock);
            playerView.setUseController(!isLocked);
        });

        // Show lock button when controls appear
        playerView.setControllerVisibilityListener(
            (androidx.media3.ui.PlayerView.ControllerVisibilityListener) visibility -> {
                btnLock.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
                btnPip.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            });
    }

    private void setupGestures() {
        if (!gesturesEnabled) return;

        playerView.setOnTouchListener((v, event) -> {
            if (isLocked) return false;

            int screenW = getWindow().getDecorView().getWidth();
            int screenH = getWindow().getDecorView().getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    seekStartPos = player.getCurrentPosition();
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = getBrightness();
                    activeGesture = -1;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;

                    if (activeGesture == -1) {
                        if (Math.abs(dx) > Math.abs(dy) * 1.5f && Math.abs(dx) > 30)
                            activeGesture = GESTURE_SEEK;
                        else if (Math.abs(dy) > 30)
                            activeGesture = touchStartX < screenW / 2 ? GESTURE_BRIGHTNESS : GESTURE_VOLUME;
                    }

                    if (activeGesture == GESTURE_SEEK) {
                        long seekDelta = (long)(dx * 300); // 300ms per pixel
                        long newPos = Math.max(0, Math.min(player.getDuration(), seekStartPos + seekDelta));
                        String sign = seekDelta >= 0 ? "+" : "";
                        showSeekIndicator(sign + (seekDelta / 1000) + "s → " + fmtTime(newPos));
                        player.seekTo(newPos);

                    } else if (activeGesture == GESTURE_VOLUME) {
                        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int newVol = (int) Math.max(0, Math.min(maxVol, startVolume - dy / screenH * maxVol * 2));
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                        int pct = (int)(newVol * 100f / maxVol);
                        showVolumeIndicator(pct + "%");

                    } else if (activeGesture == GESTURE_BRIGHTNESS) {
                        int newBrightness = (int) Math.max(0, Math.min(255, startBrightness - dy / screenH * 255 * 2));
                        setBrightness(newBrightness);
                        showBrightnessIndicator((int)(newBrightness * 100f / 255) + "%");
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    hideOverlays();
                    activeGesture = -1;
                    // If tiny movement = tap = toggle controls
                    if (Math.abs(event.getX() - touchStartX) < 10 && Math.abs(event.getY() - touchStartY) < 10) {
                        v.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    private void showSeekIndicator(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }

    private void showVolumeIndicator(String pct) {
        tvVolume.setText(pct);
        overlayRight.setVisibility(View.VISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }

    private void showBrightnessIndicator(String pct) {
        tvBrightness.setText(pct);
        overlayLeft.setVisibility(View.VISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }

    private void hideOverlays() {
        hideHandler.postDelayed(() -> {
            overlayLeft.setVisibility(View.INVISIBLE);
            overlayRight.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE);
        }, 800);
    }

    private int getBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int)(lp.screenBrightness * 255);
    }

    private void setBrightness(int value) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = value / 255f;
        getWindow().setAttributes(lp);
    }

    private String fmtTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    private void enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onUserLeaveHint() {
        // Auto-enter PiP when user presses home during video
        super.onUserLeaveHint();
        if (player != null && player.isPlaying()) {
            enterPiP();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode,
                                               android.content.res.Configuration config) {
        super.onPictureInPictureModeChanged(isInPiPMode, config);
        // Hide/show UI elements in PiP mode
        playerView.setUseController(!isInPiPMode);
        btnPip.setVisibility(isInPiPMode ? View.GONE : View.VISIBLE);
    }

    @Override protected void onPause() {
        super.onPause();
        // Don't pause if entering PiP
        if (!isInPictureInPictureMode() && player != null) player.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null && !isInPictureInPictureMode()) player.play();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
