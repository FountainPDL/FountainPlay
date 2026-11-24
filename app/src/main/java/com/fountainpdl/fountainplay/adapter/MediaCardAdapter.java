package com.fountainpdl.fountainplay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.ResumeStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Horizontal carousel cards on Home. */
public class MediaCardAdapter extends RecyclerView.Adapter<MediaCardAdapter.VH> {

    public interface OnClick { void onClick(List<MediaItem> list, int position); }

    private final List<MediaItem> items;
    private final boolean videoCards;
    private final boolean showProgress;
    private final OnClick click;
    private ResumeStore resume;

    public MediaCardAdapter(List<MediaItem> items, boolean videoCards, boolean showProgress, OnClick click) {
        this.items = new ArrayList<>(items);
        this.videoCards = videoCards;
        this.showProgress = showProgress;
        this.click = click;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (resume == null) resume = new ResumeStore(parent.getContext());
        View v = LayoutInflater.from(parent.getContext())
            .inflate(videoCards ? R.layout.item_card_video : R.layout.item_card_music, parent, false);
        Fx.press(v);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MediaItem item = items.get(position);
        h.title.setText(item.getTitle());
        if (videoCards) {
            h.sub.setText(String.format(Locale.US, "%.0f MB", item.getSize() / 1024f / 1024f));
            if (h.badge != null) h.badge.setText(item.getDuration() > 0 ? item.getFormattedDuration() : "");
            Glide.with(h.art).load(item.getPath()).centerCrop().placeholder(R.drawable.bg_video_placeholder).into(h.art);
            if (h.progress != null) {
                long pos = showProgress ? resume.getPosition(item.getPath()) : 0;
                if (pos > 0 && item.getDuration() > 0) {
                    h.progress.setVisibility(View.VISIBLE);
                    h.progress.setProgress((int) Math.min(1000, pos * 1000 / item.getDuration()));
                } else {
                    h.progress.setVisibility(View.GONE);
                }
            }
        } else {
            h.sub.setText(item.getArtist());
            if (item.getAlbumArtUri() != null) {
                Glide.with(h.art).load(item.getAlbumArtUri()).centerCrop()
                    .placeholder(R.drawable.bg_art_placeholder).error(R.drawable.bg_art_placeholder).into(h.art);
            } else {
                h.art.setImageResource(R.drawable.bg_art_placeholder);
            }
        }
        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) click.onClick(items, p);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView art; final TextView title, sub, badge; final ProgressBar progress;
        VH(View v) {
            super(v);
            art = v.findViewById(R.id.iv_card_art);
            title = v.findViewById(R.id.tv_card_title);
            sub = v.findViewById(R.id.tv_card_sub);
            badge = v.findViewById(R.id.tv_card_badge);
            progress = v.findViewById(R.id.pb_card_progress);
        }
    }
}
