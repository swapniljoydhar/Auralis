/*
 * Copyright (c) 2024 Auralis Contributors
 * LibraryFactory.kt is part of Auralis.
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
import com.auralis.musikr.Music
import com.auralis.musikr.MutableLibrary
import com.auralis.musikr.Song
import com.auralis.musikr.graph.AlbumVertex
import com.auralis.musikr.graph.ArtistVertex
import com.auralis.musikr.graph.GenreVertex
import com.auralis.musikr.graph.MusicGraph
import com.auralis.musikr.graph.PlaylistVertex
import com.auralis.musikr.graph.SongVertex
import com.auralis.musikr.graph.Vertex
import com.auralis.musikr.playlist.db.StoredPlaylists
import com.auralis.musikr.playlist.interpret.PlaylistInterpreter

internal interface LibraryFactory {
    fun create(
        graph: MusicGraph,
        storedPlaylists: StoredPlaylists,
        playlistInterpreter: PlaylistInterpreter,
    ): MutableLibrary

    companion object {
        fun new(): LibraryFactory = LibraryFactoryImpl()
    }
}

private class LibraryFactoryImpl() : LibraryFactory {
    override fun create(
        graph: MusicGraph,
        storedPlaylists: StoredPlaylists,
        playlistInterpreter: PlaylistInterpreter,
    ): MutableLibrary {
        val songs =
            graph.songVertex.mapTo(mutableSetOf()) { vertex ->
                SongImpl(SongVertexCore(vertex)).also { vertex.tag = it }
            }
        val albums =
            graph.albumVertex.mapTo(mutableSetOf()) { vertex ->
                AlbumImpl(AlbumVertexCore(vertex)).also { vertex.tag = it }
            }
        val artists =
            graph.artistVertex.mapTo(mutableSetOf()) { vertex ->
                ArtistImpl(ArtistVertexCore(vertex)).also { vertex.tag = it }
            }
        val genres =
            graph.genreVertex.mapTo(mutableSetOf()) { vertex ->
                GenreImpl(GenreVertexCore(vertex)).also { vertex.tag = it }
            }
        val playlists =
            graph.playlistVertex.mapTo(mutableSetOf()) { vertex ->
                PlaylistImpl(PlaylistVertexCore(vertex))
            }
        return LibraryImpl(
            songs,
            albums,
            artists,
            genres,
            playlists,
            storedPlaylists,
            playlistInterpreter,
        )
    }

    private class SongVertexCore(private val vertex: SongVertex) : SongCore {
        override val preSong = vertex.preSong

        override fun resolveAlbum(): Album = tag(vertex.albumVertex)

        override fun resolveArtists(): List<Artist> = vertex.artistVertices.map { tag(it) }

        override fun resolveGenres(): List<Genre> = vertex.genreVertices.map { tag(it) }
    }

    private class AlbumVertexCore(private val vertex: AlbumVertex) : AlbumCore {
        override val preAlbum = vertex.preAlbum

        override val songs: Set<Song> = vertex.songVertices.mapTo(mutableSetOf()) { tag(it) }

        override fun resolveArtists(): List<Artist> = vertex.artistVertices.map { tag(it) }
    }

    private class ArtistVertexCore(private val vertex: ArtistVertex) : ArtistCore {
        override val preArtist = vertex.preArtist

        override val songs: Set<Song> = vertex.songVertices.mapTo(mutableSetOf()) { tag(it) }

        override val albums: Set<Album> = vertex.albumVertices.mapTo(mutableSetOf()) { tag(it) }

        override fun resolveGenres(): Set<Genre> =
            vertex.genreVertices.mapTo(mutableSetOf()) { tag(it) }
    }

    private class GenreVertexCore(vertex: GenreVertex) : GenreCore {
        override val preGenre = vertex.preGenre

        override val songs: Set<Song> = vertex.songVertices.mapTo(mutableSetOf()) { tag(it) }

        override val artists: Set<Artist> = vertex.artistVertices.mapTo(mutableSetOf()) { tag(it) }
    }

    private class PlaylistVertexCore(vertex: PlaylistVertex) : PlaylistCore {
        override val prePlaylist = vertex.prePlaylist

        override val songs: List<Song> =
            vertex.songVertices.mapNotNull { vertex -> vertex?.let { tag(it) } }
    }

    private companion object {
        private inline fun <reified T : Music> tag(vertex: Vertex): T {
            val tag = vertex.tag
            check(tag is T) { "Dead Vertex Detected: $vertex" }
            return tag
        }
    }
}
