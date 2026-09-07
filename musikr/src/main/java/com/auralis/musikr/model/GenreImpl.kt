/*
 * Copyright (c) 2023 Auralis Contributors
 * GenreImpl.kt is part of Auralis.
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

import com.auralis.musikr.Artist
import com.auralis.musikr.Genre
import com.auralis.musikr.Music
import com.auralis.musikr.Song
import com.auralis.musikr.covers.CoverCollection
import com.auralis.musikr.tag.interpret.PreGenre
import com.auralis.musikr.util.update

internal interface GenreCore {
    val preGenre: PreGenre
    val songs: Set<Song>
    val artists: Set<Artist>
}

/**
 * Library-backed implementation of [Genre].
 *
 * @author Auralis Contributors
 */
internal class GenreImpl(private val core: GenreCore) : Genre {
    override val uid = Music.UID.auralis(Music.UID.Item.GENRE) { update(core.preGenre.rawName) }
    override val name = core.preGenre.name

    override val songs = core.songs
    override val artists = core.artists
    override val durationMs = core.songs.sumOf { it.durationMs }
    override val covers = CoverCollection.from(core.songs.mapNotNull { it.cover })

    private val hashCode = 31 * (31 * uid.hashCode() + core.preGenre.hashCode()) + songs.hashCode()

    override fun hashCode() = hashCode

    override fun equals(other: Any?) =
        other is GenreImpl &&
            uid == other.uid &&
            core.preGenre == other.core.preGenre &&
            songs == other.songs

    override fun toString() = "Genre(uid=$uid, name=$name)"
}
