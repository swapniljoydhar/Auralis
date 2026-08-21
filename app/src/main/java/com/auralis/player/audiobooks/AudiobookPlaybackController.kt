/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookPlaybackController.kt is part of Auralis.
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

import com.auralis.player.playback.state.PlaybackStateManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class AudiobookPlaybackController
@Inject
constructor(private val playbackManager: PlaybackStateManager) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null

    fun scheduleSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        sleepTimerJob =
            scope.launch {
                delay(durationMs.coerceAtLeast(0L))
                playbackManager.playing(false)
                sleepTimerJob = null
            }
    }

    fun scheduleSleepAtChapterEnd() {
        val song = playbackManager.currentSong ?: return
        val remainingMs =
            (song.durationMs - playbackManager.progression.calculateElapsedPositionMs())
                .coerceAtLeast(0L)
        scheduleSleepTimer(remainingMs)
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }
}
