/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookPlaybackController.kt is part of Auralis.
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
import org.oxycblt.musikr.Song

@Singleton
class AudiobookPlaybackController
@Inject
constructor(
    private val playbackManager: PlaybackStateManager,
    private val embeddedChapterReader: EmbeddedChapterReader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null
    private var scheduledDomain: PlaybackDomain? = null
    private var sleepAtChapterEnd = false
    private var embeddedSleepBoundary: EmbeddedSleepBoundary? = null

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
                if (
                    AudiobookSleepTimerPolicy.shouldPause(scheduledDomain, playbackManager.domain)
                ) {
                    playbackManager.playing(false)
                }
                clearSleepTimer()
            }
    }

    fun scheduleSleepAtChapterEnd() {
        val song = playbackManager.currentSong ?: return
        if (playbackManager.domain != PlaybackDomain.AUDIOBOOKS) return
        cancelSleepTimer()
        scheduledDomain = PlaybackDomain.AUDIOBOOKS
        sleepAtChapterEnd = true
        scope.launch {
            val chapters = embeddedChapterReader.read(song)
            if (
                chapters.isEmpty() ||
                    scheduledDomain != PlaybackDomain.AUDIOBOOKS ||
                    playbackManager.domain != PlaybackDomain.AUDIOBOOKS ||
                    playbackManager.currentSong?.uid != song.uid
            )
                return@launch
            embeddedSleepBoundary = EmbeddedSleepBoundary(song, chapters)
            refreshEmbeddedSleepBoundary(playbackManager.progression.calculateElapsedPositionMs())
        }
    }

    /** Called by the player service only for an automatic chapter-file transition. */
    fun onAutomaticChapterTransition(domain: PlaybackDomain) {
        if (sleepAtChapterEnd && scheduledDomain == domain && domain == PlaybackDomain.AUDIOBOOKS) {
            playbackManager.playing(false)
            clearSleepTimer()
        }
    }

    fun onPlaybackPositionChanged(domain: PlaybackDomain, song: Song?, positionMs: Long) {
        if (domain != PlaybackDomain.AUDIOBOOKS || scheduledDomain != domain) return
        val boundary = embeddedSleepBoundary ?: return
        if (song?.uid != boundary.song.uid) return
        refreshEmbeddedSleepBoundary(positionMs)
    }

    fun onPlaybackStateChanged(
        domain: PlaybackDomain,
        song: Song?,
        positionMs: Long,
        isPlaying: Boolean,
    ) {
        if (domain != PlaybackDomain.AUDIOBOOKS || scheduledDomain != domain) return
        val boundary = embeddedSleepBoundary ?: return
        if (song?.uid != boundary.song.uid) return
        if (isPlaying) {
            refreshEmbeddedSleepBoundary(positionMs)
        } else {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
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
        embeddedSleepBoundary = null
    }

    private fun refreshEmbeddedSleepBoundary(positionMs: Long) {
        val boundary = embeddedSleepBoundary ?: return
        val nextStartMs =
            AudiobookSleepTimerPolicy.nextEmbeddedChapterStart(boundary.chapters, positionMs)
                ?: return
        sleepTimerJob?.cancel()
        sleepTimerJob =
            scope.launch {
                delay((nextStartMs - positionMs).coerceAtLeast(0L))
                val currentSong = playbackManager.currentSong
                if (
                    scheduledDomain == PlaybackDomain.AUDIOBOOKS &&
                        playbackManager.domain == PlaybackDomain.AUDIOBOOKS &&
                        currentSong?.uid == boundary.song.uid
                ) {
                    val currentPosition = playbackManager.progression.calculateElapsedPositionMs()
                    if (currentPosition >= nextStartMs) {
                        playbackManager.playing(false)
                        clearSleepTimer()
                    } else {
                        refreshEmbeddedSleepBoundary(currentPosition)
                    }
                }
            }
    }

    private data class EmbeddedSleepBoundary(val song: Song, val chapters: List<EmbeddedChapter>)
}

internal object AudiobookSleepTimerPolicy {
    fun shouldPause(scheduledDomain: PlaybackDomain?, activeDomain: PlaybackDomain) =
        scheduledDomain == PlaybackDomain.AUDIOBOOKS && activeDomain == PlaybackDomain.AUDIOBOOKS

    fun nextEmbeddedChapterStart(chapters: Collection<EmbeddedChapter>, positionMs: Long): Long? =
        chapters.asSequence().map(EmbeddedChapter::startMs).filter { it > positionMs }.minOrNull()
}
