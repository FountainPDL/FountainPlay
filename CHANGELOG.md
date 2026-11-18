# Changelog

## 1.3.0 - Phase 1: playback, notification, build
- Notification player, lock-screen controls and Bluetooth/headset buttons now work (Media3 session registered with the service).
- Music keeps playing after the app is cleared from Recents (setting key `background_playback`, default on).
- Android 13+ notification permission is requested, including on upgrade installs.
- Tracks no longer start twice; Next/Previous work from the notification; repeat-one fixed; Up Next can jump backwards.
- Sleep timer lives in the service (works with the screen off) and can be cancelled.
- Video player rebuilt: controls re-appear after hiding, no button overlap, double-tap seek, real subtitle and audio-track selection, load .srt/.vtt/.ass, next/previous video, resume position, picture-in-picture sizing, no accidental autoplay.
- Layout: system bars no longer cover content, bottom navigation no longer cut off, back button goes Home first.
- Launcher icon fixed (adaptive icon with safe-zone padding).
- Sharing uses content URIs (no more crash); history no longer duplicates.
- Build: committed fixed keystore, auto versionCode, release APK, lint report, unit-test step.
