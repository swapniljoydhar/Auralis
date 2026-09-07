# Auralis Changelog

## 4.2.0 (Latest)

### Build & CI/CD Fixes
* **Fixed KSP/Kotlin version mismatch**: Aligned KSP version with Kotlin 2.0.21 to resolve build compilation failures.
* **Fixed CI/CD workflows**: Updated GitHub Actions to trigger on `main` and `dev` branches, use JDK 17, and run both build and lint checks.
* **Updated dependency versions**: Migrated to stable, compatible dependency versions (AGP 8.7.3, Kotlin 2.0.21, Room 2.6.1, Media3 1.5.0, Coil 3.0.0).
* **Added comprehensive ProGuard rules**: Added keep rules for Hilt, Room, ExoPlayer, Coil 3, and all model classes.
* **Fixed Spotless configuration**: Removed C++ formatting config (no native code), simplified to Kotlin-only formatting.

### Architecture & Build Modernization
* **Zero NDK / Pure Kotlin Pipeline**: Completely eliminated legacy C++ / NDK TagLib compilation and vendored submodules. `musikr` now uses pure Kotlin with Android's platform `MediaMetadataRetriever` for resilient, fast, and concurrent metadata extraction.
* **Official Media3 Migration**: Upgraded playback infrastructure from unbundled legacy ExoPlayer modules to official `androidx.media3` releases (`media3-exoplayer`, `media3-session`, `media3-ui`).

### Security Hardening
* **Secured exported components**: Added `readPermission` to `CoverProvider` to restrict cover art access.
* **Removed network permissions**: Explicitly removed `INTERNET` and `ACCESS_NETWORK_STATE` permissions (app is fully offline).
* **Fixed intent filter structure**: Separated `MAIN/LAUNCHER` intent filter from `MUSIC_PLAYER` filter for cleaner resolution.
* **Added Tasker package query**: Added specific `<queries>` entry for Tasker integration.

### Code Quality
* **Fixed memory leak in EmbeddedChapterReader**: Replaced unbounded `ConcurrentHashMap` with LRU-bounded `LinkedHashMap` (max 256 entries) to prevent OOM for large libraries.
* **Fixed ShakeDetector lifecycle**: Added proper state reset on `stop()` and null-check guard in `onSensorChanged()`.
* **Fixed vibration error handling**: Added specific `SecurityException` catch for missing vibration permission.
* **Replaced MotionUtils with direct SpringForce**: Removed dependency on Material 1.13+ `MotionUtils` API, using direct `SpringForce` construction for spring animations.
* **Updated ThemedSpeedDialView**: Replaced `MotionUtils.resolveThemeSpringForce` with standard `SpringForce` configuration.

### Audiobook Enhancements
* **Shake to Extend Sleep Timer**: Integrated an accelerometer sensor listener that allows users to gently shake their device to reset or extend an active sleep timer.
* **Speed-Scaled Chapter Sleep Timer**: The "End of Chapter" sleep timer now calculates wall-clock time accurately based on active playback speed.
* **Universal Embedded Chapter Support**: Embedded chapters are now parsed and accessible on all tracks with chapter metadata.
* **In-Player Bookmark Navigator**: Long-pressing the player's bookmark button displays an immediate list of saved bookmarks.

---

## 4.1.22

### Security & Hardening
* **Locked down playback broadcast receiver:** The dynamically-registered playback control receiver no longer accepts broadcasts from other applications.
* **Strictly-scoped notification PendingIntents:** Playback control PendingIntents now explicitly target Auralis' own package.
* **Neutralized log fingerprinting:** Reworked the copyleft notice logger.

### Audiobook Experience
* **Persistent playback speed:** The last speed actively used for audiobook playback is remembered and restored across app restarts.
* **Canonical speed range:** Recorded speeds cover the full 0.5x–3.0x audiobook range.

### Core Playback & Startup Stabilization
* **Resolved Startup Lifecycle Race:** Fixed `TabLayoutMediator is already attached` exception on launch.
* **Guarded Dynamic Bottom Sheet:** Resolved `UninitializedPropertyAccessException` in `PlaybackBottomSheetBehavior.createBackground()`.
* **Database Migration Hardening:** Enabled destructive migration fallbacks on `PersistenceDatabase`.
* **Asynchronous Cover Streaming & Caching:** Added in-memory `LruCache` (4MB) in `CoverProvider`.
* **Tasker Plugin Decoupling:** Removed 5-second blocking `Thread.sleep` polling loop.

### UI & UX Modernization (Material 3)
* **Material 3 Theme Harmonization:** Replaced legacy AppCompat attribute references with Material 3 design tokens.
* **Optimized List Rendering:** Upgraded adapters to use `ListAdapter` and `DiffUtil`.

---

## 4.1.21

### Dual-Domain Experience
* **Independent Playback Domains:** Complete isolation between `PlaybackDomain.MUSIC` and `PlaybackDomain.AUDIOBOOKS`.
* **Dedicated Audiobook Studio:** Added M4B chapter parsing, per-book progress tracking, bookmarks with notes, speed controls, and end-of-chapter sleep timer.
* **Material 3 Redesign:** Introduced adaptive dynamic color theming, AMOLED dark mode, fluid bottom-sheet controls.
