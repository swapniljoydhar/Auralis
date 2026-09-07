/*
 * Copyright (c) 2024 Auralis Contributors
 * ExoPlaybackStateHolder.kt is part of Auralis.
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
 
package com.auralis.player.playback.service

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.MediaSource
import com.auralis.musikr.MusicParent
import com.auralis.musikr.Song
import com.auralis.player.audiobooks.AudiobookCatalog
import com.auralis.player.audiobooks.AudiobookPlaybackController
import com.auralis.player.audiobooks.AudiobookProgressRepository
import com.auralis.player.audiobooks.AudiobookSettings
import com.auralis.player.audiobooks.EmbeddedChapterReader
import com.auralis.player.image.ImageSettings
import com.auralis.player.music.MusicRepository
import com.auralis.player.playback.PlaybackSettings
import com.auralis.player.playback.persist.PersistenceRepository
import com.auralis.player.playback.replaygain.ReplayGainAudioProcessor
import com.auralis.player.playback.state.DeferredPlayback
import com.auralis.player.playback.state.PlaybackCommand
import com.auralis.player.playback.state.PlaybackDomain
import com.auralis.player.playback.state.PlaybackStateHolder
import com.auralis.player.playback.state.PlaybackStateManager
import com.auralis.player.playback.state.Progression
import com.auralis.player.playback.state.RawQueue
import com.auralis.player.playback.state.RepeatMode
import com.auralis.player.playback.state.ShuffleMode
import com.auralis.player.playback.state.StateAck
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber as L

@OptIn(UnstableApi::class)
class ExoPlaybackStateHolder(
    private val context: Context,
    private val player: ExoPlayer,
    private val playbackManager: PlaybackStateManager,
    private val persistenceRepository: PersistenceRepository,
    private val audiobookProgressRepository: AudiobookProgressRepository,
    private val embeddedChapterReader: EmbeddedChapterReader,
    private val audiobookPlaybackController: AudiobookPlaybackController,
    private val audiobookSettings: AudiobookSettings,
    private val playbackSettings: PlaybackSettings,
    private val commandFactory: PlaybackCommand.Factory,
    private val replayGainProcessor: ReplayGainAudioProcessor,
    private val musicRepository: MusicRepository,
    private val imageSettings: ImageSettings,
) :
    PlaybackStateHolder,
    Player.Listener,
    MusicRepository.UpdateListener,
    PlaybackSettings.Listener,
    ImageSettings.Listener,
    AudiobookSettings.Listener {
    private val saveJob = Job()
    private val saveScope = CoroutineScope(Dispatchers.IO + saveJob)
    private val restoreScope = CoroutineScope(Dispatchers.IO + saveJob)
    @Volatile private var currentSaveJob: Job? = null
    private var openAudioEffectSession = false
    private var consecutiveErrors = 0
    private val pendingAudiobookProgress = mutableListOf<AudiobookProgressSnapshot>()
    private var pausedAudiobookPositionMs: Long? = null
    private var pausedTimestampMs: Long = 0L
    private var activeDomain = PlaybackDomain.MUSIC

    var sessionOngoing = false
        private set

    fun attach() {
        playbackManager.registerStateHolder(this)
        musicRepository.addUpdateListener(this)
        player.addListener(this)
        replayGainProcessor.attach()
        playbackSettings.registerListener(this)
        imageSettings.registerListener(this)
        audiobookSettings.registerListener(this)
        applyAudiobookAudioSettings()
    }

    fun release() {
        currentSaveJob?.cancel()
        val currentMediaItem = player.currentMediaItem
        val currentPosition = player.currentPosition
        // Drain any pending audiobook progress snapshots before saving.
        // Must synchronize since onPositionDiscontinuity may still be writing.
        val snapshots =
            synchronized(pendingAudiobookProgress) {
                pendingAudiobookProgress.toList().also { pendingAudiobookProgress.clear() }
            }
        if (sessionOngoing || snapshots.isNotEmpty() || currentMediaItem != null) {
            // The service contract requires persistence before teardown, so block the
            // caller briefly. The work is a couple of Room writes (chapter metadata is
            // cached), strictly bounded so release can never stall teardown.
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(RELEASE_SAVE_TIMEOUT_MS) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        for (snapshot in snapshots) {
                            saveAudiobookProgress(
                                snapshot.mediaItem,
                                snapshot.positionMs,
                                snapshot.domain,
                            )
                        }
                        saveAudiobookProgress(currentMediaItem, currentPosition, activeDomain)
                        if (sessionOngoing) {
                            persistenceRepository.saveState(playbackManager.toSavedState())
                        }
                    }
                }
            }
        }
        saveJob.cancel()
        playbackManager.unregisterStateHolder(this)
        musicRepository.removeUpdateListener(this)
        player.removeListener(this)
        replayGainProcessor.release()
        imageSettings.unregisterListener(this)
        playbackSettings.unregisterListener(this)
        audiobookSettings.unregisterListener(this)
        player.release()
    }

    override var parent: MusicParent? = null
        private set

    override val progression: Progression
        get() {
            if (player.currentMediaItem == null) return Progression.nil()
            val duration = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
            val clampedPosition = player.currentPosition.coerceIn(0L, duration)
            return Progression.from(
                player.playWhenReady,
                player.isPlaying,
                clampedPosition,
                player.playbackParameters.speed,
            )
        }

    override val repeatMode
        get() =
            when (val repeatMode = player.repeatMode) {
                Player.REPEAT_MODE_OFF -> RepeatMode.NONE
                Player.REPEAT_MODE_ONE -> RepeatMode.TRACK
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> throw IllegalStateException("Unknown repeat mode: $repeatMode")
            }

    override val audioSessionId: Int
        get() = player.audioSessionId

    override fun resolveQueue(): RawQueue {
        val library =
            musicRepository.library
                // No library, cannot do anything.
                ?: return RawQueue(emptyList(), emptyList(), 0)
        val heap = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        val shuffledMapping =
            if (player.shuffleModeEnabled) {
                player.unscrambleQueueIndices()
            } else {
                emptyList()
            }
        return RawQueue(heap.mapNotNull { it.song }, shuffledMapping, player.currentMediaItemIndex)
    }

    override fun handleDeferred(action: DeferredPlayback): Boolean {
        val library =
            musicRepository.library?.takeIf { !it.empty() }
                // No library, cannot do anything.
                ?: return false

        when (action) {
            // Restore state -> Start a new restoreState job
            is DeferredPlayback.RestoreState -> {
                L.d("Restoring playback state")
                restoreScope.launch {
                    val state = persistenceRepository.readState(playbackManager.domain)
                    withContext(Dispatchers.Main) {
                        if (state != null) {
                            // Apply the saved state on the main thread to prevent code expecting
                            // state updates on the main thread from crashing.
                            playbackManager.applySavedState(state, false)
                            if (action.play) {
                                playbackManager.playing(true)
                            }
                        } else if (action.fallback != null) {
                            playbackManager.playDeferred(action.fallback)
                        }
                    }
                }
            }
            // Shuffle all -> Start new playback from all songs
            is DeferredPlayback.ShuffleAll -> {
                L.d("Shuffling all tracks")
                playbackManager.play(
                    requireNotNull(commandFactory.all(ShuffleMode.ON)) {
                        "Invalid playback parameters"
                    }
                )
            }
            // Open -> Try to find the Song for the given file and then play it from all songs
            is DeferredPlayback.Open -> {
                L.d("Opening specified file")
                // Resolving runs off-thread: provider queries and library scans must never
                // block the caller, and foreign providers may omit the expected columns.
                restoreScope.launch {
                    val song =
                        library.songs.firstOrNull { it.uri == action.uri }
                            ?: findSongByDisplayAttributes(action.uri)
                    val command = song?.let { commandFactory.songFromAll(it, ShuffleMode.IMPLICIT) }
                    withContext(Dispatchers.Main) {
                        if (command != null) {
                            playbackManager.play(command)
                        } else {
                            L.w("Opened file is not in the library, ignoring ${action.uri}")
                        }
                    }
                }
            }
        }

        return true
    }

    private fun findSongByDisplayAttributes(uri: android.net.Uri): Song? {
        val cursor =
            runCatching {
                    context.applicationContext.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                        null,
                        null,
                        null,
                    )
                }
                .getOrNull() ?: return null
        return cursor.use {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (displayNameIndex < 0 || sizeIndex < 0 || !it.moveToFirst()) return null
            val displayName = it.getString(displayNameIndex)
            val size = it.getLong(sizeIndex)
            musicRepository.library?.songs?.find { song ->
                song.path.name == displayName && song.size == size
            }
        }
    }

    override fun playing(playing: Boolean) {
        player.playWhenReady = playing
    }

    override fun seekTo(positionMs: Long) {
        pausedAudiobookPositionMs = null
        player.seekTo(positionMs.coerceAtLeast(0L))
        deferSave()
        // Ack handled w/ExoPlayer events
    }

    override fun playbackSpeed(speed: Float) {
        val coerced = speed.coerceIn(0.5f, 3.0f)
        player.setPlaybackSpeed(coerced)
        if (activeDomain == PlaybackDomain.AUDIOBOOKS) {
            // Remember the listener's pace so it survives restarts and domain switches,
            // keeping a separate pace per book like dedicated audiobook players do.
            val bookKey = player.currentMediaItem?.song?.let(AudiobookCatalog::bookKey)
            if (bookKey != null) {
                audiobookSettings.recordBookSpeed(bookKey, coerced)
            } else {
                audiobookSettings.recordPlaybackSpeed(coerced)
            }
        }
        deferSave()
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun repeatMode(repeatMode: RepeatMode) {
        player.repeatMode =
            when (repeatMode) {
                RepeatMode.NONE -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.TRACK -> Player.REPEAT_MODE_ONE
            }
        updatePauseOnRepeat()
        playbackManager.ack(this, StateAck.RepeatModeChanged)
        deferSave()
    }

    override fun newPlayback(command: PlaybackCommand) {
        pausedAudiobookPositionMs = null
        parent = command.parent
        player.shuffleModeEnabled = command.shuffled
        persistOutgoingAudiobookProgress()
        activeDomain = command.domain
        audiobookPlaybackController.onNewPlayback(activeDomain)
        player.setMediaItems(command.queue.map { it.buildMediaItem() })
        playbackManager.playbackSpeed(
            if (command.domain == PlaybackDomain.AUDIOBOOKS) {
                // Resume each book at its own remembered pace.
                command.queue
                    .firstOrNull()
                    ?.let(AudiobookCatalog::bookKey)
                    ?.let(audiobookSettings::speedForBook) ?: audiobookSettings.lastPlaybackSpeed
            } else {
                1.0f
            }
        )
        player.volume = 1f
        val startIndex =
            command.song?.let { command.queue.indexOf(it).takeIf { index -> index >= 0 } }
        if (command.shuffled) {
            player.setShuffleOrder(BetterShuffleOrder(command.queue.size, startIndex ?: -1))
        }
        val target = startIndex ?: player.currentTimeline.getFirstWindowIndex(command.shuffled)
        val startPosition = command.startPositionMs.takeIf { it > 0L } ?: C.TIME_UNSET
        player.seekTo(target, startPosition)
        player.prepare()
        player.play()
        playbackManager.ack(this, StateAck.NewPlayback)
        deferSave()
    }

    override fun shuffled(shuffled: Boolean) {
        player.setShuffleModeEnabled(shuffled)
        if (player.shuffleModeEnabled) {
            // Have to manually refresh the shuffle seed and anchor it to the new current songs
            player.setShuffleOrder(
                BetterShuffleOrder(player.mediaItemCount, player.currentMediaItemIndex)
            )
        }
        playbackManager.ack(this, StateAck.QueueReordered)
        deferSave()
    }

    override fun next() {
        // Replicate the old pseudo-circular queue behavior when no repeat option is implemented.
        // Basically, you can't skip back and wrap around the queue, but you can skip forward and
        // wrap around the queue, albeit playback will be paused.
        if (player.repeatMode == Player.REPEAT_MODE_ALL || player.hasNextMediaItem()) {
            player.seekToNext()
            if (!playbackSettings.rememberPause) {
                player.play()
            }
        } else {
            player.seekTo(
                player.currentTimeline.getFirstWindowIndex(player.shuffleModeEnabled),
                C.TIME_UNSET,
            )
            // TODO: Dislike the UX implications of this, I feel should I bite the bullet
            //  and switch to dynamic skip enable/disable?
            if (!playbackSettings.rememberPause) {
                player.pause()
            }
        }
        playbackManager.ack(this, StateAck.IndexMoved)
        deferSave()
    }

    override fun prev() {
        if (playbackSettings.rewindWithPrev) {
            player.seekToPrevious()
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
        if (!playbackSettings.rememberPause) {
            player.play()
        }
        playbackManager.ack(this, StateAck.IndexMoved)
        deferSave()
    }

    override fun goto(index: Int, positionMs: Long?) {
        val indices = player.unscrambleQueueIndices()
        if (indices.isEmpty() || index !in indices.indices) {
            return
        }

        val trueIndex = indices[index]
        // A combined window+position seek is atomic in ExoPlayer; issuing the position
        // separately could land on the outgoing item before the jump executes.
        player.seekTo(trueIndex, positionMs?.coerceAtLeast(0L) ?: C.TIME_UNSET)
        if (!playbackSettings.rememberPause) {
            player.play()
        }
        playbackManager.ack(this, StateAck.IndexMoved)
        deferSave()
    }

    override fun playNext(songs: List<Song>, ack: StateAck.PlayNext) {
        val currTimeline = player.currentTimeline
        val nextIndex =
            if (currTimeline.isEmpty) {
                C.INDEX_UNSET
            } else {
                currTimeline.getNextWindowIndex(
                    player.currentMediaItemIndex,
                    Player.REPEAT_MODE_OFF,
                    player.shuffleModeEnabled,
                )
            }

        if (nextIndex == C.INDEX_UNSET) {
            player.addMediaItems(songs.map { it.buildMediaItem() })
        } else {
            player.addMediaItems(nextIndex, songs.map { it.buildMediaItem() })
        }
        playbackManager.ack(this, ack)
        deferSave()
    }

    override fun addToQueue(songs: List<Song>, ack: StateAck.AddToQueue) {
        player.addMediaItems(songs.map { it.buildMediaItem() })
        playbackManager.ack(this, ack)
        deferSave()
    }

    override fun move(from: Int, to: Int, ack: StateAck.Move) {
        val indices = player.unscrambleQueueIndices()
        if (indices.isEmpty() || from !in indices.indices || to !in indices.indices) {
            return
        }

        val trueFrom = indices[from]
        val trueTo = indices[to]
        // ExoPlayer does not actually update it's ShuffleOrder when moving items. Retain a
        // semblance of "normalcy" by doing a weird no-op swap that actually moves the item.
        when {
            trueFrom > trueTo -> {
                player.moveMediaItem(trueFrom, trueTo)
                player.moveMediaItem(trueTo + 1, trueFrom)
            }
            trueTo > trueFrom -> {
                player.moveMediaItem(trueFrom, trueTo)
                player.moveMediaItem(trueTo - 1, trueFrom)
            }
        }
        playbackManager.ack(this, ack)
        deferSave()
    }

    override fun remove(at: Int, ack: StateAck.Remove) {
        val indices = player.unscrambleQueueIndices()
        if (indices.isEmpty() || at !in indices.indices) {
            return
        }

        val trueIndex = indices[at]
        val songWillChange = player.currentMediaItemIndex == trueIndex
        player.removeMediaItem(trueIndex)
        if (songWillChange && !playbackSettings.rememberPause) {
            player.play()
        }
        playbackManager.ack(this, ack)
        deferSave()
    }

    override fun applySavedState(
        parent: MusicParent?,
        rawQueue: RawQueue,
        positionMs: Long,
        repeatMode: RepeatMode,
        ack: StateAck.NewPlayback?,
    ) {
        var sendNewPlaybackEvent = false
        var shouldSeek = false
        val restoredDomain = playbackManager.domain
        if (this.parent != parent) {
            this.parent = parent
            sendNewPlaybackEvent = true
        }
        if (rawQueue != resolveQueue()) {
            persistOutgoingAudiobookProgress()
            activeDomain = restoredDomain
            audiobookPlaybackController.onPlaybackDomainChanged(activeDomain)
            player.setMediaItems(rawQueue.heap.map { it.buildMediaItem() })
            if (rawQueue.isShuffled) {
                player.shuffleModeEnabled = true
                player.setShuffleOrder(BetterShuffleOrder(rawQueue.shuffledMapping.toIntArray()))
            } else {
                player.shuffleModeEnabled = false
            }
            player.seekTo(rawQueue.heapIndex, C.TIME_UNSET)
            player.prepare()
            player.pause()
            sendNewPlaybackEvent = true
            shouldSeek = true
        }

        activeDomain = restoredDomain
        audiobookPlaybackController.onPlaybackDomainChanged(activeDomain)

        repeatMode(repeatMode)
        // See if we differ by more than a second. This allows us to avoid a meaningless seek
        // in the case of a "tight restore" (i.e music was reloaded).
        // In the case that this is a false positive, it's not very percievable (at least compared
        // to skipping when updating the library).
        // TODO: Introduce a better state management system rather than do something finicky like
        // this.
        if (shouldSeek || abs(player.currentPosition - positionMs) > 1000L) {
            player.seekTo(positionMs)
        }

        val restoredSong = rawQueue.heap.getOrNull(rawQueue.heapIndex)
        playbackManager.playbackSpeed(
            if (restoredSong != null && activeDomain == PlaybackDomain.AUDIOBOOKS) {
                audiobookSettings.lastPlaybackSpeed
            } else {
                1.0f
            }
        )

        if (sendNewPlaybackEvent) {
            ack?.let { playbackManager.ack(this, it) }
        }
    }

    override fun saveSnapshot() {
        val snapshot = playbackManager.toSavedState()
        val currentMediaItem = player.currentMediaItem
        val currentPosition = player.currentPosition
        val currentDomain = activeDomain
        saveScope.launch {
            if (sessionOngoing) {
                persistenceRepository.saveState(snapshot)
            }
            savePendingAudiobookProgress()
            saveAudiobookProgress(currentMediaItem, currentPosition, currentDomain)
        }
    }

    override fun endSession() {
        // This session has ended, so we need to reset this flag for when the next
        // session starts.
        player.stop()
        player.clearMediaItems()
        audiobookPlaybackController.onPlaybackEnded(activeDomain)
        playbackManager.playing(false)
        save {
            // User could feasibly start playing again if they were fast enough, so
            // we need to avoid stopping the foreground state if that's the case.
            if (!playbackManager.progression.isPlaying) {
                sessionOngoing = false
                playbackManager.ack(this, StateAck.SessionEnded)
            }
        }
    }

    override fun reset(ack: StateAck.NewPlayback) {
        player.setMediaItems(listOf())
        playbackManager.ack(this, ack)
        deferSave()
    }

    // --- PLAYER OVERRIDES ---

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)

        val currentSong = player.currentMediaItem?.song
        if (
            !playWhenReady &&
                sessionOngoing &&
                currentSong != null &&
                activeDomain == PlaybackDomain.AUDIOBOOKS
        ) {
            pausedAudiobookPositionMs = player.currentPosition
            pausedTimestampMs = System.currentTimeMillis()
        } else if (playWhenReady) {
            val pausedPosition = pausedAudiobookPositionMs
            if (
                pausedPosition != null &&
                    currentSong != null &&
                    activeDomain == PlaybackDomain.AUDIOBOOKS
            ) {
                val configuredRewind = audiobookSettings.autoRewindMs
                val elapsedPauseMs = System.currentTimeMillis() - pausedTimestampMs
                val rewindMs =
                    when {
                        configuredRewind <= 0L -> 0L
                        elapsedPauseMs < 20_000L -> 0L
                        elapsedPauseMs < 5 * 60_000L ->
                            (configuredRewind / 2)
                                .coerceAtLeast(3000L)
                                .coerceAtMost(configuredRewind)
                        else -> configuredRewind
                    }
                if (rewindMs > 0L) {
                    player.seekTo((pausedPosition - rewindMs).coerceAtLeast(0L))
                }
            }
            pausedAudiobookPositionMs = null
        }

        if (player.playWhenReady) {
            // Mark that we have started playing so that the notification can now be posted.
            L.d("Player has started playing")
            sessionOngoing = true
            if (!openAudioEffectSession) {
                // Convention to start an audioeffect session on play/pause rather than
                // start/stop
                L.d("Opening audio effect session")
                broadcastAudioEffectAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                openAudioEffectSession = true
            }
        } else {
            val currentMediaItem = player.currentMediaItem
            val currentPosition = player.currentPosition
            val currentDomain = activeDomain
            saveJob { saveAudiobookProgress(currentMediaItem, currentPosition, currentDomain) }
            if (openAudioEffectSession) {
                // Make sure to close the audio session when we stop playback.
                L.d("Closing audio effect session")
                broadcastAudioEffectAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
                openAudioEffectSession = false
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        if (playbackState == Player.STATE_READY) {
            consecutiveErrors = 0
        }
        if (playbackState == Player.STATE_ENDED && player.repeatMode == Player.REPEAT_MODE_OFF) {
            audiobookPlaybackController.onPlaybackEnded(activeDomain)
            goto(0)
            player.pause()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        applyAudiobookAudioSettings()
        pausedAudiobookPositionMs = null

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            audiobookPlaybackController.onAutomaticChapterTransition(activeDomain)
            playbackManager.ack(this, StateAck.IndexMoved)
            deferSave()
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        audiobookPlaybackController.onPlaybackPositionChanged(
            activeDomain,
            newPosition.mediaItem?.song,
            newPosition.positionMs,
        )

        if (oldPosition.mediaItemIndex != newPosition.mediaItemIndex) {
            synchronized(pendingAudiobookProgress) {
                pendingAudiobookProgress +=
                    AudiobookProgressSnapshot(
                        oldPosition.mediaItem,
                        oldPosition.positionMs,
                        activeDomain,
                    )
            }
            deferSave()
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)

        // So many actions trigger progression changes that it becomes easier just to handle it
        // in an ExoPlayer callback anyway. This doesn't really cause issues anywhere.
        if (
            events.containsAny(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
            )
        ) {
            audiobookPlaybackController.onPlaybackStateChanged(
                activeDomain,
                player.currentMediaItem?.song,
                player.currentPosition,
                player.isPlaying,
            )
            L.d("Player state changed, must synchronize state")
            playbackManager.ack(this, StateAck.ProgressionChanged)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        L.e("Player error occurred: ${error.errorCodeName}")
        consecutiveErrors++
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            // Every item is failing (storage revoked, files moved, corrupt media). Stop
            // advancing so the player does not hot-loop through the queue forever.
            L.e("Too many consecutive playback errors, pausing instead of skipping")
            consecutiveErrors = 0
            player.pause()
            return
        }
        player.prepare()
        playbackManager.next()
    }

    private fun broadcastAudioEffectAction(event: String) {
        L.d("Broadcasting AudioEffect event: $event")
        context.sendBroadcast(
            Intent(event)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        )
    }

    // --- MUSICREPOSITORY METHODS ---

    override fun onMusicChanges(changes: MusicRepository.Changes) {
        if (changes.deviceLibrary && musicRepository.library?.takeIf { !it.empty() } != null) {
            // We now have a library, see if we have anything we need to do.
            L.d("Library obtained, requesting action")
            playbackManager.requestAction(this)
        }
    }

    // --- PLAYBACKSETTINGS OVERRIDES ---

    override fun onAudiobookAssignmentsChanged() {}

    override fun onAudiobookPlaybackSettingsChanged() {
        applyAudiobookAudioSettings()
    }

    private fun applyAudiobookAudioSettings() {
        player.skipSilenceEnabled =
            activeDomain == PlaybackDomain.AUDIOBOOKS && audiobookSettings.skipSilence
        if (activeDomain == PlaybackDomain.AUDIOBOOKS) {
            // Queues can span books (e.g. continuous play); each new book resumes
            // at its own remembered pace instead of inheriting the previous one.
            val song = player.currentMediaItem?.song ?: return
            val speed = audiobookSettings.speedForBook(AudiobookCatalog.bookKey(song))
            if (abs(player.playbackParameters.speed - speed) > 0.001f) {
                player.setPlaybackSpeed(speed)
            }
        }
    }

    override fun onPauseOnRepeatChanged() {
        super.onPauseOnRepeatChanged()
        updatePauseOnRepeat()
    }

    private fun updatePauseOnRepeat() {
        player.pauseAtEndOfMediaItems =
            player.repeatMode == Player.REPEAT_MODE_ONE && playbackSettings.pauseOnRepeat
    }

    private fun save(cb: () -> Unit) {
        val currentMediaItem = player.currentMediaItem
        val currentPosition = player.currentPosition
        val currentDomain = activeDomain
        saveJob {
            if (sessionOngoing) {
                persistenceRepository.saveState(playbackManager.toSavedState())
            }
            savePendingAudiobookProgress()
            saveAudiobookProgress(currentMediaItem, currentPosition, currentDomain)
            withContext(Dispatchers.Main) { cb() }
        }
    }

    private fun deferSave() {
        val currentMediaItem = player.currentMediaItem
        val currentPosition = player.currentPosition
        val currentDomain = activeDomain
        saveJob {
            L.d("Waiting for save buffer")
            delay(SAVE_BUFFER)
            yield()
            L.d("Committing saved state")
            if (sessionOngoing) {
                persistenceRepository.saveState(playbackManager.toSavedState())
            }
            savePendingAudiobookProgress()
            saveAudiobookProgress(currentMediaItem, currentPosition, currentDomain)
        }
    }

    private suspend fun savePendingAudiobookProgress() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            while (true) {
                val snapshot =
                    synchronized(pendingAudiobookProgress) {
                        pendingAudiobookProgress.removeFirstOrNull()
                    } ?: return@withContext
                saveAudiobookProgress(snapshot.mediaItem, snapshot.positionMs, snapshot.domain)
            }
        }
    }

    private fun persistOutgoingAudiobookProgress() {
        val outgoingMediaItem = player.currentMediaItem
        val outgoingPosition = player.currentPosition
        val outgoingDomain = activeDomain
        if (outgoingDomain != PlaybackDomain.AUDIOBOOKS) return
        saveScope.launch {
            saveAudiobookProgress(outgoingMediaItem, outgoingPosition, outgoingDomain)
        }
    }

    private suspend fun saveAudiobookProgress(
        mediaItem: MediaItem?,
        positionMs: Long,
        domain: PlaybackDomain,
    ) {
        val song = mediaItem?.song ?: return
        if (domain != PlaybackDomain.AUDIOBOOKS) return

        audiobookProgressRepository.save(
            bookKey = AudiobookCatalog.bookKey(song),
            chapterUid = song.uid,
            positionMs = positionMs,
            durationMs = song.durationMs,
            embeddedChapterStartMs =
                embeddedChapterReader.read(song).lastOrNull { it.startMs <= positionMs }?.startMs,
        )
    }

    private data class AudiobookProgressSnapshot(
        val mediaItem: MediaItem?,
        val positionMs: Long,
        val domain: PlaybackDomain,
    )

    private fun saveJob(block: suspend () -> Unit) {
        currentSaveJob?.let {
            L.d("Discarding prior save job")
            it.cancel()
        }
        currentSaveJob = saveScope.launch { block() }
    }

    private fun Song.buildMediaItem() = MediaItem.Builder().setUri(uri).setTag(this).build()

    private val MediaItem.song: Song?
        get() = this.localConfiguration?.tag as? Song?

    private fun Player.unscrambleQueueIndices(): List<Int> {
        val timeline = currentTimeline
        if (timeline.isEmpty) {
            return emptyList()
        }
        val queue = mutableListOf<Int>()

        // Add the active queue item.
        val currentMediaItemIndex = currentMediaItemIndex
        queue.add(currentMediaItemIndex)

        // Fill queue alternating with next and/or previous queue items.
        var firstMediaItemIndex = currentMediaItemIndex
        var lastMediaItemIndex = currentMediaItemIndex
        val shuffleModeEnabled = shuffleModeEnabled
        while ((firstMediaItemIndex != C.INDEX_UNSET || lastMediaItemIndex != C.INDEX_UNSET)) {
            // Begin with next to have a longer tail than head if an even sized queue needs to be
            // trimmed.
            if (lastMediaItemIndex != C.INDEX_UNSET) {
                lastMediaItemIndex =
                    timeline.getNextWindowIndex(
                        lastMediaItemIndex,
                        Player.REPEAT_MODE_OFF,
                        shuffleModeEnabled,
                    )
                if (lastMediaItemIndex != C.INDEX_UNSET) {
                    queue.add(lastMediaItemIndex)
                }
            }
            if (firstMediaItemIndex != C.INDEX_UNSET) {
                firstMediaItemIndex =
                    timeline.getPreviousWindowIndex(
                        firstMediaItemIndex,
                        Player.REPEAT_MODE_OFF,
                        shuffleModeEnabled,
                    )
                if (firstMediaItemIndex != C.INDEX_UNSET) {
                    queue.add(0, firstMediaItemIndex)
                }
            }
        }

        return queue
    }

    class Factory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playbackManager: PlaybackStateManager,
        private val persistenceRepository: PersistenceRepository,
        private val audiobookProgressRepository: AudiobookProgressRepository,
        private val embeddedChapterReader: EmbeddedChapterReader,
        private val audiobookPlaybackController: AudiobookPlaybackController,
        private val audiobookSettings: AudiobookSettings,
        private val playbackSettings: PlaybackSettings,
        private val commandFactory: PlaybackCommand.Factory,
        private val mediaSourceFactory: MediaSource.Factory,
        private val replayGainProcessor: ReplayGainAudioProcessor,
        private val musicRepository: MusicRepository,
        private val imageSettings: ImageSettings,
    ) {
        fun create(): ExoPlaybackStateHolder {
            // Since Auralis is a music player, only specify an audio renderer to save
            // battery/apk size/cache size]
            val audioRenderer = RenderersFactory { handler, _, audioListener, _, _ ->
                FfmpegRendererCompat.createAudioRenderers(
                    context,
                    handler,
                    audioListener,
                    replayGainProcessor,
                )
            }

            val exoPlayer =
                ExoPlayer.Builder(context, audioRenderer)
                    .setMediaSourceFactory(mediaSourceFactory)
                    // Enable automatic WakeLock support
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .setAudioAttributes(
                        // Signal that we are a music player.
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    )
                    .build()

            return ExoPlaybackStateHolder(
                context,
                exoPlayer,
                playbackManager,
                persistenceRepository,
                audiobookProgressRepository,
                embeddedChapterReader,
                audiobookPlaybackController,
                audiobookSettings,
                playbackSettings,
                commandFactory,
                replayGainProcessor,
                musicRepository,
                imageSettings,
            )
        }
    }

    private companion object {
        const val SAVE_BUFFER = 5000L
        const val RELEASE_SAVE_TIMEOUT_MS = 2000L
        const val MAX_CONSECUTIVE_ERRORS = 3
    }
}
