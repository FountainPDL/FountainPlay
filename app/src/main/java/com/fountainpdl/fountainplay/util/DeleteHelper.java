package com.fountainpdl.fountainplay.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Deletes media the way current Android requires: the system asks the user to confirm. Create it as a Fragment field. */
public final class DeleteHelper {
    private final ActivityResultLauncher<IntentSenderRequest> launcher;
    private Runnable pendingDone;

    public DeleteHelper(Fragment f) {
        launcher = f.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) finished();
        });
    }

    public void confirmAndDelete(Context ctx, List<MediaItem> items, Runnable done) {
        if (items == null || items.isEmpty()) return;
        String what = items.size() == 1 ? "\"" + items.get(0).getTitle() + "\"" : items.size() + " items";
        new MaterialAlertDialogBuilder(ctx)
            .setTitle("Delete " + what + "?")
            .setMessage("The file will be removed from your device. This can't be undone.")
            .setPositiveButton("Delete", (d, w) -> delete(ctx, items, done))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void delete(Context ctx, List<MediaItem> items, Runnable done) {
        pendingDone = done;
        List<Uri> uris = new ArrayList<>();
        List<MediaItem> notIndexed = new ArrayList<>();
        for (MediaItem m : items) {
            Uri u = MediaUris.contentUri(ctx, m);
            if (u != null) uris.add(u); else notIndexed.add(m);
        }
        // Files the system never indexed (.nomedia folders) can only be removed directly.
        for (MediaItem m : notIndexed) {
            if (m.getPath() != null && !new File(m.getPath()).delete()) {
                Toast.makeText(ctx, "Couldn't delete " + m.getTitle(), Toast.LENGTH_SHORT).show();
            }
        }
        if (uris.isEmpty()) { finished(); return; }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(ctx.getContentResolver(), uris);
                launcher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) {
                Toast.makeText(ctx, "Couldn't start delete", Toast.LENGTH_SHORT).show();
            }
        } else {
            boolean ok = true;
            for (Uri u : uris) {
                try { ok &= ctx.getContentResolver().delete(u, null, null) > 0; }
                catch (Exception e) { ok = false; }
            }
            if (!ok) Toast.makeText(ctx, "Some files couldn't be deleted", Toast.LENGTH_SHORT).show();
            finished();
        }
    }

    private void finished() {
        Runnable r = pendingDone;
        pendingDone = null;
        if (r != null) r.run();
    }
}
