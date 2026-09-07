/*
 * Copyright (c) 2023 Auralis Contributors
 * PlaybackPickerViewModel.kt is part of Auralis.
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
 
package com.auralis.player.playback.decision

import androidx.lifecycle.ViewModel
import com.auralis.player.music.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.auralis.musikr.Artist
import com.auralis.musikr.Music
import com.auralis.musikr.Song
import timber.log.Timber as L

/**
 * A [ViewModel] that stores the choices shown in the playback picker dialogs.
 *
 * @author Auralis Contributors
 */
@HiltViewModel
class PlaybackPickerViewModel @Inject constructor(private val musicRepository: MusicRepository) :
    ViewModel(), MusicRepository.UpdateListener {
    private val _currentPickerSong = MutableStateFlow<Song?>(null)
    /** The current set of [Artist] choices to show in the picker, or null if to show nothing. */
    val currentPickerSong: StateFlow<Song?>
        get() = _currentPickerSong

    init {
        musicRepository.addUpdateListener(this)
    }

    override fun onMusicChanges(changes: MusicRepository.Changes) {
        if (!changes.deviceLibrary) return
        val library = musicRepository.library ?: return
        _currentPickerSong.value = _currentPickerSong.value?.run { library.findSong(uid) }
    }

    override fun onCleared() {
        super.onCleared()
        musicRepository.removeUpdateListener(this)
    }

    /**
     * Set the [Music.UID] of the [Song] to show choices for.
     *
     * @param uid The [Music.UID] of the item to show. Must be a [Song].
     */
    fun setPickerSongUid(uid: Music.UID) {
        L.d("Opening picker for song $uid")
        _currentPickerSong.value = musicRepository.library?.findSong(uid)
        if (_currentPickerSong.value != null) {
            L.w("Given song UID was invalid")
        }
    }
}
