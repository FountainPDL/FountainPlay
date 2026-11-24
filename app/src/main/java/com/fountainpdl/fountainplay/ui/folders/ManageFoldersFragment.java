package com.fountainpdl.fountainplay.ui.folders;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.FolderAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.FolderScanner;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hide folders from Home/Library/Search (the Folders tab still shows them), and add folders that
 * contain a .nomedia file so they are scanned anyway.
 */
public class ManageFoldersFragment extends Fragment {

    private AppPreferences prefs;
    private LibraryRepo repo;
    private FolderAdapter excludedAdapter, extraAdapter;
    private TextView tvExcludedEmpty, tvExtraEmpty;

    private final ActivityResultLauncher<Uri> pickFolder =
        registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) return;
            String path = FolderScanner.fromTreeUri(uri);
            if (path == null || !new File(path).isDirectory()) {
                toast("Couldn't read that folder");
                return;
            }
            Set<String> extra = prefs.getExtraFolders();
            extra.add(path);
            prefs.setExtraFolders(extra);
            repo.refresh();
            bindLists();
            toast("Added " + FolderScanner.nameOf(path));
        });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_manage_folders, c, false);
        prefs = new AppPreferences(requireContext());
        repo = LibraryRepo.get(requireContext());
        tvExcludedEmpty = v.findViewById(R.id.tv_excluded_empty);
        tvExtraEmpty = v.findViewById(R.id.tv_extra_empty);

        excludedAdapter = new FolderAdapter(new FolderAdapter.Listener() {
            @Override public void onClick(FolderAdapter.Row row) { }
            @Override public void onAction(FolderAdapter.Row row) {
                Set<String> s = prefs.getExcludedFolders();
                s.remove((String) row.tag);
                prefs.setExcludedFolders(s);
                repo.refresh();
                bindLists();
            }
        });
        extraAdapter = new FolderAdapter(new FolderAdapter.Listener() {
            @Override public void onClick(FolderAdapter.Row row) { }
            @Override public void onAction(FolderAdapter.Row row) {
                Set<String> s = prefs.getExtraFolders();
                s.remove((String) row.tag);
                prefs.setExtraFolders(s);
                repo.refresh();
                bindLists();
            }
        });
        ((RecyclerView) v.findViewById(R.id.rv_excluded)).setLayoutManager(new LinearLayoutManager(requireContext()));
        ((RecyclerView) v.findViewById(R.id.rv_excluded)).setAdapter(excludedAdapter);
        ((RecyclerView) v.findViewById(R.id.rv_extra)).setLayoutManager(new LinearLayoutManager(requireContext()));
        ((RecyclerView) v.findViewById(R.id.rv_extra)).setAdapter(extraAdapter);

        v.findViewById(R.id.btn_back).setOnClickListener(x ->
            requireActivity().getOnBackPressedDispatcher().onBackPressed());
        v.findViewById(R.id.btn_add_excluded).setOnClickListener(x -> chooseFoldersToHide());
        v.findViewById(R.id.btn_add_extra).setOnClickListener(x -> pickFolder.launch(null));
        Fx.press(v.findViewById(R.id.btn_add_excluded));
        Fx.press(v.findViewById(R.id.btn_add_extra));
        bindLists();
        return v;
    }

    private void bindLists() {
        List<FolderAdapter.Row> ex = new ArrayList<>();
        for (String f : new TreeSet<>(prefs.getExcludedFolders())) {
            ex.add(new FolderAdapter.Row(FolderScanner.nameOf(f), f, R.drawable.ic_folder_off, f)
                .action(R.drawable.ic_visibility, "Show this folder again"));
        }
        excludedAdapter.submit(ex);
        tvExcludedEmpty.setVisibility(ex.isEmpty() ? View.VISIBLE : View.GONE);

        List<FolderAdapter.Row> xt = new ArrayList<>();
        for (String f : new TreeSet<>(prefs.getExtraFolders())) {
            xt.add(new FolderAdapter.Row(FolderScanner.nameOf(f), f, R.drawable.ic_folder, f)
                .action(R.drawable.ic_close, "Stop scanning this folder"));
        }
        extraAdapter.submit(xt);
        tvExtraEmpty.setVisibility(xt.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Pick from the folders that actually contain media. */
    private void chooseFoldersToHide() {
        Set<String> candidates = new LinkedHashSet<>();
        for (MediaItem m : repo.getAudioAll()) candidates.add(FolderScanner.parentOf(m.getPath()));
        for (MediaItem m : repo.getVideoAll()) candidates.add(FolderScanner.parentOf(m.getPath()));
        candidates.removeAll(prefs.getExcludedFolders());
        final List<String> folders = new ArrayList<>(candidates);
        Collections.sort(folders, (a, b) -> FolderScanner.nameOf(a).compareToIgnoreCase(FolderScanner.nameOf(b)));
        if (folders.isEmpty()) { toast("No folders to hide"); return; }

        String[] labels = new String[folders.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = FolderScanner.nameOf(folders.get(i)) + "\n" + folders.get(i);
        final boolean[] checked = new boolean[labels.length];
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Hide folders")
            .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("Hide", (d, w) -> {
                Set<String> s = prefs.getExcludedFolders();
                for (int i = 0; i < checked.length; i++) if (checked[i]) s.add(folders.get(i));
                prefs.setExcludedFolders(s);
                repo.refresh();
                bindLists();
            }).setNegativeButton("Cancel", null).show();
    }

    private void toast(String m) { Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}
