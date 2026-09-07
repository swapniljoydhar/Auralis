/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookSettings.kt is part of Auralis.
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
     * The last playback speed actively used during audiobook playback. Restoring an audiobook
     * session resumes at this speed instead of resetting to [defaultPlaybackSpeed], mirroring how
     * dedicated audiobook players remember the listener's pace.
     */
    val lastPlaybackSpeed: Float

    /** Record [speed] as the last speed actively used during audiobook playback. */
    fun recordPlaybackSpeed(speed: Float)

    /**
     * The rewind interval applied when audiobook playback resumes after a pause, in milliseconds.
     */
    val autoRewindMs: Long

    /** Whether long silent sections should be skipped during audiobook playback. */
    val skipSilence: Boolean

    /** Whether the Audiobooks library should include only the user-selected local folders. */
    val useSelectedFolders: Boolean

    /** Local folder identifiers selected for the Audiobooks-only library projection. */
    val selectedFolders: Set<String>

    /** The local folder model used only to group Audiobooks into books. */
    val folderOrganization: AudiobookFolderOrganization

    /** Whether the Audiobooks library uses the two-column cover grid presentation. */
    val useGridPresentation: Boolean

    /** The presentation used only by the Audiobooks listening library. */
    val libraryPresentation: AudiobookLibraryPresentation

    fun addSongs(songs: Collection<Song>)

    fun removeSongs(songs: Collection<Song>)

    interface Listener {
        fun onAudiobookAssignmentsChanged()

        fun onAudiobookPlaybackSettingsChanged() {}

        fun onAudiobookLibrarySettingsChanged() {}

        fun onAudiobookAppearanceSettingsChanged() {}
    }
}

class AudiobookSettingsImpl @Inject constructor(@ApplicationContext context: Context) :
    Settings.Impl<AudiobookSettings.Listener>(context), AudiobookSettings {
    private val key = context.getString(R.string.set_key_audiobook_manual_uids)
    private val skipDurationKey = context.getString(R.string.set_key_audiobook_skip_duration)
    private val defaultSpeedKey = context.getString(R.string.set_key_audiobook_default_speed)
    private val lastSpeedKey = context.getString(R.string.set_key_audiobook_last_speed)
    private val autoRewindKey = context.getString(R.string.set_key_audiobook_auto_rewind)
    private val skipSilenceKey = context.getString(R.string.set_key_audiobook_skip_silence)
    private val selectedFoldersEnabledKey =
        context.getString(R.string.set_key_audiobook_selected_folders_enabled)
    private val selectedFoldersKey = context.getString(R.string.set_key_audiobook_selected_folders)
    private val folderOrganizationKey =
        context.getString(R.string.set_key_audiobook_folder_organization)
    private val libraryPresentationKey =
        context.getString(R.string.set_key_audiobook_library_presentation)

    override val manualSongUids: Set<String>
        get() = sharedPreferences.getStringSet(key, emptySet()).orEmpty()

    override val skipDurationMs: Long
        get() =
            sharedPreferences.getInt(skipDurationKey, DEFAULT_SKIP_SECONDS).coerceIn(15, 60) * 1000L

    override val defaultPlaybackSpeed: Float
        get() =
            sharedPreferences.getInt(defaultSpeedKey, DEFAULT_SPEED_PERCENT).coerceIn(75, 200) /
                100f

    override val lastPlaybackSpeed: Float
        get() {
            val percent = sharedPreferences.getInt(lastSpeedKey, -1)
            return if (percent in SPEED_PERCENT_MIN..SPEED_PERCENT_MAX) {
                percent / 100f
            } else {
                defaultPlaybackSpeed
            }
        }

    override fun recordPlaybackSpeed(speed: Float) {
        val percent = (speed * 100f).toInt().coerceIn(SPEED_PERCENT_MIN, SPEED_PERCENT_MAX)
        sharedPreferences.edit { putInt(lastSpeedKey, percent) }
    }

    override val autoRewindMs: Long
        get() =
            sharedPreferences.getInt(autoRewindKey, DEFAULT_REWIND_SECONDS).coerceIn(0, 10) * 1000L

    override val skipSilence: Boolean
        get() = sharedPreferences.getBoolean(skipSilenceKey, false)

    override val useSelectedFolders: Boolean
        get() = sharedPreferences.getBoolean(selectedFoldersEnabledKey, false)

    override val selectedFolders: Set<String>
        get() = sharedPreferences.getStringSet(selectedFoldersKey, emptySet()).orEmpty()

    override val folderOrganization: AudiobookFolderOrganization
        get() =
            AudiobookFolderOrganization.fromPreference(
                sharedPreferences.getInt(folderOrganizationKey, SEPARATE_BOOK_FOLDERS)
            )

    override val useGridPresentation: Boolean
        get() = libraryPresentation == AudiobookLibraryPresentation.GRID

    override val libraryPresentation: AudiobookLibraryPresentation
        get() =
            AudiobookLibraryPresentation.fromPreference(
                sharedPreferences.getInt(libraryPresentationKey, COMPACT_PRESENTATION)
            )

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
        if (
            key == selectedFoldersEnabledKey ||
                key == selectedFoldersKey ||
                key == folderOrganizationKey
        ) {
            listener.onAudiobookLibrarySettingsChanged()
        }
        if (key == libraryPresentationKey) {
            listener.onAudiobookAppearanceSettingsChanged()
        }
    }

    private companion object {
        const val DEFAULT_SKIP_SECONDS = 30
        const val DEFAULT_SPEED_PERCENT = 100
        const val DEFAULT_REWIND_SECONDS = 2
        const val SPEED_PERCENT_MIN = 50
        const val SPEED_PERCENT_MAX = 300
        const val COMPACT_PRESENTATION = 0
        const val SEPARATE_BOOK_FOLDERS = 0
    }

    private fun update(songs: Collection<Song>, transform: (Set<String>) -> Set<String>) {
        if (songs.isEmpty()) return
        val updated = transform(manualSongUids)
        sharedPreferences.edit { putStringSet(key, updated) }
        listener?.onAudiobookAssignmentsChanged()
    }
}
