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
