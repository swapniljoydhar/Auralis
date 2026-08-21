/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookRepository.kt is part of Auxio.
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
 
package org.oxycblt.auxio.audiobooks

import javax.inject.Inject
import org.oxycblt.auxio.music.MusicRepository

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
            AudiobookCatalog.fromSongs(it, audiobookSettings.manualSongUids)
        } ?: emptyList()

    fun book(key: String): AudiobookBook? = books().firstOrNull { it.key == key }
}
