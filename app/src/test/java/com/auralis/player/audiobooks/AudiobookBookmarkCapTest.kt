/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookBookmarkCapTest.kt is part of Auralis.
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

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.oxycblt.musikr.Music
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AudiobookBookmarkCapTest {
    @Test
    fun bookmarksAreCappedAndOldestEvicted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = AudiobookBookmarkRepository(context)
        val uid = requireNotNull(Music.UID.fromString("uas00000000-0000-0000-0000-000000000001"))
        repeat(510) { index -> repo.add("book", uid, index.toLong(), null, "n$index") }
        val rows = repo.getForBook("book")
        assertTrue(rows.size <= 500)
        assertTrue(rows.sortedBy { it.createdMs } == rows)
    }
}
