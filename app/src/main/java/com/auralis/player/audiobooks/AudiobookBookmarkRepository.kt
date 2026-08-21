/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookBookmarkRepository.kt is part of Auralis.
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

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.oxycblt.musikr.Music

/** Offline audiobook bookmarks stored independently from global Music playback state. */
data class AudiobookBookmark(
    val bookKey: String,
    val chapterUid: Music.UID,
    val positionMs: Long,
    val createdMs: Long,
)

@Singleton
class AudiobookBookmarkRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getForBook(bookKey: String): List<AudiobookBookmark> =
        preferences
            .getStringSet(KEY_BOOKMARKS, emptySet())
            .orEmpty()
            .mapNotNull(::decode)
            .filter { it.bookKey == bookKey }
            .sortedBy { it.createdMs }

    fun add(bookKey: String, chapterUid: Music.UID, positionMs: Long) {
        val bookmark =
            AudiobookBookmark(
                bookKey = bookKey,
                chapterUid = chapterUid,
                positionMs = positionMs.coerceAtLeast(0L),
                createdMs = System.currentTimeMillis(),
            )
        val encoded = preferences.getStringSet(KEY_BOOKMARKS, emptySet()).orEmpty().toMutableSet()
        if (
            encoded.any {
                decode(it)?.let { existing ->
                    existing.bookKey == bookmark.bookKey &&
                        existing.chapterUid == bookmark.chapterUid &&
                        existing.positionMs == bookmark.positionMs
                } == true
            }
        ) {
            return
        }
        encoded.add(encode(bookmark))
        preferences.edit { putStringSet(KEY_BOOKMARKS, encoded) }
    }

    fun remove(bookmark: AudiobookBookmark) {
        val encoded = preferences.getStringSet(KEY_BOOKMARKS, emptySet()).orEmpty().toMutableSet()
        encoded.remove(encode(bookmark))
        preferences.edit { putStringSet(KEY_BOOKMARKS, encoded) }
    }

    private fun encode(bookmark: AudiobookBookmark) =
        listOf(
                Base64.encodeToString(bookmark.bookKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
                bookmark.chapterUid.toString(),
                bookmark.positionMs.toString(),
                bookmark.createdMs.toString(),
            )
            .joinToString(DELIMITER)

    private fun decode(value: String): AudiobookBookmark? {
        val parts = value.split(DELIMITER)
        if (parts.size != 4) return null
        val uid = Music.UID.fromString(parts[1]) ?: return null
        val bookKey =
            runCatching { String(Base64.decode(parts[0], Base64.NO_WRAP), Charsets.UTF_8) }
                .getOrNull() ?: return null
        return AudiobookBookmark(
            bookKey = bookKey,
            chapterUid = uid,
            positionMs = parts[2].toLongOrNull()?.coerceAtLeast(0L) ?: return null,
            createdMs = parts[3].toLongOrNull() ?: return null,
        )
    }

    private companion object {
        const val KEY_BOOKMARKS = "auralis_audiobook_bookmarks"
        const val DELIMITER = "|"
    }
}
