<h1 align="center">Auralis</h1>
<h4 align="center">The modern, private, local-first player for Music and Audiobooks on Android.</h4>
<p align="center">
    <img alt="Minimum SDK Version" src="https://img.shields.io/badge/API-24%2B-1450A8?style=flat">
    <img alt="License" src="https://img.shields.io/badge/license-GPL%20v3-2B6DBE.svg?style=flat">
</p>

## Product Vision

**Auralis** unites high-fidelity music playback and dedicated audiobook listening into a single, seamless, and beautifully crafted Android application. By enforcing an explicit **Dual-Domain Architecture (`PlaybackDomain.MUSIC` and `PlaybackDomain.AUDIOBOOKS`)**, Auralis provides tailored interfaces, controls, and persistence models for both mediums without allowing their states or queues to collide.

---

## 🎵 Music Experience

* **High-Fidelity Native Metadata Engine (`musikr`):** Powered by TagLib C++ JNI for fast, accurate ID3v2, Vorbis, MP4, and FLAC tag parsing.
* **Smart Library Organization:** Browse by Songs, Albums, Artists, Genres, and Playlists with fast alphabetic scrolling and customizable tabs.
* **Audiophile Playback:** True gapless audio playback, ReplayGain track/album gain normalization, and external equalizer integration.
* **Flexible Queue Management:** Dynamic queue reordering, drag-and-drop, non-repeating shuffle algorithms, and multi-mode repeat.
* **Material 3 Expressive UI:** Full Android 12+ Monet dynamic color palette theming, AMOLED black themes, edge-to-edge window inset support, and fluid collapsible bottom-sheet player panels.
* **Ecosystem Integration:** Android Auto support, lock screen controls, customizable home screen widgets, and Bluetooth media button handling.

---

## 📚 Dedicated Audiobook Studio

* **Comprehensive Book Cataloging:** Automatic detection and grouping of M4B files and multi-track MP3/M4A/OGG/OPUS chapter folders into unified books.
* **Durable Millisecond Progress Persistence:** Per-book resume state stored in transactional Room databases, persisting exact positions across app restarts.
* **Embedded & File-Based Chapters:** Seamless navigation across MP4/M4B chapters and embedded ID3 chapter markers.
* **Smart Auto-Rewind on Resume:** Automatically rewinds playback upon resuming after pause so you never lose your context.
* **Granular Playback Speed & Silence Trimming:** Fine-grained speed control (0.5x to 3.0x) with pitch correction and automatic silence skipping.
* **Bookmarks & Notes:** Save timestamped bookmarks with custom annotations and instant one-tap jump-to-location.
* **Intelligent Sleep Timers:** Countdown timers with dedicated "End of Chapter" automatic stop functionality.

---

## 🔒 Privacy & Local-First Freedom

* **100% Offline & Private:** Zero analytics, zero crash-reporting trackers, zero network requirements, and zero advertising.
* **Scoped Storage Friendly:** Safe Storage Access Framework (SAF) integration and MediaStore indexing that never moves or modifies your files without explicit permission.

---

## 🛠 Repository Structure

| Module | Purpose |
|---|---|
| `app` | Main Android application layer: UI, Navigation, Material 3 theming, Audiobooks engine, Playback service, Persistence, and Widgets. |
| `musikr` | Native indexing pipeline, TagLib JNI metadata extractor, in-memory music graph, and filesystem scanner. |
| `media` | Vendored Media3 ExoPlayer components for playback and codec decoding. |
| `design` | Original vector marks, design guidelines, and Material 3 specifications. |

---

## 🚀 Building & Testing

```bash
# Clone the repository with submodules
git submodule update --init --recursive

# Build debug APK
./gradlew --no-daemon :app:assembleDebug

# Run unit tests
./gradlew --no-daemon :app:testDebugUnitTest :musikr:test

# Run lint verification
./gradlew --no-daemon :app:lintDebug
```

---

## 📜 License

Auralis is distributed under the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**. See [`LICENSE`](LICENSE) for complete license details.
