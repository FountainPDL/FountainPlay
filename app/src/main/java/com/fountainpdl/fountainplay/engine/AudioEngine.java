package com.fountainpdl.fountainplay.engine;

/** One way of decoding and playing an audio file. The service owns the session and notification; engines only play. */
public interface AudioEngine {

    interface Callback {
        void onPlayingChanged(AudioEngine source, boolean playing);
        void onEnded(AudioEngine source);
        /** The engine cannot play this file. */
        void onError(AudioEngine source, String why);
    }

    String name();

    /** Loads a file (path or content:// / http URI) and optionally starts it at startMs. */
    void load(String path, long startMs, boolean autoPlay);

    void play();
    void pause();
    void stop();
    void release();

    void seekTo(long ms);
    long getPosition();
    long getDuration();
    boolean isPlaying();

    void setSpeed(float speed);
}
