/*
 * Copyright (c) 2024 Auralis Contributors
 * BetterShuffleOrderTest.kt is part of Auralis.
 *
 * Auralis is a free-software audio player for music and audiobooks, distributed
 * under the GNU General Public License v3.0 or later.
 */

package com.auralis.player.playback.service

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BetterShuffleOrderTest {

    @Test
    fun createShuffledList_lengthAndStartIndex() {
        val shuffleOrder = BetterShuffleOrder(10, 3)
        assertEquals(10, shuffleOrder.length)
        assertEquals(3, shuffleOrder.firstIndex)
    }

    @Test
    fun getNextAndPreviousIndex_traversesAllElements() {
        val shuffleOrder = BetterShuffleOrder(5, 0)
        var current = shuffleOrder.firstIndex
        val visited = mutableListOf<Int>()
        while (current != C.INDEX_UNSET) {
            visited.add(current)
            current = shuffleOrder.getNextIndex(current)
        }
        assertEquals(5, visited.size)
        assertEquals(5, visited.toSet().size)

        // Traverse backward
        var prev = shuffleOrder.lastIndex
        val visitedPrev = mutableListOf<Int>()
        while (prev != C.INDEX_UNSET) {
            visitedPrev.add(prev)
            prev = shuffleOrder.getPreviousIndex(prev)
        }
        assertEquals(visited.reversed(), visitedPrev)
    }

    @Test
    fun cloneAndInsert_preservesCountAndContiguity() {
        val initial = BetterShuffleOrder(intArrayOf(2, 0, 1))
        val inserted = initial.cloneAndInsert(1, 2)

        // Original size 3 + 2 inserted = 5
        assertEquals(5, inserted.length)

        // Ensure all indices from 0 to 4 exist in the sequence
        var current = inserted.firstIndex
        val indices = mutableSetOf<Int>()
        while (current != C.INDEX_UNSET) {
            indices.add(current)
            current = inserted.getNextIndex(current)
        }
        assertEquals(5, indices.size)
    }

    @Test
    fun cloneAndRemove_removesSpecifiedRange() {
        val initial = BetterShuffleOrder(intArrayOf(0, 1, 2, 3, 4))
        // Remove range [1, 3) -> indices 1 and 2
        val removed = initial.cloneAndRemove(1, 3)

        assertEquals(3, removed.length)
        var current = removed.firstIndex
        val visited = mutableListOf<Int>()
        while (current != C.INDEX_UNSET) {
            visited.add(current)
            current = removed.getNextIndex(current)
        }
        assertEquals(3, visited.size)
        // Check that remaining elements are within 0..2
        assertTrue(visited.all { it in 0..2 })
    }

    @Test
    fun cloneAndClear_returnsEmptyShuffleOrder() {
        val initial = BetterShuffleOrder(intArrayOf(0, 1, 2))
        val cleared = initial.cloneAndClear()
        assertEquals(0, cleared.length)
        assertEquals(C.INDEX_UNSET, cleared.firstIndex)
        assertEquals(C.INDEX_UNSET, cleared.lastIndex)
    }
}
