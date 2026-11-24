package com.fountainpdl.fountainplay.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.DeleteHelper;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.fountainpdl.fountainplay.util.MediaActions;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment implements LibraryRepo.Listener {

    private final DeleteHelper deleter = new DeleteHelper(this);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LibraryRepo repo;
    private MediaAdapter adapter;
    private EditText etSearch;
    private View btnClear;
    private TextView tvEmpty;
    private ChipGroup chips;
    private final Runnable runSearch = this::search;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_search, c, false);
        repo = LibraryRepo.get(requireContext());
        etSearch = v.findViewById(R.id.et_search);
        btnClear = v.findViewById(R.id.btn_clear_search);
        tvEmpty = v.findViewById(R.id.tv_search_empty);
        chips = v.findViewById(R.id.chip_group_search);

        adapter = new MediaAdapter(MediaAdapter.MODE_ROW);
        RecyclerView rv = v.findViewById(R.id.rv_results);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);
        adapter.setOnItemClick((item, pos) -> MediaLauncher.open(requireContext(), adapter.getShown(), pos));
        adapter.setOnItemMore((item, anchor) -> MediaActions.showMenu(this, anchor, item, deleter));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c2, int d) { }
            @Override public void onTextChanged(CharSequence q, int a, int b, int c2) {
                btnClear.setVisibility(q.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(runSearch);
                handler.postDelayed(runSearch, 180);
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        btnClear.setOnClickListener(x -> etSearch.setText(""));
        chips.setOnCheckedStateChangeListener((g, ids) -> search());
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        repo.addListener(this);
        repo.ensureLoaded();
        search();
    }

    @Override public void onStop() {
        super.onStop();
        repo.removeListener(this);
        handler.removeCallbacks(runSearch);
    }

    @Override public void onLibraryChanged() { if (isAdded()) search(); }

    private void search() {
        if (!isAdded()) return;
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        int checked = chips.getCheckedChipId();
        boolean songs = checked != R.id.chip_videos;
        boolean videos = checked != R.id.chip_songs;
        List<MediaItem> out = new ArrayList<>();
        if (!q.isEmpty()) {
            if (songs) for (MediaItem m : repo.getAudio()) if (matches(m, q)) out.add(m);
            if (videos) for (MediaItem m : repo.getVideo()) if (matches(m, q)) out.add(m);
        }
        adapter.submit(out);
        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(q.isEmpty() ? "Type to search your library" : "No results for \"" + q + "\"");
    }

    private static boolean matches(MediaItem m, String q) {
        return m.getTitle().toLowerCase(Locale.ROOT).contains(q)
            || m.getArtist().toLowerCase(Locale.ROOT).contains(q)
            || m.getAlbum().toLowerCase(Locale.ROOT).contains(q)
            || m.getFileName().toLowerCase(Locale.ROOT).contains(q);
    }
}
