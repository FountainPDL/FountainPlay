package com.fountainpdl.fountainplay.adapter;

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
        if (vt == 0) return new AudioVH(inf.inflate(R.layout.item_song, parent, false));
        else return new VideoVH(inf.inflate(R.layout.item_video, parent, false));
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
                Glide.with(itemView.getContext()).load(item.getAlbumArtUri())
                    .centerCrop().into(art);
            }
        }
    }

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
            Glide.with(itemView.getContext()).load(item.getPath())
                .centerCrop().into(thumbnail);
        }
    }
}
