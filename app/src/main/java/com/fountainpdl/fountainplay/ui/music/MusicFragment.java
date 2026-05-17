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
