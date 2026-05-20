package com.fountainpdl.fountainplay.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.*;

public class SettingsFragment extends Fragment {

    private AppPreferences prefs;

    // Preset color palettes  [name, primaryColor(int), accentColor(int)]
    private static final Object[][] COLOR_PRESETS = {
        {"Purple + Red (Default)", 0xFF7B2FBE, 0xFFE53935},
        {"Blue + Orange",          0xFF1565C0, 0xFFFF6F00},
        {"Teal + Pink",            0xFF00695C, 0xFFE91E63},
        {"Deep Purple + Amber",    0xFF4527A0, 0xFFFFB300},
        {"Indigo + Green",         0xFF283593, 0xFF2E7D32},
        {"Red + Blue",             0xFFC62828, 0xFF1565C0},
        {"Dark Green + Gold",      0xFF1B5E20, 0xFFFFC107},
        {"Monochrome",             0xFF424242, 0xFF757575},
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        setupTheme(view);
        setupColorScheme(view);
        setupPlayback(view);
        setupVideo(view);
        setupFolders(view);
        setupData(view);
    }

    // ── THEME ────────────────────────────────────────────────
    private void setupTheme(View v) {
        TextView tvVal = v.findViewById(R.id.tv_theme_value);
        tvVal.setText(themeLabel(prefs.getTheme()));

        v.findViewById(R.id.pref_theme).setOnClickListener(__ -> {
            String[] labels = {"Dark", "Light", "AMOLED", "Follow System"};
            String[] values = {"dark", "light", "amoled", "system"};
            int cur = indexOf(values, prefs.getTheme());
            new AlertDialog.Builder(requireContext())
                .setTitle("Theme")
                .setSingleChoiceItems(labels, cur, (d, i) -> {
                    prefs.setTheme(values[i]);
                    tvVal.setText(labels[i]);
                    // Apply night mode immediately
                    FountainApp.applyNightMode(values[i]);
                    d.dismiss();
                    // Recreate so per-activity theme (AMOLED) kicks in
                    requireActivity().recreate();
                }).show();
        });
    }

    // ── COLOR SCHEME ─────────────────────────────────────────
    private void setupColorScheme(View v) {
        View primaryDot = v.findViewById(R.id.color_preview_primary);
        View accentDot  = v.findViewById(R.id.color_preview_accent);
        updateDot(primaryDot, prefs.getPrimaryColor());
        updateDot(accentDot, prefs.getAccentColor());

        v.findViewById(R.id.pref_primary_color).setOnClickListener(__ -> {
            String[] names = new String[COLOR_PRESETS.length];
            for (int i = 0; i < COLOR_PRESETS.length; i++) names[i] = (String) COLOR_PRESETS[i][0];
            new AlertDialog.Builder(requireContext())
                .setTitle("Color Scheme")
                .setItems(names, (d, i) -> {
                    int primary = (int) COLOR_PRESETS[i][1];
                    int accent  = (int) COLOR_PRESETS[i][2];
                    prefs.setPrimaryColor(primary);
                    prefs.setAccentColor(accent);
                    updateDot(primaryDot, primary);
                    updateDot(accentDot, accent);
                    Toast.makeText(requireContext(),
                        "Color scheme applied — restart app to see all changes",
                        Toast.LENGTH_SHORT).show();
                }).show();
        });
    }

    private void updateDot(View dot, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        dot.setBackground(gd);
    }

    // ── PLAYBACK ─────────────────────────────────────────────
    private void setupPlayback(View v) {
        TextView tvSpeed = v.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        v.findViewById(R.id.pref_speed).setOnClickListener(__ -> {
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","3.0×"};
            float[]  vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
            new AlertDialog.Builder(requireContext()).setTitle("Default Speed")
                .setItems(opts, (d, i) -> {
                    prefs.setPlaybackSpeed(vals[i]);
                    tvSpeed.setText(opts[i]);
                }).show();
        });

        TextView tvSkip = v.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + "s");
        v.findViewById(R.id.pref_skip).setOnClickListener(__ -> {
            String[] opts = {"5s","10s","15s","30s","60s"};
            int[]    vals = {5, 10, 15, 30, 60};
            new AlertDialog.Builder(requireContext()).setTitle("Skip Interval")
                .setItems(opts, (d, i) -> {
                    prefs.setSkipInterval(vals[i]);
                    tvSkip.setText(opts[i]);
                }).show();
        });

        bindSwitch(v, R.id.sw_resume, AppPreferences.KEY_RESUME_PLAYBACK, prefs.getResumePlayback());
    }

    // ── VIDEO ────────────────────────────────────────────────
    private void setupVideo(View v) {
        bindSwitch(v, R.id.sw_hw_accel,    AppPreferences.KEY_HW_ACCEL, prefs.getHwAcceleration());
        bindSwitch(v, R.id.sw_gestures,    AppPreferences.KEY_GESTURES, prefs.getGesturesEnabled());
        bindSwitch(v, R.id.sw_remember_pos,AppPreferences.KEY_REMEMBER_POS, prefs.getRememberPosition());
    }

    // ── FOLDERS ──────────────────────────────────────────────
    private void setupFolders(View v) {
        setupFolderPref(v, true);   // music
        setupFolderPref(v, false);  // video
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId   = isAudio ? R.id.tv_audio_folders  : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> saved = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
        tv.setText(saved.isEmpty() ? "All folders" : saved.size() + " selected");

        view.findViewById(prefId).setOnClickListener(vv ->
            new Thread(() -> {
                List<String> folders = isAudio
                    ? MediaScanner.getAudioFolders(requireContext())
                    : MediaScanner.getVideoFolders(requireContext());
                requireActivity().runOnUiThread(() -> {
                    if (folders.isEmpty()) {
                        Toast.makeText(requireContext(), "No folders found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String[] items = folders.toArray(new String[0]);
                    boolean[] checked = new boolean[items.length];
                    for (int i = 0; i < items.length; i++) checked[i] = saved.contains(items[i]);
                    new AlertDialog.Builder(requireContext())
                        .setTitle(isAudio ? "Music Folders" : "Video Folders")
                        .setMultiChoiceItems(items, checked, (d, i, c) -> {
                            if (c) saved.add(items[i]); else saved.remove(items[i]);
                        })
                        .setPositiveButton("Apply", (d, i) -> {
                            if (isAudio) prefs.setAudioFolders(saved);
                            else prefs.setVideoFolders(saved);
                            tv.setText(saved.isEmpty() ? "All folders" : saved.size() + " selected");
                        })
                        .setNegativeButton("Cancel", null).show();
                });
            }).start()
        );
    }

    // ── DATA ─────────────────────────────────────────────────
    private void setupData(View v) {
        v.findViewById(R.id.btn_clear_history).setOnClickListener(__ ->
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear History?")
                .setMessage("This will erase all play history.")
                .setPositiveButton("Clear", (d, i) ->
                    new Thread(() ->
                        com.fountainpdl.fountainplay.db.AppDatabase
                            .get(requireContext()).historyDao().clearAll()
                    ).start()
                )
                .setNegativeButton("Cancel", null).show()
        );
    }

    // ── Helpers ───────────────────────────────────────────────
    private void bindSwitch(View root, int swId, String key, boolean current) {
        SwitchCompat sw = root.findViewById(swId);
        if (sw == null) return;
        sw.setChecked(current);
        sw.setOnCheckedChangeListener((b, c) -> prefs.setBoolean(key, c));
    }

    private String themeLabel(String val) {
        switch (val) {
            case "light":  return "Light";
            case "amoled": return "AMOLED";
            case "system": return "Follow System";
            default:       return "Dark";
        }
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return 0;
    }
}
