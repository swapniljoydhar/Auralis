/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookChapterNavigatorTest.kt is part of Auralis.
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookChapterNavigatorTest {
    @Test
    fun embeddedNavigatorDropsInvalidOffsetsAndOrdersRemainingChapters() {
        val chapters =
            AudiobookChapterNavigator.embedded(
                chapters =
                    listOf(
                        EmbeddedChapter(120_000L, "Third"),
                        EmbeddedChapter(-1L, "Invalid"),
                        EmbeddedChapter(0L, "Opening"),
                        EmbeddedChapter(120_000L, "Duplicate"),
                        EmbeddedChapter(200_000L, "Past end"),
                    ),
                durationMs = 180_000L,
                positionMs = 80_000L,
            )

        assertEquals(listOf("Opening", "Third"), chapters.map(AudiobookNavigationChapter::title))
        assertEquals(120_000L, chapters.first().endMs)
        assertTrue(chapters[0].isCurrent)
        assertFalse(chapters[1].isCurrent)
    }

    @Test
    fun fileNavigatorPreservesResolvedQueueOrderAndCurrentIndex() {
        val chapters =
            AudiobookChapterNavigator.files(
                listOf(
                    AudiobookFileChapter("Part 1", 60_000L),
                    AudiobookFileChapter("Part 2", 90_000L),
                ),
                currentIndex = 1,
            )

        assertEquals(listOf(1, 2), chapters.map(AudiobookNavigationChapter::number))
        assertEquals(60_000L, chapters[1].startMs)
        assertTrue(chapters[1].isCurrent)
    }

    @Test
    fun selectionIsRejectedAfterDomainOrQueueChanges() {
        assertTrue(
            AudiobookChapterNavigator.canSelect(
                PlaybackDomain.AUDIOBOOKS,
                "chapter-1",
                "chapter-1",
                listOf("chapter-1", "chapter-2"),
                listOf("chapter-1", "chapter-2"),
            )
        )
        assertFalse(
            AudiobookChapterNavigator.canSelect(
                PlaybackDomain.MUSIC,
                "chapter-1",
                "chapter-1",
                listOf("chapter-1", "chapter-2"),
                listOf("chapter-1", "chapter-2"),
            )
        )
        assertFalse(
            AudiobookChapterNavigator.canSelect(
                PlaybackDomain.AUDIOBOOKS,
                "chapter-1",
                "chapter-1",
                listOf("chapter-1", "chapter-2"),
                listOf("chapter-1", "chapter-3"),
            )
        )
    }
}
