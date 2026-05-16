package com.fountainpdl.fountainplay.ui.library;

import android.content.Intent;
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
import java.util.List;

public class LibraryPageFragment extends Fragment {
    private static final String ARG_TAB = "tab";

    public static LibraryPageFragment newInstance(String tab) {
        LibraryPageFragment f = new LibraryPageFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TAB, tab);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tab = getArguments() != null ? getArguments().getString(ARG_TAB, "Songs") : "Songs";

        RecyclerView rv = view.findViewById(R.id.rv_library);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<MediaItem> items = new ArrayList<>();
        MediaAdapter adapter = new MediaAdapter(items, 0);
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> {
            Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
            i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
            startActivity(i);
        });

        new Thread(() -> {
            List<MediaItem> all = MediaScanner.scanAudio(requireContext());
            List<MediaItem> result = new ArrayList<>();
            switch (tab) {
                case "Songs":    result = all; break;
                case "Albums":   result = filterByAlbum(all); break;
                case "Artists":  result = filterByArtist(all); break;
                case "Playlists": result = new ArrayList<>(); break; // future
                case "History":  result = new ArrayList<>(); break;  // future
            }
            final List<MediaItem> finalResult = result;
            requireActivity().runOnUiThread(() -> {
                items.clear();
                items.addAll(finalResult);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private List<MediaItem> filterByAlbum(List<MediaItem> all) {
        // One representative per album
        List<MediaItem> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MediaItem m : all) {
            if (seen.add(m.getAlbum())) result.add(m);
        }
        return result;
    }

    private List<MediaItem> filterByArtist(List<MediaItem> all) {
        List<MediaItem> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MediaItem m : all) {
            if (seen.add(m.getArtist())) result.add(m);
        }
        return result;
    }
}
