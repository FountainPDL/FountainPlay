# Fountain Play

Offline-first Android media player by FountainPDL Ministry. No account, no ads, no telemetry.

- Package: `com.fountainpdl.fountainplay` | Min SDK 26 | Target SDK 34
- Java + AndroidX + Material 3 + Media3 (ExoPlayer) + Room + Glide

## Building (GitHub Actions only)
Every push to `main` or `dev` builds a debug and a release APK and uploads them as workflow artifacts.
No local build or Android Studio is needed.

## Updating over an installed copy
All builds are signed with the committed `app/debug.keystore` and `versionCode` rises with every CI run
(`1000 + run number`), so a new APK installs over the old one. This only holds for APKs built after
the keystore was added: install the first one after uninstalling any older CI build once.

## Docs
See `CHANGELOG.md` for what changed in each update.
