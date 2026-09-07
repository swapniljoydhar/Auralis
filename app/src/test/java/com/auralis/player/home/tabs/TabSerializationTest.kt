/*
 * Copyright (c) 2026 Auralis Contributors
 * TabSerializationTest.kt is part of Auralis.
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
 
package com.auralis.player.home.tabs

import com.auralis.player.music.MusicType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TabSerializationTest {
    @Test
    fun defaultSequenceIncludesAudiobooksAfterExistingMusicTabs() {
        val tabs = Tab.fromIntCode(Tab.SEQUENCE_DEFAULT)
        assertNotNull(tabs)
        assertEquals(
            listOf(
                MusicType.SONGS,
                MusicType.ALBUMS,
                MusicType.ARTISTS,
                MusicType.GENRES,
                MusicType.PLAYLISTS,
                MusicType.AUDIOBOOKS,
            ),
            tabs!!.map { it.type },
        )
    }

    @Test
    fun originalFiveTabSequenceStillDecodes() {
        val original = 0b1000_1001_1010_1011_1100
        val tabs = Tab.fromIntCode(original)
        assertNotNull(tabs)
        assertEquals(5, tabs!!.size)
        assertEquals(MusicType.PLAYLISTS, tabs.last().type)
    }
}
