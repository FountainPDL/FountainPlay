package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.Intent;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** One place that knows how to start playback for a tapped item. */
public final class MediaLauncher {
    private MediaLauncher() {}

    /** Plays list[index]. Audio fills the queue with every audio item in the list; video does the same for video. */
    public static void open(Context ctx, List<MediaItem> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return;
        MediaItem item = list.get(index);
        if (item.getPath() == null) return;
        if (item.isVideo()) {
            List<MediaItem> videos = filter(list, true);
            VideoQueue.set(videos, videos.indexOf(item));
            Intent i = new Intent(ctx, VideoPlayerActivity.class);
            i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
            ctx.startActivity(i);
        } else {
            List<MediaItem> songs = filter(list, false);
            PlayQueue.get().setQueue(songs, Math.max(0, songs.indexOf(item)));
            ctx.startActivity(audioIntent(ctx, item));
        }
    }

    public static void shuffleAudio(Context ctx, List<MediaItem> list) {
        List<MediaItem> songs = filter(list, false);
        if (songs.isEmpty()) return;
        PlayQueue.get().setShuffle(true);
        int start = new Random().nextInt(songs.size());
        PlayQueue.get().setQueue(songs, start);
        ctx.startActivity(audioIntent(ctx, PlayQueue.get().current()));
    }

    public static Intent audioIntent(Context ctx, MediaItem item) {
        Intent i = new Intent(ctx, AudioPlayerActivity.class);
        if (item != null) {
            i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
        } else {
            i.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
        }
        return i;
    }

    private static List<MediaItem> filter(List<MediaItem> list, boolean video) {
        List<MediaItem> out = new ArrayList<>();
        for (MediaItem m : list) if (m.isVideo() == video && m.getPath() != null) out.add(m);
        return out;
    }
}
