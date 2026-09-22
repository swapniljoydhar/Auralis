/*
 * Copyright (c) 2026 Auralis Contributors
 * PlaybackDomain.kt is part of Auralis.
 *
 * Auralis is a free-software audio player for music and audiobooks, distributed
 * under the GNU General Public License v3.0 or later. It incorporates prior
 * free-software work; the attribution required by that license is retained in
 * PROVENANCE.md at the root of this repository.
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

import com.auralis.player.audiobooks.AudiobookCatalog
import com.auralis.player.audiobooks.AudiobookClassifier
import org.oxycblt.musikr.Song

/** The explicit local-library domain that owns a playback command and its queue. */
enum class PlaybackDomain {
    MUSIC,
    AUDIOBOOKS;

    /**
     * A queue member reduced to the signals that decide domain ownership: whether metadata marks it
     * as a book chapter, whether the listener assigned it manually, and its duration (used by the
     * group-level long-form rule).
     */
    data class QueueEntry(
        val isAudiobookMarked: Boolean,
        val isManualAudiobook: Boolean,
        val durationMs: Long,
    )

    /**
     * Validates an entire queue instead of single songs. A Music queue keeps rejecting
     * metadata-marked book chapters; an Audiobooks queue must carry a book signal (marked chapter
     * or manual assignment) or satisfy the long-form duration rule `AudiobookCatalog` already uses.
     */
    fun acceptsQueue(entries: List<QueueEntry>): Boolean {
        if (entries.isEmpty()) return false
        if (this == MUSIC) return entries.none(QueueEntry::isAudiobookMarked)
        return entries.any { it.isAudiobookMarked || it.isManualAudiobook } ||
            AudiobookCatalog.isLongFormBook(entries.map(QueueEntry::durationMs))
    }
}

/** Reduces a local song to the signals [PlaybackDomain.acceptsQueue] evaluates. */
fun Song.toQueueEntry(manualSongUids: Set<String> = emptySet()) =
    PlaybackDomain.QueueEntry(
        isAudiobookMarked = AudiobookClassifier.isAudiobook(this),
        isManualAudiobook = uid.toString() in manualSongUids,
        durationMs = durationMs,
    )
