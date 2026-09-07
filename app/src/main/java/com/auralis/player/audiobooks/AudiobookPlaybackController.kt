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

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.auralis.musikr.Song
import com.auralis.player.playback.state.PlaybackDomain
import com.auralis.player.playback.state.PlaybackStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AudiobookPlaybackController
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackStateManager,
    private val embeddedChapterReader: EmbeddedChapterReader,
    private val audiobookSettings: AudiobookSettings,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null
    private var scheduledDomain: PlaybackDomain? = null
    private var sleepAtChapterEnd = false
    private var embeddedSleepBoundary: EmbeddedSleepBoundary? = null
    private var lastScheduledDurationMs: Long = 0L
    private var plainTimerEndsAtElapsedMs: Long = 0L
    private var plainTimerRemainingMs: Long = 0L

    private val shakeDetector =
        ShakeDetector(context) {
            if (isSleepTimerActive && audiobookSettings.shakeToResetSleepTimer) {
                onShakeReset()
            }
        }

    val isSleepTimerActive: Boolean
        get() = scheduledDomain == PlaybackDomain.AUDIOBOOKS

    fun scheduleSleepTimer(durationMs: Long) {
        val domain = playbackManager.domain
        if (domain != PlaybackDomain.AUDIOBOOKS) return
        cancelSleepTimer()
        scheduledDomain = domain
        lastScheduledDurationMs = durationMs
        if (audiobookSettings.shakeToResetSleepTimer) {
            shakeDetector.start()
        }
        schedulePlainTimer(durationMs.coerceAtLeast(0L))
    }

    private fun schedulePlainTimer(remainingMs: Long) {
        plainTimerEndsAtElapsedMs = SystemClock.elapsedRealtime() + remainingMs
        val fadeMs = fadeDurationMs(remainingMs)
        sleepTimerJob =
            scope.launch {
                delay(remainingMs - fadeMs)
                runFadeOut(fadeMs)
                if (
                    AudiobookSleepTimerPolicy.shouldPause(scheduledDomain, playbackManager.domain)
                ) {
                    playbackManager.playing(false)
                }
                clearSleepTimer()
            }
    }

    /**
     * Ramps the output volume down over [fadeMs] so the sleep timer eases the listener out instead
     * of cutting off mid-word. Runs inline in the timer job so pausing or cancelling the timer
     * aborts the fade; every exit path restores full volume through [clearSleepTimer] or
     * [restoreVolume].
     */
    private suspend fun runFadeOut(fadeMs: Long) {
        if (fadeMs <= 0L) return
        val steps = (fadeMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
        val stepMs = fadeMs / steps
        repeat(steps) { step ->
            delay(stepMs)
            playbackManager.setVolume(1f - (step + 1) / steps.toFloat())
        }
    }

    private fun fadeDurationMs(remainingMs: Long): Long {
        if (!audiobookSettings.sleepFadeOut) return 0L
        return FADE_DURATION_MS.coerceAtMost(remainingMs.coerceAtLeast(0L))
    }

    private fun restoreVolume() {
        playbackManager.setVolume(1f)
    }

    fun scheduleSleepAtChapterEnd() {
        val song = playbackManager.currentSong ?: return
        if (playbackManager.domain != PlaybackDomain.AUDIOBOOKS) return
        cancelSleepTimer()
        scheduledDomain = PlaybackDomain.AUDIOBOOKS
        sleepAtChapterEnd = true
        lastScheduledDurationMs = 0L
        if (audiobookSettings.shakeToResetSleepTimer) {
            shakeDetector.start()
        }
        scope.launch {
            // Chapter parsing touches storage; never run it on the Main dispatcher.
            val chapters = withContext(Dispatchers.IO) { embeddedChapterReader.read(song) }
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

    private fun onShakeReset() {
        vibrateFeedback()
        if (lastScheduledDurationMs > 0L) {
            scheduleSleepTimer(lastScheduledDurationMs)
        } else if (sleepAtChapterEnd) {
            scheduleSleepAtChapterEnd()
        }
    }

    private fun vibrateFeedback() {
        try {
            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION") vibrator?.vibrate(120L)
            }
        } catch (_: Exception) {
            // Ignore if vibration is not supported or permission denied
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
        val boundary = embeddedSleepBoundary
        if (boundary != null && song?.uid == boundary.song.uid) {
            // Chapter-end timers are position-derived: drop the pending job while paused
            // and recompute it from the resume position.
            if (isPlaying) {
                refreshEmbeddedSleepBoundary(positionMs)
            } else {
                sleepTimerJob?.cancel()
                sleepTimerJob = null
                restoreVolume()
            }
            return
        }
        if (boundary == null && !sleepAtChapterEnd) {
            // Plain countdown timers freeze while paused and resume afterwards instead
            // of silently dying with the timer still showing as active.
            if (isPlaying) {
                if (sleepTimerJob == null && plainTimerRemainingMs > 0L) {
                    schedulePlainTimer(plainTimerRemainingMs)
                    plainTimerRemainingMs = 0L
                }
            } else {
                sleepTimerJob?.cancel()
                sleepTimerJob = null
                restoreVolume()
                plainTimerRemainingMs =
                    (plainTimerEndsAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            }
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
        restoreVolume()
        shakeDetector.stop()
        sleepTimerJob = null
        scheduledDomain = null
        sleepAtChapterEnd = false
        embeddedSleepBoundary = null
        lastScheduledDurationMs = 0L
        plainTimerEndsAtElapsedMs = 0L
        plainTimerRemainingMs = 0L
    }

    private fun refreshEmbeddedSleepBoundary(positionMs: Long) {
        val boundary = embeddedSleepBoundary ?: return
        val nextStartMs =
            AudiobookSleepTimerPolicy.nextEmbeddedChapterStart(boundary.chapters, positionMs)
                ?: return
        sleepTimerJob?.cancel()
        // A seek or speed change during the fade must not leave the volume lowered.
        restoreVolume()
        val speed = playbackManager.playbackSpeed.coerceAtLeast(0.1f)
        val remainingAudioMs = (nextStartMs - positionMs).coerceAtLeast(0L)
        val wallDelayMs = (remainingAudioMs / speed).toLong()
        val fadeMs = fadeDurationMs(wallDelayMs)
        sleepTimerJob =
            scope.launch {
                delay((wallDelayMs - fadeMs).coerceAtLeast(0L))
                runFadeOut(fadeMs)
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
                        // Drift (seek, speed change) moved the boundary: back to full
                        // volume and recompute from the fresh position.
                        restoreVolume()
                        refreshEmbeddedSleepBoundary(currentPosition)
                    }
                }
            }
    }

    private data class EmbeddedSleepBoundary(val song: Song, val chapters: List<EmbeddedChapter>)

    private companion object {
        const val FADE_DURATION_MS = 15_000L
        const val FADE_STEP_MS = 500L
    }
}

internal object AudiobookSleepTimerPolicy {
    fun shouldPause(scheduledDomain: PlaybackDomain?, activeDomain: PlaybackDomain) =
        scheduledDomain == PlaybackDomain.AUDIOBOOKS && activeDomain == PlaybackDomain.AUDIOBOOKS

    fun nextEmbeddedChapterStart(chapters: Collection<EmbeddedChapter>, positionMs: Long): Long? =
        chapters.asSequence().map(EmbeddedChapter::startMs).filter { it > positionMs }.minOrNull()
}
