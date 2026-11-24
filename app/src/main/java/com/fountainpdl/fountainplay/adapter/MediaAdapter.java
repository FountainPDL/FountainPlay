package com.fountainpdl.fountainplay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.fountainpdl.fountainplay.util.ResumeStore;
import com.fountainpdl.fountainplay.util.ThemeColors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Songs and videos as rows (MODE_ROW) or as a video card grid (MODE_GRID). Handles filtering and sorting itself. */
public class MediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements PlaybackState.Listener {

    public static final int MODE_ROW = 0, MODE_GRID = 1;

    public interface OnItemClick { void onClick(MediaItem item, int position); }
    public interface OnItemMore  { void onMore(MediaItem item, View anchor); }

    private final int mode;
    private List<MediaItem> all = new ArrayList<>();
    private List<MediaItem> shown = new ArrayList<>();
    private String query = "";
    private String sortKey = "date";
    private boolean ascending = false;
    private OnItemClick click;
    private OnItemMore more;
    private int lastAnimated = -1;
    private int primary, onSurface;
    private ResumeStore resume;
    private RecyclerView attached;

    public MediaAdapter(int mode) { this.mode = mode; }

    public void setOnItemClick(OnItemClick l) { click = l; }
    public void setOnItemMore(OnItemMore l) { more = l; }

    public void submit(List<MediaItem> items) {
        all = new ArrayList<>(items);
        apply();
    }

    public void setQuery(String q) {
        query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        apply();
    }

    /** Choosing the same key again flips the direction. */
    public void setSort(String key) {
        if (key.equals(sortKey)) ascending = !ascending;
        else { sortKey = key; ascending = "title".equals(key) || "artist".equals(key); }
        apply();
    }

    public String getSortKey() { return sortKey; }
    public boolean isAscending() { return ascending; }
    public List<MediaItem> getShown() { return new ArrayList<>(shown); }

    private void apply() {
        List<MediaItem> out = new ArrayList<>();
        for (MediaItem m : all) {
            if (query.isEmpty()
                || m.getTitle().toLowerCase(Locale.ROOT).contains(query)
                || m.getArtist().toLowerCase(Locale.ROOT).contains(query)
                || m.getAlbum().toLowerCase(Locale.ROOT).contains(query)
                || m.getFileName().toLowerCase(Locale.ROOT).contains(query)) out.add(m);
        }
        final boolean asc = ascending;
        Collections.sort(out, (a, b) -> {
            int r;
            switch (sortKey) {
                case "title":    r = a.getTitle().compareToIgnoreCase(b.getTitle()); break;
                case "artist":   r = a.getArtist().compareToIgnoreCase(b.getArtist()); break;
                case "duration": r = Long.compare(a.getDuration(), b.getDuration()); break;
                case "size":     r = Long.compare(a.getSize(), b.getSize()); break;
                default:         r = Long.compare(a.getDateAdded(), b.getDateAdded()); break;
            }
            return asc ? r : -r;
        });
        shown = out;
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    @Override public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        attached = rv;
        PlaybackState.get().addListener(this);
        Context c = rv.getContext();
        primary = ThemeColors.get(c, "colorPrimary");
        onSurface = ThemeColors.get(c, "colorOnSurface");
        resume = new ResumeStore(c);
    }

    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        PlaybackState.get().removeListener(this);
        attached = null;
    }

    // Highlight the playing song
    @Override public void onItemChanged(MediaItem item) { refreshSoon(); }
    @Override public void onPlayStateChanged(boolean playing) { }
    @Override public void onPositionChanged(long pos, long duration) { }
    private void refreshSoon() {
        if (attached != null) attached.post(this::notifyDataSetChanged);
    }

    @Override public int getItemViewType(int pos) { return mode; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder h = vt == MODE_GRID
            ? new GridVH(inf.inflate(R.layout.item_video, parent, false))
            : new RowVH(inf.inflate(R.layout.item_song, parent, false));
        Fx.press(h.itemView);
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final MediaItem item = shown.get(position);
        Fx.reset(holder.itemView);
        if (holder instanceof RowVH) bindRow((RowVH) holder, item);
        else bindGrid((GridVH) holder, item);

        holder.itemView.setOnClickListener(v -> {
            int p = holder.getBindingAdapterPosition();
            if (click != null && p != RecyclerView.NO_POSITION) click.onClick(item, p);
        });
        if (position > lastAnimated && position < 14) {
            lastAnimated = position;
            Fx.enter(holder.itemView, position);
        }
    }

    private void bindRow(RowVH h, MediaItem item) {
        h.title.setText(item.getTitle());
        h.sub.setText(item.isVideo()
            ? "Video · " + String.format(Locale.US, "%.0f MB", item.getSize() / 1024f / 1024f)
            : item.getArtist());
        h.dur.setText(item.getDuration() > 0 ? item.getFormattedDuration() : "");
        boolean playing = item.isAudio() && PlaybackState.get().getCurrentItem() != null
            && item.getPath() != null && item.getPath().equals(PlaybackState.get().getCurrentItem().getPath());
        h.title.setTextColor(playing ? primary : onSurface);
        if (item.isVideo()) {
            Glide.with(h.art).load(item.getPath()).centerCrop().placeholder(R.drawable.bg_video_placeholder).into(h.art);
        } else if (item.getAlbumArtUri() != null) {
            Glide.with(h.art).load(item.getAlbumArtUri()).centerCrop()
                .placeholder(R.drawable.bg_art_placeholder).error(R.drawable.bg_art_placeholder).into(h.art);
        } else {
            h.art.setImageResource(R.drawable.bg_art_placeholder);
        }
        h.more.setOnClickListener(v -> { if (more != null) more.onMore(item, v); });
        h.itemView.setOnLongClickListener(v -> { if (more != null) more.onMore(item, h.more); return true; });
    }

    private void bindGrid(GridVH h, MediaItem item) {
        h.title.setText(item.getTitle());
        h.info.setText(String.format(Locale.US, "%.0f MB", item.getSize() / 1024f / 1024f));
        h.dur.setText(item.getFormattedDuration());
        Glide.with(h.thumb).load(item.getPath()).centerCrop().placeholder(R.drawable.bg_video_placeholder).into(h.thumb);
        long pos = resume == null ? 0 : resume.getPosition(item.getPath());
        if (pos > 0 && item.getDuration() > 0) {
            h.progress.setVisibility(View.VISIBLE);
            h.progress.setProgress((int) Math.min(1000, pos * 1000 / item.getDuration()));
        } else {
            h.progress.setVisibility(View.GONE);
        }
        h.more.setOnClickListener(v -> { if (more != null) more.onMore(item, v); });
        h.itemView.setOnLongClickListener(v -> { if (more != null) more.onMore(item, h.more); return true; });
    }

    @Override public int getItemCount() { return shown.size(); }

    static class RowVH extends RecyclerView.ViewHolder {
        final ImageView art; final TextView title, sub, dur; final ImageButton more;
        RowVH(View v) {
            super(v);
            art = v.findViewById(R.id.iv_album_art);
            title = v.findViewById(R.id.tv_song_title);
            sub = v.findViewById(R.id.tv_song_artist);
            dur = v.findViewById(R.id.tv_song_duration);
            more = v.findViewById(R.id.btn_song_more);
        }
    }

    static class GridVH extends RecyclerView.ViewHolder {
        final ImageView thumb; final TextView title, info, dur; final ImageButton more; final ProgressBar progress;
        GridVH(View v) {
            super(v);
            thumb = v.findViewById(R.id.iv_thumbnail);
            title = v.findViewById(R.id.tv_video_title);
            info = v.findViewById(R.id.tv_video_info);
            dur = v.findViewById(R.id.tv_duration_badge);
            more = v.findViewById(R.id.btn_video_more);
            progress = v.findViewById(R.id.pb_watched);
        }
    }
}
