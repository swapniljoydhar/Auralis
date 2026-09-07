/*
 * Copyright (c) 2024 Auralis Contributors
 * PreMusic.kt is part of Auralis.
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
 
package com.auralis.musikr.tag.interpret

import android.net.Uri
import com.auralis.musikr.Music
import com.auralis.musikr.covers.Cover
import com.auralis.musikr.fs.Format
import com.auralis.musikr.fs.Path
import com.auralis.musikr.tag.Date
import com.auralis.musikr.tag.Disc
import com.auralis.musikr.tag.Name
import com.auralis.musikr.tag.ReleaseType
import com.auralis.musikr.tag.ReplayGainAdjustment
import com.auralis.musikr.util.update
import java.util.UUID

internal data class PreSong(
    val v363Uid: Music.UID,
    val v400Uid: Music.UID,
    val v401Uid: Music.UID,
    val musicBrainzId: UUID?,
    val name: Name.Known,
    val rawName: String,
    val track: Int?,
    val disc: Disc?,
    val date: Date?,
    val uri: Uri,
    val path: Path,
    val format: Format,
    val size: Long,
    val durationMs: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val replayGainAdjustment: ReplayGainAdjustment,
    val modifiedMs: Long,
    val addedMs: Long,
    val cover: Cover?,
    val preAlbum: PreAlbum,
    val preArtists: List<PreArtist>,
    val preGenres: List<PreGenre>,
)

internal data class PreAlbum(
    val musicBrainzId: UUID?,
    val name: Name,
    val rawName: String?,
    val releaseType: ReleaseType,
    val preArtists: PreArtistsFrom,
) {
    val uid =
        // Attempt to use a MusicBrainz ID first before falling back to a hashed UID.
        musicBrainzId?.let { Music.UID.musicBrainz(Music.UID.Item.ALBUM, it) }
            ?: Music.UID.auralis(Music.UID.Item.ALBUM) {
                // Hash based on only names despite the presence of a date to increase stability.
                // I don't know if there is any situation where an artist will have two albums with
                // the exact same name, but if there is, I would love to know.
                update(rawName)
                update(preArtists.preArtists.mapNotNull { it.rawName })
            }
}

internal sealed interface PreArtistsFrom {
    val preArtists: List<PreArtist>

    data class Individual(override val preArtists: List<PreArtist>) : PreArtistsFrom

    data class Album(override val preArtists: List<PreArtist>) : PreArtistsFrom
}

internal data class PreArtist(val musicBrainzId: UUID?, val name: Name, val rawName: String?) {
    val uid =
        // Attempt to use a MusicBrainz ID first before falling back to a hashed UID.
        musicBrainzId?.let { Music.UID.musicBrainz(Music.UID.Item.ARTIST, it) }
            ?: Music.UID.auralis(Music.UID.Item.ARTIST) { update(rawName) }
}

internal data class PreGenre(val name: Name, val rawName: String?)
