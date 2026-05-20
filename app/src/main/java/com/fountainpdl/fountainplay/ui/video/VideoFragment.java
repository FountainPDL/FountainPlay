package com.fountainpdl.fountainplay.ui.video;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class VideoFragment extends Fragment {

    private MediaAdapter adapter;
    private final List<MediaItem> videos = new ArrayList<>();
    private boolean isGrid = true;
    private RecyclerView rv;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_video, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rv_videos);
        TextView tvCount = view.findViewById(R.id.tv_video_count);
        SearchView sv = view.findViewById(R.id.search_view_video);

        adapter = new MediaAdapter(videos, 1);
        applyLayout();
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> {
            Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
            i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
            startActivity(i);
        });

        adapter.setOnItemLongListener((item, pos, anchor) ->
            showVideoMenu(item, pos));

        view.findViewById(R.id.btn_sort_video).setOnClickListener(v ->
            showSortDialog());

        view.findViewById(R.id.btn_toggle_layout).setOnClickListener(v -> {
            isGrid = !isGrid;
            applyLayout();
            ((ImageButton)v).setImageResource(isGrid
                ? android.R.drawable.ic_menu_sort_by_size
                : android.R.drawable.ic_menu_agenda);
        });

        view.findViewById(R.id.btn_refresh_video).setOnClickListener(v ->
            loadVideos(tvCount));

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadVideos(tvCount);
    }

    private void applyLayout() {
        if (isGrid) {
            rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        } else {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    private void loadVideos(TextView tvCount) {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders  = prefs.getVideoFolders();
            List<MediaItem> result = MediaScanner.scanVideo(
                requireContext(), folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                videos.clear(); videos.addAll(result);
                adapter.updateAll(result);
                tvCount.setText(videos.size() + " videos");
            });
        }).start();
    }

    private void showSortDialog() {
        String[] labels = {"Name","Date Added","Size","Duration","Folder"};
        String[] keys   = {"name","date","size","duration","folder"};
        new AlertDialog.Builder(requireContext()).setTitle("Sort by")
            .setItems(labels, (d, i) -> adapter.sort(keys[i]))
            .show();
    }

    private void showVideoMenu(MediaItem item, int pos) {
        String[] opts = {
            "▶  Play",
            "⭐  Add to Favourites",
            "↗  Share",
            "ℹ  Info",
            "🗑  Delete"
        };
        new AlertDialog.Builder(requireContext())
            .setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0:
                        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                        intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
                        intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
                        startActivity(intent);
                        break;
                    case 1: addVideoToFavourites(item); break;
                    case 2:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("video/*");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
                        startActivity(Intent.createChooser(share,"Share"));
                        break;
                    case 3: showVideoInfo(item); break;
                    case 4: confirmDeleteVideo(item); break;
                }
            }).show();
    }

    private void addVideoToFavourites(MediaItem item) {
        new Thread(() -> {
            com.fountainpdl.fountainplay.db.AppDatabase db =
                com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext());
            List<com.fountainpdl.fountainplay.db.entity.PlaylistEntity> all =
                db.playlistDao().getAllPlaylists();
            com.fountainpdl.fountainplay.db.entity.PlaylistEntity fav = null;
            for (com.fountainpdl.fountainplay.db.entity.PlaylistEntity p : all)
                if ("Favourites".equals(p.name)) { fav = p; break; }
            if (fav == null) {
                com.fountainpdl.fountainplay.db.entity.PlaylistEntity nf =
                    new com.fountainpdl.fountainplay.db.entity.PlaylistEntity();
                nf.name = "Favourites"; nf.createdAt = System.currentTimeMillis();
                long id = db.playlistDao().insertPlaylist(nf); nf.id = (int) id; fav = nf;
            }
            com.fountainpdl.fountainplay.db.entity.PlaylistSong s =
                new com.fountainpdl.fountainplay.db.entity.PlaylistSong();
            s.playlistId = fav.id; s.path = item.getPath();
            s.title = item.getTitle(); s.artist = "Video";
            s.duration = item.getDuration();
            db.playlistDao().insertSong(s);
            db.playlistDao().updateCount(fav.id);
            requireActivity().runOnUiThread(()->
                Toast.makeText(requireContext(),"Added to Favourites ⭐",Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showVideoInfo(MediaItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Video Info")
            .setMessage("Title:    " + item.getTitle()
                + "\nDuration: " + item.getFormattedDuration()
                + "\nSize:     " + (item.getSize()/1024/1024) + " MB"
                + "\nFolder:   " + item.getFolder()
                + "\nPath:     " + item.getPath())
            .setPositiveButton("OK", null).show();
    }

    private void confirmDeleteVideo(MediaItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete \"" + item.getTitle() + "\"?")
            .setMessage("This permanently deletes the video file.")
            .setPositiveButton("Delete", (d, i) -> new Thread(() -> {
                boolean ok = false;
                try {
                    int rows = requireContext().getContentResolver().delete(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        android.provider.MediaStore.Video.Media.DATA + "=?",
                        new String[]{item.getPath()});
                    ok = rows > 0;
                } catch (Exception ignored) {}
                if (!ok) { java.io.File f = new java.io.File(item.getPath()); ok = f.exists() && f.delete(); }
                final boolean success = ok;
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        videos.remove(item);
                        adapter.updateAll(new ArrayList<>(videos));
                        Toast.makeText(requireContext(),"Deleted",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),"Delete failed — try a file manager",Toast.LENGTH_SHORT).show();
                    }
                });
            }).start())
            .setNegativeButton("Cancel", null).show();
    }
}
