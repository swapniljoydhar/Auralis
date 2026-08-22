/*
 * Copyright (c) 2026 Auralis Project
 * AudiobookSettings.kt is part of Auralis.
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
import androidx.core.content.edit
import com.auralis.player.R
import com.auralis.player.settings.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.oxycblt.musikr.Song

interface AudiobookSettings : Settings<AudiobookSettings.Listener> {
    val manualSongUids: Set<String>

    /** The configured skip interval for audiobook controls, in milliseconds. */
    val skipDurationMs: Long

    /** The configured speed applied when audiobook playback starts. */
    val defaultPlaybackSpeed: Float

    /**
     * The rewind interval applied when audiobook playback resumes after a pause, in milliseconds.
     */
    val autoRewindMs: Long

    /** Whether long silent sections should be skipped during audiobook playback. */
    val skipSilence: Boolean

    fun addSongs(songs: Collection<Song>)

    fun removeSongs(songs: Collection<Song>)

    interface Listener {
        fun onAudiobookAssignmentsChanged()

        fun onAudiobookPlaybackSettingsChanged() {}
    }
}

class AudiobookSettingsImpl @Inject constructor(@ApplicationContext context: Context) :
    Settings.Impl<AudiobookSettings.Listener>(context), AudiobookSettings {
    private val key = context.getString(R.string.set_key_audiobook_manual_uids)
    private val skipDurationKey = context.getString(R.string.set_key_audiobook_skip_duration)
    private val defaultSpeedKey = context.getString(R.string.set_key_audiobook_default_speed)
    private val autoRewindKey = context.getString(R.string.set_key_audiobook_auto_rewind)
    private val skipSilenceKey = context.getString(R.string.set_key_audiobook_skip_silence)

    override val manualSongUids: Set<String>
        get() = sharedPreferences.getStringSet(key, emptySet()).orEmpty()

    override val skipDurationMs: Long
        get() =
            sharedPreferences.getInt(skipDurationKey, DEFAULT_SKIP_SECONDS).coerceIn(15, 60) * 1000L

    override val defaultPlaybackSpeed: Float
        get() =
            sharedPreferences.getInt(defaultSpeedKey, DEFAULT_SPEED_PERCENT).coerceIn(75, 200) /
                100f

    override val autoRewindMs: Long
        get() =
            sharedPreferences.getInt(autoRewindKey, DEFAULT_REWIND_SECONDS).coerceIn(0, 10) * 1000L

    override val skipSilence: Boolean
        get() = sharedPreferences.getBoolean(skipSilenceKey, false)

    override fun addSongs(songs: Collection<Song>) {
        update(songs) { current -> current + songs.map { it.uid.toString() } }
    }

    override fun removeSongs(songs: Collection<Song>) {
        update(songs) { current -> current - songs.map { it.uid.toString() }.toSet() }
    }

    override fun onSettingChanged(key: String, listener: AudiobookSettings.Listener) {
        if (
            key == autoRewindKey ||
                key == skipSilenceKey ||
                key == skipDurationKey ||
                key == defaultSpeedKey
        ) {
            listener.onAudiobookPlaybackSettingsChanged()
        }
    }

    private companion object {
        const val DEFAULT_SKIP_SECONDS = 30
        const val DEFAULT_SPEED_PERCENT = 100
        const val DEFAULT_REWIND_SECONDS = 2
    }

    private fun update(songs: Collection<Song>, transform: (Set<String>) -> Set<String>) {
        if (songs.isEmpty()) return
        val updated = transform(manualSongUids)
        sharedPreferences.edit { putStringSet(key, updated) }
        listener?.onAudiobookAssignmentsChanged()
    }
}
