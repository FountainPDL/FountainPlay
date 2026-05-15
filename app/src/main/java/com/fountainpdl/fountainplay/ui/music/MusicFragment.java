package com.fountainpdl.fountainplay.ui.music;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;

public class MusicFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Music Library — scanning...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        new Thread(() -> {
            int count = MediaScanner.scanAudio(requireContext()).size();
            requireActivity().runOnUiThread(() -> tv.setText("🎵 " + count + " audio files found"));
        }).start();
        return tv;
    }
}
