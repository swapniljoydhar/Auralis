/*
 * Copyright (c) 2026 Auralis Project
 * AudiobookProgressRepository.kt is part of Auralis.
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
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Music

/** A domain-level progress record independent from the global Music playback snapshot. */
data class AudiobookProgress(
    val bookKey: String,
    val chapterUid: Music.UID,
    val positionMs: Long,
    val completed: Boolean,
    val updatedMs: Long,
)

/**
 * Durable audiobook progress storage.
 *
 * Music playback continues to use PersistenceRepository. This repository never clears or replaces
 * Music playback state, which allows a listener to switch between Music and Audiobooks safely.
 */
class AudiobookProgressRepository @Inject constructor(private val dao: AudiobookProgressDao) {
    suspend fun getForBook(bookKey: String): List<AudiobookProgress> =
        withContext(Dispatchers.IO) { dao.getForBook(bookKey).mapNotNull(::toDomain) }

    suspend fun getForChapter(chapterUid: Music.UID): AudiobookProgress? =
        withContext(Dispatchers.IO) { dao.getForChapter(chapterUid.toString())?.let(::toDomain) }

    suspend fun save(
        bookKey: String,
        chapterUid: Music.UID,
        positionMs: Long,
        durationMs: Long,
        updatedMs: Long = System.currentTimeMillis(),
    ) =
        withContext(Dispatchers.IO) {
            val safeDuration = durationMs.coerceAtLeast(0L)
            val safePosition =
                positionMs.coerceIn(0L, safeDuration.takeIf { it > 0L } ?: Long.MAX_VALUE)
            val completed =
                safePosition > 0L &&
                    safeDuration > 0L &&
                    safePosition >= (safeDuration - COMPLETION_THRESHOLD_MS).coerceAtLeast(0L)
            dao.upsert(
                AudiobookProgressEntity(
                    chapterUid = chapterUid.toString(),
                    bookKey = bookKey,
                    positionMs = safePosition,
                    completed = completed,
                    updatedMs = updatedMs,
                )
            )
        }

    suspend fun clearBook(bookKey: String) =
        withContext(Dispatchers.IO) { dao.deleteForBook(bookKey) }

    private fun toDomain(entity: AudiobookProgressEntity): AudiobookProgress? {
        val uid = Music.UID.fromString(entity.chapterUid) ?: return null
        return AudiobookProgress(
            bookKey = entity.bookKey,
            chapterUid = uid,
            positionMs = entity.positionMs,
            completed = entity.completed,
            updatedMs = entity.updatedMs,
        )
    }

    private companion object {
        const val COMPLETION_THRESHOLD_MS = 10_000L
    }
}
