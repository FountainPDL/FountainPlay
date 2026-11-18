package com.fountainpdl.fountainplay.player;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Rational;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TrackSelectionDialogBuilder;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaUris;
import com.fountainpdl.fountainplay.util.ResumeStore;
import com.fountainpdl.fountainplay.util.VideoQueue;
import com.google.android.material.chip.Chip;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI   = "media_uri";
    public static final String EXTRA_TITLE = "media_title";

    private static final int G_NONE = -1, G_IGNORE = -2, G_SEEK = 0, G_VOL = 1, G_BRIGHT = 2;
    private static final long DOUBLE_TAP_MS = 260;

    private ExoPlayer player;
    private PlayerView playerView;
    private AppPreferences prefs;
    private ResumeStore resume;
    private AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View touchLayer, controlsOverlay, overlayLeft, overlayRight;
    private ImageButton btnBack, btnPlay, btnRew, btnFf, btnLock, btnUnlock, btnPip, btnMore;
    private ImageButton btnPrevVideo, btnNextVideo;
    private SeekBar seekBar;
    private TextView tvPos, tvDur, tvTitle, tvSeekIndicator, tvBrightness, tvVolume;
    private Chip chipSpeed, chipRatio, chipSubtitle, chipAudio, chipSleep;

    private boolean isLocked = false;
    private boolean controlsVisible = true;
    private boolean isUserSeeking = false;
    private boolean gesturesEnabled = true;
    private boolean wasPlayingBeforePause = false;
    private String lastMediaId;

    // gesture state
    private float downX, downY;
    private long seekStart, pendingSeek = -1;
    private int startVolume, startBrightness;
    private int gesture = G_NONE;
    private long lastTapTime = 0;
    private float lastTapX = 0;
    private Runnable pendingTap;

    private Runnable sleepRunnable;
    private int rotationMode = 0; // 0 auto, 1 landscape, 2 portrait

    private final int[] ratioModes = {
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    };
    private final String[] ratioLabels = {"Fit", "Fill", "Zoom", "Fixed W"};
    private int ratioIndex = 0;

    private final Runnable hideControls = () -> {
        if (!isLocked && player != null && player.getPlayWhenReady()) setControlsVisible(false);
    };
    private final Runnable hideIndicators = () -> {
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    };
    private final Runnable hideUnlock = () -> btnUnlock.setVisibility(View.GONE);

    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (player != null && !isUserSeeking && controlsVisible) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    seekBar.setProgress((int) (pos * 1000 / dur));
                    tvDur.setText(fmt(dur));
                }
                tvPos.setText(fmt(pos));
            }
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String[]> subtitlePicker =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) attachSubtitle(uri);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        prefs = new AppPreferences(this);
        resume = new ResumeStore(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        bindViews();
        setupImmersive();
        if (!setupPlayer()) return;
        setupControls();
        setupGestures();
        setupBack();

        handler.post(progressUpdater);
        setControlsVisible(true);
    }

    private void bindViews() {
        playerView      = findViewById(R.id.player_view);
        touchLayer      = findViewById(R.id.touch_layer);
        controlsOverlay = findViewById(R.id.controls_overlay);
        btnBack         = findViewById(R.id.btn_back);
        btnPlay         = findViewById(R.id.btn_play_video);
        btnRew          = findViewById(R.id.btn_rew);
        btnFf           = findViewById(R.id.btn_ff);
        btnLock         = findViewById(R.id.btn_lock);
        btnUnlock       = findViewById(R.id.btn_unlock);
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

    /** Full-screen video; controls are padded away from cutouts and bars. */
    private void setupImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemBars();
        final View root = findViewById(R.id.video_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
            controlsOverlay.setPadding(i.left, i.top, i.right, i.bottom);
            return insets;
        });
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        c.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    // ── Player ──────────────────────────────────────────────────────────────────

    private MediaItem buildItem(String path, String title) {
        Uri uri = path.startsWith("/") ? Uri.fromFile(new File(path)) : Uri.parse(path);
        return new MediaItem.Builder()
            .setMediaId(path)
            .setUri(uri)
            .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build())
            .build();
    }

    private boolean setupPlayer() {
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (uriStr == null && getIntent().getData() != null) {
            uriStr = getIntent().getData().toString();
            title = getIntent().getData().getLastPathSegment();
        }
        if (uriStr == null) { finish(); return false; }

        List<MediaItem> items = new ArrayList<>();
        int start = 0;
        List<com.fountainpdl.fountainplay.model.MediaItem> queue = VideoQueue.get();
        int qi = VideoQueue.getIndex();
        if (!queue.isEmpty() && qi < queue.size() && uriStr.equals(queue.get(qi).getPath())) {
            for (com.fountainpdl.fountainplay.model.MediaItem m : queue) {
                if (m.getPath() != null) items.add(buildItem(m.getPath(), m.getTitle()));
            }
            start = Math.min(qi, items.size() - 1);
        } else {
            items.add(buildItem(uriStr, title != null ? title : "Video"));
        }
        if (items.isEmpty()) { finish(); return false; }

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA).build();
        player = new ExoPlayer.Builder(this,
                new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build();
        playerView.setPlayer(player);

        long startPos = prefs.getRememberPosition() ? resume.getPosition(items.get(start).mediaId) : 0;
        player.setMediaItems(items, start, startPos);
        player.setPlaybackSpeed(prefs.getPlaybackSpeed());
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) { updatePlayIcon(); }
            @Override public void onPlayWhenReadyChanged(boolean ready, int reason) {
                updatePlayIcon();
                if (!ready) handler.removeCallbacks(hideControls);
                else if (controlsVisible) scheduleHideControls();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) tvDur.setText(fmt(player.getDuration()));
                if (state == Player.STATE_ENDED) {
                    String id = lastMediaId;
                    if (id != null) resume.clear(id);
                    setControlsVisible(true);
                }
            }
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                if (lastMediaId != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                    resume.clear(lastMediaId);
                lastMediaId = item == null ? null : item.mediaId;
                if (item != null && item.mediaMetadata.title != null)
                    tvTitle.setText(item.mediaMetadata.title);
                updateNextPrev();
                if (item != null) saveVideoHistory(item);
            }
            @Override public void onPlayerError(PlaybackException error) {
                Toast.makeText(VideoPlayerActivity.this,
                    "Can't play this video (" + error.getErrorCodeName() + ")", Toast.LENGTH_LONG).show();
                setControlsVisible(true);
            }
        });

        MediaItem first = items.get(start);
        lastMediaId = first.mediaId;
        if (first.mediaMetadata.title != null) tvTitle.setText(first.mediaMetadata.title);
        updateNextPrev();
        saveVideoHistory(first);
        return true;
    }

    private void saveVideoHistory(MediaItem item) {
        final Context app = getApplicationContext();
        final String path = item.mediaId;
        final String title = item.mediaMetadata.title == null ? "Video" : item.mediaMetadata.title.toString();
        long dur = 0;
        for (com.fountainpdl.fountainplay.model.MediaItem m : VideoQueue.get()) {
            if (path.equals(m.getPath())) { dur = m.getDuration(); break; }
        }
        final long duration = dur;
        new Thread(() -> {
            try {
                HistoryItem h = new HistoryItem();
                h.path = path; h.title = title; h.artist = "";
                h.type = 1; h.duration = duration; h.playedAt = System.currentTimeMillis();
                AppDatabase db = AppDatabase.get(app);
                db.historyDao().deleteByPath(path);
                db.historyDao().insert(h);
            } catch (Exception ignored) { }
        }).start();
    }

    private void saveProgress() {
        if (player == null || !prefs.getRememberPosition()) return;
        MediaItem cur = player.getCurrentMediaItem();
        if (cur == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (pos < 5_000 || (dur > 0 && pos > dur - 10_000)) resume.clear(cur.mediaId);
        else {
            String t = cur.mediaMetadata.title == null ? "" : cur.mediaMetadata.title.toString();
            resume.put(cur.mediaId, pos, Math.max(dur, 0), t);
        }
    }

    private void updatePlayIcon() {
        btnPlay.setImageResource(player.getPlayWhenReady()
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void updateNextPrev() {
        btnNextVideo.setAlpha(player.hasNextMediaItem() ? 1f : 0.35f);
        btnPrevVideo.setAlpha(1f);
    }

    // ── Controls ────────────────────────────────────────────────────────────────

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> { togglePlay(); scheduleHideControls(); });

        btnRew.setOnClickListener(v -> { seekBy(-skipMs()); scheduleHideControls(); });
        btnFf.setOnClickListener(v -> { seekBy(skipMs()); scheduleHideControls(); });

        btnPrevVideo.setOnClickListener(v -> {
            if (player.getCurrentPosition() > 3000 || !player.hasPreviousMediaItem()) player.seekTo(0);
            else player.seekToPreviousMediaItem();
            scheduleHideControls();
        });
        btnNextVideo.setOnClickListener(v -> {
            if (player.hasNextMediaItem()) player.seekToNextMediaItem();
            else toast("This is the last video");
            scheduleHideControls();
        });

        btnLock.setOnClickListener(v -> setLocked(true));
        btnUnlock.setOnClickListener(v -> setLocked(false));

        btnPip.setOnClickListener(v -> enterPiP());
        btnMore.setOnClickListener(v -> showMoreMenu());

        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            final String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","2.0×","3.0×"};
            final float[] vals  = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
            new AlertDialog.Builder(this).setTitle("Speed")
                .setItems(opts, (d, i) -> {
                    player.setPlaybackSpeed(vals[i]);
                    prefs.setPlaybackSpeed(vals[i]);
                    chipSpeed.setText(opts[i]);
                }).show();
        });

        chipRatio.setOnClickListener(v -> {
            ratioIndex = (ratioIndex + 1) % ratioModes.length;
            playerView.setResizeMode(ratioModes[ratioIndex]);
            chipRatio.setText(ratioLabels[ratioIndex]);
        });

        chipSubtitle.setOnClickListener(v -> {
            final String[] opts = {"Choose subtitle track", "Load subtitle file (.srt .vtt .ass)", "Turn subtitles off"};
            new AlertDialog.Builder(this).setTitle("Subtitles").setItems(opts, (d, i) -> {
                if (i == 0) showTrackDialog(C.TRACK_TYPE_TEXT, "Subtitles");
                else if (i == 1) subtitlePicker.launch(new String[]{"*/*"});
                else {
                    player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build());
                    toast("Subtitles off");
                }
            }).show();
        });

        chipAudio.setOnClickListener(v -> showTrackDialog(C.TRACK_TYPE_AUDIO, "Audio track"));

        chipSleep.setOnClickListener(v -> {
            final String[] opts = {"5 min","10 min","15 min","30 min","1 hour","Cancel timer"};
            final long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,60*60000L,0};
            new AlertDialog.Builder(this).setTitle("Sleep timer").setItems(opts, (d, i) -> {
                if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
                sleepRunnable = null;
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    sleepRunnable = () -> {
                        if (player != null) player.pause();
                        chipSleep.setText("Sleep");
                        sleepRunnable = null;
                    };
                    handler.postDelayed(sleepRunnable, ms[i]);
                } else chipSleep.setText("Sleep");
            }).show();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user && player.getDuration() > 0)
                    tvPos.setText(fmt((long) (p / 1000f * player.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
                handler.removeCallbacks(hideControls);
            }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player.getDuration() > 0)
                    player.seekTo((long) (sb.getProgress() / 1000f * player.getDuration()));
                scheduleHideControls();
            }
        });
    }

    private void setupBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (isLocked) { showUnlockTemporarily(); return; }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private long skipMs() { return prefs.getSkipInterval() * 1000L; }

    private void seekBy(long deltaMs) {
        long dur = player.getDuration();
        long target = player.getCurrentPosition() + deltaMs;
        if (dur > 0) target = Math.min(target, dur);
        player.seekTo(Math.max(0, target));
    }

    private void togglePlay() {
        if (player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_ENDED) player.pause();
        else {
            if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
            player.play();
        }
    }

    private void setLocked(boolean locked) {
        isLocked = locked;
        if (locked) {
            setControlsVisible(false);
            showUnlockTemporarily();
            toast("Controls locked");
        } else {
            btnUnlock.setVisibility(View.GONE);
            setControlsVisible(true);
        }
    }

    private void showUnlockTemporarily() {
        btnUnlock.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideUnlock);
        handler.postDelayed(hideUnlock, 2500);
    }

    private void showTrackDialog(int type, String title) {
        boolean has = false;
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) {
            if (g.getType() == type) { has = true; break; }
        }
        if (!has) { toast("No " + title.toLowerCase(Locale.ROOT) + " found in this file"); return; }
        if (type == C.TRACK_TYPE_TEXT) {
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build());
        }
        new TrackSelectionDialogBuilder(this, title, player, type)
            .setAllowAdaptiveSelections(false).build().show();
    }

    // ── Subtitles from a file ───────────────────────────────────────────────────

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) { }
        String seg = uri.getLastPathSegment();
        return seg == null ? "" : seg;
    }

    private void attachSubtitle(Uri sub) {
        MediaItem cur = player.getCurrentMediaItem();
        if (cur == null) return;
        String name = displayName(sub).toLowerCase(Locale.ROOT);
        String mime = MimeTypes.APPLICATION_SUBRIP;
        if (name.endsWith(".vtt")) mime = MimeTypes.TEXT_VTT;
        else if (name.endsWith(".ass") || name.endsWith(".ssa")) mime = MimeTypes.TEXT_SSA;
        else if (name.endsWith(".ttml") || name.endsWith(".xml") || name.endsWith(".dfxp")) mime = MimeTypes.APPLICATION_TTML;

        MediaItem.SubtitleConfiguration cfg = new MediaItem.SubtitleConfiguration.Builder(sub)
            .setMimeType(mime)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build();
        MediaItem updated = cur.buildUpon()
            .setSubtitleConfigurations(Collections.singletonList(cfg)).build();

        int idx = player.getCurrentMediaItemIndex();
        long pos = player.getCurrentPosition();
        boolean wasPlaying = player.getPlayWhenReady();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build());
        player.replaceMediaItem(idx, updated);
        player.seekTo(idx, pos);
        player.setPlayWhenReady(wasPlaying);
        toast("Subtitle loaded");
    }

    // ── More menu ───────────────────────────────────────────────────────────────

    private void showMoreMenu() {
        final boolean loop = player.getRepeatMode() == Player.REPEAT_MODE_ONE;
        final String[] opts = {"Share", "File info", loop ? "Loop: on (tap to turn off)" : "Loop this video", "Rotate screen"};
        new AlertDialog.Builder(this).setTitle("Options").setItems(opts, (d, i) -> {
            MediaItem cur = player.getCurrentMediaItem();
            switch (i) {
                case 0: shareCurrent(cur); break;
                case 1: showFileInfo(cur); break;
                case 2:
                    player.setRepeatMode(loop ? Player.REPEAT_MODE_OFF : Player.REPEAT_MODE_ONE);
                    toast(loop ? "Loop off" : "Loop on");
                    break;
                case 3: cycleRotation(); break;
            }
        }).show();
    }

    private void shareCurrent(MediaItem cur) {
        if (cur == null) return;
        com.fountainpdl.fountainplay.model.MediaItem m = new com.fountainpdl.fountainplay.model.MediaItem();
        m.setPath(cur.mediaId);
        m.setType(com.fountainpdl.fountainplay.model.MediaItem.TYPE_VIDEO);
        for (com.fountainpdl.fountainplay.model.MediaItem q : VideoQueue.get()) {
            if (cur.mediaId.equals(q.getPath())) { m = q; break; }
        }
        Intent chooser = MediaUris.shareIntent(this, m);
        if (chooser == null) toast("This file can't be shared");
        else startActivity(chooser);
    }

    private void showFileInfo(MediaItem cur) {
        if (cur == null) return;
        VideoSize vs = player.getVideoSize();
        String size = "";
        if (cur.mediaId.startsWith("/")) size = "\nSize: " + (new File(cur.mediaId).length() / 1024 / 1024) + " MB";
        String msg = "Name: " + (cur.mediaMetadata.title == null ? "" : cur.mediaMetadata.title)
            + "\nDuration: " + fmt(player.getDuration())
            + "\nResolution: " + (vs.width > 0 ? vs.width + " × " + vs.height : "unknown")
            + size + "\nPath: " + cur.mediaId;
        new AlertDialog.Builder(this).setTitle("File info").setMessage(msg)
            .setPositiveButton("OK", null).show();
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
                    seekStart = player.getCurrentPosition();
                    startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = readBrightness();
                    pendingSeek = -1;
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    if (gesture == G_IGNORE || isLocked || !gesturesEnabled) return true;
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    if (gesture == G_NONE) {
                        if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.5f) gesture = G_SEEK;
                        else if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx) * 1.5f)
                            gesture = downX < sw / 2f ? G_BRIGHT : G_VOL;
                        else return true;
                    }
                    applyGesture(dx, dy, sw, sh);
                    return true;
                }

                case MotionEvent.ACTION_UP: {
                    boolean swiped = gesture >= 0;
                    boolean ignored = gesture == G_IGNORE;
                    if (gesture == G_SEEK && pendingSeek >= 0) player.seekTo(pendingSeek);
                    gesture = G_NONE;
                    pendingSeek = -1;
                    if (swiped) scheduleHideIndicators();
                    else if (!ignored && Math.hypot(e.getX() - downX, e.getY() - downY) <= slop * 2f)
                        handleTap(e.getX(), sw);
                    return true;
                }

                case MotionEvent.ACTION_CANCEL:
                    gesture = G_NONE;
                    pendingSeek = -1;
                    scheduleHideIndicators();
                    return true;
            }
            return true;
        });
    }

    private void applyGesture(float dx, float dy, int sw, int sh) {
        if (gesture == G_SEEK) {
            long dur = player.getDuration();
            long delta = (long) (dx / sw * 120_000f); // a full-width swipe = 2 minutes
            long target = seekStart + delta;
            if (dur > 0) target = Math.min(target, dur);
            target = Math.max(0, target);
            pendingSeek = target;
            showSeekLabel((delta >= 0 ? "+" : "-") + Math.abs(delta / 1000) + "s  →  " + fmt(target));
        } else if (gesture == G_VOL) {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int nv = Math.max(0, Math.min(max, Math.round(startVolume - dy / (sh * 0.55f) * max)));
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, nv, 0);
            tvVolume.setText(Math.round(nv * 100f / max) + "%");
            overlayRight.setVisibility(View.VISIBLE);
            overlayLeft.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE);
        } else if (gesture == G_BRIGHT) {
            int nb = Math.max(1, Math.min(255, Math.round(startBrightness - dy / (sh * 0.55f) * 255)));
            setBrightness(nb);
            tvBrightness.setText(Math.round(nb * 100f / 255) + "%");
            overlayLeft.setVisibility(View.VISIBLE);
            overlayRight.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE);
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
        lastTapX = x;
        pendingTap = () -> {
            pendingTap = null;
            if (isLocked) showUnlockTemporarily();
            else setControlsVisible(!controlsVisible);
        };
        handler.postDelayed(pendingTap, DOUBLE_TAP_MS - 40);
    }

    private void doubleTap(float x, int sw) {
        if (x < sw * 0.4f) {
            seekBy(-skipMs());
            showSeekLabel("-" + prefs.getSkipInterval() + "s");
        } else if (x > sw * 0.6f) {
            seekBy(skipMs());
            showSeekLabel("+" + prefs.getSkipInterval() + "s");
        } else {
            togglePlay();
            showSeekLabel(player.getPlayWhenReady() ? "Play" : "Pause");
        }
        scheduleHideIndicators();
    }

    private void showSeekLabel(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }

    private void scheduleHideIndicators() {
        handler.removeCallbacks(hideIndicators);
        handler.postDelayed(hideIndicators, 700);
    }

    private int readBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int) (lp.screenBrightness * 255);
    }

    private void setBrightness(int v) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = v / 255f;
        getWindow().setAttributes(lp);
    }

    // ── Controls visibility ─────────────────────────────────────────────────────

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

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControls);
        handler.postDelayed(hideControls, 3500);
    }

    // ── Picture in picture ──────────────────────────────────────────────────────

    private void enterPiP() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || player == null) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            toast("Picture-in-picture is not supported on this device");
            return;
        }
        int w = 16, h = 9;
        VideoSize vs = player.getVideoSize();
        if (vs.width > 0 && vs.height > 0) { w = vs.width; h = vs.height; }
        float r = (float) w / h;
        if (r > 2.39f) { w = 239; h = 100; }
        else if (r < 1f / 2.39f) { w = 100; h = 239; }
        try {
            enterPictureInPictureMode(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(w, h)).build());
        } catch (IllegalStateException ignored) { }
    }

    @Override public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (player != null && player.isPlaying() && prefs.getBoolean("auto_pip", true)) enterPiP();
    }

    @Override public void onPictureInPictureModeChanged(boolean inPiP, Configuration cfg) {
        super.onPictureInPictureModeChanged(inPiP, cfg);
        if (inPiP) { setControlsVisible(false); btnUnlock.setVisibility(View.GONE); }
        else if (!isLocked) setControlsVisible(true);
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    @Override protected void onPause() {
        super.onPause();
        saveProgress();
        if (player != null && !isInPictureInPictureMode()) {
            wasPlayingBeforePause = player.getPlayWhenReady();
            player.pause();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Only resume if it was playing when we left: a paused video stays paused.
        if (player != null && wasPlayingBeforePause && !isInPictureInPictureMode()) player.play();
        wasPlayingBeforePause = false;
    }

    @Override protected void onStop() {
        super.onStop();
        // The PiP window was closed with its X button: end playback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) finishAndRemoveTask();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            saveProgress();
            player.release();
            player = null;
        }
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000, m = s / 60, h = m / 60; s %= 60; m %= 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%d:%02d", m, s);
    }
}
