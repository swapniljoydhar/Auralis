# Current stabilization audit

Audited: 2026-08-29. Scope: current Auralis Android app only. The debug Kotlin compilation and debug APK packaging pass. No low-risk, release-blocking defect was found in this pass.

## Next-session priorities

### P1 — protect the public cover provider from excessive work

`CoverProvider` is exported so media-browser clients can load artwork. Each request can synchronously wait up to five seconds while loading a cover. It runs on a pipe worker rather than the UI thread, but repeated external requests could still consume worker threads and repeatedly read local artwork.

**Recommended fix:** benchmark the actual external clients first, then add a small bounded in-memory cache and request coalescing. Do not make the provider private without validating lock-screen/media-browser artwork.

**Files:** `app/src/main/java/com/auralis/player/image/CoverProvider.kt`, `app/src/main/AndroidManifest.xml`.

### P1 — replace the Tasker start polling contract

The Tasker action blocks its runner thread for up to about 5.1 seconds while polling a process-global foreground flag. It is bounded, so it is not an immediate ANR issue, but it can make automation feel slow and can report success even if the service has not actually become ready.

**Recommended fix:** replace polling with an explicit service-ready acknowledgement or return immediately after a successful foreground-service start. Validate against a real Tasker profile before changing behavior.

**File:** `app/src/main/java/com/auralis/player/tasker/Start.kt`.

### P2 — replace programmatic legacy dialogs

The fallback audiobook chapter dialog and the bookmarks screen construct large UI trees programmatically. They are functional, but they make Material 3 spacing, theming, large-font behavior, and accessibility harder to maintain.

**Recommended fix:** migrate each screen individually to XML/view binding, starting with the legacy chapter dialog. Keep the existing domain-purity checks and chapter-selection tests intact.

**Files:** `app/src/main/java/com/auralis/player/playback/PlaybackPanelFragment.kt`, `app/src/main/java/com/auralis/player/audiobooks/AudiobookBookmarksFragment.kt`.

### P2 — dependency modernization requires a device regression pass

The project deliberately pins old Fragment and RecyclerView versions while using a preview Material dependency. Updating these can resolve toolchain warnings, but can also alter back navigation, sheet scrolling, and list behavior.

**Recommended fix:** upgrade one dependency family at a time in a dedicated branch; test Music, Audiobooks, back navigation, queue bottom sheet, widgets, and rotation on a physical device after each change.

**File:** `app/build.gradle`.

### P3 — remove broad adapter refreshes where lists are large

Several adapters use `notifyDataSetChanged()`. This is safe but can cause visible refreshes in long local libraries.

**Recommended fix:** use `ListAdapter`/`DiffUtil` for the affected small dynamic lists only after profiling confirms an issue. Avoid a mechanical global replacement.

**Files:** `AudiobookBookmarksFragment.kt`, `AudiobookListFragment.kt`, `LocationAdapter.kt`, `TabAdapter.kt`.

## Known-safe current state

- Music and Audiobooks retain separate playback domains and persisted state.
- Bookmarks, including optional local notes, remain backward compatible with existing stored bookmarks.
- Android backup is disabled to keep local listening history and notes on-device.
- Optional FFmpeg loading now falls back only for expected reflection or linkage failures.
