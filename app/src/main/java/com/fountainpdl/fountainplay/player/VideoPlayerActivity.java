package com.fountainpdl.fountainplay.player;

import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.BaseActivity;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.google.android.material.chip.Chip;

public class VideoPlayerActivity extends BaseActivity {

    public static final String EXTRA_URI   = "media_uri";
    public static final String EXTRA_TITLE = "media_title";

    private ExoPlayer player;
    private PlayerView playerView;
    private AppPreferences prefs;
    private AudioManager audio;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Controls
    private View controlsOverlay;
    private ImageButton btnBack, btnPlay, btnRew, btnFf, btnLock, btnPip, btnMore;
    private ImageButton btnPrevVideo, btnNextVideo;
    private SeekBar seekBar;
    private TextView tvPos, tvDur, tvTitle, tvSeekIndicator;
    private View overlayLeft, overlayRight;
    private TextView tvBrightness, tvVolume;
    private Chip chipSpeed, chipRatio, chipSubtitle, chipAudio, chipSleep;

    // State
    private boolean isLocked = false;
    private boolean controlsVisible = true;
    private boolean isUserSeeking = false;
    private boolean gesturesEnabled = true;

    // Gesture tracking
    private float touchStartX, touchStartY;
    private long seekStart;
    private int startVolume, startBrightness;
    private int activeGesture = -1; // 0=seek 1=vol 2=bright
    private static final int G_SEEK = 0, G_VOL = 1, G_BRIGHT = 2;

    // Aspect ratio cycling
    private int[] ratioModes = {
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    };
    private String[] ratioLabels = {"Fit","Fill","Zoom","Fixed W"};
    private int ratioIndex = 0;

    private final Runnable hideControls = () -> {
        if (!isLocked) setControlsVisible(false);
    };

    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (player != null && !isUserSeeking) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    seekBar.setProgress((int)(pos * 1000 / dur));
                    tvPos.setText(fmt(pos));
                    tvDur.setText(fmt(dur));
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        prefs = new AppPreferences(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        bindViews();
        setupPlayer();
        setupControls();
        if (gesturesEnabled) setupGestures();

        handler.post(progressUpdater);
        scheduleHideControls();
    }

    private void bindViews() {
        playerView      = findViewById(R.id.player_view);
        controlsOverlay = findViewById(R.id.controls_overlay);
        btnBack         = findViewById(R.id.btn_back);
        btnPlay         = findViewById(R.id.btn_play_video);
        btnRew          = findViewById(R.id.btn_rew);
        btnFf           = findViewById(R.id.btn_ff);
        btnLock         = findViewById(R.id.btn_lock);
        btnPip          = findViewById(R.id.btn_pip);
        btnMore         = findViewById(R.id.btn_more);
        btnPrevVideo    = findViewById(R.id.btn_prev_video);
        btnNextVideo    = findViewById(R.id.btn_next_video);
        seekBar         = findViewById(R.id.video_seek_bar);
        tvPos           = findViewById(R.id.tv_pos);
        tvDur           = findViewById(R.id.tv_dur);
        tvTitle         = findViewById(R.id.tv_video_title);
        tvSeekIndicator = findViewById(R.id.tv_seek_indicator);
        overlayLeft     = findViewById(R.id.overlay_left);
        overlayRight    = findViewById(R.id.overlay_right);
        tvBrightness    = findViewById(R.id.tv_brightness_val);
        tvVolume        = findViewById(R.id.tv_volume_val);
        chipSpeed       = findViewById(R.id.chip_speed_video);
        chipRatio       = findViewById(R.id.chip_ratio);
        chipSubtitle    = findViewById(R.id.chip_subtitle);
        chipAudio       = findViewById(R.id.chip_audio_track);
        chipSleep       = findViewById(R.id.chip_sleep_video);
    }

    private void setupPlayer() {
        Uri uri;
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr != null) uri = Uri.parse(uriStr);
        else if (getIntent().getData() != null) uri = getIntent().getData();
        else { finish(); return; }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (tvTitle != null && title != null) tvTitle.setText(title);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
        player.setPlaybackSpeed(prefs.getPlaybackSpeed());

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) {
                btnPlay.setImageResource(playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) tvDur.setText(fmt(player.getDuration()));
            }
        });
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause(); else player.play();
            scheduleHideControls();
        });

        int skip = prefs.getSkipInterval() * 1000;
        btnRew.setOnClickListener(v -> {
            player.seekTo(Math.max(0, player.getCurrentPosition() - skip));
            scheduleHideControls();
        });
        btnFf.setOnClickListener(v -> {
            player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + skip));
            scheduleHideControls();
        });

        btnPrevVideo.setOnClickListener(v -> {
            // If in a queue context, skip prev — else restart
            player.seekTo(0);
        });
        btnNextVideo.setOnClickListener(v -> finish()); // placeholder — no video queue yet

        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnLock.setImageResource(isLocked
                ? android.R.drawable.ic_lock_lock
                : android.R.drawable.ic_lock_idle_lock);
            if (isLocked) setControlsVisible(false);
        });

        btnPip.setOnClickListener(v -> enterPiP());

        btnMore.setOnClickListener(v -> showMoreMenu());

        // Speed chip
        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","2.0×","3.0×"};
            float[]  vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
            new AlertDialog.Builder(this).setTitle("Speed")
                .setItems(opts, (d, i) -> {
                    player.setPlaybackSpeed(vals[i]);
                    chipSpeed.setText(opts[i]);
                }).show();
        });

        // Aspect ratio chip
        chipRatio.setOnClickListener(v -> {
            ratioIndex = (ratioIndex + 1) % ratioModes.length;
            playerView.setResizeMode(ratioModes[ratioIndex]);
            chipRatio.setText(ratioLabels[ratioIndex]);
        });

        // Subtitle chip
        chipSubtitle.setOnClickListener(v ->
            Toast.makeText(this, "Subtitle loading coming in next update", Toast.LENGTH_SHORT).show());

        // Audio track chip
        chipAudio.setOnClickListener(v ->
            Toast.makeText(this, "Multi-track audio coming in next update", Toast.LENGTH_SHORT).show());

        // Sleep timer
        chipSleep.setOnClickListener(v -> {
            String[] opts = {"5 min","10 min","15 min","30 min","1 hour","Cancel"};
            long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,60*60000L,-1};
            new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(opts,(d,i)->{
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    handler.postDelayed(()->{ player.pause(); chipSleep.setText("Sleep"); }, ms[i]);
                }
            }).show();
        });

        // Seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user && player.getDuration() > 0)
                    tvPos.setText(fmt((long)(p/1000f*player.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player.getDuration() > 0)
                    player.seekTo((long)(sb.getProgress()/1000f*player.getDuration()));
            }
        });
    }

    private void showMoreMenu() {
        String[] opts = {"Share","File Info","Next Video","Loop"};
        new AlertDialog.Builder(this).setTitle("Options")
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("video/*");
                        String u = getIntent().getStringExtra(EXTRA_URI);
                        if (u!=null) share.putExtra(Intent.EXTRA_STREAM, Uri.parse(u));
                        startActivity(Intent.createChooser(share,"Share"));
                        break;
                    case 3:
                        player.setRepeatMode(
                            player.getRepeatMode()==Player.REPEAT_MODE_OFF
                            ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                        Toast.makeText(this,
                            player.getRepeatMode()==Player.REPEAT_MODE_ONE?"Loop ON":"Loop OFF",
                            Toast.LENGTH_SHORT).show();
                        break;
                }
            }).show();
    }

    private void setupGestures() {
        controlsOverlay.setOnTouchListener((v, event) -> {
            int sw = getWindow().getDecorView().getWidth();
            int sh = getWindow().getDecorView().getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX(); touchStartY = event.getY();
                    seekStart   = player.getCurrentPosition();
                    startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = getBrightness();
                    activeGesture = -1;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isLocked) break;
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;
                    if (activeGesture == -1) {
                        if (Math.abs(dx) > Math.abs(dy)*1.5f && Math.abs(dx) > 20) activeGesture = G_SEEK;
                        else if (Math.abs(dy) > 20)
                            activeGesture = event.getX() < sw/2 ? G_BRIGHT : G_VOL;
                    }
                    if (activeGesture == G_SEEK) {
                        long delta = (long)(dx * 250);
                        long newPos = Math.max(0, Math.min(player.getDuration(), seekStart + delta));
                        String sign = delta >= 0 ? "+" : "";
                        showSeekLabel(sign + (delta/1000) + "s → " + fmt(newPos));
                        player.seekTo(newPos);
                    } else if (activeGesture == G_VOL) {
                        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int nv = Math.max(0, Math.min(max, startVolume - (int)(dy/sh*max*2)));
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, nv, 0);
                        showVolLabel((int)(nv*100f/max)+"%");
                    } else if (activeGesture == G_BRIGHT) {
                        int nb = Math.max(0, Math.min(255, startBrightness - (int)(dy/sh*255*2)));
                        setBrightness(nb);
                        showBrightLabel((int)(nb*100f/255)+"%");
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    hideGestureOverlays();
                    activeGesture = -1;
                    // Small movement = tap = toggle controls
                    float mdx = Math.abs(event.getX()-touchStartX);
                    float mdy = Math.abs(event.getY()-touchStartY);
                    if (mdx < 12 && mdy < 12) {
                        if (!isLocked) toggleControls();
                    }
                    break;
            }
            return true;
        });
    }

    private void toggleControls() {
        setControlsVisible(!controlsVisible);
        if (controlsVisible) scheduleHideControls();
    }

    private void setControlsVisible(boolean show) {
        controlsVisible = show;
        controlsOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(hideControls);
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControls);
        handler.postDelayed(hideControls, 3500);
    }

    private void showSeekLabel(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }
    private void showVolLabel(String pct) {
        tvVolume.setText(pct);
        overlayRight.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }
    private void showBrightLabel(String pct) {
        tvBrightness.setText(pct);
        overlayLeft.setVisibility(View.VISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }
    private void hideGestureOverlays() {
        handler.postDelayed(()->{ overlayLeft.setVisibility(View.INVISIBLE);
            overlayRight.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE); }, 700);
    }

    private int getBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int)(lp.screenBrightness * 255);
    }
    private void setBrightness(int v) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = v / 255f;
        getWindow().setAttributes(lp);
    }

    private void enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams p = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9)).build();
            enterPictureInPictureMode(p);
        }
    }

    @Override public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (player != null && player.isPlaying()) enterPiP();
    }

    @Override public void onPictureInPictureModeChanged(boolean inPiP,
            android.content.res.Configuration cfg) {
        super.onPictureInPictureModeChanged(inPiP, cfg);
        setControlsVisible(!inPiP);
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s=ms/1000, m=s/60, h=m/60; s%=60; m%=60;
        return h>0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%d:%02d",m,s);
    }

    @Override protected void onPause() {
        super.onPause();
        if (!isInPictureInPictureMode() && player != null) player.pause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!isInPictureInPictureMode() && player != null) player.play();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
