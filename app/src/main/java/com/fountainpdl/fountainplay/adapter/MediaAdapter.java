package com.fountainpdl.fountainplay.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.*;

public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClickListener  { void onItemClick(MediaItem item, int pos); }
    public interface OnItemLongListener  { void onItemLong(MediaItem item, int pos, View anchor); }

    private List<MediaItem> items;
    private final List<MediaItem> allItems = new ArrayList<>();
    private final int viewType;
    private final Set<Integer> selected = new HashSet<>();
    private boolean selectionMode = false;
    private OnItemClickListener clickListener;
    private OnItemLongListener  longListener;

    public MediaAdapter(List<MediaItem> items, int viewType) {
        this.items = items;
        this.allItems.addAll(items);
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener l) { clickListener = l; }
    public void setOnItemLongListener(OnItemLongListener l)   { longListener = l; }

    // ── Filtering ──
    public void filter(String query) {
        items.clear();
        if (query == null || query.isEmpty()) { items.addAll(allItems); }
        else {
            String q = query.toLowerCase();
            for (MediaItem m : allItems)
                if (m.getTitle().toLowerCase().contains(q) || m.getArtist().toLowerCase().contains(q))
                    items.add(m);
        }
        notifyDataSetChanged();
    }

    public void updateAll(List<MediaItem> newItems) {
        allItems.clear(); allItems.addAll(newItems);
        items.clear(); items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ── Sorting ──
    public void sort(String by) {
        switch (by) {
            case "name":  Collections.sort(items, (a,b) -> a.getTitle().compareToIgnoreCase(b.getTitle())); break;
            case "artist":Collections.sort(items, (a,b) -> a.getArtist().compareToIgnoreCase(b.getArtist())); break;
            case "size":  Collections.sort(items, (a,b) -> Long.compare(b.getSize(), a.getSize())); break;
            case "date":  Collections.sort(items, (a,b) -> Long.compare(b.getDateAdded(), a.getDateAdded())); break;
            case "duration": Collections.sort(items, (a,b) -> Long.compare(b.getDuration(), a.getDuration())); break;
            case "folder": Collections.sort(items, (a,b) -> a.getFolder().compareToIgnoreCase(b.getFolder())); break;
        }
        notifyDataSetChanged();
    }

    // ── Selection ──
    public void toggleSelection(int pos) {
        if (selected.contains(pos)) selected.remove(pos); else selected.add(pos);
        notifyItemChanged(pos);
    }
    public void selectAll() { for (int i=0;i<items.size();i++) selected.add(i); notifyDataSetChanged(); }
    public void clearSelection() { selected.clear(); selectionMode = false; notifyDataSetChanged(); }
    public boolean isSelectionMode() { return !selected.isEmpty(); }
    public Set<Integer> getSelected() { return selected; }
    public List<MediaItem> getSelectedItems() {
        List<MediaItem> r = new ArrayList<>();
        for (int i : selected) if (i < items.size()) r.add(items.get(i));
        return r;
    }

    @Override public int getItemViewType(int pos) { return viewType; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (vt == 0) return new AudioVH(inf.inflate(R.layout.item_song, parent, false));
        else return new VideoVH(inf.inflate(R.layout.item_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        MediaItem item = items.get(pos);
        boolean isSel = selected.contains(pos);
        holder.itemView.setActivated(isSel);
        holder.itemView.setAlpha(isSel ? 0.7f : 1.0f);
        if (holder instanceof AudioVH) ((AudioVH)holder).bind(item);
        else ((VideoVH)holder).bind(item);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode()) { toggleSelection(pos); }
            else if (clickListener != null) clickListener.onItemClick(item, pos);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) longListener.onItemLong(item, pos, v);
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    // ── ViewHolders ──
    static class AudioVH extends RecyclerView.ViewHolder {
        ImageView art; TextView title, artist, duration;
        AudioVH(View v) { super(v);
            art = v.findViewById(R.id.iv_album_art);
            title = v.findViewById(R.id.tv_song_title);
            artist = v.findViewById(R.id.tv_song_artist);
            duration = v.findViewById(R.id.tv_song_duration); }
        void bind(MediaItem item) {
            title.setText(item.getTitle());
            artist.setText(item.getArtist());
            duration.setText(item.getFormattedDuration());
            if (item.getAlbumArtUri() != null)
                Glide.with(itemView).load(item.getAlbumArtUri()).centerCrop()
                    .placeholder(R.drawable.bg_play_button).into(art);
            else art.setImageResource(R.drawable.bg_play_button);
        }
    }

    static class VideoVH extends RecyclerView.ViewHolder {
        ImageView thumb; TextView title, info, dur;
        VideoVH(View v) { super(v);
            thumb = v.findViewById(R.id.iv_thumbnail);
            title = v.findViewById(R.id.tv_video_title);
            info = v.findViewById(R.id.tv_video_info);
            dur = v.findViewById(R.id.tv_duration_badge); }
        void bind(MediaItem item) {
            title.setText(item.getTitle()); dur.setText(item.getFormattedDuration());
            info.setText((item.getSize() / (1024*1024)) + " MB");
            Glide.with(itemView).load(item.getPath()).centerCrop()
                .placeholder(R.drawable.bg_play_button).into(thumb);
        }
    }
}
