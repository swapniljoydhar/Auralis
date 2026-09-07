/*
 * Copyright (c) 2025 Auralis Contributors
 * DBCache.kt is part of Auralis.
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
 
package com.auralis.musikr.cache.db

import android.content.Context
import android.net.Uri
import com.auralis.musikr.cache.Audio
import com.auralis.musikr.cache.Cache
import com.auralis.musikr.cache.CacheResult
import com.auralis.musikr.cache.CachedFile
import com.auralis.musikr.cache.MutableCache
import com.auralis.musikr.fs.File
import com.auralis.musikr.metadata.Properties
import com.auralis.musikr.tag.parse.ParsedTags
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An immutable [Cache] backed by an internal Room database.
 *
 * Create an instance with [from].
 */
class DBCache private constructor(private val readDao: CacheReadDao) : Cache {
    private var mapping: Map<Uri, CachedFileData>? = null
    private val mappingLock = Mutex()

    override suspend fun read(file: File): CacheResult {
        val currentMapping =
            mappingLock.withLock {
                mapping ?: readDao.selectAllSongs().associateBy { it.uri }.also { mapping = it }
            }
        val dbSong = currentMapping[file.uri] ?: return CacheResult.Miss(file)
        if (dbSong.modifiedMs != file.modifiedMs) {
            return CacheResult.Stale(file, dbSong.addedMs)
        }
        // Rows written by older/buggy versions (or partially-updated rows) can carry nulls in
        // columns the audio model requires. Treat those rows as stale so the file is
        // re-extracted instead of crashing on an unchecked null dereference.
        val durationMs = dbSong.durationMs
        val bitrateKbps = dbSong.bitrateKbps
        val sampleRateHz = dbSong.sampleRateHz
        val releaseTypes = dbSong.releaseTypes
        val artistMusicBrainzIds = dbSong.artistMusicBrainzIds
        val artistNames = dbSong.artistNames
        val artistSortNames = dbSong.artistSortNames
        val albumArtistMusicBrainzIds = dbSong.albumArtistMusicBrainzIds
        val albumArtistNames = dbSong.albumArtistNames
        val albumArtistSortNames = dbSong.albumArtistSortNames
        val genreNames = dbSong.genreNames
        if (
            dbSong.mimeType != null &&
                (durationMs == null ||
                    bitrateKbps == null ||
                    sampleRateHz == null ||
                    releaseTypes == null ||
                    artistMusicBrainzIds == null ||
                    artistNames == null ||
                    artistSortNames == null ||
                    albumArtistMusicBrainzIds == null ||
                    albumArtistNames == null ||
                    albumArtistSortNames == null ||
                    genreNames == null)
        ) {
            return CacheResult.Stale(file, dbSong.addedMs)
        }
        val song =
            CachedFile(
                file,
                dbSong.mimeType?.let {
                    Audio(
                        Properties(
                            dbSong.mimeType,
                            durationMs ?: 0L,
                            bitrateKbps ?: 0,
                            sampleRateHz ?: 0,
                        ),
                        ParsedTags(
                            musicBrainzId = dbSong.musicBrainzId,
                            name = dbSong.name,
                            sortName = dbSong.sortName,
                            durationMs = durationMs ?: 0L,
                            track = dbSong.track,
                            disc = dbSong.disc,
                            subtitle = dbSong.subtitle,
                            date = dbSong.date,
                            albumMusicBrainzId = dbSong.albumMusicBrainzId,
                            albumName = dbSong.albumName,
                            albumSortName = dbSong.albumSortName,
                            releaseTypes = releaseTypes.orEmpty(),
                            artistMusicBrainzIds = artistMusicBrainzIds.orEmpty(),
                            artistNames = artistNames.orEmpty(),
                            artistSortNames = artistSortNames.orEmpty(),
                            albumArtistMusicBrainzIds = albumArtistMusicBrainzIds.orEmpty(),
                            albumArtistNames = albumArtistNames.orEmpty(),
                            albumArtistSortNames = albumArtistSortNames.orEmpty(),
                            genreNames = genreNames.orEmpty(),
                            replayGainTrackAdjustment = dbSong.replayGainTrackAdjustment,
                            replayGainAlbumAdjustment = dbSong.replayGainAlbumAdjustment,
                        ),
                        coverId = dbSong.coverId,
                    )
                },
                addedMs = dbSong.addedMs,
            )
        return CacheResult.Hit(song)
    }

    companion object {
        /**
         * Create a new instance of [DBCache] from the given [context].
         *
         * This instance should be a singleton, since it implicitly holds a Room database. As a
         * result, you should only create EITHER a [DBCache] or a [MutableDBCache].
         *
         * @param context The context to use to create the Room database.
         * @return A new instance of [DBCache].
         */
        fun from(context: Context) = from(CacheDatabase.from(context))

        internal fun from(db: CacheDatabase) = DBCache(db.readDao())
    }
}

/**
 * A mutable [Cache] backed by an internal Room database.
 *
 * Create an instance with [from].
 */
class MutableDBCache
private constructor(private val inner: DBCache, private val writeDao: CacheWriteDao) :
    MutableCache {
    override suspend fun read(file: File) = inner.read(file)

    override suspend fun write(cachedFile: CachedFile) {
        val dbSong =
            CachedFileData(
                uri = cachedFile.file.uri,
                modifiedMs = cachedFile.file.modifiedMs,
                addedMs = cachedFile.addedMs,
                mimeType = cachedFile.audio?.properties?.mimeType,
                durationMs = cachedFile.audio?.properties?.durationMs,
                bitrateKbps = cachedFile.audio?.properties?.bitrateKbps,
                sampleRateHz = cachedFile.audio?.properties?.sampleRateHz,
                musicBrainzId = cachedFile.audio?.tags?.musicBrainzId,
                name = cachedFile.audio?.tags?.name,
                sortName = cachedFile.audio?.tags?.sortName,
                track = cachedFile.audio?.tags?.track,
                disc = cachedFile.audio?.tags?.disc,
                subtitle = cachedFile.audio?.tags?.subtitle,
                date = cachedFile.audio?.tags?.date,
                albumMusicBrainzId = cachedFile.audio?.tags?.albumMusicBrainzId,
                albumName = cachedFile.audio?.tags?.albumName,
                albumSortName = cachedFile.audio?.tags?.albumSortName,
                releaseTypes = cachedFile.audio?.tags?.releaseTypes,
                artistMusicBrainzIds = cachedFile.audio?.tags?.artistMusicBrainzIds,
                artistNames = cachedFile.audio?.tags?.artistNames,
                artistSortNames = cachedFile.audio?.tags?.artistSortNames,
                albumArtistMusicBrainzIds = cachedFile.audio?.tags?.albumArtistMusicBrainzIds,
                albumArtistNames = cachedFile.audio?.tags?.albumArtistNames,
                albumArtistSortNames = cachedFile.audio?.tags?.albumArtistSortNames,
                genreNames = cachedFile.audio?.tags?.genreNames,
                replayGainTrackAdjustment = cachedFile.audio?.tags?.replayGainTrackAdjustment,
                replayGainAlbumAdjustment = cachedFile.audio?.tags?.replayGainAlbumAdjustment,
                coverId = cachedFile.audio?.coverId,
            )
        writeDao.updateSong(dbSong)
    }

    override suspend fun cleanup(excluding: List<CachedFile>) {
        writeDao.deleteExcludingUris(excluding.mapTo(mutableSetOf()) { it.file.uri.toString() })
    }

    companion object {
        /**
         * Create a new instance of [MutableDBCache] from the given [context].
         *
         * This instance should be a singleton, since it implicitly holds a Room database. As a
         * result, you should only create EITHER a [DBCache] or a [MutableDBCache].
         *
         * @param context The context to use to create the Room database.
         * @return A new instance of [MutableDBCache].
         */
        fun from(context: Context): MutableDBCache {
            val db = CacheDatabase.from(context)
            return MutableDBCache(DBCache.from(db), db.writeDao())
        }
    }
}
