<h1 align="center">Auralis</h1>
<h4 align="center">A private, local-first player for music and audiobooks on Android.</h4>
<p align="center">
    <img alt="Minimum SDK Version" src="https://img.shields.io/badge/API-24%2B-1450A8?style=flat">
    <img alt="License" src="https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat">
</p>

## Product direction

Auralis brings two listening experiences into one focused application without flattening their differences. The opening hub lets listeners choose **Music** or **Audiobooks**, and each mode exposes the navigation, sorting, playback controls, and persistence model that fit that medium. Shared playback infrastructure keeps the app small and reliable; mode-specific state prevents music and audiobook assumptions from leaking into one another.

## Music

The Music space indexes local audio with embedded artwork and metadata. It provides songs, albums, artists, genres, playlists, search, queue management, shuffle, repeat, persistent playback, gapless playback, ReplayGain, external equalizer integration, Android Auto, headset controls, widgets, and an edge-to-edge Material interface.

## Audiobooks

The Audiobooks space supports M4B books and MP3 chapter folders. It groups chapters into books, shows chapter durations and completion state, resumes from the last position, and provides chapter navigation, bookmarks, configurable skip intervals, playback speed, auto-rewind, silence skipping, and sleep-at-chapter-end behavior. Assigning MP3 chapters to Audiobooks changes only the library projection; it does not move or delete the underlying files, and removing the assignment returns them to Music.

## Privacy and storage

Auralis is designed for local playback. It does not require an account or cloud synchronization. Android media permissions are used to discover and play local audio, while notification and foreground-service permissions support background playback. Library metadata, playback state, bookmarks, and settings remain on the device.

## Build

The project uses Gradle, Android SDK/NDK tooling, a patched Media3 playback stack, and the native metadata parser in `musikr`. Initialize the repository’s nested submodules before building:

```bash
git submodule update --init --recursive
./gradlew --no-daemon --max-workers=1 :app:assembleDebug
./gradlew --no-daemon --max-workers=1 :app:testDebugUnitTest :musikr:test
./gradlew --no-daemon --max-workers=1 :app:lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. A release build must be signed with a release key before distribution. Never commit a private keystore, signing password, API token, or local `local.properties` file.

## Repository structure

| Module | Responsibility |
|---|---|
| `app` | Android UI, navigation, mode separation, playback service, settings, persistence, widgets, and audiobook behavior. |
| `musikr` | Kotlin and native metadata indexing, MediaStore/SAF access, tag parsing, and music-domain models. |
| `media` | Vendored Media3 components used for playback, extraction, and optional FFmpeg decoding. |
| `design` | Auralis visual assets and brand references. |

## Engineering principles

Auralis favors local-first behavior, explicit state transitions, safe handling of external intents and content URIs, bounded background work, and user-visible loading, empty, and error states. Music and Audiobooks are separate projections over shared storage rather than one mixed library. Unsupported operations should remain no-ops or return a safe result instead of crashing the process.

## License and provenance

Auralis is distributed under the **GNU General Public License, version 3 or later**. This repository incorporates and modifies GPL-licensed open-source components. Copyright notices, license headers, `NOTICE` files, and third-party notices remain in the source tree where required; product rebranding does not remove legal attribution.

See [`LICENSE`](LICENSE), [`NOTICE`](NOTICE), and the relevant module notices for complete licensing information. The `media` and `taglib` submodules retain their own upstream license files and pinned revisions.
