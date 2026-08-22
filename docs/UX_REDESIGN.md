# Auralis Native UX Redesign

## Product structure

Auralis has two intentional local-library domains. **Music** is optimized for
quick discovery and expressive track control. **Audiobooks** is optimized for
long-form continuity, chapters, bookmarks, speed, and sleep. The shared player
service remains invisible to the user; the active domain changes the controls
and metadata hierarchy rather than mixing both experiences in one screen.

| Surface | Primary content | Primary action | Excluded controls |
| --- | --- | --- | --- |
| Music library | Songs, albums, artists, playlists | Start a track or collection | Chapter, bookmark, and resume controls |
| Music now playing | Artwork, title, artist, progress, transport | Play/pause | Audiobook-specific chapter treatment |
| Audiobook library | Clearly separated books with author and progress | Resume or open a book | Music collection affordances |
| Audiobook detail | Cover, title, author, progress, chapters | Resume from saved position | Ambiguous secondary actions |
| Audiobook now playing | Book title, chapter title, progress, chapter navigation | Resume listening | Repeat and shuffle emphasis |

## Visual direction

The native interface uses calm, local-first visual language: an ink background
(`@color/auralis_ink`), elevated slate surfaces, pale typography, and a muted
aurora-blue action color. Artwork and book covers use one large rounded card;
controls use a single filled primary play control plus quiet icon actions.

> The visual objective is clarity at one-handed phone size: one clear primary
> action, readable metadata, and no unrelated controls competing with listening.

## Playback hierarchy

Music now playing prioritizes artwork, track title, artist/album, elapsed and
remaining time, then previous/play-next transport. Repeat and shuffle remain
available but visually secondary.

Audiobook now playing prioritizes book title, chapter title, elapsed and
remaining time, then rewind, play/pause, fast-forward, and Chapters. Playback
speed and sleep remain in the existing toolbar menu so they are available
without crowding the listening surface.

## Accessibility and interaction checks

All action icons retain content descriptions and at least the existing Material
touch-target overlays. Empty or malformed local metadata resolves to a neutral
placeholder rather than producing a crash or invisible action. Domain changes
must preserve the independent Music and Audiobooks session snapshots.
