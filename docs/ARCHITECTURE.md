# Auralis architecture

## Product shell

Auralis starts in a mode hub and opens either the Music or Audiobooks projection. `HomeViewModel` owns the selected projection, while navigation and toolbar actions preserve the mode boundary. The shell may share artwork, playback, persistence, and settings infrastructure, but each projection owns its own tabs, sort semantics, empty states, and detail screens.

## Music projection

Music discovery and metadata are provided by `musikr` and the app’s music repository. Songs, albums, artists, genres, playlists, search, queues, and Android media-browser integration are music-domain concerns. Music sorting and playlist actions must never be invoked for an audiobook projection.

## Audiobooks projection

Audiobooks are represented by a catalog, repository, progress repository, bookmark repository, and playback controller. A book is a local grouping of chapters. Progress is keyed by stable book/chapter identifiers, and settings such as speed, skip interval, silence skipping, auto-rewind, and sleep-at-chapter-end are kept separate from Music preferences.

## Playback boundary

The playback service is shared so Android notifications, media buttons, widgets, and Android Auto have one system-facing entry point. The service translates domain items into media-session items, validates queue indices, and persists state after a coherent snapshot rather than while a mutable queue is being written. External actions are treated as untrusted input and are ignored or converted to safe no-op results when they do not map to a valid current mode.

## Storage boundary

Auralis reads local audio through MediaStore and Storage Access Framework locations. Artwork is exposed through a constrained content provider whose URI matcher accepts only the application’s cover path. Unknown paths return null or an empty cursor rather than throwing from an exported component.

## Verification boundary

The minimum release gate is a successful debug build, passing `app` and `musikr` unit tests, passing debug lint, and passing `spotlessCheck`. A release APK must be signed with a documented key fingerprint; private key material and machine-local SDK configuration never enter version control.
