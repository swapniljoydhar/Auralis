/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookSleepTimerPolicyTest.kt is part of Auralis.
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
 
package com.auralis.player.audiobooks

import com.auralis.player.playback.state.PlaybackDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookSleepTimerPolicyTest {
    @Test
    fun timerDoesNotPauseMusicAfterAudiobookSwitch() {
        assertFalse(
            AudiobookSleepTimerPolicy.shouldPause(PlaybackDomain.AUDIOBOOKS, PlaybackDomain.MUSIC)
        )
    }

    @Test
    fun audiobookTimerPausesItsOwnDomain() {
        assertTrue(
            AudiobookSleepTimerPolicy.shouldPause(
                PlaybackDomain.AUDIOBOOKS,
                PlaybackDomain.AUDIOBOOKS,
            )
        )
    }

    @Test
    fun nextEmbeddedBoundaryUsesTheNearestFutureChapterRegardlessOfSourceOrder() {
        val chapters =
            listOf(
                EmbeddedChapter(120_000L, "Chapter 3"),
                EmbeddedChapter(0L, "Opening"),
                EmbeddedChapter(60_000L, "Chapter 2"),
            )

        assertEquals(60_000L, AudiobookSleepTimerPolicy.nextEmbeddedChapterStart(chapters, 1L))
        assertEquals(
            120_000L,
            AudiobookSleepTimerPolicy.nextEmbeddedChapterStart(chapters, 60_000L),
        )
    }

    @Test
    fun finalEmbeddedChapterHasNoFutureBoundary() {
        assertNull(
            AudiobookSleepTimerPolicy.nextEmbeddedChapterStart(
                listOf(EmbeddedChapter(0L, "Opening"), EmbeddedChapter(60_000L, "Final")),
                60_000L,
            )
        )
    }
}
