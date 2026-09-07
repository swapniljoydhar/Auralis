/*
 * Copyright (c) 2026 Auralis Contributors
 * Mp4ChapterReaderTest.kt is part of Auralis.
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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4ChapterReaderTest {
    @Test
    fun readsFfmpegStyleChapterListFromM4aOrM4bContainer() {
        val chapters =
            Mp4ChapterReader.read(
                ByteArrayInputStream(
                    box(
                        "moov",
                        box(
                            "udta",
                            box("chpl", chapterList(0L to "Opening", 1_250L to "Chapter two")),
                        ),
                    )
                )
            )

        assertEquals(
            listOf(EmbeddedChapter(0L, "Opening"), EmbeddedChapter(1_250L, "Chapter two")),
            chapters,
        )
    }

    @Test
    fun malformedMp4ChapterListIsIgnoredWithoutThrowing() {
        val malformed = box("moov", box("udta", box("chpl", byteArrayOf(1, 0, 0, 0))))

        assertTrue(Mp4ChapterReader.read(ByteArrayInputStream(malformed)).isEmpty())
    }

    private fun chapterList(vararg chapters: Pair<Long, String>): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0, chapters.size.toByte()))
        chapters.forEach { (startMs, title) ->
            writeLong(output, startMs * 10_000L)
            val titleBytes = title.toByteArray(Charsets.UTF_8)
            output.write(titleBytes.size)
            output.write(titleBytes)
        }
        return output.toByteArray()
    }

    private fun box(type: String, payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        writeInt(output, payload.size + 8)
        output.write(type.toByteArray(Charsets.US_ASCII))
        output.write(payload)
        return output.toByteArray()
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        output.write(value ushr 24)
        output.write(value ushr 16)
        output.write(value ushr 8)
        output.write(value)
    }

    private fun writeLong(output: ByteArrayOutputStream, value: Long) {
        for (shift in 56 downTo 0 step 8) output.write((value ushr shift).toInt())
    }
}
