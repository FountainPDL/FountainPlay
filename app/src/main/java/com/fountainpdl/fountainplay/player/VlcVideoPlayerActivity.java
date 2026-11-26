package com.fountainpdl.fountainplay.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.FolderScanner;
import com.fountainpdl.fountainplay.util.MediaUris;
import com.fountainpdl.fountainplay.util.ResumeStore;
import com.fountainpdl.fountainplay.util.ThemeSetup;
import com.fountainpdl.fountainplay.util.VideoQueue;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.VLCVideoLayout;

/**
 * The second video engine. VLC plays what ExoPlayer cannot: AVI, WMV, RMVB, FLV oddities, DVD/VOB, DTS and
 * TrueHD audio, odd codecs. The normal player hands over to this screen automatically.
 */
public class VlcVideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI   = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_START = "start_ms";

    private static final int G_NONE = -1, G_IGNORE = -2, G_SEEK = 0, G_VOL = 1, G_BRIGHT = 2;
    private static final long DOUBLE_TAP_MS = 260;

    private LibVLC libVLC;
    private MediaPlayer mp;
    private VLCVideoLayout vlcLayout;
    private AppPreferences prefs;
    private ResumeStore resume;
    private AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View touchLayer, controlsOverlay, overlayLeft, overlayRight;
    private View btnBack, btnRew, btnFf, btnPip, btnMore, btnPrevVideo, btnNextVideo;
    private ImageButton btnPlay;
    private MaterialButton btnUnlock;
    private SeekBar seekBar;
    private TextView tvPos, tvDur, tvTitle, tvPlayLabel, tvSeekIndicator, tvBrightness, tvVolume;
    private Chip chipLock, chipSpeed, chipRatio, chipSubtitle, chipAudio, chipSleep;

    private final List<MediaItem> queue = new ArrayList<>();
    private int index = 0;
    private String currentPath;
    private String currentTitle = "";
    private long pendingSeek = -1;
    private boolean isLocked = false, controlsVisible = true, isUserSeeking = false, gesturesEnabled = true;
    private boolean wasPlayingBeforePause = false, loop = false, started = false;

    private float downX, downY;
    private long seekStart, gestureSeek = -1, lastTapTime = 0;
    private int startVolume, startBrightness, gesture = G_NONE, rotationMode = 0, ratioIndex = 0;
    private Runnable pendingTap, sleepRunnable;

    private final MediaPlayer.ScaleType[] scales = {
        MediaPlayer.ScaleType.SURFACE_BEST_FIT, MediaPlayer.ScaleType.SURFACE_FILL,
        MediaPlayer.ScaleType.SURFACE_16_9, MediaPlayer.ScaleType.SURFACE_4_3, MediaPlayer.ScaleType.SURFACE_ORIGINAL };
    private final String[] scaleLabels = {"Fit", "Fill", "16:9", "4:3", "100%"};

    private final Runnable hideControls = () -> { if (!isLocked && mp != null && mp.isPlaying()) setControlsVisible(false); };
    private final Runnable hideIndicators = () -> {
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    };
    private final Runnable hideUnlock = () -> btnUnlock.setVisibility(View.GONE);
    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (mp != null && !isUserSeeking && controlsVisible) {
                long len = mp.getLength(), pos = mp.getTime();
                if (len > 0) { seekBar.setProgress((int) (pos * 1000 / len)); tvDur.setText(fmt(len)); }
                tvPos.setText(fmt(pos));
            }
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String[]> subtitlePicker =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) addSubtitle(uri); });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeSetup.apply(this, false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vlc_video);
        prefs = new AppPreferences(this);
        resume = new ResumeStore(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        bindViews();
        setupImmersive();
        if (!setupQueue()) { finish(); return; }
        setupVlc();
        setupControls();
        setupGestures();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (isLocked) { showUnlockTemporarily(); return; }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        handler.post(progressUpdater);
        setControlsVisible(true);
    }

    private void bindViews() {
        vlcLayout = findViewById(R.id.vlc_layout);
        touchLayer = findViewById(R.id.touch_layer);
        controlsOverlay = findViewById(R.id.controls_overlay);
        btnBack = findViewById(R.id.btn_back);
        btnPlay = findViewById(R.id.btn_play_video);
        btnRew = findViewById(R.id.btn_rew);
        btnFf = findViewById(R.id.btn_ff);
        btnPip = findViewById(R.id.btn_pip);
        btnMore = findViewById(R.id.btn_more);
        btnPrevVideo = findViewById(R.id.btn_prev_video);
        btnNextVideo = findViewById(R.id.btn_next_video);
        btnUnlock = findViewById(R.id.btn_unlock);
        seekBar = findViewById(R.id.video_seek_bar);
        tvPos = findViewById(R.id.tv_pos);
        tvDur = findViewById(R.id.tv_dur);
        tvTitle = findViewById(R.id.tv_video_title);
        tvPlayLabel = findViewById(R.id.tv_play_label);
        tvSeekIndicator = findViewById(R.id.tv_seek_indicator);
        overlayLeft = findViewById(R.id.overlay_left);
        overlayRight = findViewById(R.id.overlay_right);
        tvBrightness = findViewById(R.id.tv_brightness_val);
        tvVolume = findViewById(R.id.tv_volume_val);
        chipLock = findViewById(R.id.chip_lock);
        chipSpeed = findViewById(R.id.chip_speed_video);
        chipRatio = findViewById(R.id.chip_ratio);
        chipSubtitle = findViewById(R.id.chip_subtitle);
        chipAudio = findViewById(R.id.chip_audio_track);
        chipSleep = findViewById(R.id.chip_sleep_video);
        btnPip.setVisibility(View.GONE);   // picture-in-picture is only in the standard player
    }

    private void setupImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemBars();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.video_root), (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            controlsOverlay.setPadding(i.left, i.top, i.right, i.bottom);
            return insets;
        });
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        c.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    // ── Queue and playback ──────────────────────────────────────────────────────

    private boolean setupQueue() {
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (uriStr == null && getIntent().getData() != null) {
            uriStr = getIntent().getData().toString();
            title = getIntent().getData().getLastPathSegment();
        }
        if (uriStr == null) return false;
        List<MediaItem> q = VideoQueue.get();
        int qi = -1;
        for (int i = 0; i < q.size(); i++) if (uriStr.equals(q.get(i).getPath())) { qi = i; break; }
        if (qi >= 0) { queue.addAll(q); index = qi; }
        else {
            MediaItem single = new MediaItem(0, title == null ? "Video" : title, "", uriStr, 0, MediaItem.TYPE_VIDEO);
            queue.add(single);
            index = 0;
        }
        long start = getIntent().getLongExtra(EXTRA_START, -1);
        pendingSeek = start > 0 ? start : (prefs.getRememberPosition() ? resume.getPosition(uriStr) : 0);
        return true;
    }

    private void setupVlc() {
        ArrayList<String> options = new ArrayList<>();
        options.add("--audio-time-stretch");
        options.add("--network-caching=1500");
        libVLC = new LibVLC(this, options);
        mp = new MediaPlayer(libVLC);
        mp.attachViews(vlcLayout, null, true, false);
        mp.setEventListener(event -> { final int type = event.type; runOnUiThread(() -> onVlcEvent(type)); });
        openCurrent();
    }

    private void openCurrent() {
        MediaItem item = queue.get(index);
        currentPath = item.getPath();
        currentTitle = item.getTitle();
        tvTitle.setText(currentTitle);
        Uri uri = currentPath.startsWith("/") ? Uri.fromFile(new File(currentPath)) : Uri.parse(currentPath);
        Media media = new Media(libVLC, uri);
        media.setHWDecoderEnabled(true, false);   // hardware where possible, software when not
        mp.setMedia(media);
        media.release();
        started = false;
        mp.play();
        updateNextPrev();
    }

    private void onVlcEvent(int type) {
        switch (type) {
            case MediaPlayer.Event.Playing:
                if (!started) {
                    started = true;
                    if (pendingSeek > 0) { mp.setTime(pendingSeek); pendingSeek = -1; }
                    mp.setRate(prefs.getPlaybackSpeed());
                }
                updatePlayIcon();
                scheduleHideControls();
                break;
            case MediaPlayer.Event.Paused:
            case MediaPlayer.Event.Stopped:
                updatePlayIcon();
                break;
            case MediaPlayer.Event.EndReached:
                resume.clear(currentPath);
                if (loop) { openCurrent(); }
                else if (index < queue.size() - 1) { index++; pendingSeek = 0; openCurrent(); }
                else { updatePlayIcon(); setControlsVisible(true); }
                break;
            case MediaPlayer.Event.EncounteredError:
                toast("This file can't be played");
                setControlsVisible(true);
                break;
            default: break;
        }
    }

    private void updatePlayIcon() {
        boolean playing = mp != null && mp.isPlaying();
        btnPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        tvPlayLabel.setText(playing ? "Pause" : "Play");
    }

    private void updateNextPrev() { btnNextVideo.setAlpha(index < queue.size() - 1 ? 1f : 0.35f); }

    private void saveProgress() {
        if (mp == null || currentPath == null || !prefs.getRememberPosition()) return;
        long pos = mp.getTime(), len = mp.getLength();
        if (pos < 5_000 || (len > 0 && pos > len - 10_000)) resume.clear(currentPath);
        else resume.put(currentPath, pos, Math.max(len, 0), currentTitle);
    }

    // ── Controls ────────────────────────────────────────────────────────────────

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.btn_rew_label)).setText("Back " + prefs.getSkipInterval() + "s");
        ((TextView) findViewById(R.id.btn_ff_label)).setText("Fwd " + prefs.getSkipInterval() + "s");
        btnPlay.setOnClickListener(v -> { togglePlay(); scheduleHideControls(); });
        btnRew.setOnClickListener(v -> { seekBy(-skipMs()); scheduleHideControls(); });
        btnFf.setOnClickListener(v -> { seekBy(skipMs()); scheduleHideControls(); });
        btnPrevVideo.setOnClickListener(v -> {
            if (mp.getTime() > 3000 || index == 0) mp.setTime(0);
            else { index--; pendingSeek = 0; openCurrent(); }
            scheduleHideControls();
        });
        btnNextVideo.setOnClickListener(v -> {
            if (index < queue.size() - 1) { index++; pendingSeek = 0; openCurrent(); }
            else toast("This is the last video");
            scheduleHideControls();
        });
        chipLock.setOnClickListener(v -> setLocked(true));
        btnUnlock.setOnClickListener(v -> setLocked(false));
        btnMore.setOnClickListener(v -> showMoreMenu());

        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            final String[] opts = {"0.25×", "0.5×", "0.75×", "1.0×", "1.25×", "1.5×", "2.0×", "3.0×"};
            final float[] vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
            new MaterialAlertDialogBuilder(this).setTitle("Speed").setItems(opts, (d, i) -> {
                mp.setRate(vals[i]);
                prefs.setPlaybackSpeed(vals[i]);
                chipSpeed.setText(opts[i]);
            }).show();
        });
        chipRatio.setOnClickListener(v -> {
            ratioIndex = (ratioIndex + 1) % scales.length;
            mp.setVideoScale(scales[ratioIndex]);
            chipRatio.setText(scaleLabels[ratioIndex]);
        });
        chipSubtitle.setOnClickListener(v -> {
            final String[] opts = {"Choose subtitle track", "Load subtitle file (.srt .vtt .ass)", "Turn subtitles off"};
            new MaterialAlertDialogBuilder(this).setTitle("Subtitles").setItems(opts, (d, i) -> {
                if (i == 0) chooseTrack(mp.getSpuTracks(), true);
                else if (i == 1) subtitlePicker.launch(new String[]{"*/*"});
                else { mp.setSpuTrack(-1); toast("Subtitles off"); }
            }).show();
        });
        chipAudio.setOnClickListener(v -> chooseTrack(mp.getAudioTracks(), false));
        chipSleep.setOnClickListener(v -> {
            final String[] opts = {"5 min", "10 min", "15 min", "30 min", "1 hour", "Turn timer off"};
            final long[] ms = {5 * 60000L, 10 * 60000L, 15 * 60000L, 30 * 60000L, 60 * 60000L, 0};
            new MaterialAlertDialogBuilder(this).setTitle("Sleep timer").setItems(opts, (d, i) -> {
                if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
                sleepRunnable = null;
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    sleepRunnable = () -> { if (mp != null) mp.pause(); chipSleep.setText("Sleep"); sleepRunnable = null; };
                    handler.postDelayed(sleepRunnable, ms[i]);
                } else chipSleep.setText("Sleep");
            }).show();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user && mp.getLength() > 0) tvPos.setText(fmt((long) (p / 1000f * mp.getLength())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; handler.removeCallbacks(hideControls); }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (mp.getLength() > 0) mp.setTime((long) (sb.getProgress() / 1000f * mp.getLength()));
                scheduleHideControls();
            }
        });
    }

    private void chooseTrack(final MediaPlayer.TrackDescription[] tracks, final boolean subtitles) {
        if (tracks == null || tracks.length == 0) { toast("Nothing to choose in this file"); return; }
        String[] names = new String[tracks.length];
        for (int i = 0; i < tracks.length; i++) names[i] = tracks[i].name;
        new MaterialAlertDialogBuilder(this).setTitle(subtitles ? "Subtitles" : "Audio track")
            .setItems(names, (d, i) -> {
                if (subtitles) mp.setSpuTrack(tracks[i].id); else mp.setAudioTrack(tracks[i].id);
            }).show();
    }

    /** VLC reads real files, so a picked subtitle is copied into the cache first. */
    private void addSubtitle(Uri uri) {
        String name = "subtitle.srt";
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) name = c.getString(0);
        } catch (Exception ignored) { }
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException();
            File out = new File(getCacheDir(), "sub_" + System.currentTimeMillis() + "_" + name.replace('/', '_'));
            try (OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
            mp.addSlave(IMedia.Slave.Type.Subtitle, Uri.fromFile(out), true);
            toast("Subtitle loaded");
        } catch (Exception e) {
            toast("Couldn't load that subtitle");
        }
    }

    private long skipMs() { return prefs.getSkipInterval() * 1000L; }

    private void seekBy(long delta) {
        long len = mp.getLength();
        long target = mp.getTime() + delta;
        if (len > 0) target = Math.min(target, len);
        mp.setTime(Math.max(0, target));
    }

    private void togglePlay() {
        if (mp.isPlaying()) mp.pause(); else mp.play();
        updatePlayIcon();
    }

    private void setLocked(boolean locked) {
        isLocked = locked;
        if (locked) { setControlsVisible(false); showUnlockTemporarily(); toast("Controls locked"); }
        else { btnUnlock.setVisibility(View.GONE); setControlsVisible(true); }
    }

    private void showUnlockTemporarily() {
        btnUnlock.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideUnlock);
        handler.postDelayed(hideUnlock, 2500);
    }

    private void showMoreMenu() {
        final String[] opts = {"Share", "File info", loop ? "Loop: on (tap to turn off)" : "Loop this video", "Rotate screen"};
        new MaterialAlertDialogBuilder(this).setTitle("Options").setItems(opts, (d, i) -> {
            switch (i) {
                case 0: {
                    MediaItem m = new MediaItem(0, currentTitle, "", currentPath, 0, MediaItem.TYPE_VIDEO);
                    Intent chooser = MediaUris.shareIntent(this, m);
                    if (chooser == null) toast("This file can't be shared"); else startActivity(chooser);
                    break;
                }
                case 1:
                    new MaterialAlertDialogBuilder(this).setTitle("File info")
                        .setMessage("Name: " + FolderScanner.nameOf(currentPath) + "\nDuration: " + fmt(mp.getLength())
                            + "\nEngine: VLC\nPath: " + currentPath)
                        .setPositiveButton("OK", null).show();
                    break;
                case 2: loop = !loop; toast(loop ? "Loop on" : "Loop off"); break;
                case 3: cycleRotation(); break;
            }
        }).show();
    }

    private void cycleRotation() {
        rotationMode = (rotationMode + 1) % 3;
        if (rotationMode == 1) { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); toast("Landscape"); }
        else if (rotationMode == 2) { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); toast("Portrait"); }
        else { setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); toast("Auto rotate"); }
    }

    // ── Gestures ────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float edge = 24 * getResources().getDisplayMetrics().density;
        touchLayer.setOnTouchListener((v, e) -> {
            int sw = v.getWidth(), sh = v.getHeight();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX(); downY = e.getY();
                    gesture = (downX < edge || downX > sw - edge) ? G_IGNORE : G_NONE;
                    seekStart = mp.getTime();
                    startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = readBrightness();
                    gestureSeek = -1;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    if (gesture == G_IGNORE || isLocked || !gesturesEnabled) return true;
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    if (gesture == G_NONE) {
                        if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.5f) gesture = G_SEEK;
                        else if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx) * 1.5f) gesture = downX < sw / 2f ? G_BRIGHT : G_VOL;
                        else return true;
                    }
                    applyGesture(dx, dy, sw, sh);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    boolean swiped = gesture >= 0, ignored = gesture == G_IGNORE;
                    if (gesture == G_SEEK && gestureSeek >= 0) mp.setTime(gestureSeek);
                    gesture = G_NONE;
                    gestureSeek = -1;
                    if (swiped) scheduleHideIndicators();
                    else if (!ignored && Math.hypot(e.getX() - downX, e.getY() - downY) <= slop * 2f) handleTap(e.getX(), sw);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    gesture = G_NONE; gestureSeek = -1; scheduleHideIndicators();
                    return true;
            }
            return true;
        });
    }

    private void applyGesture(float dx, float dy, int sw, int sh) {
        if (gesture == G_SEEK) {
            long len = mp.getLength();
            long delta = (long) (dx / sw * 120_000f);
            long target = seekStart + delta;
            if (len > 0) target = Math.min(target, len);
            target = Math.max(0, target);
            gestureSeek = target;
            showLabel((delta >= 0 ? "+" : "-") + Math.abs(delta / 1000) + "s  →  " + fmt(target));
        } else if (gesture == G_VOL) {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int nv = Math.max(0, Math.min(max, Math.round(startVolume - dy / (sh * 0.55f) * max)));
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, nv, 0);
            tvVolume.setText(Math.round(nv * 100f / max) + "%");
            overlayRight.setVisibility(View.VISIBLE); overlayLeft.setVisibility(View.INVISIBLE); tvSeekIndicator.setVisibility(View.INVISIBLE);
        } else if (gesture == G_BRIGHT) {
            int nb = Math.max(1, Math.min(255, Math.round(startBrightness - dy / (sh * 0.55f) * 255)));
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = nb / 255f;
            getWindow().setAttributes(lp);
            tvBrightness.setText(Math.round(nb * 100f / 255) + "%");
            overlayLeft.setVisibility(View.VISIBLE); overlayRight.setVisibility(View.INVISIBLE); tvSeekIndicator.setVisibility(View.INVISIBLE);
        }
    }

    private void handleTap(final float x, final int sw) {
        long now = SystemClock.uptimeMillis();
        if (pendingTap != null && now - lastTapTime < DOUBLE_TAP_MS) {
            handler.removeCallbacks(pendingTap);
            pendingTap = null;
            if (!isLocked) doubleTap(x, sw);
            return;
        }
        lastTapTime = now;
        pendingTap = () -> {
            pendingTap = null;
            if (isLocked) showUnlockTemporarily(); else setControlsVisible(!controlsVisible);
        };
        handler.postDelayed(pendingTap, DOUBLE_TAP_MS - 40);
    }

    private void doubleTap(float x, int sw) {
        if (x < sw * 0.4f) { seekBy(-skipMs()); showLabel("-" + prefs.getSkipInterval() + "s"); }
        else if (x > sw * 0.6f) { seekBy(skipMs()); showLabel("+" + prefs.getSkipInterval() + "s"); }
        else { togglePlay(); showLabel(mp.isPlaying() ? "Play" : "Pause"); }
        scheduleHideIndicators();
    }

    private void showLabel(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }

    private void scheduleHideIndicators() { handler.removeCallbacks(hideIndicators); handler.postDelayed(hideIndicators, 700); }

    private int readBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int) (lp.screenBrightness * 255);
    }

    private void setControlsVisible(boolean show) {
        controlsVisible = show;
        handler.removeCallbacks(hideControls);
        controlsOverlay.animate().cancel();
        if (show) {
            controlsOverlay.setVisibility(View.VISIBLE);
            controlsOverlay.animate().alpha(1f).setDuration(140).start();
            scheduleHideControls();
        } else {
            controlsOverlay.animate().alpha(0f).setDuration(140).withEndAction(() -> {
                if (!controlsVisible) controlsOverlay.setVisibility(View.GONE);
            }).start();
        }
    }

    private void scheduleHideControls() { handler.removeCallbacks(hideControls); handler.postDelayed(hideControls, 3500); }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    @Override protected void onPause() {
        super.onPause();
        saveProgress();
        if (mp != null) { wasPlayingBeforePause = mp.isPlaying(); mp.pause(); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (mp != null && wasPlayingBeforePause) mp.play();
        wasPlayingBeforePause = false;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mp != null) {
            saveProgress();
            mp.setEventListener(null);
            mp.stop();
            mp.detachViews();
            mp.release();
            mp = null;
        }
        if (libVLC != null) { libVLC.release(); libVLC = null; }
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000, m = s / 60, h = m / 60; s %= 60; m %= 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%d:%02d", m, s);
    }
}
