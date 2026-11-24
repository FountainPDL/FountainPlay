package com.fountainpdl.fountainplay.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The "⋮" menu for songs and videos, plus the dialogs it opens. */
public final class MediaActions {
    private MediaActions() {}

    public static void showMenu(final Fragment f, View anchor, final MediaItem item, final DeleteHelper deleter) {
        final Activity act = f.getActivity();
        if (act == null) return;
        PopupMenu pm = new PopupMenu(act, anchor);
        if (item.isAudio()) {
            pm.getMenu().add(0, 1, 0, "Play next");
            pm.getMenu().add(0, 2, 1, "Add to queue");
            pm.getMenu().add(0, 3, 2, "Add to playlist");
            pm.getMenu().add(0, 4, 3, "Add to or remove from Favourites");
        }
        pm.getMenu().add(0, 5, 4, "Share");
        pm.getMenu().add(0, 6, 5, "Details");
        pm.getMenu().add(0, 7, 6, "Delete");
        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1: PlayQueue.get().addNext(item); toast(act, "Will play next"); break;
                case 2: PlayQueue.get().addToQueue(item); toast(act, "Added to queue"); break;
                case 3: addToPlaylist(act, item); break;
                case 4: toggleFavourite(act, item); break;
                case 5: share(act, item); break;
                case 6: showDetails(act, item); break;
                case 7: deleter.confirmAndDelete(act, java.util.Collections.singletonList(item),
                        () -> LibraryRepo.get(act).refresh()); break;
            }
            return true;
        });
        pm.show();
    }

    public static void toggleFavourite(final Activity act, final MediaItem item) {
        final Context app = act.getApplicationContext();
        new Thread(() -> {
            final boolean now = Favourites.toggle(app, item);
            act.runOnUiThread(() -> toast(act, now ? "Added to Favourites" : "Removed from Favourites"));
        }).start();
    }

    public static void share(Context ctx, MediaItem item) {
        Intent chooser = MediaUris.shareIntent(ctx, item);
        if (chooser == null) toast(ctx, "This file can't be shared");
        else ctx.startActivity(chooser);
    }

    public static void showDetails(Context ctx, MediaItem item) {
        String dur = item.getDuration() > 0 ? item.getFormattedDuration() : "unknown";
        String msg = "Name: " + item.getFileName()
            + (item.isAudio() ? "\nArtist: " + item.getArtist() + "\nAlbum: " + item.getAlbum() : "")
            + "\nDuration: " + dur
            + "\nSize: " + String.format(Locale.US, "%.1f MB", item.getSize() / 1024f / 1024f)
            + "\nFolder: " + item.getFolder();
        new MaterialAlertDialogBuilder(ctx).setTitle("Details").setMessage(msg)
            .setPositiveButton("OK", null).show();
    }

    /** Lists playlists; the first entry creates a new one. */
    public static void addToPlaylist(final Activity act, final MediaItem item) {
        final Context app = act.getApplicationContext();
        new Thread(() -> {
            final List<PlaylistEntity> lists = AppDatabase.get(app).playlistDao().getAllPlaylists();
            act.runOnUiThread(() -> {
                if (act.isFinishing() || act.isDestroyed()) return;
                final List<String> names = new ArrayList<>();
                names.add("+ New playlist…");
                for (PlaylistEntity p : lists) names.add(p.name);
                new MaterialAlertDialogBuilder(act).setTitle("Add to playlist")
                    .setItems(names.toArray(new String[0]), (d, which) -> {
                        if (which == 0) createAndAdd(act, item);
                        else addTo(act, item, lists.get(which - 1));
                    }).show();
            });
        }).start();
    }

    private static void createAndAdd(final Activity act, final MediaItem item) {
        final EditText input = new EditText(act);
        input.setHint("Playlist name");
        input.setSingleLine(true);
        new MaterialAlertDialogBuilder(act).setTitle("New playlist").setView(input)
            .setPositiveButton("Create", (d, w) -> {
                final String name = input.getText().toString().trim();
                if (name.isEmpty()) return;
                final Context app = act.getApplicationContext();
                new Thread(() -> {
                    PlaylistDao dao = AppDatabase.get(app).playlistDao();
                    PlaylistEntity p = new PlaylistEntity();
                    p.name = name;
                    p.createdAt = System.currentTimeMillis();
                    p.id = (int) dao.insertPlaylist(p);
                    dao.insertSong(Favourites.toSong(p.id, item));
                    dao.updateCount(p.id);
                    act.runOnUiThread(() -> toast(act, "Added to " + name));
                }).start();
            }).setNegativeButton("Cancel", null).show();
    }

    private static void addTo(final Activity act, final MediaItem item, final PlaylistEntity playlist) {
        final Context app = act.getApplicationContext();
        new Thread(() -> {
            PlaylistDao dao = AppDatabase.get(app).playlistDao();
            dao.insertSong(Favourites.toSong(playlist.id, item));
            dao.updateCount(playlist.id);
            act.runOnUiThread(() -> toast(act, "Added to " + playlist.name));
        }).start();
    }

    private static void toast(Context c, String m) { Toast.makeText(c, m, Toast.LENGTH_SHORT).show(); }
}
