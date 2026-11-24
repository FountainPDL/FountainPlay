package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.FolderAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.Favourites;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsPageFragment extends Fragment {

    private FolderAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_page_playlists, c, false);
        tvEmpty = v.findViewById(R.id.tv_empty);
        RecyclerView rv = v.findViewById(R.id.rv_playlists);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FolderAdapter(new FolderAdapter.Listener() {
            @Override public void onClick(FolderAdapter.Row row) { showActions((PlaylistEntity) row.tag); }
            @Override public void onAction(FolderAdapter.Row row) { confirmDelete((PlaylistEntity) row.tag); }
        });
        rv.setAdapter(adapter);
        v.findViewById(R.id.btn_new_playlist).setOnClickListener(x -> promptNew());
        Fx.press(v.findViewById(R.id.btn_new_playlist));
        return v;
    }

    @Override public void onStart() { super.onStart(); load(); }

    private void load() {
        final android.content.Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            final List<PlaylistEntity> lists = AppDatabase.get(app).playlistDao().getAllPlaylists();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                List<FolderAdapter.Row> rows = new ArrayList<>();
                for (PlaylistEntity p : lists) {
                    FolderAdapter.Row r = new FolderAdapter.Row(p.name,
                        p.songCount + (p.songCount == 1 ? " song" : " songs"), R.drawable.ic_queue_music, p);
                    if (!Favourites.NAME.equals(p.name)) r.action(R.drawable.ic_delete, "Delete playlist");
                    rows.add(r);
                }
                adapter.submit(rows);
                tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void promptNew() {
        final EditText input = new EditText(requireContext());
        input.setHint("Playlist name");
        input.setSingleLine(true);
        new MaterialAlertDialogBuilder(requireContext()).setTitle("New playlist").setView(input)
            .setPositiveButton("Create", (d, w) -> {
                final String name = input.getText().toString().trim();
                if (name.isEmpty()) return;
                final android.content.Context app = requireContext().getApplicationContext();
                new Thread(() -> {
                    PlaylistEntity p = new PlaylistEntity();
                    p.name = name;
                    p.createdAt = System.currentTimeMillis();
                    AppDatabase.get(app).playlistDao().insertPlaylist(p);
                    if (getActivity() != null) getActivity().runOnUiThread(this::load);
                }).start();
            }).setNegativeButton("Cancel", null).show();
    }

    private void showActions(final PlaylistEntity p) {
        String[] opts = {"Play", "Shuffle", "Show songs"};
        new MaterialAlertDialogBuilder(requireContext()).setTitle(p.name).setItems(opts, (d, which) -> {
            final android.content.Context app = requireContext().getApplicationContext();
            new Thread(() -> {
                final List<MediaItem> songs = new ArrayList<>();
                PlaylistDao dao = AppDatabase.get(app).playlistDao();
                for (PlaylistSong s : dao.getSongsForPlaylist(p.id)) songs.add(Favourites.toItem(s));
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (songs.isEmpty()) { toast("This playlist is empty"); return; }
                    if (which == 0) MediaLauncher.open(requireContext(), songs, 0);
                    else if (which == 1) MediaLauncher.shuffleAudio(requireContext(), songs);
                    else showSongs(p, songs);
                });
            }).start();
        }).show();
    }

    private void showSongs(PlaylistEntity p, final List<MediaItem> songs) {
        String[] names = new String[songs.size()];
        for (int i = 0; i < songs.size(); i++) names[i] = songs.get(i).getTitle() + " · " + songs.get(i).getArtist();
        new MaterialAlertDialogBuilder(requireContext()).setTitle(p.name)
            .setItems(names, (d, which) -> MediaLauncher.open(requireContext(), songs, which))
            .setPositiveButton("Close", null).show();
    }

    private void confirmDelete(final PlaylistEntity p) {
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete \"" + p.name + "\"?")
            .setMessage("The songs stay on your device.")
            .setPositiveButton("Delete", (d, w) -> {
                final android.content.Context app = requireContext().getApplicationContext();
                new Thread(() -> {
                    PlaylistDao dao = AppDatabase.get(app).playlistDao();
                    dao.clearPlaylist(p.id);
                    dao.deletePlaylist(p);
                    if (getActivity() != null) getActivity().runOnUiThread(this::load);
                }).start();
            }).setNegativeButton("Cancel", null).show();
    }

    private void toast(String m) { Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}
