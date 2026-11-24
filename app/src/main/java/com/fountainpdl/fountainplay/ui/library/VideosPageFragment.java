package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.DeleteHelper;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.fountainpdl.fountainplay.util.MediaActions;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class VideosPageFragment extends Fragment implements LibraryRepo.Listener {

    private final DeleteHelper deleter = new DeleteHelper(this);
    private LibraryRepo repo;
    private MediaAdapter adapter;
    private TextView tvCount, tvEmpty;
    private MaterialButton btnSort;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_page_videos, c, false);
        repo = LibraryRepo.get(requireContext());
        tvCount = v.findViewById(R.id.tv_count);
        tvEmpty = v.findViewById(R.id.tv_empty);
        btnSort = v.findViewById(R.id.btn_sort);

        adapter = new MediaAdapter(MediaAdapter.MODE_GRID);
        RecyclerView rv = v.findViewById(R.id.rv_videos);
        float widthDp = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        rv.setLayoutManager(new GridLayoutManager(requireContext(), Math.max(2, (int) (widthDp / 180))));
        rv.setAdapter(adapter);

        adapter.setOnItemClick((item, pos) -> MediaLauncher.open(requireContext(), adapter.getShown(), pos));
        adapter.setOnItemMore((item, anchor) -> MediaActions.showMenu(this, anchor, item, deleter));

        ((EditText) v.findViewById(R.id.et_filter)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c2, int d) { }
            @Override public void onTextChanged(CharSequence q, int a, int b, int c2) {
                adapter.setQuery(q.toString());
                updateCount();
            }
            @Override public void afterTextChanged(Editable e) { }
        });

        v.findViewById(R.id.btn_play_all).setOnClickListener(x -> {
            List<MediaItem> list = adapter.getShown();
            if (!list.isEmpty()) MediaLauncher.open(requireContext(), list, 0);
        });
        btnSort.setOnClickListener(this::showSortMenu);
        Fx.press(v.findViewById(R.id.btn_play_all));
        updateSortLabel();
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        repo.addListener(this);
        repo.ensureLoaded();
        adapter.submit(repo.getVideo());
        updateCount();
    }

    @Override public void onStop() { super.onStop(); repo.removeListener(this); }

    @Override public void onLibraryChanged() {
        if (!isAdded()) return;
        adapter.submit(repo.getVideo());
        updateCount();
    }

    private void updateCount() {
        int n = adapter.getItemCount();
        tvCount.setText(n + (n == 1 ? " video" : " videos"));
        tvEmpty.setVisibility(n == 0 && repo.isLoaded() ? View.VISIBLE : View.GONE);
    }

    private void showSortMenu(View anchor) {
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "Date added");
        pm.getMenu().add(0, 2, 1, "Name");
        pm.getMenu().add(0, 3, 2, "Duration");
        pm.getMenu().add(0, 4, 3, "Size");
        pm.setOnMenuItemClickListener(mi -> {
            String[] keys = {"", "date", "title", "duration", "size"};
            adapter.setSort(keys[mi.getItemId()]);
            updateSortLabel();
            return true;
        });
        pm.show();
    }

    private void updateSortLabel() {
        String key = adapter.getSortKey();
        String name = "title".equals(key) ? "Name" : "duration".equals(key) ? "Duration"
            : "size".equals(key) ? "Size" : "Newest";
        btnSort.setText(name + (adapter.isAscending() ? " ↑" : " ↓"));
    }
}
