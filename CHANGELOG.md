# Changelog

## 1.4.0 - Phase 2: themes, labeled UI, new screens
- Theme system: Light / Dark / AMOLED / follow system, six accent palettes (Fountain is taken from the app icon), Material You colours on Android 12+. Every screen reads colours from the theme; no colours are hard-coded in layouts, drawables or Java.
- Home and Library no longer stuck on dark: the old layouts used fixed dark colours and the base theme was dark-only.
- Buttons are labeled and use standard icons (Shuffle, Previous, Play/Pause, Next, Repeat, Speed, Sleep, Queue, Playlist, Share, Like...). Video controls are labeled too.
- New Home: greeting, Shuffle music and Resume, Continue watching, New videos, New music, Favourites.
- New Library: Videos, Audio, Folders, Playlists; filter, sort, Play all, Shuffle.
- Folders: "Folders with media" and "All folders" modes, show hidden files, .nomedia folders flagged.
- Manage folders: hide folders from Home/Library/Search (Folders tab still shows them) and add .nomedia folders to scan.
- New Search tab (All / Songs / Videos), History screen from the Home menu.
- Settings rebuilt: scrollable and grouped; toggles for notification/lock-screen controls and background playback.
- Delete uses the system confirmation (Android 11+). Audio library now includes podcasts and audiobooks.
- Livelier UI: staggered entrances, press feedback, album-art colour backdrop on Now Playing, pill navigation, elastic tab indicator.
- Your real launcher icon (adaptive, nothing cut off).

## 1.3.0 - Phase 1: playback, notification, build
- Notification player, lock-screen controls and Bluetooth buttons work (Media3 session registered with the service).
- Music keeps playing after the app is cleared from Recents.
- Android 13+ notification permission requested, including on upgrade installs.
- Video player rebuilt (controls re-appear, no overlap, double-tap seek, subtitle and audio tracks, resume position, PiP).
- Layout insets fixed, icon fixed, sharing fixed, fixed signing key and auto versionCode.
