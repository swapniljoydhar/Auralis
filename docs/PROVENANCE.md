# Auralis Provenance & Architecture Foundations

**Auralis** is an open-source, local-first audio player application for Android licensed under the GNU General Public License v3.0 (or later).

## Foundational Design

The Auralis project represents a complete, cohesive media player combining full-featured local music library management with a purpose-built audiobook player. 

### Architecture Pillars:
- **Dual Playback Domains**: Absolute separation between `PlaybackDomain.MUSIC` and `PlaybackDomain.AUDIOBOOKS`.
- **Native Indexing Engine (`musikr`)**: High-performance metadata extraction powered by C++ TagLib JNI and custom file system scanners.
- **Material 3 Expressive UI**: Dynamic color harmony with Android 12+ Monet theming, custom bottom sheet physics, and responsive layout controllers.
- **Durable Local Persistence**: Transactional Room databases managing playback session snapshots, audiobook progress down to the millisecond, chapter markers, and user playlists.
- **Zero Cloud / Local-First Privacy**: 100% offline, zero tracking, zero telemetry, and privacy-respecting storage permissions.

## Licensing

Auralis is distributed under the **GPL-3.0-or-later** license. All product naming, identifiers, brand assets, vector icons, and custom domain controllers are property of the Auralis open-source project.
