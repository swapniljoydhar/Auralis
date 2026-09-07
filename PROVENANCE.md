# Auralis Provenance & Project Origins

**Auralis** is a unified, local-first Android music and audiobook listening platform engineered under the GNU General Public License v3.0 (or later).

## Product Identity & Core Architecture

Auralis is designed from the ground up to unify two distinct listening paradigms into a single, high-performance application:

1. **High-Fidelity Music Experience**: Desktop-grade metadata extraction via TagLib C++ JNI, gapless playback, ReplayGain normalization, dynamic queue management, and Material 3 expressive theming.
2. **Dedicated Audiobook Ecosystem**: Per-book progress persistence, embedded ID3 and M4B/MP4 chapter navigation, bookmarks with custom notes, smart auto-rewind on resume, silence skipping, variable speed/pitch playback, and end-of-chapter sleep timers.

Auralis enforces a strict `PlaybackDomain` separation (`MUSIC` vs `AUDIOBOOKS`), guaranteeing that queues, shuffle algorithms, and resume states remain completely isolated and never pollute each other.

## Legal & Licensing

Auralis is distributed under the terms of the **GNU General Public License v3.0 (GPL-3.0-or-later)**. All original source files, icons, design specifications, and architecture models are copyright Auralis Contributors.
