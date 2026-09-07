/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookListeningState.kt is part of Auralis.
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
 
package com.auralis.player.audiobooks

enum class AudiobookLifecycle {
    NOT_STARTED,
    CURRENT,
    FINISHED,
}

data class AudiobookProgressInput(
    val durationMs: Long,
    val positionMs: Long,
    val completed: Boolean,
    val updatedMs: Long,
)

data class AudiobookListeningSummary(
    val lifecycle: AudiobookLifecycle,
    val completedChapters: Int,
    val totalChapters: Int,
    val listenedMs: Long,
    val totalDurationMs: Long,
    val latestUpdatedMs: Long,
) {
    val percentage: Int
        get() =
            if (totalDurationMs <= 0L) 0
            else ((listenedMs * 100L) / totalDurationMs).toInt().coerceIn(0, 100)

    val remainingMs: Long
        get() = (totalDurationMs - listenedMs).coerceAtLeast(0L)
}

object AudiobookListeningState {
    fun summarize(chapters: Collection<AudiobookProgressInput>): AudiobookListeningSummary {
        val values = chapters.toList()
        val totalDurationMs = values.sumOf { it.durationMs.coerceAtLeast(0L) }
        val completedChapters = values.count { it.completed }
        val listenedMs =
            values.sumOf { chapter ->
                if (chapter.completed) chapter.durationMs.coerceAtLeast(0L)
                else chapter.positionMs.coerceIn(0L, chapter.durationMs.coerceAtLeast(0L))
            }
        val lifecycle =
            when {
                values.isNotEmpty() && completedChapters == values.size ->
                    AudiobookLifecycle.FINISHED
                listenedMs > 0L -> AudiobookLifecycle.CURRENT
                else -> AudiobookLifecycle.NOT_STARTED
            }
        return AudiobookListeningSummary(
            lifecycle,
            completedChapters,
            values.size,
            listenedMs,
            totalDurationMs,
            values.maxOfOrNull(AudiobookProgressInput::updatedMs) ?: 0L,
        )
    }
}
