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
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import com.fountainpdl.fountainplay.util.PlayQueue;
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
        String tab = getArguments() != null ? getArguments().getString(ARG_TAB, "Songs") : "Songs";
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
                case "Albums":  result = onePerAlbum(all);  break;
                case "Artists": result = onePerArtist(all); break;
                default:        result = all;               break;
            }
            final List<MediaItem> fr = result;
            requireActivity().runOnUiThread(() -> {
                items.clear(); items.addAll(fr); adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void loadHistory(RecyclerView rv, TextView tvEmpty) {
        new Thread(() -> {
            List<com.fountainpdl.fountainplay.db.entity.HistoryItem> history =
                AppDatabase.get(requireContext()).historyDao().getAll();
            requireActivity().runOnUiThread(() -> {
                if (history.isEmpty()) { tvEmpty.setText("No history yet"); tvEmpty.setVisibility(View.VISIBLE); return; }
                List<MediaItem> items = new ArrayList<>();
                for (com.fountainpdl.fountainplay.db.entity.HistoryItem h : history) {
                    MediaItem m = new MediaItem(0, h.title, h.artist, h.path, h.duration, h.type);
                    m.setAlbumArtUri(h.albumArtUri);
                    items.add(m);
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
        new Thread(() -> {
            List<PlaylistEntity> playlists =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) {
                    tvEmpty.setText("No playlists yet.\nLong-press a song to create one.");
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);
                Button btnCreate = new Button(requireContext());
                btnCreate.setText("+ Create Playlist");
                btnCreate.setOnClickListener(vv -> showCreatePlaylistDialog(rv, tvEmpty));
                container.addView(btnCreate);

                for (PlaylistEntity p : playlists) {
                    TextView tv = new TextView(requireContext());
                    tv.setText("▶  " + p.name + "  (" + p.songCount + " songs)");
                    tv.setTextColor(0xFFFFFFFF); tv.setTextSize(15);
                    tv.setPadding(32, 28, 32, 28);
                    tv.setOnClickListener(vv -> openPlaylist(p));
                    tv.setOnLongClickListener(vv -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Delete \"" + p.name + "\"?")
                            .setPositiveButton("Delete", (d, i) -> new Thread(() -> {
                                AppDatabase.get(requireContext()).playlistDao().deletePlaylist(p);
                                requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                            }).start())
                            .setNegativeButton("Cancel", null).show();
                        return true;
                    });
                    container.addView(tv);
                }

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(
                            @NonNull ViewGroup p, int t) {
                        return new RecyclerView.ViewHolder(container) {};
                    }
                    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {}
                    @Override public int getItemCount() { return 1; }
                });
            });
        }).start();
    }

    private void showCreatePlaylistDialog(RecyclerView rv, TextView tvEmpty) {
        EditText et = new EditText(requireContext());
        et.setHint("Playlist name");
        new AlertDialog.Builder(requireContext()).setTitle("New Playlist")
            .setView(et)
            .setPositiveButton("Create", (d, i) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) return;
                new Thread(() -> {
                    PlaylistEntity p = new PlaylistEntity();
                    p.name = name; p.createdAt = System.currentTimeMillis();
                    AppDatabase.get(requireContext()).playlistDao().insertPlaylist(p);
                    requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                }).start();
            }).setNegativeButton("Cancel", null).show();
    }

    private void openPlaylist(PlaylistEntity playlist) {
        new Thread(() -> {
            List<PlaylistSong> songs =
                AppDatabase.get(requireContext()).playlistDao().getSongsForPlaylist(playlist.id);
            if (songs.isEmpty()) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Playlist is empty", Toast.LENGTH_SHORT).show());
                return;
            }
            List<MediaItem> items = new ArrayList<>();
            for (PlaylistSong s : songs) {
                MediaItem m = new MediaItem(0, s.title, s.artist, s.path, s.duration, 0);
                m.setAlbumArtUri(s.albumArtUri);
                items.add(m);
            }
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
        Map<String, MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getAlbum(), m);
        return new ArrayList<>(map.values());
    }

    private List<MediaItem> onePerArtist(List<MediaItem> all) {
        Map<String, MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getArtist(), m);
        return new ArrayList<>(map.values());
    }
}
