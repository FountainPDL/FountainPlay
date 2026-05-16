package com.fountainpdl.fountainplay.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class SettingsFragment extends Fragment {

    private AppPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        // Theme
        TextView tvTheme = view.findViewById(R.id.tv_theme_value);
        tvTheme.setText(capitalize(prefs.getTheme()));
        view.findViewById(R.id.pref_theme).setOnClickListener(v -> {
            String[] opts = {"Dark", "Light", "System"};
            new AlertDialog.Builder(requireContext())
                .setTitle("Theme").setItems(opts, (d, i) -> {
                    String val = opts[i].toLowerCase();
                    prefs.setTheme(val);
                    tvTheme.setText(opts[i]);
                }).show();
        });

        // Speed
        TextView tvSpeed = view.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        view.findViewById(R.id.pref_speed).setOnClickListener(v -> {
            String[] opts = {"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
            float[] vals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            new AlertDialog.Builder(requireContext())
                .setTitle("Default Speed").setItems(opts, (d, i) -> {
                    prefs.setPlaybackSpeed(vals[i]);
                    tvSpeed.setText(opts[i]);
                }).show();
        });

        // Skip interval
        TextView tvSkip = view.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + " seconds");
        view.findViewById(R.id.pref_skip).setOnClickListener(v -> {
            String[] opts = {"5 seconds","10 seconds","15 seconds","30 seconds","60 seconds"};
            int[] vals = {5, 10, 15, 30, 60};
            new AlertDialog.Builder(requireContext())
                .setTitle("Skip Interval").setItems(opts, (d, i) -> {
                    prefs.setSkipInterval(vals[i]);
                    tvSkip.setText(opts[i]);
                }).show();
        });

        // Switches
        SwitchCompat swResume = view.findViewById(R.id.sw_resume);
        swResume.setChecked(prefs.getResumePlayback());
        swResume.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_RESUME_PLAYBACK, checked));

        SwitchCompat swHw = view.findViewById(R.id.sw_hw_accel);
        swHw.setChecked(prefs.getHwAcceleration());
        swHw.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_HW_ACCELERATION, checked));

        SwitchCompat swGestures = view.findViewById(R.id.sw_gestures);
        swGestures.setChecked(prefs.getGesturesEnabled());
        swGestures.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_GESTURES_ENABLED, checked));

        SwitchCompat swPos = view.findViewById(R.id.sw_remember_pos);
        swPos.setChecked(prefs.getRememberPosition());
        swPos.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_REMEMBER_POSITION, checked));

        // Folder selectors
        setupFolderPref(view, true);
        setupFolderPref(view, false);
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId = isAudio ? R.id.tv_audio_folders : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> current = isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders();
        tv.setText(current.isEmpty() ? "All folders" : current.size() + " selected");

        view.findViewById(prefId).setOnClickListener(v -> {
            List<String> folders = isAudio
                ? MediaScanner.getAudioFolders(requireContext())
                : MediaScanner.getVideoFolders(requireContext());
            if (folders.isEmpty()) { tv.setText("No folders found"); return; }
            String[] items = folders.toArray(new String[0]);
            boolean[] checked = new boolean[items.length];
            Set<String> sel = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
            for (int i = 0; i < items.length; i++) checked[i] = sel.contains(items[i]);

            new AlertDialog.Builder(requireContext())
                .setTitle(isAudio ? "Music Folders" : "Video Folders")
                .setMultiChoiceItems(items, checked, (d, i, c) -> {
                    if (c) sel.add(items[i]); else sel.remove(items[i]);
                })
                .setPositiveButton("OK", (d, i) -> {
                    if (isAudio) prefs.setAudioFolders(sel); else prefs.setVideoFolders(sel);
                    tv.setText(sel.isEmpty() ? "All folders" : sel.size() + " selected");
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
