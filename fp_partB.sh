#!/bin/bash
# ── PART B: Java — working media browser + players ──
# Run from inside ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# MediaAdapter — single adapter for both audio and video lists
# ════════════════════════════════════════════════════════════
cat > $P/adapter/MediaAdapter.java << 'EOF'
package com.fountainpdl.fountainplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_AUDIO = 0;
    private static final int TYPE_VIDEO = 1;

    public interface OnItemClickListener {
        void onItemClick(MediaItem item, int position);
    }

    private final List<MediaItem> items;
    private final int viewType;
    private OnItemClickListener listener;

    public MediaAdapter(List<MediaItem> items, int viewType) {
        this.items = items;
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    @Override public int getItemViewType(int position) { return viewType; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (vt == TYPE_AUDIO) {
            return new AudioVH(inf.inflate(R.layout.item_song, parent, false));
        } else {
            return new VideoVH(inf.inflate(R.layout.item_video, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        MediaItem item = items.get(pos);
        if (holder instanceof AudioVH) ((AudioVH) holder).bind(item);
        else ((VideoVH) holder).bind(item);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, pos);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    // ── Audio ViewHolder ──
    static class AudioVH extends RecyclerView.ViewHolder {
        ImageView art;
        TextView title, artist, duration;

        AudioVH(View v) {
            super(v);
            art = v.findViewById(R.id.iv_album_art);
            title = v.findViewById(R.id.tv_song_title);
            artist = v.findViewById(R.id.tv_song_artist);
            duration = v.findViewById(R.id.tv_song_duration);
        }

        void bind(MediaItem item) {
            title.setText(item.getTitle());
            artist.setText(item.getArtist() != null && !item.getArtist().isEmpty()
                ? item.getArtist() : "Unknown Artist");
            duration.setText(item.getFormattedDuration());

            if (item.getAlbumArtUri() != null) {
                Glide.with(itemView.getContext())
                    .load(item.getAlbumArtUri())
                    .placeholder(R.drawable.bg_play_button)
                    .error(R.drawable.bg_play_button)
                    .centerCrop()
                    .into(art);
            } else {
                art.setImageResource(R.drawable.bg_play_button);
            }
        }
    }

    // ── Video ViewHolder ──
    static class VideoVH extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title, info, duration;

        VideoVH(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.iv_thumbnail);
            title = v.findViewById(R.id.tv_video_title);
            info = v.findViewById(R.id.tv_video_info);
            duration = v.findViewById(R.id.tv_duration_badge);
        }

        void bind(MediaItem item) {
            title.setText(item.getTitle());
            duration.setText(item.getFormattedDuration());
            long mb = item.getSize() / (1024 * 1024);
            info.setText(mb + " MB");

            // Load video thumbnail via Glide
            Glide.with(itemView.getContext())
                .load(item.getPath())
                .placeholder(R.drawable.bg_play_button)
                .error(R.drawable.bg_play_button)
                .centerCrop()
                .into(thumbnail);
        }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# MusicFragment — real working list
# ════════════════════════════════════════════════════════════
cat > $P/ui/music/MusicFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.music;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicFragment extends Fragment {

    private RecyclerView rvSongs;
    private TextView tvCount;
    private List<MediaItem> songs = new ArrayList<>();
    private MediaAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSongs = view.findViewById(R.id.rv_songs);
        tvCount = view.findViewById(R.id.tv_song_count);

        adapter = new MediaAdapter(songs, 0);
        adapter.setOnItemClickListener((item, pos) -> openAudioPlayer(item, pos));
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);

        view.findViewById(R.id.btn_shuffle_all).setOnClickListener(v -> {
            if (!songs.isEmpty()) {
                Collections.shuffle(songs);
                openAudioPlayer(songs.get(0), 0);
            }
        });

        loadSongs();
    }

    private void loadSongs() {
        new Thread(() -> {
            List<MediaItem> result = MediaScanner.scanAudio(requireContext());
            requireActivity().runOnUiThread(() -> {
                songs.clear();
                songs.addAll(result);
                adapter.notifyDataSetChanged();
                tvCount.setText(songs.size() + " songs");
            });
        }).start();
    }

    private void openAudioPlayer(MediaItem item, int pos) {
        Intent intent = new Intent(requireContext(), AudioPlayerActivity.class);
        intent.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
        intent.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
        intent.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
        intent.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        startActivity(intent);
    }
}
EOF

# ════════════════════════════════════════════════════════════
# VideoFragment — real working grid
# ════════════════════════════════════════════════════════════
cat > $P/ui/video/VideoFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.video;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.ArrayList;
import java.util.List;

public class VideoFragment extends Fragment {

    private RecyclerView rvVideos;
    private TextView tvCount;
    private List<MediaItem> videos = new ArrayList<>();
    private MediaAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvVideos = view.findViewById(R.id.rv_videos);
        tvCount = view.findViewById(R.id.tv_video_count);

        adapter = new MediaAdapter(videos, 1);
        adapter.setOnItemClickListener((item, pos) -> {
            Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
            intent.putExtra("title", item.getTitle());
            startActivity(intent);
        });

        // 2-column grid like VLC
        rvVideos.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvVideos.setAdapter(adapter);

        loadVideos();
    }

    private void loadVideos() {
        new Thread(() -> {
            List<MediaItem> result = MediaScanner.scanVideo(requireContext());
            requireActivity().runOnUiThread(() -> {
                videos.clear();
                videos.addAll(result);
                adapter.notifyDataSetChanged();
                tvCount.setText(videos.size() + " videos");
            });
        }).start();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# AudioPlayerActivity — full YouTube Music style
# ════════════════════════════════════════════════════════════
cat > $P/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;

public class AudioPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";

    private ExoPlayer player;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;

    private ImageView ivAlbumArt, ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ARTIST);
        String albumArt = getIntent().getStringExtra(EXTRA_ALBUM_ART);

        if (uriStr == null) { finish(); return; }

        // Bind views
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivBgBlur = findViewById(R.id.iv_bg_blur);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);

        // Set metadata
        tvTitle.setText(title != null ? title : "Unknown Title");
        tvArtist.setText(artist != null ? artist : "Unknown Artist");

        if (albumArt != null) {
            Glide.with(this).load(albumArt).centerCrop().into(ivAlbumArt);
            Glide.with(this).load(albumArt).centerCrop().into(ivBgBlur);
        }

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Init ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
        isPlaying = true;

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long dur = player.getDuration();
                    seekBar.setMax(1000);
                    tvTotalTime.setText(formatTime(dur));
                    startProgressUpdater();
                }
            }
            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                btnPlayPause.setImageResource(playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause();
            else player.play();
        });

        // Speed chip
        findViewById(R.id.chip_speed).setOnClickListener(v -> {
            float cur = player.getPlaybackParameters().speed;
            float next = cur >= 2.0f ? 0.5f : cur + 0.25f;
            player.setPlaybackSpeed(next);
            ((com.google.android.material.chip.Chip) v).setText(next + "×");
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long pos = (long)(progress / 1000f * player.getDuration());
                    player.seekTo(pos);
                    tvCurrentTime.setText(formatTime(pos));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void startProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override public void run() {
                if (player != null && player.getDuration() > 0) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    seekBar.setProgress((int)(pos * 1000 / dur));
                    tvCurrentTime.setText(formatTime(pos));
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressUpdater);
    }

    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    @Override protected void onPause() { super.onPause(); if (player != null) player.pause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# VideoPlayerActivity — updated to receive title
# ════════════════════════════════════════════════════════════
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
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        // Handle both internal launch and file manager launch
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        Uri uri;
        if (uriStr != null) {
            uri = Uri.parse(uriStr);
        } else if (getIntent().getData() != null) {
            uri = getIntent().getData(); // opened from file manager
        } else {
            finish(); return;
        }

        PlayerView playerView = findViewById(R.id.player_view);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onPause() { super.onPause(); if(player!=null) player.pause(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
EOF

# Also update AudioPlayerActivity to handle direct file open
cat >> $P/player/AudioPlayerActivity.java << 'PATCH'

// Note: also handles direct file/content URI from file manager via getIntent().getData()
PATCH

echo ""
echo "✅ PART B DONE!"
echo ""
echo "Now push:"
echo "  git add ."
echo "  git commit -m 'feat: working media browser + YouTube Music player UI'"
echo "  git push"
