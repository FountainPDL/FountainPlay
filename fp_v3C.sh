#!/bin/bash
# ── v3 PART C: Fragments, Library, Settings ──
# Run from ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# 1. SelectableMediaAdapter — supports multi-select + context menu
# ════════════════════════════════════════════════════════════
cat > $P/adapter/MediaAdapter.java << 'EOF'
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
EOF

# ════════════════════════════════════════════════════════════
# 2. MusicFragment — search, sort, multi-select, batch ops, queue
# ════════════════════════════════════════════════════════════
cat > $P/ui/music/MusicFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.music;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class MusicFragment extends Fragment {

    private RecyclerView rv; private TextView tvCount;
    private MediaAdapter adapter;
    private final List<MediaItem> songs = new ArrayList<>();
    private String currentSort = "name";
    private SearchView searchView;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_music, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = view.findViewById(R.id.rv_songs);
        tvCount = view.findViewById(R.id.tv_song_count);
        searchView = view.findViewById(R.id.search_view);

        adapter = new MediaAdapter(songs, 0);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> openPlayer(item, pos));
        adapter.setOnItemLongListener((item, pos, anchor) -> showContextMenu(item, pos, anchor));

        view.findViewById(R.id.btn_shuffle_all).setOnClickListener(v -> {
            if (songs.isEmpty()) return;
            List<MediaItem> shuffled = new ArrayList<>(songs);
            Collections.shuffle(shuffled);
            PlayQueue.get().setQueue(shuffled, 0);
            openPlayer(shuffled.get(0), 0);
        });

        view.findViewById(R.id.btn_sort).setOnClickListener(v -> showSortDialog());
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> loadSongs());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadSongs();
    }

    private void loadSongs() {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders = prefs.getAudioFolders();
            List<MediaItem> result = MediaScanner.scanAudio(requireContext(),
                folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                songs.clear(); songs.addAll(result);
                adapter.updateAll(result);
                adapter.sort(currentSort);
                tvCount.setText(songs.size() + " songs");
            });
        }).start();
    }

    private void openPlayer(MediaItem item, int pos) {
        // Set queue to full list from current position
        PlayQueue.get().setQueue(songs, songs.indexOf(item));
        Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
        i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
        i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
        i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
        i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        startActivity(i);
    }

    private void showSortDialog() {
        String[] opts = {"Name","Artist","Date Added","Duration","Size","Folder"};
        String[] keys = {"name","artist","date","duration","size","folder"};
        new AlertDialog.Builder(requireContext()).setTitle("Sort by")
            .setItems(opts, (d, i) -> { currentSort = keys[i]; adapter.sort(keys[i]); })
            .show();
    }

    private void showContextMenu(MediaItem item, int pos, View anchor) {
        String[] opts = {"Play","Play Next","Add to Queue","Add to Playlist",
                         "Share","Info","Delete"};
        new AlertDialog.Builder(requireContext())
            .setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0: openPlayer(item, pos); break;
                    case 1: PlayQueue.get().addNext(item); Toast.makeText(requireContext(),"Added next",Toast.LENGTH_SHORT).show(); break;
                    case 2: PlayQueue.get().addToQueue(item); Toast.makeText(requireContext(),"Added to queue",Toast.LENGTH_SHORT).show(); break;
                    case 3: showAddToPlaylistDialog(item); break;
                    case 4: shareItem(item); break;
                    case 5: showInfo(item); break;
                    case 6: confirmDelete(item); break;
                }
            }).show();
    }

    private void showAddToPlaylistDialog(MediaItem item) {
        new Thread(() -> {
            var playlists = com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) { Toast.makeText(requireContext(),"No playlists. Create one in Library.",Toast.LENGTH_SHORT).show(); return; }
                String[] names = playlists.stream().map(p -> p.name).toArray(String[]::new);
                new AlertDialog.Builder(requireContext()).setTitle("Add to Playlist")
                    .setItems(names, (d, i) -> new Thread(() -> {
                        var song = new com.fountainpdl.fountainplay.db.entity.PlaylistSong();
                        song.playlistId = playlists.get(i).id;
                        song.path = item.getPath(); song.title = item.getTitle();
                        song.artist = item.getArtist(); song.albumArtUri = item.getAlbumArtUri();
                        song.duration = item.getDuration();
                        com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext()).playlistDao().insertSong(song);
                        com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext()).playlistDao().updateCount(playlists.get(i).id);
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),"Added!",Toast.LENGTH_SHORT).show());
                    }).start()).show();
            });
        }).start();
    }

    private void shareItem(MediaItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(item.getPath()));
        startActivity(Intent.createChooser(share, "Share"));
    }

    private void showInfo(MediaItem item) {
        String info = "Title: " + item.getTitle() + "\nArtist: " + item.getArtist()
            + "\nAlbum: " + item.getAlbum() + "\nDuration: " + item.getFormattedDuration()
            + "\nSize: " + (item.getSize()/1024/1024) + " MB\nPath: " + item.getPath()
            + "\nFolder: " + item.getFolder();
        new AlertDialog.Builder(requireContext()).setTitle("File Info")
            .setMessage(info).setPositiveButton("OK", null).show();
    }

    private void confirmDelete(MediaItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete " + item.getTitle() + "?")
            .setMessage("This will permanently delete the file.")
            .setPositiveButton("Delete", (d, i) -> {
                new java.io.File(item.getPath()).delete();
                songs.remove(item);
                adapter.updateAll(songs);
                tvCount.setText(songs.size() + " songs");
            })
            .setNegativeButton("Cancel", null).show();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 3. UPDATE fragment_music.xml — add search + sort button
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_music.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="@color/background_dark">

    <!-- Toolbar -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="8dp" android:background="@color/surface_dark">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Music" android:textSize="20sp" android:textStyle="bold"
            android:textColor="@color/white" android:paddingHorizontal="8dp" />
        <TextView android:id="@+id/tv_song_count"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:textColor="@color/on_surface_variant_dark"
            android:textSize="13sp" android:paddingStart="4dp" />
        <ImageButton android:id="@+id/btn_sort"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
        <ImageButton android:id="@+id/btn_refresh"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Search -->
    <androidx.appcompat.widget.SearchView android:id="@+id/search_view"
        android:layout_width="match_parent" android:layout_height="48dp"
        android:background="@color/surface_variant_dark" />

    <!-- Shuffle all -->
    <LinearLayout android:id="@+id/btn_shuffle_all"
        android:layout_width="match_parent" android:layout_height="44dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="16dp" android:background="@color/fp_purple_dark"
        android:clickable="true" android:focusable="true">
        <ImageView android:layout_width="18dp" android:layout_height="18dp"
            android:src="@android:drawable/ic_media_play" android:tint="@color/white" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="  Shuffle All" android:textColor="@color/white"
            android:textSize="14sp" android:textStyle="bold" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_songs"
        android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:clipToPadding="false"
        android:paddingBottom="120dp" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 4. VideoFragment — search, sort, select
# ════════════════════════════════════════════════════════════
cat > $P/ui/video/VideoFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.video;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class VideoFragment extends Fragment {

    private RecyclerView rv; private TextView tvCount;
    private MediaAdapter adapter;
    private final List<MediaItem> videos = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_video, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rv = view.findViewById(R.id.rv_videos);
        tvCount = view.findViewById(R.id.tv_video_count);
        SearchView sv = view.findViewById(R.id.search_view_video);

        adapter = new MediaAdapter(videos, 1);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> {
            Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
            i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
            startActivity(i);
        });
        adapter.setOnItemLongListener((item, pos, anchor) -> showVideoMenu(item));

        view.findViewById(R.id.btn_sort_video).setOnClickListener(v -> {
            String[] opts = {"Name","Date Added","Size","Duration","Folder"};
            String[] keys = {"name","date","size","duration","folder"};
            new AlertDialog.Builder(requireContext()).setTitle("Sort by")
                .setItems(opts, (d,i) -> adapter.sort(keys[i])).show();
        });
        view.findViewById(R.id.btn_refresh_video).setOnClickListener(v -> loadVideos());

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadVideos();
    }

    private void loadVideos() {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders = prefs.getVideoFolders();
            List<MediaItem> result = MediaScanner.scanVideo(requireContext(), folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                videos.clear(); videos.addAll(result);
                adapter.updateAll(result);
                tvCount.setText(videos.size() + " videos");
            });
        }).start();
    }

    private void showVideoMenu(MediaItem item) {
        String[] opts = {"Play","Share","Info","Delete"};
        new AlertDialog.Builder(requireContext()).setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch(i) {
                    case 0: Intent intent = new Intent(requireContext(), VideoPlayerActivity.class); intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath()); startActivity(intent); break;
                    case 1: Intent share = new Intent(Intent.ACTION_SEND); share.setType("video/*"); share.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(item.getPath())); startActivity(Intent.createChooser(share,"Share")); break;
                    case 2: new AlertDialog.Builder(requireContext()).setTitle("Info").setMessage("Title: "+item.getTitle()+"\nDuration: "+item.getFormattedDuration()+"\nSize: "+(item.getSize()/1024/1024)+" MB\nPath: "+item.getPath()).setPositiveButton("OK",null).show(); break;
                    case 3: new AlertDialog.Builder(requireContext()).setTitle("Delete?").setPositiveButton("Delete",(dd,ii)->{ new java.io.File(item.getPath()).delete(); videos.remove(item); adapter.updateAll(videos); }).setNegativeButton("Cancel",null).show(); break;
                }
            }).show();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 5. fragment_video.xml — with search + sort
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_video.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:background="@color/background_dark">

    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="8dp" android:background="@color/surface_dark">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Videos" android:textSize="20sp" android:textStyle="bold"
            android:textColor="@color/white" android:paddingHorizontal="8dp" />
        <TextView android:id="@+id/tv_video_count"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
        <ImageButton android:id="@+id/btn_sort_video"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
        <ImageButton android:id="@+id/btn_refresh_video"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <androidx.appcompat.widget.SearchView android:id="@+id/search_view_video"
        android:layout_width="match_parent" android:layout_height="48dp"
        android:background="@color/surface_variant_dark" />

    <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_videos"
        android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:clipToPadding="false"
        android:paddingHorizontal="4dp" android:paddingBottom="120dp" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 6. LibraryFragment — working playlists + history
# ════════════════════════════════════════════════════════════
cat > $P/ui/library/LibraryFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.fountainpdl.fountainplay.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {
    private static final String[] TABS = {"Songs","Albums","Artists","Playlists","History"};

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_library, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 vp = view.findViewById(R.id.view_pager);
        TabLayout tabs = view.findViewById(R.id.tab_layout);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                return LibraryPageFragment.newInstance(TABS[pos]);
            }
            @Override public int getItemCount() { return TABS.length; }
        });
        new TabLayoutMediator(tabs, vp, (tab, pos) -> tab.setText(TABS[pos])).attach();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 7. LibraryPageFragment — real data per tab
# ════════════════════════════════════════════════════════════
cat > $P/ui/library/LibraryPageFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.*;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class LibraryPageFragment extends Fragment {
    private static final String ARG_TAB = "tab";
    public static LibraryPageFragment newInstance(String tab) {
        LibraryPageFragment f = new LibraryPageFragment();
        Bundle b = new Bundle(); b.putString(ARG_TAB, tab); f.setArguments(b); return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_library_page, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tab = getArguments() != null ? getArguments().getString(ARG_TAB,"Songs") : "Songs";
        RecyclerView rv = view.findViewById(R.id.rv_library);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (tab.equals("Playlists")) { loadPlaylists(rv, tvEmpty); return; }
        if (tab.equals("History"))   { loadHistory(rv, tvEmpty);   return; }

        List<MediaItem> items = new ArrayList<>();
        MediaAdapter adapter = new MediaAdapter(items, 0);
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((item, pos) -> {
            PlayQueue.get().setQueue(items, items.indexOf(item));
            Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
            i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
            startActivity(i);
        });

        new Thread(() -> {
            List<MediaItem> all = MediaScanner.scanAudio(requireContext());
            List<MediaItem> result;
            switch (tab) {
                case "Albums":  result = onePerAlbum(all); break;
                case "Artists": result = onePerArtist(all); break;
                default: result = all;
            }
            final List<MediaItem> fr = result;
            requireActivity().runOnUiThread(() -> {
                items.clear(); items.addAll(fr);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void loadHistory(RecyclerView rv, TextView tvEmpty) {
        new Thread(() -> {
            List<HistoryItem> history = AppDatabase.get(requireContext()).historyDao().getAll();
            requireActivity().runOnUiThread(() -> {
                if (history.isEmpty()) { tvEmpty.setText("No history yet"); tvEmpty.setVisibility(View.VISIBLE); return; }
                // Convert to MediaItems for display
                List<MediaItem> items = new ArrayList<>();
                for (HistoryItem h : history) {
                    MediaItem m = new MediaItem(0, h.title, h.artist, h.path, h.duration, h.type);
                    m.setAlbumArtUri(h.albumArtUri); items.add(m);
                }
                MediaAdapter adapter = new MediaAdapter(items, 0);
                rv.setAdapter(adapter);
                adapter.setOnItemClickListener((item, pos) -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    startActivity(i);
                });
            });
        }).start();
    }

    private void loadPlaylists(RecyclerView rv, TextView tvEmpty) {
        // Playlists use a simple TextView list adapter
        new Thread(() -> {
            List<PlaylistEntity> playlists = AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) {
                    tvEmpty.setText("No playlists yet.\nLong-press a song to add to playlist.");
                    tvEmpty.setVisibility(View.VISIBLE);
                }

                // Show create playlist button at top
                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);

                Button btnCreate = new Button(requireContext());
                btnCreate.setText("+ Create Playlist");
                btnCreate.setOnClickListener(v -> {
                    EditText et = new EditText(requireContext());
                    et.setHint("Playlist name");
                    new AlertDialog.Builder(requireContext()).setTitle("New Playlist")
                        .setView(et).setPositiveButton("Create", (d, i) -> {
                            String name = et.getText().toString().trim();
                            if (name.isEmpty()) return;
                            new Thread(() -> {
                                PlaylistEntity p = new PlaylistEntity();
                                p.name = name; p.createdAt = System.currentTimeMillis();
                                AppDatabase.get(requireContext()).playlistDao().insertPlaylist(p);
                                requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                            }).start();
                        }).setNegativeButton("Cancel", null).show();
                });
                container.addView(btnCreate);

                for (PlaylistEntity p : playlists) {
                    TextView tv = new TextView(requireContext());
                    tv.setText("▶ " + p.name + "  (" + p.songCount + " songs)");
                    tv.setTextColor(0xFFFFFFFF); tv.setTextSize(15); tv.setPadding(32,32,32,32);
                    tv.setOnClickListener(vv -> openPlaylist(p));
                    tv.setOnLongClickListener(vv -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Delete \"" + p.name + "\"?")
                            .setPositiveButton("Delete", (d,i) -> new Thread(() -> {
                                AppDatabase.get(requireContext()).playlistDao().deletePlaylist(p);
                                requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                            }).start())
                            .setNegativeButton("Cancel", null).show();
                        return true;
                    });
                    container.addView(tv);
                }

                rv.setAdapter(null);
                // Wrap container in a ScrollView via a simple adapter
                rv.setAdapter(new RecyclerView.Adapter() {
                    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                        return new RecyclerView.ViewHolder(container) {};
                    }
                    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {}
                    @Override public int getItemCount() { return 1; }
                });
            });
        }).start();
    }

    private void openPlaylist(PlaylistEntity playlist) {
        new Thread(() -> {
            List<com.fountainpdl.fountainplay.db.entity.PlaylistSong> songs =
                AppDatabase.get(requireContext()).playlistDao().getSongsForPlaylist(playlist.id);
            if (songs.isEmpty()) { requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),"Playlist is empty",Toast.LENGTH_SHORT).show()); return; }
            List<MediaItem> items = new ArrayList<>();
            for (var s : songs) { MediaItem m = new MediaItem(0, s.title, s.artist, s.path, s.duration, 0); m.setAlbumArtUri(s.albumArtUri); items.add(m); }
            requireActivity().runOnUiThread(() -> {
                PlayQueue.get().setQueue(items, 0);
                Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                i.putExtra(AudioPlayerActivity.EXTRA_URI, items.get(0).getPath());
                i.putExtra(AudioPlayerActivity.EXTRA_TITLE, items.get(0).getTitle());
                i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, items.get(0).getArtist());
                startActivity(i);
            });
        }).start();
    }

    private List<MediaItem> onePerAlbum(List<MediaItem> all) {
        Map<String,MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getAlbum(), m);
        return new ArrayList<>(map.values());
    }
    private List<MediaItem> onePerArtist(List<MediaItem> all) {
        Map<String,MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getArtist(), m);
        return new ArrayList<>(map.values());
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 8. SettingsFragment — working theme + color
# ════════════════════════════════════════════════════════════
cat > $P/ui/settings/SettingsFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.*;

public class SettingsFragment extends Fragment {
    private AppPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        // Theme
        TextView tvTheme = view.findViewById(R.id.tv_theme_value);
        tvTheme.setText(capitalize(prefs.getTheme()));
        view.findViewById(R.id.pref_theme).setOnClickListener(v -> {
            String[] opts = {"Dark","Light","System (Auto)","AMOLED"};
            String[] vals = {"dark","light","system","amoled"};
            new AlertDialog.Builder(requireContext()).setTitle("Theme")
                .setItems(opts, (d, i) -> {
                    prefs.setTheme(vals[i]);
                    tvTheme.setText(opts[i]);
                    FountainApp.applyTheme(vals[i]);
                    requireActivity().recreate();
                }).show();
        });

        // Primary color
        view.findViewById(R.id.pref_primary_color).setOnClickListener(v -> showColorPicker(true));
        view.findViewById(R.id.pref_accent_color).setOnClickListener(v -> showColorPicker(false));

        // Speed
        TextView tvSpeed = view.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        view.findViewById(R.id.pref_speed).setOnClickListener(v -> {
            String[] opts = {"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
            float[] vals = {0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f};
            new AlertDialog.Builder(requireContext()).setTitle("Default Speed")
                .setItems(opts, (d,i) -> { prefs.setPlaybackSpeed(vals[i]); tvSpeed.setText(opts[i]); }).show();
        });

        // Skip
        TextView tvSkip = view.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + " seconds");
        view.findViewById(R.id.pref_skip).setOnClickListener(v -> {
            String[] opts = {"5s","10s","15s","30s","60s"};
            int[] vals = {5,10,15,30,60};
            new AlertDialog.Builder(requireContext()).setTitle("Skip Interval")
                .setItems(opts,(d,i)->{ prefs.setSkipInterval(vals[i]); tvSkip.setText(opts[i]); }).show();
        });

        // Switches
        ((SwitchCompat)view.findViewById(R.id.sw_resume)).setChecked(prefs.getResumePlayback());
        ((SwitchCompat)view.findViewById(R.id.sw_resume)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_RESUME_PLAYBACK,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_hw_accel)).setChecked(prefs.getHwAcceleration());
        ((SwitchCompat)view.findViewById(R.id.sw_hw_accel)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_HW_ACCELERATION,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_gestures)).setChecked(prefs.getGesturesEnabled());
        ((SwitchCompat)view.findViewById(R.id.sw_gestures)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_GESTURES_ENABLED,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_remember_pos)).setChecked(prefs.getRememberPosition());
        ((SwitchCompat)view.findViewById(R.id.sw_remember_pos)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_REMEMBER_POSITION,c2));

        setupFolderPref(view, true);
        setupFolderPref(view, false);

        // Clear history
        view.findViewById(R.id.btn_clear_history).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext()).setTitle("Clear History?")
                .setPositiveButton("Clear", (d,i) -> new Thread(() ->
                    com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext()).historyDao().clearAll()
                ).start())
                .setNegativeButton("Cancel",null).show());
    }

    private void showColorPicker(boolean isPrimary) {
        int[] colors = {0xFF7B2FBE,0xFF2196F3,0xFF4CAF50,0xFFFF5722,0xFFE91E63,0xFF009688,0xFF673AB7,0xFF795548};
        String[] names = {"Purple","Blue","Green","Orange","Pink","Teal","Deep Purple","Brown"};
        new AlertDialog.Builder(requireContext()).setTitle(isPrimary ? "Primary Color" : "Accent Color")
            .setItems(names, (d,i) -> {
                if (isPrimary) prefs.setPrimaryColor(colors[i]);
                else prefs.setAccentColor(colors[i]);
                Toast.makeText(requireContext(),"Restart app to apply",Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId   = isAudio ? R.id.tv_audio_folders   : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> cur = isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders();
        tv.setText(cur.isEmpty() ? "All folders" : cur.size() + " selected");
        view.findViewById(prefId).setOnClickListener(v -> new Thread(() -> {
            List<String> folders = isAudio ? MediaScanner.getAudioFolders(requireContext()) : MediaScanner.getVideoFolders(requireContext());
            if (folders.isEmpty()) return;
            String[] items = folders.toArray(new String[0]);
            boolean[] checked = new boolean[items.length];
            Set<String> sel = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
            for (int i=0;i<items.length;i++) checked[i] = sel.contains(items[i]);
            requireActivity().runOnUiThread(() ->
                new AlertDialog.Builder(requireContext())
                    .setTitle(isAudio ? "Music Folders" : "Video Folders")
                    .setMultiChoiceItems(items, checked, (d,i,c2) -> { if(c2) sel.add(items[i]); else sel.remove(items[i]); })
                    .setPositiveButton("OK",(d,i)->{
                        if(isAudio) prefs.setAudioFolders(sel); else prefs.setVideoFolders(sel);
                        tv.setText(sel.isEmpty()?"All folders":sel.size()+" selected");
                    }).setNegativeButton("Cancel",null).show());
        }).start());
    }

    private String capitalize(String s) { return s==null||s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }
}
EOF

# ════════════════════════════════════════════════════════════
# 9. Add btn_clear_history to settings layout
# ════════════════════════════════════════════════════════════
python3 << 'PYEOF'
with open("app/src/main/res/layout/fragment_settings.xml","r") as f:
    c = f.read()
inject = '''
        <!-- CLEAR HISTORY -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  DATA" android:textSize="11sp" android:textColor="#BB86FC"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />
        <LinearLayout android:id="@+id/btn_clear_history"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Clear Play History" android:textColor="#EF5350" android:textSize="15sp" />
        </LinearLayout>

    </LinearLayout>
</androidx.core.widget.NestedScrollView>'''
c = c.replace("    </LinearLayout>\n</androidx.core.widget.NestedScrollView>", inject)
with open("app/src/main/res/layout/fragment_settings.xml","w") as f:
    f.write(c)
print("Settings layout updated")
PYEOF

echo ""
echo "✅ PART C DONE — run fp_v3D.sh next"
