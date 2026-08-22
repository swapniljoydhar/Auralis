/*
 * Copyright (c) 2026 Auralis Contributors
 * EmbeddedChapterReader.kt is part of Auralis.
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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.Song

data class EmbeddedChapter(val startMs: Long, val title: String)

/** Reads ID3v2 `CHAP` frames embedded in one local MP3 file. */
class EmbeddedChapterReader @Inject constructor(@ApplicationContext private val context: Context) {
    private val cache = ConcurrentHashMap<String, List<EmbeddedChapter>>()

    suspend fun read(song: Song): List<EmbeddedChapter> {
        val key = song.uid.toString()
        cache[key]?.let {
            return it
        }
        return withContext(Dispatchers.IO) {
            val chapters =
                runCatching {
                        context.contentResolver
                            .openInputStream(song.uri)
                            ?.use(::readChapters)
                            .orEmpty()
                    }
                    .getOrDefault(emptyList())
            cache.putIfAbsent(key, chapters) ?: chapters
        }
    }

    private fun readChapters(input: java.io.InputStream): List<EmbeddedChapter> {
        val header = readExact(input, 10) ?: return emptyList()
        if (!header.copyOfRange(0, 3).contentEquals("ID3".toByteArray())) {
            return emptyList()
        }
        val version = header[3].toInt() and 0xFF
        if (version !in 3..4) return emptyList()
        val payloadSize = synchsafeInt(header, 6)
        if (payloadSize <= 0 || payloadSize > MAX_TAG_BYTES) return emptyList()
        val payload = readExact(input, payloadSize) ?: return emptyList()

        var offset =
            if ((header[5].toInt() and 0x40) != 0) extendedHeaderSize(payload, version) else 0
        val chapters = mutableListOf<EmbeddedChapter>()
        while (offset + FRAME_HEADER_BYTES <= payload.size) {
            val id = payload.copyOfRange(offset, offset + 4).toString(Charsets.ISO_8859_1)
            if (id.any { it == '\u0000' }) break
            val size =
                if (version == 4) synchsafeInt(payload, offset + 4)
                else bigEndianInt(payload, offset + 4)
            offset += FRAME_HEADER_BYTES
            if (size <= 0 || offset + size > payload.size) break
            if (id == "CHAP") {
                parseChapter(payload.copyOfRange(offset, offset + size), version)
                    ?.let(chapters::add)
            }
            offset += size
        }
        return chapters.distinctBy { it.startMs }.sortedBy { it.startMs }
    }

    private fun readExact(input: java.io.InputStream, size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(bytes, offset, size - offset)
            if (read < 0) return null
            offset += read
        }
        return bytes
    }

    private fun parseChapter(frame: ByteArray, version: Int): EmbeddedChapter? {
        val elementEnd = frame.indexOf(0)
        if (elementEnd < 0 || elementEnd + CHAPTER_HEADER_BYTES > frame.size) return null
        val startMs = bigEndianInt(frame, elementEnd + 1).toLong() and 0xFFFFFFFFL
        var offset = elementEnd + CHAPTER_HEADER_BYTES
        var title: String? = null
        while (offset + FRAME_HEADER_BYTES <= frame.size) {
            val id = frame.copyOfRange(offset, offset + 4).toString(Charsets.ISO_8859_1)
            if (id.any { it == '\u0000' }) break
            val size =
                if (version == 4) synchsafeInt(frame, offset + 4)
                else bigEndianInt(frame, offset + 4)
            offset += FRAME_HEADER_BYTES
            if (size <= 0 || offset + size > frame.size) break
            if (id == "TIT2") title = decodeText(frame.copyOfRange(offset, offset + size))
            offset += size
        }
        return EmbeddedChapter(startMs, title?.takeIf(String::isNotBlank) ?: "Chapter")
    }

    private fun decodeText(frame: ByteArray): String? {
        if (frame.isEmpty()) return null
        val charset =
            when (frame[0].toInt()) {
                0 -> Charsets.ISO_8859_1
                1 -> Charsets.UTF_16
                2 -> Charsets.UTF_16BE
                3 -> Charsets.UTF_8
                else -> return null
            }
        return frame.copyOfRange(1, frame.size).toString(charset).trimEnd('\u0000').trim()
    }

    private fun extendedHeaderSize(payload: ByteArray, version: Int): Int {
        if (payload.size < 4) return 0
        val size = if (version == 4) synchsafeInt(payload, 0) else bigEndianInt(payload, 0)
        return (size + if (version == 3) 4 else 0).coerceIn(0, payload.size)
    }

    private fun synchsafeInt(bytes: ByteArray, offset: Int) =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun bigEndianInt(bytes: ByteArray, offset: Int) =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private companion object {
        const val MAX_TAG_BYTES = 16 * 1024 * 1024
        const val FRAME_HEADER_BYTES = 10
        const val CHAPTER_HEADER_BYTES = 17
    }
}
