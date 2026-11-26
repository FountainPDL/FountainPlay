package com.fountainpdl.fountainplay.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.Locale;
import androidx.navigation.fragment.NavHostFragment;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.player.LockScreenActivity;
import com.fountainpdl.fountainplay.util.AllFiles;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Scrollable, grouped settings. Every row is built from one layout, so everything follows the theme. */
public class SettingsFragment extends Fragment {

    private AppPreferences prefs;
    private LinearLayout container;
    private LayoutInflater inflater;

    private static final String[] THEME_KEYS   = {"system", "light", "dark", "amoled"};
    private static final String[] THEME_LABELS = {"Follow system", "Light", "Dark", "AMOLED black"};
    private static final String[] ACCENT_KEYS   = {"fountain", "ocean", "sunset", "forest", "rose", "graphite"};
    private static final String[] ACCENT_LABELS = {"Fountain (violet and crimson)", "Ocean", "Sunset", "Forest", "Rose", "Graphite"};
    private static final int[] SKIP_VALUES = {5, 10, 15, 30, 60};
    private static final float[] SPEED_VALUES = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

    private final ActivityResultLauncher<String> notificationPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!isAdded()) return;
            if (!granted && !notificationsAllowed()) openNotificationSettings();
            build();
        });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_settings, c, false);
        inflater = inf;
        prefs = new AppPreferences(requireContext());
        container = v.findViewById(R.id.settings_container);
        // Rows are rebuilt from the saved settings every time. The system must never "restore" their state.
        container.setSaveEnabled(false);
        build();
        return v;
    }

    /** Coming back from a system settings screen: show the new permission / battery status. */
    @Override public void onResume() {
        super.onResume();
        if (container != null) build();
        // All files access was just turned on: index everything now
        if (AllFiles.granted() && !prefs.getBoolean("all_files_indexed", false)) {
            prefs.setBoolean("all_files_indexed", true);
            LibraryRepo.get(requireContext()).refresh();
        }
    }

    private void build() {
        container.removeAllViews();

        header("Appearance");
        choiceRow(R.drawable.ic_visibility, "Theme", label(THEME_KEYS, THEME_LABELS, prefs.getTheme()), v -> pickTheme());
        choiceRow(R.drawable.ic_palette, "Accent colour", label(ACCENT_KEYS, ACCENT_LABELS, prefs.getAccent()), v -> pickAccent());
        boolean dynOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && DynamicColors.isDynamicColorAvailable();
        toggleRow(R.drawable.ic_palette, "Material You colours",
            dynOk ? "Use your wallpaper's colours instead of the accent" : "Needs Android 12 or newer",
            AppPreferences.KEY_DYNAMIC_COLOR, false, dynOk, checked -> requireActivity().recreate());

        header("Notifications and background");
        toggleRow(R.drawable.ic_music_note, "Keep playing when the app is closed",
            "Music continues after you swipe the app away from Recents",
            AppPreferences.KEY_BACKGROUND, true, true, null);
        toggleRow(R.drawable.ic_notifications, "Lock screen player",
            "Cover art, controls and \"Slide to unlock\" over the lock screen when the screen turns off while music plays",
            AppPreferences.KEY_LOCK_SCREEN, true, true, checked -> {
                if (checked && !canOverlay()) askOverlay();
            });
        choiceRow(R.drawable.ic_visibility, "Display over other apps",
            canOverlay() ? "Allowed" : "Blocked", v -> askOverlay());
        if (isXiaomi()) {
            actionRow(R.drawable.ic_info, "Xiaomi / MIUI permissions",
                "Turn on Show on lock screen and Display pop-up windows while running in the background, and set Autostart on",
                v -> openMiuiPermissions());
        }
        actionRow(R.drawable.ic_play, "Preview the lock screen player",
            "See how it looks without locking your phone", v ->
                startActivity(new Intent(requireContext(), LockScreenActivity.class)
                    .putExtra(LockScreenActivity.EXTRA_PREVIEW, true)));

        header("Background playback check");
        choiceRow(R.drawable.ic_notifications, "Notification permission",
            notificationsAllowed() ? "Allowed" : "Blocked", v -> askNotificationPermission());
        choiceRow(R.drawable.ic_schedule, "Battery restriction",
            ignoringBatteryOptimizations() ? "Unrestricted" : "Optimized", v -> openBatterySettings());
        actionRow(R.drawable.ic_info, "If music still stops",
            "Some phones (Xiaomi, Huawei, Oppo, Samsung and others) stop music apps in the background. "
                + "Set Fountain Play to Unrestricted above, and allow notifications.", null);

        header("Video playback");
        toggleRow(R.drawable.ic_play, "Remember position",
            "Resume videos where you stopped; feeds Continue watching",
            AppPreferences.KEY_REMEMBER_POS, true, true, null);
        toggleRow(R.drawable.ic_fast_forward, "Swipe gestures",
            "Brightness on the left, volume on the right, seek across",
            AppPreferences.KEY_GESTURES, true, true, null);
        toggleRow(R.drawable.ic_pip, "Automatic picture-in-picture",
            "Keep playing in a small window when you leave the app",
            AppPreferences.KEY_AUTO_PIP, true, true, null);
        choiceRow(R.drawable.ic_fast_forward, "Skip interval", prefs.getSkipInterval() + " seconds", v -> pickSkip());
        choiceRow(R.drawable.ic_speed, "Default speed", speedLabel(prefs.getPlaybackSpeed()), v -> pickSpeed());

        header("Library");
        actionRow(R.drawable.ic_folder_off, "Manage folders",
            "Hide folders, or scan folders that contain a .nomedia file", v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_manage_folders));
        choiceRow(R.drawable.ic_folder, "All files access",
            AllFiles.granted() ? "Allowed" : AllFiles.supported() ? "Not allowed" : "Not needed", v -> openAllFiles());
        toggleRow(R.drawable.ic_refresh, "Scan all storage automatically",
            "Finds songs and videos Android never indexed, in any folder. Needs All files access.",
            AppPreferences.KEY_SCAN_ALL, true, AllFiles.supported(),
            checked -> LibraryRepo.get(requireContext()).refresh());
        toggleRow(R.drawable.ic_folder_off, "Include .nomedia and hidden folders",
            "Also scan folders Android skips (can add app caches and voice notes)",
            AppPreferences.KEY_SCAN_NOMEDIA, false, AllFiles.supported(),
            checked -> LibraryRepo.get(requireContext()).refresh());
        actionRow(R.drawable.ic_refresh, "Rescan library", "Look for new and removed files", v -> {
            LibraryRepo.get(requireContext()).refresh();
            Toast.makeText(requireContext(), "Scanning…", Toast.LENGTH_SHORT).show();
        });

        header("About");
        actionRow(R.drawable.ic_info, "Fountain Play", version(), null);
        actionRow(R.drawable.ic_favorite, "FountainPDL Ministry",
            "Offline, no account, no ads, no tracking", null);
    }

    // ── Row builders ────────────────────────────────────────────────────────────

    private void header(String text) {
        TextView t = (TextView) inflater.inflate(R.layout.item_setting_header, container, false);
        t.setText(text);
        container.addView(t);
    }

    private View baseRow(@DrawableRes int icon, String title, String sub) {
        View row = inflater.inflate(R.layout.item_setting_row, container, false);
        row.setSaveEnabled(false);
        ((ImageView) row.findViewById(R.id.iv_setting_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.tv_setting_title)).setText(title);
        TextView s = row.findViewById(R.id.tv_setting_sub);
        if (sub == null || sub.isEmpty()) s.setVisibility(View.GONE); else s.setText(sub);
        container.addView(row);
        return row;
    }

    private void actionRow(@DrawableRes int icon, String title, String sub, View.OnClickListener l) {
        View row = baseRow(icon, title, sub);
        if (l != null) row.setOnClickListener(l); else { row.setClickable(false); row.setFocusable(false); }
    }

    private void choiceRow(@DrawableRes int icon, String title, String value, View.OnClickListener l) {
        View row = baseRow(icon, title, null);
        TextView val = row.findViewById(R.id.tv_setting_value);
        val.setText(value);
        val.setVisibility(View.VISIBLE);
        row.setOnClickListener(l);
    }

    private interface OnToggle { void onChanged(boolean checked); }

    /**
     * A switch row. The setting is saved only when the user taps it. (Listening for "checked changed"
     * would also fire when Android restores the screen, and that used to save every switch as off.)
     */
    private void toggleRow(@DrawableRes int icon, String title, String sub, final String key,
                           boolean def, boolean enabled, final OnToggle after) {
        View row = baseRow(icon, title, sub);
        final MaterialSwitch sw = row.findViewById(R.id.sw_setting);
        sw.setId(View.generateViewId());       // every row used the same id: never share state between switches
        sw.setSaveEnabled(false);
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setEnabled(enabled);
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.5f);
        if (!enabled) return;
        sw.setOnClickListener(v -> {
            boolean now = sw.isChecked();
            prefs.setBoolean(key, now);
            if (after != null) after.onChanged(now);
        });
        row.setOnClickListener(v -> sw.performClick());
    }

    // ── Pickers ─────────────────────────────────────────────────────────────────

    private void pickTheme() {
        int sel = indexOf(THEME_KEYS, prefs.getTheme());
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Theme")
            .setSingleChoiceItems(THEME_LABELS, sel, (d, which) -> {
                prefs.setTheme(THEME_KEYS[which]);
                d.dismiss();
                applyNightMode(THEME_KEYS[which]);
                requireActivity().recreate();
            }).show();
    }

    public static void applyNightMode(String theme) {
        switch (theme) {
            case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "dark":
            case "amoled": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void pickAccent() {
        int sel = indexOf(ACCENT_KEYS, prefs.getAccent());
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Accent colour")
            .setSingleChoiceItems(ACCENT_LABELS, sel, (d, which) -> {
                prefs.setAccent(ACCENT_KEYS[which]);
                d.dismiss();
                requireActivity().recreate();
            }).show();
    }

    private void pickSkip() {
        String[] labels = new String[SKIP_VALUES.length];
        int sel = 1;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = SKIP_VALUES[i] + " seconds";
            if (SKIP_VALUES[i] == prefs.getSkipInterval()) sel = i;
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Skip interval")
            .setSingleChoiceItems(labels, sel, (d, which) -> {
                prefs.setSkipInterval(SKIP_VALUES[which]);
                d.dismiss();
                build();
            }).show();
    }

    private void pickSpeed() {
        String[] labels = new String[SPEED_VALUES.length];
        int sel = 2;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = speedLabel(SPEED_VALUES[i]);
            if (Math.abs(SPEED_VALUES[i] - prefs.getPlaybackSpeed()) < 0.01f) sel = i;
        }
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Default speed")
            .setSingleChoiceItems(labels, sel, (d, which) -> {
                prefs.setPlaybackSpeed(SPEED_VALUES[which]);
                d.dismiss();
                build();
            }).show();
    }

    // ── Notification permission and battery ─────────────────────────────────────

    private boolean notificationsAllowed() {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
    }

    private void askNotificationPermission() {
        if (notificationsAllowed()) {
            Toast.makeText(requireContext(), "Notifications are already allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            openNotificationSettings();
        }
    }

    private void openNotificationSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(i);
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private boolean ignoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
    }

    private void openBatterySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            Toast.makeText(requireContext(), "Find Fountain Play and choose Don't optimise", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext());
    }

    private void askOverlay() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName())));
            Toast.makeText(requireContext(), "Allow Fountain Play to display over other apps", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private boolean isXiaomi() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco");
    }

    private void openMiuiPermissions() {
        String pkg = requireContext().getPackageName();
        try {
            Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
            i.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            i.putExtra("extra_pkgname", pkg);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i2 = new Intent("miui.intent.action.APP_PERM_EDITOR");
                i2.setPackage("com.miui.securitycenter");
                i2.putExtra("extra_pkgname", pkg);
                startActivity(i2);
            } catch (Exception e2) {
                openAppDetails();
            }
        }
    }

    private void openAllFiles() {
        if (!AllFiles.supported()) {
            Toast.makeText(requireContext(), "Not needed on this Android version", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(AllFiles.settingsIntent(requireContext()));
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private void openAppDetails() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + requireContext().getPackageName())));
        } catch (Exception ignored) { }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return 0;
    }

    private static String label(String[] keys, String[] labels, String key) { return labels[indexOf(keys, key)]; }

    private static String speedLabel(float s) { return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "×"; }

    private String version() {
        try {
            PackageInfo pi = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            return "Version " + pi.versionName;
        } catch (Exception e) {
            return "";
        }
    }
}
