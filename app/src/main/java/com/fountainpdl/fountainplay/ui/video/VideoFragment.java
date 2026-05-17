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
