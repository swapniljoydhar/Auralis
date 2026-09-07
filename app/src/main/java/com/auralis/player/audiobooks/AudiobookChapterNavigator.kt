/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookChapterNavigator.kt is part of Auralis.
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

import com.auralis.player.playback.state.PlaybackDomain

sealed interface AudiobookChapterSource {
    data class Embedded(val startMs: Long) : AudiobookChapterSource

    data class File(val queueIndex: Int) : AudiobookChapterSource
}

data class AudiobookNavigationChapter(
    val number: Int,
    val title: String,
    val startMs: Long,
    val endMs: Long?,
    val source: AudiobookChapterSource,
    val isCurrent: Boolean,
)

data class AudiobookFileChapter(val title: String, val durationMs: Long)

/** Converts local chapter sources into stable, validated, display-ready navigation rows. */
object AudiobookChapterNavigator {
    fun embedded(
        chapters: Collection<EmbeddedChapter>,
        durationMs: Long,
        positionMs: Long,
    ): List<AudiobookNavigationChapter> {
        val ordered =
            chapters
                .asSequence()
                .filter { it.startMs >= 0L && (durationMs <= 0L || it.startMs < durationMs) }
                .distinctBy(EmbeddedChapter::startMs)
                .sortedBy(EmbeddedChapter::startMs)
                .toList()
        val activeIndex = ordered.indexOfLast { it.startMs <= positionMs }
        return ordered.mapIndexed { index, chapter ->
            AudiobookNavigationChapter(
                number = index + 1,
                title = chapter.title.ifBlank { "Chapter ${index + 1}" },
                startMs = chapter.startMs,
                endMs = ordered.getOrNull(index + 1)?.startMs ?: durationMs.takeIf { it > 0L },
                source = AudiobookChapterSource.Embedded(chapter.startMs),
                isCurrent = index == activeIndex,
            )
        }
    }

    fun files(
        chapters: List<AudiobookFileChapter>,
        currentIndex: Int,
    ): List<AudiobookNavigationChapter> {
        var startMs = 0L
        return chapters.mapIndexed { index, chapter ->
            val durationMs = chapter.durationMs.coerceAtLeast(0L)
            AudiobookNavigationChapter(
                number = index + 1,
                title = chapter.title.ifBlank { "Chapter ${index + 1}" },
                startMs = startMs.also { startMs += durationMs },
                endMs = startMs,
                source = AudiobookChapterSource.File(index),
                isCurrent = index == currentIndex,
            )
        }
    }

    fun canSelect(
        activeDomain: PlaybackDomain,
        expectedSongUid: String,
        activeSongUid: String?,
        expectedQueueUids: List<String>,
        activeQueueUids: List<String>,
    ) =
        activeDomain == PlaybackDomain.AUDIOBOOKS &&
            activeSongUid == expectedSongUid &&
            activeQueueUids == expectedQueueUids
}
