package com.fountainpdl.fountainplay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.Fx;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
    public interface OnClick { void onClick(MediaItem item); }
    private final List<MediaItem> items;
    private final int currentIndex;
    private final OnClick click;

    public QueueAdapter(List<MediaItem> items, int cur, OnClick click) {
        this.items = items; this.currentIndex = cur; this.click = click;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_queue, p, false);
        Fx.press(v);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MediaItem item = items.get(pos);
        h.num.setText(String.valueOf(pos + 1));
        h.title.setText(item.getTitle());
        h.artist.setText(item.getArtist());
        h.dur.setText(item.getDuration() > 0 ? item.getFormattedDuration() : "");
        h.indicator.setVisibility(pos == currentIndex ? View.VISIBLE : View.GONE);
        h.num.setVisibility(pos == currentIndex ? View.GONE : View.VISIBLE);
        if (item.getAlbumArtUri() != null)
            Glide.with(h.art.getContext()).load(item.getAlbumArtUri()).centerCrop()
                .placeholder(R.drawable.bg_art_placeholder).error(R.drawable.bg_art_placeholder).into(h.art);
        else h.art.setImageResource(R.drawable.bg_art_placeholder);
        h.itemView.setAlpha(pos < currentIndex ? 0.5f : 1.0f);
        h.itemView.setOnClickListener(v -> click.onClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView num, title, artist, dur;
        final ImageView art, indicator;
        VH(View v) {
            super(v);
            num = v.findViewById(R.id.tv_queue_num);
            title = v.findViewById(R.id.tv_queue_title);
            artist = v.findViewById(R.id.tv_queue_artist);
            dur = v.findViewById(R.id.tv_queue_dur);
            art = v.findViewById(R.id.iv_queue_art);
            indicator = v.findViewById(R.id.iv_now_playing_indicator);
        }
    }
}
