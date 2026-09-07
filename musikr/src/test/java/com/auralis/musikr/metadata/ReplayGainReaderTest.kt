/*
 * Copyright (c) 2026 Auralis Contributors
 * ReplayGainReaderTest.kt is part of Auralis.
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

package com.auralis.musikr.metadata

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainReaderTest {
    @Test
    fun id3v23TxxxGainsAreRead() {
        val tag = id3Tag(
            version = 3,
            frames = listOf(
                txxx("REPLAYGAIN_TRACK_GAIN", "-6.44 dB", v24 = false),
                txxx("REPLAYGAIN_ALBUM_GAIN", "-7.10 dB", v24 = false),
            ),
        )
        val gains = ReplayGainReader.read(ByteArrayInputStream(tag))

        assertEquals(listOf("-6.44 dB"), gains.id3v2["TXXX:REPLAYGAIN_TRACK_GAIN"])
        assertEquals(listOf("-7.10 dB"), gains.id3v2["TXXX:REPLAYGAIN_ALBUM_GAIN"])
        assertTrue(gains.xiph.isEmpty())
        assertTrue(gains.mp4.isEmpty())
    }

    @Test
    fun id3v24TxxxGainsAreRead() {
        val tag = id3Tag(
            version = 4,
            frames = listOf(txxx("replaygain_track_gain", "-1.50 dB", v24 = true)),
        )
        val gains = ReplayGainReader.read(ByteArrayInputStream(tag))

        assertEquals(listOf("-1.50 dB"), gains.id3v2["TXXX:REPLAYGAIN_TRACK_GAIN"])
    }

    @Test
    fun id3DescriptionsAreCaseInsensitive() {
        val tag = id3Tag(
            version = 3,
            frames = listOf(txxx("ReplayGain_Album_Gain", "+2.00 dB", v24 = false)),
        )
        val gains = ReplayGainReader.read(ByteArrayInputStream(tag))

        assertEquals(listOf("+2.00 dB"), gains.id3v2["TXXX:REPLAYGAIN_ALBUM_GAIN"])
    }

    @Test
    fun flacVorbisCommentGainsAreRead() {
        val block = vorbisCommentBlock(
            listOf("REPLAYGAIN_TRACK_GAIN=-5.25 dB", "REPLAYGAIN_ALBUM_GAIN=-5.75 dB")
        )
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray())
        // Last metadata block, type 4 (VORBIS_COMMENT), 24-bit big-endian length.
        out.write(0x80 or 4)
        out.write((block.size shr 16) and 0xFF)
        out.write((block.size shr 8) and 0xFF)
        out.write(block.size and 0xFF)
        out.write(block)
        val gains = ReplayGainReader.read(ByteArrayInputStream(out.toByteArray()))

        assertEquals(listOf("-5.25 dB"), gains.xiph["REPLAYGAIN_TRACK_GAIN"])
        assertEquals(listOf("-5.75 dB"), gains.xiph["REPLAYGAIN_ALBUM_GAIN"])
    }

    @Test
    fun opusTagsGainsAndR128AreRead() {
        val comments = vorbisCommentBlock(
            listOf("R128_TRACK_GAIN=-1444", "REPLAYGAIN_ALBUM_GAIN=-6.00 dB")
        )
        val packet = "OpusTags".toByteArray() + comments
        // Minimal single-page OGG stream: identification packet then the tags packet.
        val page = oggPage(listOf(ByteArray(19), packet))
        val gains = ReplayGainReader.read(ByteArrayInputStream(page))

        assertEquals(listOf("-1444"), gains.xiph["R128_TRACK_GAIN"])
        assertEquals(listOf("-6.00 dB"), gains.xiph["REPLAYGAIN_ALBUM_GAIN"])
    }

    @Test
    fun mp4FreeformGainsAreRead() {
        val ilst = box(
            "ilst",
            box("----", meanBox() + nameBox("replaygain_track_gain") + dataBox("-4.20 dB")) +
                box("----", meanBox() + nameBox("replaygain_album_gain") + dataBox("-4.80 dB")),
        )
        // meta is a FullBox: 4-byte version/flags precede its children.
        val meta = box("meta", byteArrayOf(0, 0, 0, 0) + box("hdlr", ByteArray(8)) + ilst)
        val file = box("ftyp", "M4A ".toByteArray() + ByteArray(8)) + box("moov", box("udta", meta))
        val gains = ReplayGainReader.read(ByteArrayInputStream(file))

        assertEquals(
            listOf("-4.20 dB"),
            gains.mp4["----:COM.APPLE.ITUNES:REPLAYGAIN_TRACK_GAIN"],
        )
        assertEquals(
            listOf("-4.80 dB"),
            gains.mp4["----:COM.APPLE.ITUNES:REPLAYGAIN_ALBUM_GAIN"],
        )
    }

    @Test
    fun garbageInputYieldsNoGains() {
        val gains = ReplayGainReader.read(ByteArrayInputStream(ByteArray(256) { it.toByte() }))
        assertTrue(gains.id3v2.isEmpty())
        assertTrue(gains.xiph.isEmpty())
        assertTrue(gains.mp4.isEmpty())
    }

    @Test
    fun truncatedInputYieldsNoGains() {
        val gains = ReplayGainReader.read(ByteArrayInputStream("ID3".toByteArray()))
        assertTrue(gains.id3v2.isEmpty())
        assertTrue(gains.xiph.isEmpty())
        assertTrue(gains.mp4.isEmpty())
    }

    @Test
    fun truncatedId3PayloadYieldsNoGains() {
        // Declares a 64-byte tag but only carries the header plus two stray bytes,
        // so the sniff passes and the payload read is what fails.
        val header =
            "ID3".toByteArray() + byteArrayOf(4, 0, 0, 0, 0, 0, 64) + byteArrayOf(0, 0)
        val gains = ReplayGainReader.read(ByteArrayInputStream(header))
        assertTrue(gains.id3v2.isEmpty())
    }

    // --- Synthetic container builders ---

    private fun txxx(description: String, value: String, v24: Boolean): ByteArray {
        val body = byteArrayOf(0) +
            description.toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0) +
            value.toByteArray(Charsets.ISO_8859_1)
        val size = body.size
        val sizeBytes = if (v24) synchsafe(size) else byteArrayOf(
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte(),
        )
        return "TXXX".toByteArray() + sizeBytes + byteArrayOf(0, 0) + body
    }

    private fun id3Tag(version: Int, frames: List<ByteArray>): ByteArray {
        val payload = frames.reduce { a, b -> a + b }
        return "ID3".toByteArray() + byteArrayOf(version.toByte(), 0) + byteArrayOf(0) +
            synchsafe(payload.size) + payload
    }

    private fun synchsafe(value: Int) = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

    private fun vorbisCommentBlock(entries: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        fun writeString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val size = bytes.size
            out.write(size and 0xFF)
            out.write((size shr 8) and 0xFF)
            out.write((size shr 16) and 0xFF)
            out.write((size shr 24) and 0xFF)
            out.write(bytes)
        }
        writeString("Auralis test")
        val count = entries.size
        out.write(count and 0xFF)
        out.write((count shr 8) and 0xFF)
        out.write((count shr 16) and 0xFF)
        out.write((count shr 24) and 0xFF)
        entries.forEach(::writeString)
        return out.toByteArray()
    }

    private fun oggPage(packets: List<ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        // Capture pattern + version + header type + granule position + serial + sequence + CRC.
        out.write("OggS".toByteArray())
        out.write(ByteArray(22))
        val segments = packets.flatMap { packet ->
            var remaining = packet.size
            buildList {
                while (remaining >= 255) {
                    add(255)
                    remaining -= 255
                }
                add(remaining)
            }
        }
        out.write(segments.size)
        segments.forEach(out::write)
        packets.forEach(out::write)
        return out.toByteArray()
    }

    private fun box(type: String, payload: ByteArray): ByteArray {
        val size = payload.size + 8
        return byteArrayOf(
            ((size shr 24) and 0xFF).toByte(),
            ((size shr 16) and 0xFF).toByte(),
            ((size shr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte(),
        ) + type.toByteArray(Charsets.US_ASCII) + payload
    }

    private fun meanBox(): ByteArray =
        box("mean", byteArrayOf(0, 0, 0, 0) + "com.apple.iTunes".toByteArray())

    private fun nameBox(name: String): ByteArray =
        box("name", byteArrayOf(0, 0, 0, 0) + name.toByteArray())

    private fun dataBox(value: String): ByteArray =
        box(
            "data",
            // FullBox header + type(1 = UTF-8) + locale + text.
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0) + value.toByteArray(),
        )
}
