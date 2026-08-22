/*
 * Copyright (c) 2026 Auralis Project
 * PersistenceRepository.kt is part of Auralis.
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
 
package com.auralis.player.playback.persist

import androidx.room.withTransaction
import com.auralis.player.music.MusicRepository
import com.auralis.player.playback.state.PlaybackDomain
import com.auralis.player.playback.state.PlaybackStateManager
import javax.inject.Inject
import org.oxycblt.musikr.MusicParent
import timber.log.Timber as L

/** Persists independently recoverable local playback snapshots for each Auralis domain. */
interface PersistenceRepository {
    suspend fun readState(domain: PlaybackDomain): PlaybackStateManager.SavedState?

    suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean
}

class PersistenceRepositoryImpl
@Inject
constructor(
    private val database: PersistenceDatabase,
    private val domainPlaybackStateDao: DomainPlaybackStateDao,
    private val musicRepository: MusicRepository,
) : PersistenceRepository {

    override suspend fun readState(domain: PlaybackDomain): PlaybackStateManager.SavedState? {
        val library = musicRepository.library?.takeIf { !it.empty() } ?: return null
        return try {
            val persisted = domainPlaybackStateDao.getState(domain.name) ?: return null
            val heap = domainPlaybackStateDao.getHeap(domain.name).map { library.findSong(it.uid) }
            val mapping = domainPlaybackStateDao.getMapping(domain.name).map { it.index }
            val parent = persisted.parentUid?.let { musicRepository.find(it) as? MusicParent }
            PlaybackStateManager.SavedState(
                domain = domain,
                positionMs = persisted.positionMs,
                repeatMode = persisted.repeatMode,
                parent = parent,
                heap = heap,
                shuffledMapping = mapping,
                index = persisted.index,
                songUid = persisted.songUid,
            )
        } catch (e: Exception) {
            L.e(e, "Unable to read $domain playback state")
            null
        }
    }

    override suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean {
        val domain = state?.domain ?: PlaybackDomain.MUSIC
        return try {
            database.withTransaction {
                domainPlaybackStateDao.clearState(domain.name)
                domainPlaybackStateDao.clearHeap(domain.name)
                domainPlaybackStateDao.clearMapping(domain.name)

                if (state != null) {
                    domainPlaybackStateDao.insertState(
                        DomainPlaybackState(
                            domain = domain.name,
                            index = state.index,
                            positionMs = state.positionMs,
                            repeatMode = state.repeatMode,
                            songUid = state.songUid,
                            parentUid = state.parent?.uid,
                        )
                    )
                    domainPlaybackStateDao.insertHeap(
                        state.heap.mapIndexed { index, song ->
                            DomainQueueHeapItem(domain.name, index, requireNotNull(song).uid)
                        }
                    )
                    domainPlaybackStateDao.insertMapping(
                        state.shuffledMapping.mapIndexed { index, mappedIndex ->
                            DomainQueueMappingItem(domain.name, index, mappedIndex)
                        }
                    )
                }
            }
            true
        } catch (e: Exception) {
            L.e(e, "Unable to replace $domain playback state")
            false
        }
    }
}
