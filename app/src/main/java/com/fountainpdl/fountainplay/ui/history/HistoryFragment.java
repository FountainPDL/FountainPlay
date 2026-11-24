package com.fountainpdl.fountainplay.ui.history;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.DeleteHelper;
import com.fountainpdl.fountainplay.util.MediaActions;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private final DeleteHelper deleter = new DeleteHelper(this);
    private MediaAdapter adapter;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_history, c, false);
        tvEmpty = v.findViewById(R.id.tv_empty);
        RecyclerView rv = v.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MediaAdapter(MediaAdapter.MODE_ROW);
        // History stays in play order, newest first
        rv.setAdapter(adapter);
        adapter.setOnItemClick((item, pos) -> MediaLauncher.open(requireContext(), adapter.getShown(), pos));
        adapter.setOnItemMore((item, anchor) -> MediaActions.showMenu(this, anchor, item, deleter));

        v.findViewById(R.id.btn_back).setOnClickListener(x ->
            requireActivity().getOnBackPressedDispatcher().onBackPressed());
        v.findViewById(R.id.btn_clear_history).setOnClickListener(x ->
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Clear history?")
                .setMessage("Your play history will be removed. Your files are not touched.")
                .setPositiveButton("Clear", (d, w) -> {
                    final Context app = requireContext().getApplicationContext();
                    new Thread(() -> {
                        AppDatabase.get(app).historyDao().clearAll();
                        if (getActivity() != null) getActivity().runOnUiThread(this::load);
                    }).start();
                }).setNegativeButton("Cancel", null).show());
        return v;
    }

    @Override public void onStart() { super.onStart(); load(); }

    private void load() {
        final Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            List<HistoryItem> rows = AppDatabase.get(app).historyDao().getAll();
            final List<MediaItem> items = new ArrayList<>();
            long order = rows.size();
            for (HistoryItem h : rows) {
                MediaItem m = new MediaItem(0, h.title, h.artist, h.path, h.duration, h.type);
                m.setAlbumArtUri(h.albumArtUri);
                m.setDateAdded(order--);          // keeps newest-first when the adapter sorts by "date"
                items.add(m);
            }
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.submit(items);
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }
}
