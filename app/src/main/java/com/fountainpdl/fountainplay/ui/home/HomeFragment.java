package com.fountainpdl.fountainplay.ui.home;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.databinding.FragmentHomeBinding;
import java.util.Calendar;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = h < 12 ? "Good morning ☀️" : h < 17 ? "Good afternoon 🎵" : "Good evening 🌙";
        binding.tvGreeting.setText(greet);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
