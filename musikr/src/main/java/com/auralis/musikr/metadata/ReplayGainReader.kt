/*
 * Copyright (c) 2026 Auralis Contributors
 * ReplayGainReader.kt is part of Auralis.
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

import java.io.InputStream

/**
 * ReplayGain adjustments recovered from a container's native tag area, grouped the way [Metadata]
 * expects them so the tag parser can pick them up without container-specific knowledge.
 */
internal data class ReplayGainTags(
    val id3v2: Map<String, List<String>> = emptyMap(),
    val xiph: Map<String, List<String>> = emptyMap(),
    val mp4: Map<String, List<String>> = emptyMap(),
)

/**
 * Reads ReplayGain (and Opus R128) gain values from local audio files without native code.
 *
 * Android's [android.media.MediaMetadataRetriever] does not expose ReplayGain, so the values are
 * parsed directly from the container tag areas: ID3v2 `TXXX` frames, FLAC/OGG/Opus Vorbis comments,
 * and MP4/iTunes freeform atoms. All reads are bounded and every malformed input degrades to "no
 * gain found" rather than an error.
 */
internal object ReplayGainReader {
    fun read(input: InputStream): ReplayGainTags =
        runCatching {
                val buffered = input.buffered()
                buffered.mark(SNIFF_BYTES)
                val sniff = ByteArray(SNIFF_BYTES)
                if (readFully(buffered, sniff) < SNIFF_BYTES) return ReplayGainTags()
                buffered.reset()
                when {
                    sniff.startsWith("ID3") -> readId3(buffered)
                    sniff.startsWith("fLaC") -> readFlac(buffered)
                    sniff.startsWith("OggS") -> readOgg(buffered)
                    sniff.copyOfRange(4, 8).contentEquals("ftyp".toByteArray()) -> readMp4(buffered)
                    else -> ReplayGainTags()
                }
            }
            .getOrDefault(ReplayGainTags())

    // --- ID3v2 (MP3, and occasionally other containers with a leading tag) ---

    private fun readId3(input: InputStream): ReplayGainTags {
        val header = ByteArray(ID3_HEADER_BYTES)
        if (readFully(input, header) < ID3_HEADER_BYTES || !header.startsWith("ID3")) {
            return ReplayGainTags()
        }
        val version = header[3].toInt() and 0xFF
        if (version !in 3..4) return ReplayGainTags()
        val flags = header[5].toInt()
        var payloadSize = synchsafe(header, 6)
        if (payloadSize <= 0 || payloadSize > MAX_ID3_BYTES) return ReplayGainTags()
        var payload = ByteArray(payloadSize)
        if (readFully(input, payload) < payloadSize) return ReplayGainTags()
        if ((flags and ID3_FLAG_UNSYNC) != 0) {
            payload = deUnsynchronize(payload)
            payloadSize = payload.size
        }
        var offset = 0
        if ((flags and ID3_FLAG_EXTENDED) != 0) {
            if (payloadSize < 4) return ReplayGainTags()
            val extSize = if (version == 4) synchsafe(payload, 0) else bigEndian(payload, 0) + 4
            offset = extSize.coerceIn(0, payloadSize)
        }
        var track: String? = null
        var album: String? = null
        while (offset + ID3_FRAME_HEADER_BYTES <= payloadSize) {
            val id = payload.copyOfRange(offset, offset + 4).toString(Charsets.ISO_8859_1)
            if (id.any { it == '\u0000' }) break
            val size =
                if (version == 4) synchsafe(payload, offset + 4) else bigEndian(payload, offset + 4)
            offset += ID3_FRAME_HEADER_BYTES
            if (size <= 0 || size > MAX_ID3_FRAME_BYTES || offset + size > payloadSize) break
            if (id == "TXXX") {
                val (description, value) = parseTxxx(payload.copyOfRange(offset, offset + size))
                when (description?.uppercase()) {
                    "REPLAYGAIN_TRACK_GAIN" -> if (track == null) track = value
                    "REPLAYGAIN_ALBUM_GAIN" -> if (album == null) album = value
                }
                if (track != null && album != null) break
            }
            offset += size
        }
        return ReplayGainTags(
            id3v2 =
                buildMap {
                    track?.let { put("TXXX:REPLAYGAIN_TRACK_GAIN", listOf(it)) }
                    album?.let { put("TXXX:REPLAYGAIN_ALBUM_GAIN", listOf(it)) }
                }
        )
    }

    private fun parseTxxx(frame: ByteArray): Pair<String?, String?> {
        if (frame.size < 2) return null to null
        val encoding = frame[0].toInt()
        val charset =
            when (encoding) {
                0 -> Charsets.ISO_8859_1
                1 -> Charsets.UTF_16
                2 -> Charsets.UTF_16BE
                3 -> Charsets.UTF_8
                else -> return null to null
            }
        val wide = encoding == 1 || encoding == 2
        var end = 1
        if (wide) {
            while (end + 1 < frame.size && !isWideTerminator(frame, end)) {
                end += 2
            }
            if (end + 1 >= frame.size) return null to null
        } else {
            while (end < frame.size && frame[end] != 0.toByte()) end++
            if (end >= frame.size) return null to null
        }
        val terminatorBytes = if (wide) 2 else 1
        val description =
            frame
                .copyOfRange(1, end)
                .toString(charset)
                .trimEnd('\u0000')
                .trim()
                .takeIf(String::isNotEmpty)
        val valueStart = end + terminatorBytes
        if (valueStart >= frame.size) return description to null
        val value =
            frame
                .copyOfRange(valueStart, frame.size)
                .toString(charset)
                .trimEnd('\u0000')
                .trim()
                .takeIf(String::isNotEmpty)
        return description to value
    }

    private fun isWideTerminator(frame: ByteArray, offset: Int) =
        frame[offset] == 0.toByte() && frame[offset + 1] == 0.toByte()

    private fun deUnsynchronize(payload: ByteArray): ByteArray {
        val out = ByteArray(payload.size)
        var w = 0
        var r = 0
        while (r < payload.size) {
            val byte = payload[r++]
            out[w++] = byte
            if (byte == 0xFF.toByte() && r < payload.size && payload[r] == 0.toByte()) r++
        }
        return out.copyOf(w)
    }

    // --- FLAC ---

    private fun readFlac(input: InputStream): ReplayGainTags {
        val marker = ByteArray(4)
        if (readFully(input, marker) < 4 || !marker.contentEquals("fLaC".toByteArray())) {
            return ReplayGainTags()
        }
        var scanned = 4
        while (scanned < MAX_FLAC_SCAN_BYTES) {
            val header = ByteArray(4)
            if (readFully(input, header) < 4) return ReplayGainTags()
            scanned += 4
            val last = (header[0].toInt() and 0x80) != 0
            val type = header[0].toInt() and 0x7F
            val length =
                ((header[1].toInt() and 0xFF) shl 16) or
                    ((header[2].toInt() and 0xFF) shl 8) or
                    (header[3].toInt() and 0xFF)
            val overBudget = scanned + length > MAX_FLAC_SCAN_BYTES
            if (length < 0 || length > MAX_VORBIS_COMMENT_BYTES || overBudget) {
                return ReplayGainTags()
            }
            if (type == FLAC_TYPE_VORBIS_COMMENT) {
                val block = ByteArray(length)
                if (readFully(input, block) < length) return ReplayGainTags()
                return parseVorbisComment(block)
            }
            if (!skipFully(input, length.toLong())) return ReplayGainTags()
            scanned += length
            if (last) break
        }
        return ReplayGainTags()
    }

    // --- OGG (Vorbis / Opus) ---

    private fun readOgg(input: InputStream): ReplayGainTags {
        // Vorbis comments live in the second packet of the first logical stream. Reassemble
        // packets from the page structure, bounded so hostile files cannot force large reads.
        val packets = mutableListOf<ByteArray>()
        val current = java.io.ByteArrayOutputStream()
        var pages = 0
        val pageHeader = ByteArray(OGG_PAGE_HEADER_BYTES)
        while (pages < MAX_OGG_PAGES && packets.size < 2) {
            if (readFully(input, pageHeader) < OGG_PAGE_HEADER_BYTES) break
            if (!pageHeader.copyOfRange(0, 4).contentEquals("OggS".toByteArray())) break
            pages++
            val segmentCount = pageHeader[26].toInt() and 0xFF
            val segments = ByteArray(segmentCount)
            if (readFully(input, segments) < segmentCount) break
            for (segment in segments) {
                val size = segment.toInt() and 0xFF
                if (current.size() + size > MAX_OGG_PACKET_BYTES) return ReplayGainTags()
                val data = ByteArray(size)
                if (readFully(input, data) < size) return ReplayGainTags()
                current.write(data)
                if (size < OGG_MAX_SEGMENT_SIZE) {
                    packets += current.toByteArray()
                    current.reset()
                    if (packets.size >= 2) break
                }
            }
        }
        val comments = packets.getOrNull(1) ?: return ReplayGainTags()
        return when {
            comments.startsWith(byteArrayOf(0x03) + "vorbis".toByteArray()) ->
                parseVorbisComment(comments.copyOfRange(7, comments.size))
            comments.startsWith("OpusTags") ->
                parseVorbisComment(comments.copyOfRange(8, comments.size))
            else -> ReplayGainTags()
        }
    }

    // --- MP4 / M4A / M4B freeform atoms ---

    private fun readMp4(input: InputStream): ReplayGainTags {
        val ilst =
            findBox(input, MAX_MP4_SCAN_BYTES, listOf("moov", "udta", "meta"), "ilst")
                ?: return ReplayGainTags()
        var track: String? = null
        var album: String? = null
        var offset = 0
        while (offset + 8 <= ilst.size && (track == null || album == null)) {
            val size = unsignedInt(ilst, offset).toInt()
            val type = ilst.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
            if (size < 8 || offset + size > ilst.size) break
            if (type == "----" && size <= MAX_MP4_FREEFORM_BYTES) {
                val (name, value) = parseFreeform(ilst.copyOfRange(offset + 8, offset + size))
                when (name?.uppercase()) {
                    "REPLAYGAIN_TRACK_GAIN" -> if (track == null) track = value
                    "REPLAYGAIN_ALBUM_GAIN" -> if (album == null) album = value
                }
            }
            offset += size
        }
        return ReplayGainTags(
            mp4 =
                buildMap {
                    track?.let { put("----:COM.APPLE.ITUNES:REPLAYGAIN_TRACK_GAIN", listOf(it)) }
                    album?.let { put("----:COM.APPLE.ITUNES:REPLAYGAIN_ALBUM_GAIN", listOf(it)) }
                }
        )
    }

    /** Walk [path] of nested container boxes, then return the payload of [target]. */
    private fun findBox(
        input: InputStream,
        bytesAvailable: Long,
        path: List<String>,
        target: String,
    ): ByteArray? {
        var remaining = bytesAvailable
        while (remaining >= 8) {
            val header = ByteArray(8)
            if (readFully(input, header) < 8) return null
            var size = unsignedInt(header, 0)
            val type = header.copyOfRange(4, 8).toString(Charsets.US_ASCII)
            var headerBytes = 8L
            if (size == 1L) {
                val large = ByteArray(8)
                if (readFully(input, large) < 8) return null
                headerBytes += 8
                size = unsignedLong(large, 0)
            } else if (size == 0L) {
                size = remaining
            }
            val payloadSize = size - headerBytes
            if (size < headerBytes || payloadSize > remaining - headerBytes) return null
            remaining -= size
            val want = path.firstOrNull()
            when {
                type == target && path.isEmpty() -> {
                    if (payloadSize > MAX_MP4_ILST_BYTES) return null
                    val payload = ByteArray(payloadSize.toInt())
                    if (readFully(input, payload) < payload.size) return null
                    return payload
                }
                type == want -> {
                    // `meta` is a FullBox: skip its 4-byte version/flags before descending.
                    val childAvailable =
                        if (type == "meta") {
                            if (payloadSize < 4) return null
                            if (!skipFully(input, 4)) return null
                            payloadSize - 4
                        } else {
                            payloadSize
                        }
                    // The target must be nested inside this box; a bounded recursive scan
                    // consumes exactly the box payload.
                    val found = findBox(input, childAvailable, path.drop(1), target)
                    if (found != null) return found
                }
                else -> if (!skipFully(input, payloadSize)) return null
            }
        }
        return null
    }

    private fun parseFreeform(payload: ByteArray): Pair<String?, String?> {
        var name: String? = null
        var value: String? = null
        var offset = 0
        while (offset + 8 <= payload.size) {
            val size = unsignedInt(payload, offset).toInt()
            val type = payload.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
            if (size < 8 || offset + size > payload.size) break
            val body = payload.copyOfRange(offset + 8, offset + size)
            when (type) {
                // mean/name are FullBoxes with a 4-byte header; only iTunes freeform is read.
                "mean" -> {
                    val mean = body.dropFullBoxHeader().toString(Charsets.UTF_8).trim()
                    if (mean != "com.apple.iTunes") return null to null
                }
                "name" ->
                    name =
                        body
                            .dropFullBoxHeader()
                            .toString(Charsets.UTF_8)
                            .trim()
                            .takeIf(String::isNotEmpty)
                "data" -> {
                    // data: 4-byte FullBox header + 4-byte type + 4-byte locale + UTF-8 text.
                    if (body.size > 12) {
                        value =
                            body
                                .copyOfRange(12, body.size)
                                .toString(Charsets.UTF_8)
                                .trimEnd('\u0000')
                                .trim()
                                .takeIf(String::isNotEmpty)
                    }
                }
            }
            offset += size
        }
        return name to value
    }

    private fun ByteArray.dropFullBoxHeader(): ByteArray =
        if (size >= 4) copyOfRange(4, size) else ByteArray(0)

    // --- Vorbis comments (FLAC native blocks, OGG packets) ---

    private fun parseVorbisComment(block: ByteArray): ReplayGainTags {
        var offset = 0
        fun readString(): String? {
            if (offset + 4 > block.size) return null
            val length =
                (block[offset].toInt() and 0xFF) or
                    ((block[offset + 1].toInt() and 0xFF) shl 8) or
                    ((block[offset + 2].toInt() and 0xFF) shl 16) or
                    ((block[offset + 3].toInt() and 0xFF) shl 24)
            offset += 4
            if (length < 0 || length > MAX_VORBIS_ENTRY_BYTES || offset + length > block.size) {
                return null
            }
            val value = block.copyOfRange(offset, offset + length).toString(Charsets.UTF_8)
            offset += length
            return value
        }
        // Vendor string.
        readString() ?: return ReplayGainTags()
        if (offset + 4 > block.size) return ReplayGainTags()
        val count =
            (block[offset].toInt() and 0xFF) or
                ((block[offset + 1].toInt() and 0xFF) shl 8) or
                ((block[offset + 2].toInt() and 0xFF) shl 16) or
                ((block[offset + 3].toInt() and 0xFF) shl 24)
        offset += 4
        if (count < 0 || count > MAX_VORBIS_ENTRIES) return ReplayGainTags()
        val entries = mutableMapOf<String, String>()
        repeat(count) {
            val entry = readString() ?: return ReplayGainTags()
            val separator = entry.indexOf('=')
            if (separator > 0) {
                val key = entry.substring(0, separator).uppercase()
                if (key !in entries) entries[key] = entry.substring(separator + 1).trim()
            }
        }
        return ReplayGainTags(
            xiph =
                buildMap {
                    entries["R128_TRACK_GAIN"]?.let { put("R128_TRACK_GAIN", listOf(it)) }
                    entries["R128_ALBUM_GAIN"]?.let { put("R128_ALBUM_GAIN", listOf(it)) }
                    entries["REPLAYGAIN_TRACK_GAIN"]?.let {
                        put("REPLAYGAIN_TRACK_GAIN", listOf(it))
                    }
                    entries["REPLAYGAIN_ALBUM_GAIN"]?.let {
                        put("REPLAYGAIN_ALBUM_GAIN", listOf(it))
                    }
                }
        )
    }

    // --- Stream helpers ---

    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
        return offset
    }

    private fun skipFully(input: InputStream, size: Long): Boolean {
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

    private fun ByteArray.startsWith(prefix: String): Boolean {
        val bytes = prefix.toByteArray(Charsets.ISO_8859_1)
        if (size < bytes.size) return false
        return copyOfRange(0, bytes.size).contentEquals(bytes)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return copyOfRange(0, prefix.size).contentEquals(prefix)
    }

    private fun synchsafe(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun bigEndian(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun unsignedInt(bytes: ByteArray, offset: Int): Long =
        ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)

    private fun unsignedLong(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        repeat(8) { index -> value = (value shl 8) or (bytes[offset + index].toLong() and 0xFF) }
        return value
    }

    private const val SNIFF_BYTES = 12
    private const val ID3_HEADER_BYTES = 10
    private const val ID3_FRAME_HEADER_BYTES = 10
    private const val ID3_FLAG_UNSYNC = 0x80
    private const val ID3_FLAG_EXTENDED = 0x40
    private const val MAX_ID3_BYTES = 2 * 1024 * 1024
    private const val MAX_ID3_FRAME_BYTES = 256 * 1024
    private const val FLAC_TYPE_VORBIS_COMMENT = 4
    private const val MAX_FLAC_SCAN_BYTES = 4 * 1024 * 1024
    private const val MAX_VORBIS_COMMENT_BYTES = 1024 * 1024
    private const val MAX_VORBIS_ENTRY_BYTES = 64 * 1024
    private const val MAX_VORBIS_ENTRIES = 512
    private const val OGG_PAGE_HEADER_BYTES = 27
    private const val OGG_MAX_SEGMENT_SIZE = 255
    private const val MAX_OGG_PAGES = 64
    private const val MAX_OGG_PACKET_BYTES = 256 * 1024
    private const val MAX_MP4_SCAN_BYTES = 32L * 1024 * 1024
    private const val MAX_MP4_ILST_BYTES = 2L * 1024 * 1024
    private const val MAX_MP4_FREEFORM_BYTES = 64 * 1024
}
