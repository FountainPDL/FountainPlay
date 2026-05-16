package com.fountainpdl.fountainplay.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ((TextView)view.findViewById(R.id.tv_greeting)).setText(
            h < 12 ? "Good morning ☀️" : h < 17 ? "Good afternoon 🎵" : "Good evening 🌙");

        RecyclerView rvRecentMusic = view.findViewById(R.id.rv_recent_music);
        RecyclerView rvRecentVideos = view.findViewById(R.id.rv_recent_videos);
        RecyclerView rvRecentPlayed = view.findViewById(R.id.rv_recently_played);

        rvRecentMusic.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentVideos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        new Thread(() -> {
            List<MediaItem> audio = MediaScanner.scanAudio(requireContext());
            List<MediaItem> video = MediaScanner.scanVideo(requireContext());

            // Recently added = first 10 from date-sorted scan
            List<MediaItem> recentAudio = audio.size() > 10 ? audio.subList(0, 10) : audio;
            List<MediaItem> recentVideo = video.size() > 10 ? video.subList(0, 10) : video;

            requireActivity().runOnUiThread(() -> {
                rvRecentMusic.setAdapter(new MusicCardAdapter(recentAudio, item -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
                    i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
                    startActivity(i);
                }));

                rvRecentVideos.setAdapter(new VideoCardAdapter(recentVideo, item -> {
                    Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
                    i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
                    startActivity(i);
                }));

                // Recently played = same as recent audio for now (will improve with history)
                rvRecentPlayed.setAdapter(new MusicCardAdapter(recentAudio, item -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
                    i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
                    startActivity(i);
                }));
            });
        }).start();
    }

    // ── Inline Music Card Adapter ──
    static class MusicCardAdapter extends RecyclerView.Adapter<MusicCardAdapter.VH> {
        interface OnClick { void onClick(MediaItem item); }
        final List<MediaItem> items; final OnClick click;
        MusicCardAdapter(List<MediaItem> i, OnClick c) { items = i; click = c; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_home_music_card, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); h.itemView.setOnClickListener(v -> click.onClick(items.get(pos))); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView art; TextView title, sub;
            VH(View v) { super(v); art = v.findViewById(R.id.iv_card_art); title = v.findViewById(R.id.tv_card_title); sub = v.findViewById(R.id.tv_card_sub); }
            void bind(MediaItem m) {
                title.setText(m.getTitle()); sub.setText(m.getArtist());
                Glide.with(itemView.getContext()).load(m.getAlbumArtUri()).centerCrop().placeholder(R.drawable.bg_play_button).into(art);
            }
        }
    }

    // ── Inline Video Card Adapter ──
    static class VideoCardAdapter extends RecyclerView.Adapter<VideoCardAdapter.VH> {
        interface OnClick { void onClick(MediaItem item); }
        final List<MediaItem> items; final OnClick click;
        VideoCardAdapter(List<MediaItem> i, OnClick c) { items = i; click = c; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_home_video_card, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); h.itemView.setOnClickListener(v -> click.onClick(items.get(pos))); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView thumb; TextView title, duration;
            VH(View v) { super(v); thumb = v.findViewById(R.id.iv_card_thumb); title = v.findViewById(R.id.tv_card_title); duration = v.findViewById(R.id.tv_card_duration); }
            void bind(MediaItem m) {
                title.setText(m.getTitle()); duration.setText(m.getFormattedDuration());
                Glide.with(itemView.getContext()).load(m.getPath()).centerCrop().placeholder(R.drawable.bg_play_button).into(thumb);
            }
        }
    }
}
