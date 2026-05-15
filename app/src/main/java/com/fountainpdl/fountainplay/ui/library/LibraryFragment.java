package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

public class LibraryFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("📚 Library — Playlists, History, Downloads");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        return tv;
    }
}
