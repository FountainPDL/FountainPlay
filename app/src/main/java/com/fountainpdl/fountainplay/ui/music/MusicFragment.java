package com.fountainpdl.fountainplay.ui.music;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class MusicFragment extends Fragment {

    private MediaAdapter adapter;
    private final List<MediaItem> songs = new ArrayList<>();
    private String currentSort = "name";
    private boolean sortAscending = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_music, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv    = view.findViewById(R.id.rv_songs);
        TextView tvCount   = view.findViewById(R.id.tv_song_count);
        SearchView sv      = view.findViewById(R.id.search_view);

        adapter = new MediaAdapter(songs, 0);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // ── Tap: open player ──────────────────────────────────
        adapter.setOnItemClickListener((item, pos) -> openPlayer(item));

        // ── Long-press: full context menu ─────────────────────
        adapter.setOnItemLongListener((item, pos, anchor) ->
            showSongMenu(item, pos));

        // ── Shuffle All ───────────────────────────────────────
        view.findViewById(R.id.btn_shuffle_all).setOnClickListener(v -> {
            if (songs.isEmpty()) return;
            List<MediaItem> shuffled = new ArrayList<>(songs);
            Collections.shuffle(shuffled);
            PlayQueue.get().setQueue(shuffled, 0);
            PlayQueue.get().setShuffle(true);
            openPlayer(shuffled.get(0));
        });

        // ── Sort ─────────────────────────────────────────────
        view.findViewById(R.id.btn_sort).setOnClickListener(v -> showSortDialog());

        // ── Refresh ───────────────────────────────────────────
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> loadSongs(tvCount));

        // ── Search ────────────────────────────────────────────
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadSongs(tvCount);
    }

    private void loadSongs(TextView tvCount) {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders  = prefs.getAudioFolders();
            List<MediaItem> result = MediaScanner.scanAudio(
                requireContext(), folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                songs.clear();
                songs.addAll(result);
                adapter.updateAll(result);
                adapter.sort(currentSort);
                tvCount.setText(songs.size() + " songs");
            });
        }).start();
    }

    private void openPlayer(MediaItem item) {
        int idx = songs.indexOf(item);
        PlayQueue.get().setQueue(songs, idx < 0 ? 0 : idx);
        Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
        i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
        i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
        i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
        i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        startActivity(i);
    }

    private void showSortDialog() {
        String[] labels = {"Name ↑","Name ↓","Artist","Date Added","Duration","Size","Folder"};
        String[] keys   = {"name_asc","name_desc","artist","date","duration","size","folder"};
        new AlertDialog.Builder(requireContext()).setTitle("Sort by")
            .setItems(labels, (d, i) -> {
                currentSort = keys[i];
                if (currentSort.endsWith("_desc")) {
                    adapter.sort(currentSort.replace("_desc",""));
                    adapter.reverse();
                } else {
                    adapter.sort(currentSort);
                }
            }).show();
    }

    private void showSongMenu(MediaItem item, int pos) {
        String[] opts = {
            "▶  Play",
            "⏭  Play Next",
            "➕  Add to Queue",
            "📋  Add to Playlist",
            "⭐  Add to Favourites",
            "↗  Share",
            "ℹ  Info",
            "🗑  Delete"
        };
        new AlertDialog.Builder(requireContext())
            .setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0: openPlayer(item); break;
                    case 1:
                        PlayQueue.get().addNext(item);
                        toast("Will play next");
                        break;
                    case 2:
                        PlayQueue.get().addToQueue(item);
                        toast("Added to queue");
                        break;
                    case 3: showAddToPlaylistDialog(item); break;
                    case 4: addToFavourites(item); break;
                    case 5: shareItem(item); break;
                    case 6: showInfo(item); break;
                    case 7: confirmDelete(item, pos); break;
                }
            }).show();
    }

    private void showAddToPlaylistDialog(MediaItem item) {
        new Thread(() -> {
            List<PlaylistEntity> playlists =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) {
                    toast("No playlists — create one in Library");
                    return;
                }
                String[] names = new String[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;
                new AlertDialog.Builder(requireContext()).setTitle("Add to Playlist")
                    .setItems(names, (d, i) -> addToPlaylist(item, playlists.get(i)))
                    .show();
            });
        }).start();
    }

    private void addToPlaylist(MediaItem item, PlaylistEntity playlist) {
        new Thread(() -> {
            PlaylistSong s = new PlaylistSong();
            s.playlistId = playlist.id;
            s.path = item.getPath(); s.title = item.getTitle();
            s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
            s.duration = item.getDuration();
            AppDatabase.get(requireContext()).playlistDao().insertSong(s);
            AppDatabase.get(requireContext()).playlistDao().updateCount(playlist.id);
            requireActivity().runOnUiThread(() -> toast("Added to " + playlist.name));
        }).start();
    }

    private void addToFavourites(MediaItem item) {
        // Create/find a "Favourites" playlist and add to it
        new Thread(() -> {
            List<PlaylistEntity> all =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            PlaylistEntity fav = null;
            for (PlaylistEntity p : all) {
                if ("Favourites".equals(p.name)) { fav = p; break; }
            }
            if (fav == null) {
                PlaylistEntity newFav = new PlaylistEntity();
                newFav.name = "Favourites";
                newFav.createdAt = System.currentTimeMillis();
                long id = AppDatabase.get(requireContext()).playlistDao().insertPlaylist(newFav);
                newFav.id = (int) id;
                fav = newFav;
            }
            PlaylistSong s = new PlaylistSong();
            s.playlistId = fav.id;
            s.path = item.getPath(); s.title = item.getTitle();
            s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
            s.duration = item.getDuration();
            AppDatabase.get(requireContext()).playlistDao().insertSong(s);
            AppDatabase.get(requireContext()).playlistDao().updateCount(fav.id);
            requireActivity().runOnUiThread(() -> toast("Added to Favourites ⭐"));
        }).start();
    }

    private void shareItem(MediaItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
        share.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " - " + item.getArtist());
        startActivity(Intent.createChooser(share, "Share"));
    }

    private void showInfo(MediaItem item) {
        String info = "Title:    " + item.getTitle()
            + "\nArtist:   " + item.getArtist()
            + "\nAlbum:    " + item.getAlbum()
            + "\nDuration: " + item.getFormattedDuration()
            + "\nSize:     " + (item.getSize() / 1024 / 1024) + " MB"
            + "\nFolder:   " + item.getFolder()
            + "\nPath:     " + item.getPath();
        new AlertDialog.Builder(requireContext())
            .setTitle("File Info")
            .setMessage(info)
            .setPositiveButton("OK", null).show();
    }

    private void confirmDelete(MediaItem item, int pos) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete file?")
            .setMessage("\"" + item.getTitle() + "\" will be permanently deleted.")
            .setPositiveButton("Delete", (d, i) -> deleteFile(item, pos))
            .setNegativeButton("Cancel", null).show();
    }

    private void deleteFile(MediaItem item, int pos) {
        new Thread(() -> {
            boolean deleted = false;
            // Try MediaStore first (works on Android 10+)
            try {
                Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                int rows = requireContext().getContentResolver().delete(
                    collection,
                    MediaStore.Audio.Media.DATA + "=?",
                    new String[]{item.getPath()});
                deleted = rows > 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Fallback: direct file delete
            if (!deleted) {
                java.io.File f = new java.io.File(item.getPath());
                deleted = f.exists() && f.delete();
            }
            final boolean success = deleted;
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    songs.remove(item);
                    adapter.updateAll(new ArrayList<>(songs));
                    toast("Deleted");
                } else {
                    toast("Could not delete — try a file manager");
                }
            });
        }).start();
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
