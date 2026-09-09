# Auralis Provenance & Project Origins

**Auralis** is a standalone, local-first Android music and audiobook listening platform engineered under the GNU General Public License v3.0 (or later).

## Product Identity & Core Architecture

Auralis is designed from the ground up to unify two distinct listening paradigms into a single, high-performance application:

1. **High-Fidelity Music Experience**: Asynchronous metadata extraction via `musikr`, gapless playback, ReplayGain normalization, dynamic queue management, and Material 3 Expressive theming.
2. **Dedicated Audiobook Ecosystem**: Per-book progress persistence, embedded ID3v2 `CHAP` and MP4/`chpl` chapter navigation, bookmarks with custom notes, smart auto-rewind on resume, silence skipping, variable speed/pitch playback, and end-of-chapter sleep timers.

Auralis enforces a strict `PlaybackDomain` separation (`MUSIC` vs `AUDIOBOOKS`), guaranteeing that queues, shuffle algorithms, and resume states remain completely isolated and never pollute each other.

## Legal & Licensing

Auralis is distributed under the terms of the **GNU General Public License v3.0 (GPL-3.0-or-later)**. All original source files, icons, design specifications, and architecture models are copyright Auralis Contributors.

## Architectural Highlights

Auralis features a complete implementation that:

- Unifies music and audiobook playback under a single `PlaybackStateManager` with domain-isolated state snapshots
- Implements a custom `musikr` metadata engine using Android system media APIs (no TagLib/NDK dependency)
- Uses Hilt dependency injection throughout for clean component management
- Features Material 3 Expressive UI specifications with fluid spring physics animations
- Provides a dedicated audiobook progress database separate from music playback state
- Implements shake-to-reset sleep timers using accelerometer sensors
