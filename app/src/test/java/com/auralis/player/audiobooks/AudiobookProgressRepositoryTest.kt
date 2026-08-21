/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookProgressRepositoryTest.kt is part of Auralis.
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

import com.auralis.player.playback.persist.AudiobookProgressDao
import com.auralis.player.playback.persist.AudiobookProgressEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.oxycblt.musikr.Music

class AudiobookProgressRepositoryTest {
    private val chapterUid =
        requireNotNull(Music.UID.fromString("uas00000000-0000-0000-0000-000000000001"))

    @Test
    fun zeroPositionShortChapterIsNotComplete() = runBlocking {
        val dao = RecordingDao()
        AudiobookProgressRepository(dao).save("book", chapterUid, 0L, 5_000L, updatedMs = 1L)

        assertFalse(dao.saved!!.completed)
    }

    @Test
    fun positivePositionWithinCompletionThresholdIsComplete() = runBlocking {
        val dao = RecordingDao()
        AudiobookProgressRepository(dao).save("book", chapterUid, 1L, 5_000L, updatedMs = 1L)

        assertTrue(dao.saved!!.completed)
    }

    private class RecordingDao : AudiobookProgressDao {
        var saved: AudiobookProgressEntity? = null

        override suspend fun getForBook(bookKey: String): List<AudiobookProgressEntity> =
            emptyList()

        override suspend fun getForChapter(chapterUid: String): AudiobookProgressEntity? = null

        override suspend fun upsert(progress: AudiobookProgressEntity) {
            saved = progress
        }

        override suspend fun deleteForBook(bookKey: String) = Unit
    }
}
