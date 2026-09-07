# Auralis Changelog

## 4.2.0 (Latest)

### Architecture & Build Modernization
* **Zero NDK / Pure Kotlin Pipeline**: Completely eliminated legacy C++ / NDK TagLib compilation and vendored submodules. `musikr` now uses pure Kotlin with Android's platform `MediaMetadataRetriever` for resilient, fast, and concurrent metadata extraction.
* **Official Media3 Migration**: Upgraded playback infrastructure from unbundled legacy ExoPlayer modules to official `androidx.media3` releases (`media3-exoplayer`, `media3-session`, `media3-ui`).
* **Modernized Toolchain**: Aligned build pipeline with Gradle 9.3.1 and Android Gradle Plugin 9.1.1.
* **Complete Legacy Purge**: Purged all legacy third-party branding, dead files, and obsolete artifacts across the entire codebase, resource keys, and documentation.

### Audiobook Enhancements
* **Shake to Extend Sleep Timer**: Integrated an accelerometer sensor listener that allows users to gently shake their device to reset or extend an active sleep timer. Provides gentle haptic feedback on reset and consumes zero battery when the sleep timer is idle. Configurable via Audiobook Settings.
* **Speed-Scaled Chapter Sleep Timer**: The "End of Chapter" sleep timer now calculates wall-clock time accurately based on active playback speed (e.g. 1.25x, 1.5x, 2.0x).
* **Universal Embedded Chapter Support**: Embedded chapters are now parsed and accessible on all tracks with chapter metadata, whether single-file (M4B) or multi-file audiobooks.
* **In-Player Bookmark Navigator**: Long-pressing the player's bookmark button displays an immediate list of saved bookmarks for the current book with one-tap jump-to-position functionality.

---

## 4.1.22

### Security & Hardening
* **Locked down playback broadcast receiver:** The dynamically-registered playback control receiver no longer accepts broadcasts from other applications; only protected system events (headset plug, audio becoming noisy) and Auralis' own notification/widget actions are delivered.
* **Strictly-scoped notification PendingIntents:** Playback control PendingIntents now explicitly target Auralis' own package, preventing any cross-app resolution.
* **Neutralized log fingerprinting:** Reworked the copyleft notice logger; logs are cleaner and no longer inherited from upstream project voice.

### Audiobook Experience
* **Persistent playback speed:** The last speed actively used for audiobook playback is remembered and restored across app restarts and Music/Audiobooks domain switches, instead of resetting to the default.
* **Canonical speed range:** Recorded speeds cover the full 0.5x–3.0x audiobook range.

### Core Playback & Startup Stabilization
* **Resolved Startup Lifecycle Race:** Fixed `TabLayoutMediator is already attached` exception on launch by tracking mediator state, detaching prior to tab re-instantiation, and adding stable item IDs to `HomePagerAdapter`.
* **Guarded Dynamic Bottom Sheet:** Resolved `UninitializedPropertyAccessException` in `PlaybackBottomSheetBehavior.createBackground()` on early layout passes.
* **Database Migration Hardening:** Enabled destructive migration fallbacks on `PersistenceDatabase` to prevent database schema mismatch crashes during app updates.
* **Asynchronous Cover Streaming & Caching:** Added in-memory `LruCache` (4MB) and tightened worker pipe timeouts in `CoverProvider` to eliminate disk I/O contention during rapid media-session and lock-screen cover queries.
* **Tasker Plugin Decoupling:** Removed 5-second blocking `Thread.sleep` polling loop from `StartActionRunner`, replacing it with asynchronous foreground service startup dispatch.

### UI & UX Modernization (Material 3)
* **Material 3 Theme Harmonization:** Replaced legacy AppCompat attribute references with Material 3 design tokens (`MR.attr.colorError`, `MR.attr.colorOnSurface`, `MR.attr.colorOnSurfaceVariant`) across all dialogs and detail screens.
* **Programmatic View Mounting:** Implemented `ensureChildren()` in `CoverView` to guarantee image views and clip shapes are mounted when instantiated programmatically in code.
* **Optimized List Rendering:** Upgraded `BookmarkAdapter` and `LocationAdapter` to use `ListAdapter` and targeted range updates (`DiffUtil`), eliminating full RecyclerView invalidation (`notifyDataSetChanged()`).
* **MaterialAlertDialog Integration:** Converted all audiobook speed, chapter selection, sleep timer, and bookmark creation dialogs to native `MaterialAlertDialogBuilder`.

---

## 4.1.21

### Dual-Domain Experience
* **Independent Playback Domains:** Complete isolation between `PlaybackDomain.MUSIC` and `PlaybackDomain.AUDIOBOOKS`.
* **Dedicated Audiobook Studio:** Added M4B chapter parsing, per-book progress tracking, bookmarks with notes, speed controls, and end-of-chapter sleep timer.
* **Material 3 Redesign:** Introduced adaptive dynamic color theming, AMOLED dark mode, fluid bottom-sheet controls, and updated vector branding.
