# Auralis Audiobooks

## Purpose

Auralis keeps **Music** and **Audiobooks** as separate local-library experiences. They share the device media index and one playback service, but Audiobooks is a read-only projection with an explicit `PlaybackDomain.AUDIOBOOKS`; its folder settings, progress, bookmarks, sleep state, and long-form controls do not change Music snapshots or Music playback.

## Local discovery

The local index accepts normal audio containers before Audiobook classification. Auralis recognizes **M4B** directly as an audiobook. **MP3, M4A, OGG, OGA, and OPUS** become audiobook candidates when one of the following applies:

| Signal | Result |
|---|---|
| Path, album, or genre carries an audiobook marker | The local audio file is included in the Audiobooks projection. |
| A listener manually assigns the file | The file is included without changing Music classification. |
| A grouped local album is long-form | The chapter set is included conservatively. |
| No audiobook signal is present | The file remains available only in Music. |

This rule prevents ordinary M4A, OGG/OGA, or OPUS music albums from leaking into the Audiobooks library merely because of their container format.

## Listening flow

The Audiobooks library starts with a local preparation state while indexing is active. When complete, it presents books in **In progress**, **Not started**, and **Finished** groups. Listeners can use compact rows or a cover grid, open a book, resume its next unfinished chapter, inspect progress, navigate chapters, manage local bookmarks, adjust speed and auto-rewind, and use the Audiobooks-only sleep timer.

Every audiobook command is created with `PlaybackDomain.AUDIOBOOKS`. The shared service saves a domain-specific snapshot and refuses mixed-domain queue behavior. Switching to Music therefore preserves the Audiobooks queue and progress, and Audiobooks folder selection creates a copied projection rather than modifying the shared Music snapshot.

## Chapters and long-form context

| Container behavior | Chapter behavior |
|---|---|
| MP3 with ID3v2 `CHAP` frames | Embedded chapters drive navigation, bookmarks, progress context, and end-of-chapter sleep. |
| M4A or M4B with MP4 Nero-style `chpl` metadata | Embedded chapter titles and timestamps are read locally for the same navigation, bookmark, progress, and sleep paths. |
| Multi-file books in any supported audio container | File order provides stable chapter navigation and durable per-file progress. |
| OGG, OGA, or OPUS without a common embedded chapter convention | The file remains playable and discoverable; multi-file chapter ordering remains available. |

The embedded readers are bounded and tolerant of malformed metadata. Invalid chapter data is ignored rather than causing an indexing or playback failure.

## Verification boundary

The focused native regression suite checks that concurrent selected-folder projections remain separate from the source Music snapshot, that M4A, OGG, OGA, and OPUS markers classify conservatively, and that a local MP4 `chpl` list yields stable embedded chapters. The suite validates in-process projection behavior; device-level media-provider behavior and codec availability still require testing on representative Android devices and files.
