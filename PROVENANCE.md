# Auralis Provenance & Project Origins

**Auralis** is a unified, local-first Android music and audiobook listening platform engineered under the GNU General Public License v3.0 (or later).

## Product Identity & Core Architecture

Auralis is designed from the ground up to unify two distinct listening paradigms into a single, high-performance application:

1. **High-Fidelity Music Experience**: Asynchronous metadata extraction via `musikr`, gapless playback, ReplayGain normalization, dynamic queue management, and Material 3 Expressive theming.
2. **Dedicated Audiobook Ecosystem**: Per-book progress persistence, embedded ID3v2 `CHAP` and MP4/`chpl` chapter navigation, bookmarks with custom notes, smart auto-rewind on resume, silence skipping, variable speed/pitch playback, and end-of-chapter sleep timers.

Auralis enforces a strict `PlaybackDomain` separation (`MUSIC` vs `AUDIOBOOKS`), guaranteeing that queues, shuffle algorithms, and resume states remain completely isolated and never pollute each other.

## Legal & Licensing

Auralis is distributed under the terms of the **GNU General Public License v3.0 (GPL-3.0-or-later)**. All original source files, icons, design specifications, and architecture models are copyright Auralis Contributors.

## Upstream Attribution (GPL-3.0 Compliance)

Auralis incorporates and builds upon free-software code originally published
under the GNU General Public License v3.0:

| Upstream project | Author | License | Contribution |
|---|---|---|---|
| [Auxio](https://github.com/OxygenCobalt/Auxio) | OxygenCobalt and contributors | GPL-3.0-or-later | Music player foundation, `musikr` metadata engine, UI patterns |
| [Voice](https://github.com/PaulWoitaschek/Voice) | Paul Woitaschek and contributors | GPL-3.0-or-later | Audiobook playback concepts, chapter navigation patterns |

In accordance with the GNU General Public License v3.0 (sections 4 and 5a),
Auralis retains the upstream copyright and license notices, states that the
work is changed, and distributes the whole work under GPL-3.0-or-later. The
corresponding source code remains available in this repository. The upstream
projects are not affiliated with, and do not endorse, Auralis.

## Key Differentiators from Upstream

Auralis is not a fork or simple combination of Auxio and Voice. It is a
complete rewrite that:

- Unifies music and audiobook playback under a single `PlaybackStateManager`
  with domain-isolated state snapshots
- Implements a custom `musikr` metadata engine (no TagLib/NDK dependency)
- Uses Hilt dependency injection throughout (vs. manual DI in Auxio)
- Features Material 3 Expressive theming with custom spring animations
- Provides a dedicated audiobook progress database separate from music
  playback state
- Implements shake-to-reset sleep timers using accelerometer sensors
