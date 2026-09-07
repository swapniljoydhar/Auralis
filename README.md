# Auralis

**The modern, private, local-first player for Music and Audiobooks on Android.**

[![API](https://img.shields.io/badge/API-24%2B-1450A8?style=flat)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat)](LICENSE)

---

## 🌟 Vision & Architecture

**Auralis** is an audio player engineered to provide an uncompromising experience for both **music enthusiasts** and **audiobook listeners**. Rather than forcing audiobooks into music queues or treating music as long-form speech, Auralis uses an explicit **Dual-Domain Architecture (`PlaybackDomain.MUSIC` and `PlaybackDomain.AUDIOBOOKS`)**. 

Each domain maintains its own distinct:
* **Playback State & Queue**: Listening to an audiobook never clears or interrupts your current music queue, and playing an album never loses your audiobook position.
* **Specialized UI & Controls**: Music features album artwork, repeat, and ReplayGain preamp options; Audiobooks feature chapter navigation, sleep timers, bookmarks, variable speed, and auto-rewind.
* **State Persistence**: Audiobook progress is continuously persisted down to the exact millisecond in a transactional Room database.

---

## 🎵 Music Experience

* **High-Performance Metadata Engine (`musikr`)**: Robust, asynchronous scanning pipeline combining Android media APIs with a pure-Kotlin tag reader (ID3v2, Vorbis comments, MP4 freeform) — no NDK, no native dependencies.
* **Audiophile Playback**: True gapless playback, ReplayGain track/album normalization (including Opus R128) recovered straight from your files, custom pre-amp controls, and system equalizer integration.
* **Comprehensive Library Organization**: Fast browsing by Songs, Albums, Artists, Genres, and Playlists with fast alphabetic index scrolling and customizable navigation tabs.
* **Dynamic Queue Management**: Drag-and-drop queue reordering, non-repeating shuffle algorithms, and flexible repeat modes.
* **Material 3 Expressive UI**: Dynamic color theming (Monet on Android 12+), AMOLED pure black theme, edge-to-edge window insets, and fluid collapsible bottom-sheet player panel.
* **System Integration**: Android Auto support, lock screen playback controls, customizable home screen widgets, and Bluetooth media button handling.

---

## 📚 Dedicated Audiobook Studio

* **Unified Book Cataloging**: Automatically groups M4B audiobooks and multi-track MP3/M4A/OGG/OPUS folder hierarchies into coherent, organized books.
* **Durable Millisecond Progress Persistence**: Transactional Room database persistence guarantees that resume positions are never lost across app restarts or device reboots.
* **Embedded & File-Based Chapters**: Seamless navigation across MP4/M4B embedded chapters and file-based chapters, with direct jump-to-chapter support.
* **Intelligent Sleep Timers with Shake-to-Reset**:
  * Set countdown timers (15, 30, 45, 60, 90 minutes) or auto-stop at the "End of Chapter" (scaled dynamically to playback speed).
  * **Gentle fade-out**: the volume eases down over the final seconds instead of cutting off mid-word (toggleable in settings).
  * Timers pause with playback and resume where they left off — they never silently die.
  * **Shake to Extend**: Gently shake your device in bed while the sleep timer is running to reset or extend it, confirmed with a subtle haptic vibration pulse—no need to look at your screen.
* **Smart Auto-Rewind on Resume**: Automatically rewinds playback by a configurable interval (off, 2s, 5s, 10s) upon resuming after a pause so you never lose context.
* **Bookmarks & Study Notes**: Save timestamped bookmarks with custom notes and jump directly to bookmarked moments from the player or the dedicated bookmarks browser.
* **Granular Playback Speed & Silence Trimming**: Fine-grained speed control (0.5× to 3.0×) with pitch correction and automatic silence skipping. Each book remembers its own pace.

---

## 🔒 Privacy & Local-First Freedom

* **100% Offline & Private**: Zero analytics, zero telemetry, zero crash trackers, zero network requests, and zero advertisements.
* **Scoped Storage Friendly**: Standard Storage Access Framework (SAF) and MediaStore indexing that never alters, moves, or uploads your files.

---

## 🛠 Project Structure

```
Auralis/
├── app/                  # Main Android application module
│   ├── src/main/java/com/auralis/player/
│   │   ├── audiobooks/   # Audiobook catalog, bookmarks, sleep timer, shake detector
│   │   ├── detail/       # Detail views for albums, artists, genres, playlists
│   │   ├── home/         # Home screen with customizable tabs and library browsing
│   │   ├── playback/     # Playback service, ExoPlayer management, ReplayGain
│   │   ├── search/       # Library search with fast filter chips
│   │   └── settings/     # Preference screens and settings management
│   └── src/main/res/     # Layouts, vector drawables, Material 3 styles
├── musikr/               # High-speed media extraction and indexing library
│   └── src/main/java/com/auralis/musikr/
│       ├── metadata/     # MediaMetadataRetriever metadata parsing
│       ├── model/        # Graph models for songs, albums, artists, genres
│       └── pipeline/     # Asynchronous file extraction and evaluation pipeline
└── design/               # Material 3 specifications and design tokens
```

---

## 🚀 Building & Testing

```bash
# Build debug APK
./gradlew app:assembleDebug

# Run unit tests
./gradlew musikr:testDebug app:testDebug

# Check formatting
./gradlew spotlessCheck
```

---

## 📜 License

Auralis is distributed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**. See [`LICENSE`](LICENSE) and [`PROVENANCE.md`](PROVENANCE.md) for full licensing information.
