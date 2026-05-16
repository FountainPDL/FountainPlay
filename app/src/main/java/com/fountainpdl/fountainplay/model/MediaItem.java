package com.fountainpdl.fountainplay.model;

public class MediaItem {
    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_VIDEO = 1;

    private long id, duration, size, dateAdded;
    private String title, artist, album, path, albumArtUri;
    private int type;

    public MediaItem() {}
    public MediaItem(long id, String title, String artist, String path, long duration, int type) {
        this.id = id; this.title = title; this.artist = artist;
        this.path = path; this.duration = duration; this.type = type;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title != null ? title : "Unknown"; }
    public void setTitle(String t) { this.title = t; }
    public String getArtist() { return artist != null && !artist.equals("<unknown>") ? artist : "Unknown Artist"; }
    public void setArtist(String a) { this.artist = a; }
    public String getAlbum() { return album != null ? album : "Unknown Album"; }
    public void setAlbum(String a) { this.album = a; }
    public String getPath() { return path; }
    public void setPath(String p) { this.path = p; }
    public long getDuration() { return duration; }
    public void setDuration(long d) { this.duration = d; }
    public long getSize() { return size; }
    public void setSize(long s) { this.size = s; }
    public int getType() { return type; }
    public void setType(int t) { this.type = t; }
    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String u) { this.albumArtUri = u; }
    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long d) { this.dateAdded = d; }
    public boolean isAudio() { return type == TYPE_AUDIO; }
    public boolean isVideo() { return type == TYPE_VIDEO; }

    public String getFormattedDuration() {
        long s = duration / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
    public String getFolder() {
        if (path == null) return "";
        int idx = path.lastIndexOf('/');
        return idx > 0 ? path.substring(0, idx) : path;
    }
    public String getFileName() {
        if (path == null) return title;
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
