# Auralis changelog

## Unreleased

### Product identity

Auralis now presents a single, coherent product identity across the Android namespace, application ID, manifest, themes, launcher resources, splash screen, GitHub-facing documentation, and release metadata. The launcher mark is the Auralis sound-ribbon and open-book symbol stored under `design/` and wired into the adaptive icon and splash screen.

### Listening spaces

The home hub keeps Music and Audiobooks deliberately separate. Music retains song, album, artist, genre, playlist, search, queue, shuffle, repeat, ReplayGain, Android Auto, widget, and headset workflows. Audiobooks retain book, chapter, resume, progress, bookmark, speed, skip, silence-skipping, auto-rewind, and sleep-at-chapter-end workflows. Music-only sorting is blocked safely while an audiobook projection is active.

### Stability and correctness

The normal sort-mode RecyclerView binding path no longer throws `NotImplementedError`, the default settings migration is a safe no-op, unsupported sort operations preserve the current order instead of crashing, and the exported cover provider returns safe results for unknown URIs and metadata queries. Release resource definitions no longer depend on debug-only identity overlays.

### Verification

The debug APK builds with the pinned Media3, taglib, utfcpp, FFmpeg, Android SDK, NDK, and CMake inputs initialized. The app unit tests, `musikr` tests, and debug lint task are part of the release gate. Warnings that remain are recorded by lint and are not silently treated as proof of defect-free runtime behavior.

## Provenance

Auralis incorporates and modifies GPL-licensed components. This changelog describes the Auralis product and its changes; legal attribution remains in source headers, `NOTICE`, `LICENSE`, and the vendored submodules. The project does not claim to be an official release of any upstream component.
