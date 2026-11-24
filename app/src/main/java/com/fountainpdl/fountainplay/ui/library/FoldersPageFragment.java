package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.FolderAdapter;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.DeleteHelper;
import com.fountainpdl.fountainplay.util.FolderScanner;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.fountainpdl.fountainplay.util.MediaActions;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Two ways to browse:
 *  - "Folders with media": every folder that holds audio or video (hidden-from-library folders included)
 *  - "All folders": the real file system, with an optional view of hidden files and .nomedia folders
 */
public class FoldersPageFragment extends Fragment implements LibraryRepo.Listener {

    private static final int MODE_MEDIA = 0, MODE_ALL = 1;

    private final DeleteHelper deleter = new DeleteHelper(this);
    private LibraryRepo repo;
    private AppPreferences prefs;
    private RecyclerView rv;
    private TextView tvEmpty, tvPath;
    private View pathBar;
    private Chip chipHidden;
    private FolderAdapter folderAdapter;
    private MediaAdapter mediaAdapter;
    private OnBackPressedCallback backCallback;

    private int mode = MODE_MEDIA;
    private String openMediaFolder = null;   // MODE_MEDIA: folder whose files are shown
    private File currentDir = null;          // MODE_ALL: directory being browsed (null = storage roots)

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_page_folders, c, false);
        repo = LibraryRepo.get(requireContext());
        prefs = new AppPreferences(requireContext());

        rv = v.findViewById(R.id.rv_folders);
        tvEmpty = v.findViewById(R.id.tv_empty);
        tvPath = v.findViewById(R.id.tv_folder_path);
        pathBar = v.findViewById(R.id.path_bar);
        chipHidden = v.findViewById(R.id.chip_show_hidden);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        folderAdapter = new FolderAdapter(new FolderAdapter.Listener() {
            @Override public void onClick(FolderAdapter.Row row) { onRowClick(row); }
            @Override public void onAction(FolderAdapter.Row row) { }
        });
        mediaAdapter = new MediaAdapter(MediaAdapter.MODE_ROW);
        mediaAdapter.setOnItemClick((item, pos) -> MediaLauncher.open(requireContext(), mediaAdapter.getShown(), pos));
        mediaAdapter.setOnItemMore((item, anchor) -> MediaActions.showMenu(this, anchor, item, deleter));

        chipHidden.setChecked(prefs.getBoolean(AppPreferences.KEY_SHOW_HIDDEN, false));
        chipHidden.setOnCheckedChangeListener((b, checked) -> {
            prefs.setBoolean(AppPreferences.KEY_SHOW_HIDDEN, checked);
            render();
        });

        ((ChipGroup) v.findViewById(R.id.chip_group_mode)).setOnCheckedStateChangeListener((group, ids) -> {
            mode = ids.contains(R.id.chip_mode_all) ? MODE_ALL : MODE_MEDIA;
            openMediaFolder = null;
            currentDir = null;
            render();
        });

        v.findViewById(R.id.btn_folder_up).setOnClickListener(x -> goUp());

        backCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { goUp(); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        repo.addListener(this);
        repo.ensureLoaded();
        render();
    }

    @Override public void onStop() { super.onStop(); repo.removeListener(this); }

    // Only the visible page may handle the system Back button
    @Override public void onResume() { super.onResume(); render(); }
    @Override public void onPause() { super.onPause(); if (backCallback != null) backCallback.setEnabled(false); }

    @Override public void onLibraryChanged() { if (isAdded() && mode == MODE_MEDIA) render(); }

    private void goUp() {
        if (mode == MODE_MEDIA) {
            openMediaFolder = null;
        } else if (currentDir != null) {
            File parent = currentDir.getParentFile();
            boolean atRoot = false;
            for (File r : FolderScanner.storageRoots(requireContext())) if (r.equals(currentDir)) atRoot = true;
            currentDir = (atRoot || parent == null) ? null : parent;
        }
        render();
    }

    private void render() {
        if (!isAdded()) return;
        chipHidden.setVisibility(mode == MODE_ALL ? View.VISIBLE : View.GONE);
        boolean inside = (mode == MODE_MEDIA && openMediaFolder != null) || (mode == MODE_ALL && currentDir != null);
        pathBar.setVisibility(inside ? View.VISIBLE : View.GONE);
        backCallback.setEnabled(inside && isResumed());
        if (mode == MODE_MEDIA) renderMediaMode(); else renderAllMode();
    }

    // ── Folders with media ──────────────────────────────────────────────────────

    private void renderMediaMode() {
        if (openMediaFolder != null) {
            tvPath.setText(openMediaFolder);
            List<MediaItem> inFolder = new ArrayList<>();
            for (MediaItem m : repo.getAudioAll()) if (FolderScanner.parentOf(m.getPath()).equals(openMediaFolder)) inFolder.add(m);
            for (MediaItem m : repo.getVideoAll()) if (FolderScanner.parentOf(m.getPath()).equals(openMediaFolder)) inFolder.add(m);
            mediaAdapter.submit(inFolder);
            mediaAdapter.setQuery("");
            rv.setAdapter(mediaAdapter);
            tvEmpty.setVisibility(inFolder.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        Map<String, int[]> counts = new LinkedHashMap<>();   // folder -> {songs, videos}
        for (MediaItem m : repo.getAudioAll()) bump(counts, FolderScanner.parentOf(m.getPath()), 0);
        for (MediaItem m : repo.getVideoAll()) bump(counts, FolderScanner.parentOf(m.getPath()), 1);
        List<String> folders = new ArrayList<>(counts.keySet());
        Collections.sort(folders, (a, b) -> FolderScanner.nameOf(a).compareToIgnoreCase(FolderScanner.nameOf(b)));
        List<FolderAdapter.Row> rows = new ArrayList<>();
        for (String f : folders) {
            int[] n = counts.get(f);
            rows.add(new FolderAdapter.Row(FolderScanner.nameOf(f), describe(n[0], n[1]) + "\n" + f,
                R.drawable.ic_folder, f));
        }
        folderAdapter.submit(rows);
        rv.setAdapter(folderAdapter);
        tvEmpty.setText("No folders with audio or video yet");
        tvEmpty.setVisibility(rows.isEmpty() && repo.isLoaded() ? View.VISIBLE : View.GONE);
    }

    private static void bump(Map<String, int[]> map, String key, int idx) {
        int[] n = map.get(key);
        if (n == null) { n = new int[2]; map.put(key, n); }
        n[idx]++;
    }

    private static String describe(int songs, int videos) {
        List<String> parts = new ArrayList<>();
        if (songs > 0) parts.add(songs + (songs == 1 ? " song" : " songs"));
        if (videos > 0) parts.add(videos + (videos == 1 ? " video" : " videos"));
        return android.text.TextUtils.join(" · ", parts);
    }

    // ── All folders (file browser) ──────────────────────────────────────────────

    private void renderAllMode() {
        List<FolderAdapter.Row> rows = new ArrayList<>();
        boolean showHidden = prefs.getBoolean(AppPreferences.KEY_SHOW_HIDDEN, false);
        if (currentDir == null) {
            for (File root : FolderScanner.storageRoots(requireContext())) {
                rows.add(new FolderAdapter.Row(FolderScanner.prettyName(root, requireContext()),
                    root.getAbsolutePath(), R.drawable.ic_folder, root));
            }
        } else {
            tvPath.setText(currentDir.getAbsolutePath());
            for (FolderScanner.Entry e : FolderScanner.list(currentDir, showHidden)) {
                if (e.isDir) {
                    String sub = e.noMedia ? "Hidden from the system scan (.nomedia)" : "Folder";
                    rows.add(new FolderAdapter.Row(e.file.getName(), sub,
                        e.noMedia ? R.drawable.ic_folder_off : R.drawable.ic_folder, e.file));
                } else {
                    boolean video = FolderScanner.isVideo(e.file.getName());
                    String size = String.format(Locale.US, "%.1f MB", e.file.length() / 1024f / 1024f);
                    rows.add(new FolderAdapter.Row(e.file.getName(), (video ? "Video · " : "Audio · ") + size,
                        video ? R.drawable.ic_movie : R.drawable.ic_music_note, e.file));
                }
            }
        }
        folderAdapter.submit(rows);
        rv.setAdapter(folderAdapter);
        tvEmpty.setText(currentDir == null ? "No storage found" : "No folders or media files here");
        tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onRowClick(FolderAdapter.Row row) {
        if (mode == MODE_MEDIA) {
            openMediaFolder = (String) row.tag;
            render();
            return;
        }
        File f = (File) row.tag;
        if (f.isDirectory()) {
            currentDir = f;
            render();
            return;
        }
        // Play the tapped file, with the other media files in the same folder as the queue
        boolean showHidden = prefs.getBoolean(AppPreferences.KEY_SHOW_HIDDEN, false);
        List<MediaItem> queue = new ArrayList<>();
        int index = 0;
        File dir = f.getParentFile();
        if (dir != null) {
            for (FolderScanner.Entry e : FolderScanner.list(dir, showHidden)) {
                if (e.isDir) continue;
                if (e.file.equals(f)) index = queue.size();
                queue.add(FolderScanner.fromFile(e.file, false));
            }
        }
        if (queue.isEmpty()) queue.add(FolderScanner.fromFile(f, false));
        MediaLauncher.open(requireContext(), queue, Math.min(index, queue.size() - 1));
    }
}
