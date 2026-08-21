<h1 align="center"><b>Auralis</b></h1>
<h4 align="center">A private, local-first player for music and audiobooks on Android.</h4>
<p align="center">
    <img alt="Minimum SDK Version" src="https://img.shields.io/badge/API-24%2B-1450A8?style=flat">
    <img alt="License" src="https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat">
</p>

## About

Auralis is an offline Android player with two deliberately separated listening spaces. **Music** is organized around songs, albums, artists, genres, playlists, search, queues, and fast everyday playback. **Audiobooks** is organized around books, chapters, resume positions, progress, bookmarks, speed, skip intervals, silence skipping, auto-rewind, and sleep-at-chapter-end behavior.

The application reads local media, does not require an account, and keeps library and playback state on the device. The opening hub asks whether the listener wants Music or Audiobooks; each choice opens a focused library projection rather than exposing both experiences in one mixed tab strip. The toolbar still provides a quick way to switch modes.

Auralis supports M4B audiobook playback and can classify ordinary MP3 files as audiobook chapters. When MP3 chapters are assigned to Audiobooks, they disappear from the Music projection without deleting or moving the files and reappear as one audiobook grouped by folder. Removing the assignment returns them to Music.

## Feature areas

### Music

Auralis provides local Music indexing, embedded artwork, songs, albums, artists, genres, playlists, search, queue management, shuffle and repeat, playback persistence, gapless playback, ReplayGain, external equalizer integration, Android Auto, headset controls, widgets, and edge-to-edge Material UI.

### Audiobooks

Audiobooks provide M4B and MP3-chapter playback, cover-led detail pages, chapter durations, completion state, resume-first actions, chapter navigation, visible skip/speed/sleep controls, configurable skip duration, default playback speed, auto-rewind, silence skipping, bookmarks, and stopping at the end of the current chapter.

### Privacy and storage

Auralis is designed for local playback. It does not require cloud synchronization or an account. Android media permissions are used only to discover and play local audio, while notification and foreground-service permissions support background playback.

## Build

The project uses Gradle, Android SDK/NDK tooling, a patched Media3 playback stack, and the native metadata parser in `musikr`.

```bash
./gradlew --no-daemon --max-workers=1 spotlessApply
./gradlew --no-daemon --max-workers=1 check
./gradlew --no-daemon --max-workers=1 :app:assembleDebug
```

For a local device or emulator with USB debugging enabled:

```bash
./gradlew installDebug
```

The debug variant is intended for testing. A release build must be signed with a release key before distribution. Do not commit a private signing keystore or passwords to the repository.

## Repository structure

| Module | Responsibility |
|---|---|
| `app` | Android UI, navigation, Music and Audiobooks projections, playback service, settings, and persistence. |
| `musikr` | Native and Kotlin metadata indexing, MediaStore/SAF access, and music-domain models. |
| `media` | Patched Media3 components used for playback and M4B/FFmpeg support. |

## License and attribution

Auralis is distributed under the **GNU General Public License, version 3 or later**. It incorporates and modifies GPL-licensed open-source components. Copyright notices, license headers, and third-party notices remain in the source tree where required; rebranding the application does not remove those legal attributions.

See [`LICENSE`](LICENSE) and the third-party notices in the relevant modules for the complete licensing information.
