/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookBookmarkRepository.kt is part of Auralis.
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
    val embeddedChapterStartMs: Long?,
    val note: String,
)

@Singleton
class AudiobookBookmarkRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val lock = Any()

    fun getForBook(bookKey: String): List<AudiobookBookmark> =
        synchronized(lock) { readAllLocked() }
            .filter { it.bookKey == bookKey }
            .sortedBy { it.createdMs }

    fun add(
        bookKey: String,
        chapterUid: Music.UID,
        positionMs: Long,
        embeddedChapterStartMs: Long? = null,
        note: String = "",
    ) {
        val bookmark =
            AudiobookBookmark(
                bookKey = bookKey,
                chapterUid = chapterUid,
                positionMs = positionMs.coerceAtLeast(0L),
                createdMs = System.currentTimeMillis(),
                embeddedChapterStartMs = embeddedChapterStartMs?.coerceAtLeast(0L),
                note = note.trim().take(MAX_NOTE_LENGTH),
            )
        synchronized(lock) {
            val current = readAllLocked().toMutableList()
            val duplicate =
                current.any {
                    it.bookKey == bookmark.bookKey &&
                        it.chapterUid == bookmark.chapterUid &&
                        it.positionMs == bookmark.positionMs
                }
            if (duplicate) return
            current.add(bookmark)
            writeEncodedLocked(dropOldestBeyondCap(current))
        }
    }

    fun remove(bookmark: AudiobookBookmark) {
        synchronized(lock) {
            val remaining =
                readAllLocked().filterNot { it == bookmark }.toSet().map(::encode).toSet()
            preferences.edit { putStringSet(KEY_BOOKMARKS, remaining) }
        }
    }

    private fun readAllLocked(): List<AudiobookBookmark> = decodeAll(readEncodedLocked())

    private fun readEncodedLocked(): Set<String> =
        preferences.getStringSet(KEY_BOOKMARKS, emptySet()).orEmpty().toSet()

    private fun writeEncodedLocked(bookmarks: List<AudiobookBookmark>) {
        preferences.edit { putStringSet(KEY_BOOKMARKS, bookmarks.map(::encode).toSet()) }
    }

    private fun decodeAll(encoded: Set<String>): List<AudiobookBookmark> =
        encoded.mapNotNull(::decode)

    private fun dropOldestBeyondCap(all: List<AudiobookBookmark>): List<AudiobookBookmark> {
        if (all.size <= MAX_BOOKMARKS) return all
        return all.sortedBy { it.createdMs }.takeLast(MAX_BOOKMARKS)
    }

    private fun encode(bookmark: AudiobookBookmark) =
        listOf(
                Base64.encodeToString(bookmark.bookKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
                bookmark.chapterUid.toString(),
                bookmark.positionMs.toString(),
                bookmark.createdMs.toString(),
                bookmark.embeddedChapterStartMs?.toString().orEmpty(),
                Base64.encodeToString(bookmark.note.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            )
            .joinToString(DELIMITER)

    private fun decode(value: String): AudiobookBookmark? {
        val parts = value.split(DELIMITER)
        if (parts.size !in 4..6) return null
        val uid = Music.UID.fromString(parts[1]) ?: return null
        val bookKey = decodeBase64(parts[0]) ?: return null
        return AudiobookBookmark(
            bookKey = bookKey,
            chapterUid = uid,
            positionMs = parts[2].toLongOrNull()?.coerceAtLeast(0L) ?: return null,
            createdMs = parts[3].toLongOrNull() ?: return null,
            embeddedChapterStartMs = parts.getOrNull(4)?.toLongOrNull()?.coerceAtLeast(0L),
            note = decodeNote(parts),
        )
    }

    private fun decodeNote(parts: List<String>): String {
        val encoded = parts.getOrNull(5)?.takeIf(String::isNotEmpty) ?: return ""
        return decodeBase64(encoded).orEmpty()
    }

    private fun decodeBase64(value: String): String? =
        runCatching { String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8) }.getOrNull()

    private companion object {
        const val KEY_BOOKMARKS = "auralis_audiobook_bookmarks"
        const val DELIMITER = "|"
        const val MAX_NOTE_LENGTH = 280
        const val MAX_BOOKMARKS = 500
    }
}
