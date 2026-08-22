/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookListeningStateTest.kt is part of Auralis.
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

import org.junit.Assert.assertEquals
import org.junit.Test

class AudiobookListeningStateTest {
    @Test
    fun noDurableListeningPositionIsNotStarted() {
        val summary =
            AudiobookListeningState.summarize(
                listOf(AudiobookProgressInput(60_000L, 0L, false, 0L))
            )

        assertEquals(AudiobookLifecycle.NOT_STARTED, summary.lifecycle)
        assertEquals(0, summary.percentage)
    }

    @Test
    fun partialBookShowsCurrentPercentageAndRemainingTime() {
        val summary =
            AudiobookListeningState.summarize(
                listOf(
                    AudiobookProgressInput(60_000L, 60_000L, true, 1L),
                    AudiobookProgressInput(60_000L, 30_000L, false, 2L),
                )
            )

        assertEquals(AudiobookLifecycle.CURRENT, summary.lifecycle)
        assertEquals(75, summary.percentage)
        assertEquals(30_000L, summary.remainingMs)
    }

    @Test
    fun allChaptersCompleteShowsFinished() {
        val summary =
            AudiobookListeningState.summarize(
                listOf(
                    AudiobookProgressInput(60_000L, 60_000L, true, 1L),
                    AudiobookProgressInput(90_000L, 90_000L, true, 2L),
                )
            )

        assertEquals(AudiobookLifecycle.FINISHED, summary.lifecycle)
        assertEquals(100, summary.percentage)
    }
}
