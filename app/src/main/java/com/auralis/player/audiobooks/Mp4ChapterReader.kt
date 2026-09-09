/*
 * Copyright (c) 2026 Auralis Contributors
 * Mp4ChapterReader.kt is part of Auralis.
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

import java.io.InputStream

/** Reads the Nero-style `chpl` chapter-list box commonly written into local M4A and M4B files. */
internal object Mp4ChapterReader {
    fun read(input: InputStream): List<EmbeddedChapter> =
        runCatching { readBoxes(input, MAX_SCAN_BYTES) }.getOrDefault(emptyList())

    private fun readBoxes(input: InputStream, bytesAvailable: Long): List<EmbeddedChapter> {
        var remaining = bytesAvailable
        while (remaining >= BOX_HEADER_BYTES) {
            val header = readExact(input, BOX_HEADER_BYTES.toInt()) ?: return emptyList()
            val size = unsignedInt(header, 0)
            val type = header.copyOfRange(4, 8).toString(Charsets.US_ASCII)
            val payloadSize = size - BOX_HEADER_BYTES
            if (size < BOX_HEADER_BYTES || payloadSize > remaining - BOX_HEADER_BYTES) {
                return emptyList()
            }
            remaining -= size

            when (type) {
                "moov",
                "udta" ->
                    readBoxes(input, payloadSize).takeIf(List<EmbeddedChapter>::isNotEmpty)?.let {
                        return it
                    }
                "chpl" -> {
                    if (payloadSize > MAX_CHAPTER_LIST_BYTES) return emptyList()
                    return parseChapterList(
                        readExact(input, payloadSize.toInt()) ?: return emptyList()
                    )
                }
                else -> if (!skipExactly(input, payloadSize)) return emptyList()
            }
        }
        return emptyList()
    }

    private fun parseChapterList(payload: ByteArray): List<EmbeddedChapter> {
        if (payload.size < CHAPTER_LIST_HEADER_BYTES) return emptyList()
        val count = payload[CHAPTER_LIST_HEADER_BYTES - 1].toInt() and 0xFF
        var offset = CHAPTER_LIST_HEADER_BYTES
        val chapters = buildList {
            repeat(count) {
                if (offset + CHAPTER_TIMESTAMP_BYTES + 1 > payload.size) return emptyList()
                val startMs = unsignedLong(payload, offset) / TICKS_PER_MILLISECOND
                offset += CHAPTER_TIMESTAMP_BYTES
                val titleSize = payload[offset].toInt() and 0xFF
                offset += 1
                if (offset + titleSize > payload.size) return emptyList()
                val title = payload.copyOfRange(offset, offset + titleSize).toString(Charsets.UTF_8)
                offset += titleSize
                add(EmbeddedChapter(startMs, title.takeIf(String::isNotBlank) ?: "Chapter"))
            }
        }
        return chapters.distinctBy(EmbeddedChapter::startMs).sortedBy(EmbeddedChapter::startMs)
    }

    private fun readExact(input: InputStream, size: Int): ByteArray? {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(bytes, offset, size - offset)
            if (read < 0) return null
            offset += read
        }
        return bytes
    }

    private fun skipExactly(input: InputStream, size: Long): Boolean {
        var remaining = size
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else if (input.read() < 0) {
                return false
            } else {
                remaining -= 1
            }
        }
        return true
    }

    private fun unsignedInt(bytes: ByteArray, offset: Int) =
        ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)

    private fun unsignedLong(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        repeat(CHAPTER_TIMESTAMP_BYTES) { index ->
            value = (value shl 8) or (bytes[offset + index].toLong() and 0xFF)
        }
        return value
    }

    private const val BOX_HEADER_BYTES = 8L
    private const val CHAPTER_LIST_HEADER_BYTES = 9
    private const val CHAPTER_TIMESTAMP_BYTES = 8
    private const val TICKS_PER_MILLISECOND = 10_000L
    private const val MAX_SCAN_BYTES = 64L * 1024 * 1024
    private const val MAX_CHAPTER_LIST_BYTES = 1L * 1024 * 1024
}
