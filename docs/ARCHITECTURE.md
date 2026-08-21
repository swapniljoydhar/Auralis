# Auralis architecture

## Product shell

Auralis starts in a mode hub and opens either the Music or Audiobooks projection. `HomeViewModel` owns the selected projection, while navigation and toolbar actions preserve the mode boundary. The shell may share artwork, playback, persistence, and settings infrastructure, but each projection owns its own tabs, sort semantics, empty states, and detail screens.

## Music projection

Music discovery and metadata are provided by `musikr` and the app’s music repository. Songs, albums, artists, genres, playlists, search, queues, and Android media-browser integration are music-domain concerns. Music sorting and playlist actions must never be invoked for an audiobook projection.

## Audiobooks projection

Audiobooks are represented by a catalog, repository, progress repository, bookmark repository, and playback controller. A book is a local grouping of chapters. Progress is keyed by stable book/chapter identifiers, and settings such as speed, skip interval, silence skipping, auto-rewind, and sleep-at-chapter-end are kept separate from Music preferences.

## Playback boundary

The playback service owns one ExoPlayer, one media session, one foreground notification, and one simplified cross-mode widget. `PlaybackStateManager` is the explicit `PlaybackDomain` coordinator: it switches only between `MUSIC` and `AUDIOBOOKS`, snapshots the outgoing state, restores the incoming state, and rejects a command or queue operation that contains items outside the active domain. The service restart path reads the snapshot for the coordinator’s current domain; persistence does not silently default to Music.

`DomainPlaybackState`, `DomainQueueHeapItem`, and `DomainQueueMappingItem` store separate, transactionally written session snapshots for Music and Audiobooks. A session end is destructive by design: the player stops and clears its media items, the state mirror clears its queue and progression, and UI and widget projections receive an explicit reset. This makes a finished session observably empty instead of leaving stale media metadata or controls behind.

The local media-browser tree begins at `auralis:root` and exposes `auralis:music` and `auralis:audiobooks` as separate browsable roots. Each root only exposes its own local descendants. There is no Android Auto feature in Auralis’s supported product scope.

## Storage boundary

Auralis is local-only: it reads device audio through MediaStore and Storage Access Framework locations and never turns a media URI into a network playback source. Artwork is exposed through a constrained content provider whose URI matcher accepts only the application’s cover path. Unknown paths return null or an empty cursor rather than throwing from an exported component.

## Verification boundary

The minimum release gate is a successful debug build, passing `app` and `musikr` unit tests, passing debug lint, and passing `spotlessCheck`. A release APK must be signed with a documented key fingerprint; private key material and machine-local SDK configuration never enter version control.
