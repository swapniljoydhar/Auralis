/*
 * Copyright (c) 2024 Auralis Contributors
 * MetadataExtractor.kt is part of Auralis.
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
 
package org.oxycblt.musikr.metadata

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.fs.File

internal interface MetadataExtractor {
    suspend fun extract(deviceFile: File): MetadataResult

    companion object {
        fun from(context: Context): MetadataExtractor =
            MetadataExtractorImpl(context.contentResolver)
    }
}

sealed interface MetadataResult {
    data class Success(val metadata: Metadata?) : MetadataResult

    data object NoMetadata : MetadataResult

    data object NotAudio : MetadataResult

    data object ProviderFailed : MetadataResult
}

private class MetadataExtractorImpl(private val contentResolver: ContentResolver) :
    MetadataExtractor {
    override suspend fun extract(deviceFile: File): MetadataResult =
        withContext(Dispatchers.IO) {
            try {
                contentResolver.openFileDescriptor(deviceFile.uri, "r")?.use { pfd ->
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(pfd.fileDescriptor)
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                        val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                            ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                        val track = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                        val disc = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                        val compilation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)
                        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mpeg"
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { it / 1000 } ?: 0
                        val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
                        val cover = retriever.embeddedPicture

                        val id3v2 = mutableMapOf<String, List<String>>()
                        title?.let { id3v2["TIT2"] = listOf(it) }
                        artist?.let { id3v2["TPE1"] = listOf(it) }
                        album?.let { id3v2["TALB"] = listOf(it) }
                        albumArtist?.let { id3v2["TPE2"] = listOf(it) }
                        genre?.let { id3v2["TCON"] = listOf(it) }
                        date?.let { id3v2["TDRC"] = listOf(it) }
                        track?.let { id3v2["TRCK"] = listOf(it) }
                        disc?.let { id3v2["TPOS"] = listOf(it) }
                        compilation?.let { id3v2["TCMP"] = listOf(it) }

                        val properties = Properties(
                            mimeType = mimeType,
                            durationMs = duration,
                            bitrateKbps = bitrate,
                            sampleRateHz = sampleRate,
                        )

                        MetadataResult.Success(
                            Metadata(
                                id3v2 = id3v2,
                                xiph = emptyMap(),
                                mp4 = emptyMap(),
                                cover = cover,
                                properties = properties,
                            )
                        )
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Throwable) {}
                    }
                } ?: MetadataResult.ProviderFailed
            } catch (e: Exception) {
                MetadataResult.NoMetadata
            }
        }
}

