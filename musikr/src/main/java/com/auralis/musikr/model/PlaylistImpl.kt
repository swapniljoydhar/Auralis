/*
 * Copyright (c) 2023 Auralis Contributors
 * PlaylistImpl.kt is part of Auralis.
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

import com.auralis.musikr.Playlist
import com.auralis.musikr.Song
import com.auralis.musikr.covers.CoverCollection
import com.auralis.musikr.playlist.interpret.PrePlaylistInfo
import com.auralis.musikr.tag.Name

internal interface PlaylistCore {
    val prePlaylist: PrePlaylistInfo
    val songs: List<Song>
}

internal class PlaylistImpl(val core: PlaylistCore) : Playlist {
    override val uid = core.prePlaylist.handle.uid
    override val name: Name.Known = core.prePlaylist.name
    override val durationMs = core.songs.sumOf { it.durationMs }
    override val covers = CoverCollection.from(core.songs.mapNotNull { it.cover })
    override val songs = core.songs

    private var hashCode =
        31 * (31 * uid.hashCode() + core.prePlaylist.hashCode()) + songs.hashCode()

    override fun equals(other: Any?) =
        other is PlaylistImpl && core.prePlaylist == other.core.prePlaylist && songs == other.songs

    override fun hashCode() = hashCode

    override fun toString() = "Playlist(uid=$uid, name=$name)"
}
