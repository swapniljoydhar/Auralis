/*
 * Copyright (c) 2024 Auralis Contributors
 * PlaylistFile.kt is part of Auralis.
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
 
package com.auralis.musikr.playlist

import com.auralis.musikr.Music
import com.auralis.musikr.Song

internal data class PlaylistFile(
    val name: String,
    val songPointers: List<SongPointer>,
    val handle: PlaylistHandle,
)

internal sealed interface SongPointer {
    data class UID(val uid: Music.UID) : SongPointer
    //    data class Path(val options: List<Path>) : SongPointer
}

internal interface PlaylistHandle {
    val uid: Music.UID

    suspend fun rename(name: String)

    suspend fun add(songs: List<Song>)

    suspend fun rewrite(songs: List<Song>)

    suspend fun delete()
}
