package com.fountainpdl.fountainplay.player;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.QueueAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerActivity extends AppCompatActivity
    implements PlaybackState.Listener, PlayQueue.Listener {

    public static final String EXTRA_URI       = "media_uri";
    public static final String EXTRA_TITLE     = "media_title";
    public static final String EXTRA_ARTIST    = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";
    public static final String EXTRA_RESUME    = "resume_playback";

    private PlaybackService service;
    private boolean bound = false;
    private AppPreferences prefs;

    // Views
    private ViewPager2 viewPager;
    private ImageView ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvQueueInfo;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBack, btnShuffle, btnRepeat, btnFavorite, btnOptions;
    private Chip chipSpeed, chipSleep, chipShare;
    private View dot0, dot1, dot2;
    private boolean isUserSeeking = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound = true;
            String uriStr = getIntent().getStringExtra(EXTRA_URI);
            boolean resume = getIntent().getBooleanExtra(EXTRA_RESUME, false);
            if (uriStr != null && !resume) {
                MediaItem cur = PlayQueue.get().current();
                if (cur != null && cur.getPath() != null && cur.getPath().equals(uriStr)) {
                    service.playItem(cur);
                } else {
                    MediaItem item = new MediaItem(0,
                        getIntent().getStringExtra(EXTRA_TITLE),
                        getIntent().getStringExtra(EXTRA_ARTIST),
                        uriStr, 0, MediaItem.TYPE_AUDIO);
                    item.setAlbumArtUri(getIntent().getStringExtra(EXTRA_ALBUM_ART));
                    service.playItem(item);
                }
            }
            syncUI();
        }
        @Override public void onServiceDisconnected(ComponentName name) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();
        setupViewPager();
        setupControls();

        PlaybackState.get().addListener(this);
        PlayQueue.get().addListener(this);

        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, connection, BIND_AUTO_CREATE);
    }

    private void bindViews() {
        viewPager     = findViewById(R.id.view_pager);
        ivBgBlur      = findViewById(R.id.iv_bg_blur);
        tvTitle       = findViewById(R.id.tv_title);
        tvArtist      = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime   = findViewById(R.id.tv_total_time);
        tvQueueInfo   = findViewById(R.id.tv_queue_info);
        seekBar       = findViewById(R.id.seek_bar);
        btnPlayPause  = findViewById(R.id.btn_play_pause);
        btnPrev       = findViewById(R.id.btn_prev);
        btnNext       = findViewById(R.id.btn_next);
        btnBack       = findViewById(R.id.btn_back);
        btnShuffle    = findViewById(R.id.btn_shuffle);
        btnRepeat     = findViewById(R.id.btn_repeat);
        btnFavorite   = findViewById(R.id.btn_favorite);
        btnOptions    = findViewById(R.id.btn_options);
        chipSpeed     = findViewById(R.id.chip_speed);
        chipSleep     = findViewById(R.id.chip_sleep);
        chipShare     = findViewById(R.id.chip_share);
        dot0          = findViewById(R.id.dot0);
        dot1          = findViewById(R.id.dot1);
        dot2          = findViewById(R.id.dot2);
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                switch (pos) {
                    case 1: return new LyricsPageFragment();
                    case 2: return new QueuePageFragment();
                    default: return new NowPlayingPageFragment();
                }
            }
            @Override public int getItemCount() { return 3; }
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { updateDots(pos); }
        });
        viewPager.setPageTransformer((page, pos) -> {
            page.setAlpha(1 - Math.abs(pos) * 0.4f);
            page.setScaleX(1 - Math.abs(pos) * 0.06f);
            page.setScaleY(1 - Math.abs(pos) * 0.06f);
        });
    }

    private void updateDots(int pos) {
        int on = 0xFFFFFFFF, off = 0x60A89BC2;
        dot0.setBackgroundColor(pos == 0 ? on : off);
        dot1.setBackgroundColor(pos == 1 ? on : off);
        dot2.setBackgroundColor(pos == 2 ? on : off);
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish()); // keeps playing

        btnPlayPause.setOnClickListener(v -> { if (bound) { service.playPause(); pulse(btnPlayPause); }});
        btnNext.setOnClickListener(v -> { if (bound) service.skipNext(); });
        btnPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });

        btnShuffle.setOnClickListener(v -> {
            PlayQueue.get().setShuffle(!PlayQueue.get().isShuffle());
            updateShuffleUI();
        });
        btnRepeat.setOnClickListener(v -> { PlayQueue.get().cycleRepeat(); updateRepeatUI(); });

        btnFavorite.setOnClickListener(v ->
            Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show());

        btnOptions.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            String[] opts = {"Add to Queue","Add to Playlist","Set as Ringtone","Share","File Info"};
            new AlertDialog.Builder(this).setTitle(cur.getTitle())
                .setItems(opts, (d, i) -> {
                    switch (i) {
                        case 0: PlayQueue.get().addToQueue(cur); Toast.makeText(this,"Added to queue",Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(this,"Ringtone: use a file manager to set",Toast.LENGTH_SHORT).show(); break;
                        case 3: shareAudio(cur); break;
                        case 4: showFileInfo(cur); break;
                    }
                }).show();
        });

        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            if (!bound) return;
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","3.0×"};
            float[] vals  = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
            float cur = service.getPlayer().getPlaybackParameters().speed;
            int sel = 3;
            for (int i = 0; i < vals.length; i++) if (Math.abs(vals[i]-cur) < 0.01f) { sel=i; break; }
            final int[] picked = {sel};
            new AlertDialog.Builder(this).setTitle("Playback Speed")
                .setSingleChoiceItems(opts, sel, (d, i) -> picked[0] = i)
                .setPositiveButton("OK", (d, i) -> {
                    service.setPlaybackSpeed(vals[picked[0]]);
                    chipSpeed.setText(opts[picked[0]]);
                    prefs.setPlaybackSpeed(vals[picked[0]]);
                }).show();
        });

        chipSleep.setOnClickListener(v -> {
            String[] opts = {"5 min","10 min","15 min","30 min","45 min","1 hour","Cancel"};
            long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,45*60000L,60*60000L,-1};
            new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(opts,(d,i)->{
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    new android.os.Handler().postDelayed(()->{
                        if (bound) service.getPlayer().pause();
                        chipSleep.setText("Sleep");
                    }, ms[i]);
                }
            }).show();
        });

        chipShare.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) shareAudio(cur);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser && bound && service.getDuration()>0)
                    tvCurrentTime.setText(fmt((long)(prog/1000f*service.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking=true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking=false;
                if (bound && service.getDuration()>0)
                    service.seekTo((long)(sb.getProgress()/1000f*service.getDuration()));
            }
        });
    }

    private void shareAudio(MediaItem item) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("audio/*");
        i.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
        i.putExtra(Intent.EXTRA_TEXT, item.getTitle()+" - "+item.getArtist());
        startActivity(Intent.createChooser(i,"Share"));
    }

    private void showFileInfo(MediaItem item) {
        new AlertDialog.Builder(this).setTitle("File Info")
            .setMessage("Title: "+item.getTitle()+"\nArtist: "+item.getArtist()
                +"\nAlbum: "+item.getAlbum()+"\nDuration: "+item.getFormattedDuration()
                +"\nSize: "+(item.getSize()/1024/1024)+" MB\nPath: "+item.getPath())
            .setPositiveButton("OK",null).show();
    }

    private void syncUI() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) updateTrackUI(cur);
        btnPlayPause.setImageResource(bound && service.isPlaying()
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        updateShuffleUI(); updateRepeatUI(); updateQueueInfo();
        if (bound && service.getDuration()>0) tvTotalTime.setText(fmt(service.getDuration()));
    }

    private void updateTrackUI(MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        tvTitle.setSelected(true);
        String art = item.getAlbumArtUri();
        Glide.with(this).load(art).centerCrop().placeholder(R.drawable.bg_play_button).into(ivBgBlur);
        // Update now-playing fragment
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f instanceof NowPlayingPageFragment) ((NowPlayingPageFragment)f).updateArt(art);
    }

    private void updateShuffleUI() {
        boolean on = PlayQueue.get().isShuffle();
        btnShuffle.setAlpha(on ? 1.0f : 0.4f);
        btnShuffle.setColorFilter(on ? 0xFFBB86FC : 0xFFA89BC2);
    }

    private void updateRepeatUI() {
        int rm = PlayQueue.get().getRepeatMode();
        btnRepeat.setAlpha(rm != PlayQueue.REPEAT_NONE ? 1.0f : 0.4f);
        btnRepeat.setColorFilter(rm == PlayQueue.REPEAT_ONE ? 0xFFBB86FC : 0xFFA89BC2);
    }

    private void updateQueueInfo() {
        int idx = PlayQueue.get().getIndex(), total = PlayQueue.get().size();
        tvQueueInfo.setText(total > 1 ? (idx+1)+" / "+total : "Now Playing");
    }

    @Override public void onItemChanged(MediaItem item) { runOnUiThread(()->{ if(item!=null) updateTrackUI(item); }); }
    @Override public void onPlayStateChanged(boolean p) { runOnUiThread(()->btnPlayPause.setImageResource(p?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play)); }
    @Override public void onPositionChanged(long pos, long dur) {
        runOnUiThread(()->{
            if (!isUserSeeking && dur>0) {
                seekBar.setProgress((int)(pos*1000/dur));
                tvCurrentTime.setText(fmt(pos));
                tvTotalTime.setText(fmt(dur));
            }
        });
    }
    @Override public void onQueueChanged() { runOnUiThread(this::updateQueueInfo); }
    @Override public void onTrackChanged(MediaItem item, int idx) { runOnUiThread(()->{updateTrackUI(item);updateQueueInfo();}); }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f,1.2f,1f,1.2f,
            Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    private String fmt(long ms) {
        if (ms<=0) return "0:00";
        long s=ms/1000,m=s/60,h=m/60; s%=60; m%=60;
        return h>0?String.format("%d:%02d:%02d",h,m,s):String.format("%d:%02d",m,s);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        if (bound) { unbindService(connection); bound=false; }
    }

    // ─── Inner Fragment Pages ───────────────────────────────

    public static class NowPlayingPageFragment extends Fragment {
        private ImageView albumArt;
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_now_playing, c, false);
            albumArt = v.findViewById(R.id.iv_album_art);
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.getAlbumArtUri() != null)
                Glide.with(requireContext()).load(cur.getAlbumArtUri()).centerCrop().into(albumArt);
            return v;
        }
        public void updateArt(String url) {
            if (albumArt == null || url == null) return;
            albumArt.animate().alpha(0).setDuration(150).withEndAction(()->{
                Glide.with(requireContext()).load(url).centerCrop().into(albumArt);
                albumArt.animate().alpha(1).setDuration(150).start();
            }).start();
        }
    }

    public static class LyricsPageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            return inf.inflate(R.layout.fragment_player_lyrics, c, false);
        }
    }

    public static class QueuePageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_queue, c, false);
            RecyclerView rv = v.findViewById(R.id.rv_queue);
            TextView tvCount = v.findViewById(R.id.tv_queue_count);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            List<MediaItem> queue = new ArrayList<>(PlayQueue.get().getQueue());
            int curIdx = PlayQueue.get().getIndex();
            tvCount.setText(queue.size() + " songs");
            rv.setAdapter(new QueueAdapter(queue, curIdx, item -> {
                List<MediaItem> q = PlayQueue.get().getQueue();
                int idx = q.indexOf(item);
                if (idx > PlayQueue.get().getIndex()) {
                    for (int i = PlayQueue.get().getIndex(); i < idx; i++) PlayQueue.get().next();
                }
            }));
            rv.scrollToPosition(Math.max(0, curIdx - 2));
            return v;
        }
    }
}
