/*
 * Copyright (c) 2024 Auralis Contributors
 * ArtistImpl.kt is part of Auralis.
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
 
package com.auralis.musikr.model

import com.auralis.musikr.Album
import com.auralis.musikr.Artist
import com.auralis.musikr.Genre
import com.auralis.musikr.Song
import com.auralis.musikr.covers.CoverCollection
import com.auralis.musikr.tag.interpret.PreArtist

internal interface ArtistCore {
    val preArtist: PreArtist
    val songs: Set<Song>
    val albums: Set<Album>

    fun resolveGenres(): Set<Genre>
}

/**
 * Library-backed implementation of [Artist].
 *
 * @author Auralis Contributors
 */
internal class ArtistImpl(private val core: ArtistCore) : Artist {
    override val uid = core.preArtist.uid
    override val name = core.preArtist.name

    override val songs = core.songs
    override var explicitAlbums = core.albums
    override var implicitAlbums = core.songs.mapTo(mutableSetOf()) { it.album } - core.albums

    override val genres: List<Genre>
        get() = core.resolveGenres().toList()

    override val durationMs = core.songs.sumOf { it.durationMs }
    override val covers =
        CoverCollection.from(
            core.songs.mapNotNull { it.cover }.ifEmpty { core.albums.flatMap { it.covers.covers } }
        )

    private val hashCode =
        31 * (31 * uid.hashCode() + core.preArtist.hashCode()) * core.songs.hashCode()

    override fun hashCode() = hashCode

    override fun equals(other: Any?) =
        other is ArtistImpl &&
            uid == other.uid &&
            core.preArtist == other.core.preArtist &&
            songs == other.songs

    override fun toString() = "Artist(uid=$uid, name=$name)"
}
