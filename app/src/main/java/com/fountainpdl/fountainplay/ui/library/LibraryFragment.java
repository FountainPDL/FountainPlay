package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/** Library: Videos, Audio, Folders, Playlists. */
public class LibraryFragment extends Fragment {

    private static final String[] TITLES = {"Videos", "Audio", "Folders", "Playlists"};
    private static final int[] ICONS = {
        R.drawable.ic_movie, R.drawable.ic_music_note, R.drawable.ic_folder, R.drawable.ic_queue_music };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_library, c, false);
        TabLayout tabs = v.findViewById(R.id.library_tabs);
        ViewPager2 pager = v.findViewById(R.id.library_pager);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return new VideosPageFragment();
                    case 1: return new AudioPageFragment();
                    case 2: return new FoldersPageFragment();
                    default: return new PlaylistsPageFragment();
                }
            }
            @Override public int getItemCount() { return TITLES.length; }
        });
        new TabLayoutMediator(tabs, pager, (tab, pos) -> {
            tab.setText(TITLES[pos]);
            tab.setIcon(ICONS[pos]);
        }).attach();

        v.findViewById(R.id.btn_library_refresh).setOnClickListener(x -> {
            LibraryRepo.get(requireContext()).refresh();
            Toast.makeText(requireContext(), "Refreshing…", Toast.LENGTH_SHORT).show();
        });
        return v;
    }
}
