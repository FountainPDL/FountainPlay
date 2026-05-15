package com.fountainpdl.fountainplay.ui.music;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicFragment extends Fragment {

    private RecyclerView rvSongs;
    private TextView tvCount;
    private List<MediaItem> songs = new ArrayList<>();
    private MediaAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSongs = view.findViewById(R.id.rv_songs);
        tvCount = view.findViewById(R.id.tv_song_count);

        adapter = new MediaAdapter(songs, 0);
        adapter.setOnItemClickListener((item, pos) -> openAudioPlayer(item, pos));
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);

        view.findViewById(R.id.btn_shuffle_all).setOnClickListener(v -> {
            if (!songs.isEmpty()) {
                Collections.shuffle(songs);
                openAudioPlayer(songs.get(0), 0);
            }
        });

        loadSongs();
    }

    private void loadSongs() {
        new Thread(() -> {
            List<MediaItem> result = MediaScanner.scanAudio(requireContext());
            requireActivity().runOnUiThread(() -> {
                songs.clear();
                songs.addAll(result);
                adapter.notifyDataSetChanged();
                tvCount.setText(songs.size() + " songs");
            });
        }).start();
    }

    private void openAudioPlayer(MediaItem item, int pos) {
        Intent intent = new Intent(requireContext(), AudioPlayerActivity.class);
        intent.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
        intent.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
        intent.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
        intent.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        startActivity(intent);
    }
}
