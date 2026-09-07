# Auralis Changelog

## 4.1.22 (Latest)

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
