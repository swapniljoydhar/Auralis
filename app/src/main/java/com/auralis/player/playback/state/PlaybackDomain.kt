/*
 * Copyright (c) 2026 Auralis Contributors
 * PlaybackDomain.kt is part of Auralis.
 *
 * Auralis is a derivative work of the Auxio Project and incorporates
 * audiobook-oriented work inspired by Voice. Original copyright and GPL
 * attribution are retained in PROVENANCE.md and the repository history.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.auralis.player.playback.state

import com.auralis.player.audiobooks.AudiobookClassifier
import org.oxycblt.musikr.Song

/** The explicit local-library domain that owns a playback command and its queue. */
enum class PlaybackDomain {
    MUSIC,
    AUDIOBOOKS;

    fun accepts(song: Song): Boolean =
        when (this) {
            MUSIC -> !AudiobookClassifier.isAudiobook(song)
            AUDIOBOOKS -> AudiobookClassifier.isAudiobook(song)
        }
}
