package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.fountainpdl.fountainplay.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    // Songs / Albums / Artists / Videos / Playlists / History
    private static final String[] TABS = {
        "Songs", "Albums", "Artists", "Videos", "Playlists", "History"
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_library, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 vp   = view.findViewById(R.id.view_pager);
        TabLayout  tabs = view.findViewById(R.id.tab_layout);

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                return LibraryPageFragment.newInstance(TABS[pos]);
            }
            @Override public int getItemCount() { return TABS.length; }
        });

        new TabLayoutMediator(tabs, vp, (tab, pos) -> tab.setText(TABS[pos])).attach();
    }
}
