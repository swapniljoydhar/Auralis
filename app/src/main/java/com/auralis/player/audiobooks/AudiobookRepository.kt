/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookRepository.kt is part of Auralis.
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

import com.auralis.player.music.MusicRepository
import javax.inject.Inject

/**
 * Read-only audiobook projection over the existing Music library.
 *
 * This deliberately does not create a second scanner. When the Music library is refreshed, the
 * Audiobooks tab can request a new projection and receives the same Song instances used elsewhere
 * in the app.
 */
class AudiobookRepository
@Inject
constructor(
    private val musicRepository: MusicRepository,
    private val audiobookSettings: AudiobookSettings,
) {
    fun books(): List<AudiobookBook> =
        musicRepository.library?.songs?.let {
            val selected = audiobookSettings.selectedFolders
            val source =
                AudiobookFolderScope.filterSnapshot(
                    snapshot = it,
                    enabled = audiobookSettings.useSelectedFolders,
                    selectedFolders = selected,
                ) { song ->
                    song.path.directory.components.unixString
                }
            AudiobookCatalog.fromSongs(
                songs = source,
                manualSongUids = audiobookSettings.manualSongUids,
                organization = audiobookSettings.folderOrganization,
                selectedFolders = audiobookSettings.selectedFolders,
            )
        } ?: emptyList()

    fun book(key: String): AudiobookBook? = books().firstOrNull { it.key == key }

    fun folders(): List<String> =
        musicRepository.library
            ?.songs
            ?.let { AudiobookCatalog.fromSongs(it, audiobookSettings.manualSongUids) }
            ?.flatMap { book -> book.chapters.map { it.song.path.directory.components.unixString } }
            ?.distinct()
            ?.sorted()
            .orEmpty()
}
