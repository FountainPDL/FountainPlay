package com.fountainpdl.fountainplay.engine;

import com.fountainpdl.fountainplay.util.FolderScanner;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Decides which engine gets a file first. ExoPlayer is fast and light; VLC plays almost everything else. */
public final class EngineSelector {
    private EngineSelector() {}

    /** Audio formats that Android's decoders and ExoPlayer's parsers cannot handle. */
    private static final Set<String> VLC_AUDIO = new HashSet<>(Arrays.asList(
        "ape", "wma", "wv", "mpc", "tta", "dts", "dtshd", "thd", "mlp", "tak", "aiff", "aif", "aifc",
        "au", "snd", "ra", "rm", "caf", "gsm", "spx", "mod", "xm", "it", "s3m", "mka", "oma", "voc", "w64", "shn"));

    /** Video formats that need VLC's parsers and software decoders. */
    private static final Set<String> VLC_VIDEO = new HashSet<>(Arrays.asList(
        "avi", "wmv", "asf", "rm", "rmvb", "divx", "xvid", "vob", "ogm", "dat", "wtv", "mxf", "dv", "iso", "mpe", "evo", "ifo"));

    public static boolean audioNeedsVlc(String path) { return VLC_AUDIO.contains(FolderScanner.ext(path)); }
    public static boolean videoNeedsVlc(String path) { return VLC_VIDEO.contains(FolderScanner.ext(path)); }
}
