/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookCatalog.kt is part of Auralis.
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

import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.tag.Name

/**
 * A chapter is backed by the existing [Song] model so Music playback remains unchanged.
 * Audiobook-specific state is attached by [AudiobookProgressRepository] in the app layer.
 */
data class AudiobookChapter(val song: Song, val number: Int, val title: String) {
    val uid: Music.UID
        get() = song.uid

    val durationMs: Long
        get() = song.durationMs
}

/** A format-independent audiobook book composed of one or more local audio chapters. */
data class AudiobookBook(
    val key: String,
    val title: String,
    val author: String?,
    val chapters: List<AudiobookChapter>,
) {
    val totalDurationMs: Long
        get() = chapters.sumOf { it.durationMs.coerceAtLeast(0L) }

    val chapterCount: Int
        get() = chapters.size
}

/** The normalized signals needed to classify one local audio file. */
data class AudiobookFileSignals(
    val extension: String?,
    val path: String?,
    val album: String?,
    val genres: List<String>,
)

/**
 * Conservative audiobook detection that works with locally indexed M4B, MP3, M4A, OGG, OGA, and
 * OPUS files without re-scanning files.
 *
 * M4B is a strong signal because it is the audiobook-oriented MPEG-4 extension. Other supported
 * local audio containers are intentionally classified only when their path, album, or genre carries
 * an explicit audiobook marker, preventing ordinary music albums from leaking into the Audiobooks
 * section.
 */
object AudiobookClassifier {
    private val audiobookExtensions = setOf("m4b")
    private val audiobookMarkers =
        setOf(
            "audiobook",
            "audiobooks",
            "audio book",
            "audio books",
            "spoken word",
            "narrated",
            "narration",
        )

    fun isAudiobook(song: Song): Boolean {
        val signals =
            AudiobookFileSignals(
                extension = song.path.name?.substringAfterLast('.', ""),
                path = song.path.components.unixString,
                album = song.album.name.asRawOrNull(),
                genres = song.genres.mapNotNull { it.name.asRawOrNull() },
            )
        return isAudiobook(signals)
    }

    fun isAudiobook(signals: AudiobookFileSignals): Boolean {
        if (signals.extension?.lowercase() in audiobookExtensions) {
            return true
        }

        return markerMatches(signals.path) ||
            markerMatches(signals.album) ||
            signals.genres.any(::markerMatches)
    }

    internal fun markerMatches(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val tokens = value.lowercase().split(Regex("[^a-z0-9]+"))
        val normalized = tokens.filter(String::isNotBlank).joinToString(" ")
        return audiobookMarkers.any { marker ->
            marker == normalized || marker.split(' ').all { token -> tokens.contains(token) }
        }
    }
}

/** Groups classified chapters into stable books while preserving ordinary Music albums. */
object AudiobookCatalog {
    fun fromSongs(
        songs: Collection<Song>,
        manualSongUids: Set<String> = emptySet(),
        organization: AudiobookFolderOrganization =
            AudiobookFolderOrganization.SEPARATE_BOOK_FOLDERS,
        selectedFolders: Set<String> = emptySet(),
    ): List<AudiobookBook> {
        return songs
            .asSequence()
            .groupBy { song -> bookGroup(song, organization, selectedFolders) }
            .filter { (_, chapters) ->
                chapters.any { song ->
                    song.uid.toString() in manualSongUids || AudiobookClassifier.isAudiobook(song)
                } || isLongFormBook(chapters.map(Song::durationMs))
            }
            .map { (group, chapters) ->
                val ordered =
                    chapters
                        .sortedWith(
                            compareBy<Song>({ it.disc?.number ?: 0 }, { it.track ?: Int.MAX_VALUE })
                                .thenBy { it.path.name.orEmpty().lowercase() }
                                .thenBy { it.uid.toString() }
                        )
                        .mapIndexed { index, song ->
                            AudiobookChapter(song = song, number = index + 1, title = song.name.raw)
                        }
                AudiobookBook(
                    key = group.key,
                    title = group.title ?: bookTitle(ordered),
                    author = group.author ?: bookAuthor(ordered),
                    chapters = ordered,
                )
            }
            .sortedBy { it.title.lowercase() }
            .toList()
    }

    /**
     * Recognize local MP3 chapter sets that have no audiobook filename or tag marker. This keeps
     * ordinary music albums in Music while making multi-hour books visible without requiring the
     * former cross-library manual assignment flow.
     */
    internal fun isLongFormBook(durationsMs: Collection<Long>): Boolean {
        val durations = durationsMs.filter { it > 0L }
        val totalDurationMs = durations.sum()
        return (durations.size == 1 && totalDurationMs >= MIN_SINGLE_FILE_DURATION_MS) ||
            (durations.size >= MIN_CHAPTER_COUNT &&
                totalDurationMs >= MIN_BOOK_DURATION_MS &&
                durations.count { it >= MIN_CHAPTER_DURATION_MS } >= MIN_CHAPTER_COUNT)
    }

    /**
     * Album metadata identifies a book when several downloaded books share one directory. The
     * directory remains the fallback for chapter files without a usable book-level album title.
     */
    fun bookKey(song: Song): String {
        return bookGroup(song, AudiobookFolderOrganization.SEPARATE_BOOK_FOLDERS, emptySet()).key
    }

    private fun bookGroup(
        song: Song,
        organization: AudiobookFolderOrganization,
        selectedFolders: Set<String>,
    ): AudiobookFolderGroup {
        val volume = song.path.volume.mediaStoreName ?: song.path.volume.toString()
        val folderGroup =
            AudiobookFolderOrganizationResolver.group(
                volume = volume,
                directory = song.path.directory.components.unixString,
                organization = organization,
                selectedFolders = selectedFolders,
            )
        return folderGroup
    }

    private fun bookTitle(chapters: List<AudiobookChapter>): String {
        val albumTitle = chapters.firstOrNull()?.song?.album?.name.asRawOrNull()
        if (!albumTitle.isNullOrBlank() && !albumTitle.isGenericAlbumName()) {
            return albumTitle
        }

        return chapters.firstOrNull()?.song?.path?.directory?.name?.takeUnless {
            it.isNullOrBlank()
        } ?: chapters.firstOrNull()?.title ?: "Unknown audiobook"
    }

    private fun bookAuthor(chapters: List<AudiobookChapter>): String? {
        val song = chapters.firstOrNull()?.song ?: return null
        return song.album.artists.firstOrNull()?.name.asRawOrNull()
            ?: song.artists.firstOrNull()?.name.asRawOrNull()
    }

    private fun String.isGenericAlbumName() =
        lowercase() in setOf("album", "unknown album", "audiobook", "audiobooks")

    private const val MIN_CHAPTER_COUNT = 2
    private const val MIN_CHAPTER_DURATION_MS = 10 * 60_000L
    private const val MIN_BOOK_DURATION_MS = 2 * 60 * 60_000L
    private const val MIN_SINGLE_FILE_DURATION_MS = 2 * 60 * 60_000L
}

private fun Name?.asRawOrNull(): String? =
    (this as? Name.Known)?.raw?.trim()?.takeIf(String::isNotEmpty)
