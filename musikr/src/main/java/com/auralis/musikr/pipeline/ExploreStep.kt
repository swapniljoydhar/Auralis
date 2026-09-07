/*
 * Copyright (c) 2024 Auralis Contributors
 * ExploreStep.kt is part of Auralis.
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
 
package com.auralis.musikr.pipeline

import android.content.Context
import com.auralis.musikr.Config
import com.auralis.musikr.Storage
import com.auralis.musikr.cache.CacheResult
import com.auralis.musikr.cache.CachedFile
import com.auralis.musikr.covers.CoverResult
import com.auralis.musikr.fs.FS
import com.auralis.musikr.fs.File
import com.auralis.musikr.playlist.m3u.M3U
import com.auralis.musikr.util.mapParallel
import com.auralis.musikr.util.merge
import com.auralis.musikr.util.tryAsyncWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel

internal interface ExploreStep {
    suspend fun explore(scope: CoroutineScope, explored: Channel<Explored>): Deferred<Result<Unit>>

    companion object {
        fun from(context: Context, config: Config): ExploreStep =
            ExploreStepImpl(config.fs, config.storage)
    }
}

private class ExploreStepImpl(private val fs: FS, private val storage: Storage) : ExploreStep {
    override suspend fun explore(
        scope: CoroutineScope,
        explored: Channel<Explored>,
    ): Deferred<Result<Unit>> {
        val files = Channel<File>(Channel.UNLIMITED)
        val filesTask = fs.explore(files)

        val classified = Channel<Classified>(Channel.UNLIMITED)
        val classifiedTask =
            scope.mapParallel(PARALLELISM, files, classified, Dispatchers.IO) { file ->
                if (
                    file.mimeType == M3U.MIME_TYPE ||
                        (!file.mimeType.startsWith("audio/") &&
                            file.mimeType != "application/ogg" &&
                            file.mimeType != "application/x-ogg" &&
                            file.mimeType != "application/octet-stream")
                ) {
                    return@mapParallel Finalized(NotAudio)
                }
                when (val cacheResult = storage.cache.read(file)) {
                    is CacheResult.Hit -> NeedsHydration(cacheResult.file)
                    is CacheResult.Stale -> Finalized(NewSong(cacheResult.file))
                    is CacheResult.Miss -> Finalized(NewSong(cacheResult.file))
                }
            }

        val finalized = Channel<Finalized>(Channel.UNLIMITED)
        val exploredTask =
            scope.mapParallel(PARALLELISM, classified, finalized, Dispatchers.IO) { item ->
                when (item) {
                    is Finalized -> item
                    is NeedsHydration -> {
                        val audio = item.cachedFile.audio ?: return@mapParallel Finalized(NotAudio)
                        val coverId =
                            when (
                                val result = audio.coverId?.let { id -> storage.covers.obtain(id) }
                            ) {
                                is CoverResult.Hit -> result.cover
                                is CoverResult.Miss ->
                                    return@mapParallel Finalized(NewSong(item.cachedFile.file))
                                null -> null
                            }

                        Finalized(
                            RawSong(
                                item.cachedFile.file,
                                audio.properties,
                                audio.tags,
                                coverId,
                                item.cachedFile.addedMs,
                            )
                        )
                    }
                }
            }
        val playlists = Channel<Explored>(Channel.UNLIMITED)
        val playlistsTask =
            scope.tryAsyncWith(playlists, Dispatchers.IO) {
                for (playlist in storage.storedPlaylists.read()) {
                    val rawPlaylist = RawPlaylist(playlist)
                    it.send(rawPlaylist)
                }
            }

        val mergeTask =
            scope.tryAsyncWith(explored, Dispatchers.Main) {
                for (item in finalized) {
                    it.send(item.explored)
                }
                for (playlist in playlists) {
                    it.send(playlist)
                }
            }

        return scope.merge(filesTask, classifiedTask, exploredTask, playlistsTask, mergeTask)
    }

    private sealed interface Classified

    private data class NeedsHydration(val cachedFile: CachedFile) : Classified

    private data class Finalized(val explored: Explored) : Classified

    private companion object {
        const val PARALLELISM = 8
    }
}
