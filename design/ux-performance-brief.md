# Auralis music and audiobook UX pass

## Confirmed problems

The installed build exposes the audiobook experience as a far-right home tab, classifies MP3 files only when their path or metadata contains an audiobook marker, buries audiobook controls in the playback overflow menu, still presents the debug-era app label, and runs substantial bottom-sheet UI work from `ViewTreeObserver.OnPreDraw` on every frame.

## Product direction

Keep the existing Auralis music library and playback contracts. Present the app as **Auralis** rather than “Auralis Debug”. Use a book-plus-wave launcher mark. On the first home visit, show a focused mode chooser with two large choices: Music and Audiobooks. Remember the choice, provide a visible mode switch on the home toolbar, and ensure Audiobooks is visible even if the prior tab-customization setting hid it.

Audiobooks need an explicit user-controlled import/classification path for MP3 files. Automatic detection remains conservative, while manually assigned audiobook UIDs are stored locally and merged into the audiobook catalog. The audiobook list should expose an Add audiobooks action and a clear empty state.

The audiobook detail/player surface should display cover, title, author, progress, resume/restart, chapter rows, skip-back/skip-forward, playback speed, and sleep timer without requiring the three-dot overflow for the primary audiobook controls.

## Performance acceptance checks

1. Remove the unconditional `OnPreDraw` listener from the main shell and replace only the slide/state-specific updates that are required for sheet visuals and back handling.
2. Avoid setting visibility, alpha, shape, or drag flags when their values have not changed.
3. Keep the existing playback and queue sheet behavior intact.
4. Run Spotless, app and musikr JVM tests, aggregate `check`, debug lint, and debug APK assembly.

## UX acceptance checks

1. The application label is no longer “Auralis Debug”.
2. A new launcher icon visibly combines an open book and an audio waveform.
3. The first-open chooser can enter Music or Audiobooks and the selected mode can be changed without opening settings.
4. Audiobooks can be manually assigned from the Music song flow and remain assigned after a restart.
5. Primary audiobook controls are visible in the player/detail surface, with the overflow retained only for secondary actions.
