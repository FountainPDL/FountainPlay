package com.fountainpdl.fountainplay.ui.video;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;

public class VideoFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Video Library — scanning...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        new Thread(() -> {
            int count = MediaScanner.scanVideo(requireContext()).size();
            requireActivity().runOnUiThread(() -> tv.setText("🎬 " + count + " video files found"));
        }).start();
        return tv;
    }
}
