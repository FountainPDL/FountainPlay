package com.fountainpdl.fountainplay.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.*;

public class SettingsFragment extends Fragment {
    private AppPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        // Theme
        TextView tvTheme = view.findViewById(R.id.tv_theme_value);
        tvTheme.setText(capitalize(prefs.getTheme()));
        view.findViewById(R.id.pref_theme).setOnClickListener(v -> {
            String[] opts = {"Dark","Light","System (Auto)","AMOLED"};
            String[] vals = {"dark","light","system","amoled"};
            new AlertDialog.Builder(requireContext()).setTitle("Theme")
                .setItems(opts, (d, i) -> {
                    prefs.setTheme(vals[i]);
                    tvTheme.setText(opts[i]);
                    FountainApp.applyTheme(vals[i]);
                    requireActivity().recreate();
                }).show();
        });

        // Primary color
        view.findViewById(R.id.pref_primary_color).setOnClickListener(v -> showColorPicker(true));
        view.findViewById(R.id.pref_accent_color).setOnClickListener(v -> showColorPicker(false));

        // Speed
        TextView tvSpeed = view.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        view.findViewById(R.id.pref_speed).setOnClickListener(v -> {
            String[] opts = {"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
            float[] vals = {0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f};
            new AlertDialog.Builder(requireContext()).setTitle("Default Speed")
                .setItems(opts, (d,i) -> { prefs.setPlaybackSpeed(vals[i]); tvSpeed.setText(opts[i]); }).show();
        });

        // Skip
        TextView tvSkip = view.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + " seconds");
        view.findViewById(R.id.pref_skip).setOnClickListener(v -> {
            String[] opts = {"5s","10s","15s","30s","60s"};
            int[] vals = {5,10,15,30,60};
            new AlertDialog.Builder(requireContext()).setTitle("Skip Interval")
                .setItems(opts,(d,i)->{ prefs.setSkipInterval(vals[i]); tvSkip.setText(opts[i]); }).show();
        });

        // Switches
        ((SwitchCompat)view.findViewById(R.id.sw_resume)).setChecked(prefs.getResumePlayback());
        ((SwitchCompat)view.findViewById(R.id.sw_resume)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_RESUME_PLAYBACK,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_hw_accel)).setChecked(prefs.getHwAcceleration());
        ((SwitchCompat)view.findViewById(R.id.sw_hw_accel)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_HW_ACCELERATION,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_gestures)).setChecked(prefs.getGesturesEnabled());
        ((SwitchCompat)view.findViewById(R.id.sw_gestures)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_GESTURES_ENABLED,c2));
        ((SwitchCompat)view.findViewById(R.id.sw_remember_pos)).setChecked(prefs.getRememberPosition());
        ((SwitchCompat)view.findViewById(R.id.sw_remember_pos)).setOnCheckedChangeListener((b,c2) -> prefs.setBoolean(AppPreferences.KEY_REMEMBER_POSITION,c2));

        setupFolderPref(view, true);
        setupFolderPref(view, false);

        // Clear history
        view.findViewById(R.id.btn_clear_history).setOnClickListener(v ->
            new AlertDialog.Builder(requireContext()).setTitle("Clear History?")
                .setPositiveButton("Clear", (d,i) -> new Thread(() ->
                    com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext()).historyDao().clearAll()
                ).start())
                .setNegativeButton("Cancel",null).show());
    }

    private void showColorPicker(boolean isPrimary) {
        int[] colors = {0xFF7B2FBE,0xFF2196F3,0xFF4CAF50,0xFFFF5722,0xFFE91E63,0xFF009688,0xFF673AB7,0xFF795548};
        String[] names = {"Purple","Blue","Green","Orange","Pink","Teal","Deep Purple","Brown"};
        new AlertDialog.Builder(requireContext()).setTitle(isPrimary ? "Primary Color" : "Accent Color")
            .setItems(names, (d,i) -> {
                if (isPrimary) prefs.setPrimaryColor(colors[i]);
                else prefs.setAccentColor(colors[i]);
                Toast.makeText(requireContext(),"Restart app to apply",Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId   = isAudio ? R.id.tv_audio_folders   : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> cur = isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders();
        tv.setText(cur.isEmpty() ? "All folders" : cur.size() + " selected");
        view.findViewById(prefId).setOnClickListener(v -> new Thread(() -> {
            List<String> folders = isAudio ? MediaScanner.getAudioFolders(requireContext()) : MediaScanner.getVideoFolders(requireContext());
            if (folders.isEmpty()) return;
            String[] items = folders.toArray(new String[0]);
            boolean[] checked = new boolean[items.length];
            Set<String> sel = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
            for (int i=0;i<items.length;i++) checked[i] = sel.contains(items[i]);
            requireActivity().runOnUiThread(() ->
                new AlertDialog.Builder(requireContext())
                    .setTitle(isAudio ? "Music Folders" : "Video Folders")
                    .setMultiChoiceItems(items, checked, (d,i,c2) -> { if(c2) sel.add(items[i]); else sel.remove(items[i]); })
                    .setPositiveButton("OK",(d,i)->{
                        if(isAudio) prefs.setAudioFolders(sel); else prefs.setVideoFolders(sel);
                        tv.setText(sel.isEmpty()?"All folders":sel.size()+" selected");
                    }).setNegativeButton("Cancel",null).show());
        }).start());
    }

    private String capitalize(String s) { return s==null||s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1); }
}
