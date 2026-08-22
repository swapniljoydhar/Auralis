/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookPlaybackController.kt is part of Auralis.
 *
 * Auralis is a derivative work of the Auxio Project and incorporates
 * audiobook-oriented work inspired by Voice. Original copyright and GPL
 * attribution are retained in PROVENANCE.md and the repository history.
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

import com.auralis.player.playback.state.PlaybackDomain
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
    private var scheduledDomain: PlaybackDomain? = null
    private var sleepAtChapterEnd = false

    val isSleepTimerActive: Boolean
        get() = scheduledDomain == PlaybackDomain.AUDIOBOOKS

    fun scheduleSleepTimer(durationMs: Long) {
        val domain = playbackManager.domain
        if (domain != PlaybackDomain.AUDIOBOOKS) return
        cancelSleepTimer()
        scheduledDomain = domain
        sleepTimerJob =
            scope.launch {
                delay(durationMs.coerceAtLeast(0L))
                if (scheduledDomain == domain && playbackManager.domain == domain) {
                    playbackManager.playing(false)
                }
                clearSleepTimer()
            }
    }

    fun scheduleSleepAtChapterEnd() {
        if (
            playbackManager.domain != PlaybackDomain.AUDIOBOOKS ||
                playbackManager.currentSong == null
        )
            return
        cancelSleepTimer()
        scheduledDomain = PlaybackDomain.AUDIOBOOKS
        sleepAtChapterEnd = true
    }

    /** Called by the player service only for an automatic chapter-file transition. */
    fun onAutomaticChapterTransition(domain: PlaybackDomain) {
        if (sleepAtChapterEnd && scheduledDomain == domain && domain == PlaybackDomain.AUDIOBOOKS) {
            playbackManager.playing(false)
            clearSleepTimer()
        }
    }

    /** Prevent a timer created by an audiobook session from affecting another mode or session. */
    fun onPlaybackDomainChanged(domain: PlaybackDomain) {
        if (scheduledDomain != null && scheduledDomain != domain) cancelSleepTimer()
    }

    fun onNewPlayback(domain: PlaybackDomain) {
        if (scheduledDomain != null) cancelSleepTimer()
        onPlaybackDomainChanged(domain)
    }

    fun onPlaybackEnded(domain: PlaybackDomain) {
        if (sleepAtChapterEnd && scheduledDomain == domain) clearSleepTimer()
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        clearSleepTimer()
    }

    private fun clearSleepTimer() {
        sleepTimerJob = null
        scheduledDomain = null
        sleepAtChapterEnd = false
    }
}
