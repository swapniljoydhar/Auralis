/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookLibrarySettings.kt is part of Auralis.
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

/** The presentation used only by the Audiobooks listening library. */
enum class AudiobookLibraryPresentation(private val preferenceValue: Int) {
    COMPACT(0),
    GRID(1);

    companion object {
        fun fromPreference(value: Int) =
            entries.firstOrNull { it.preferenceValue == value } ?: COMPACT
    }
}

/** Pure folder-scoping contract for the Audiobooks projection over the shared local media index. */
object AudiobookFolderScope {
    fun includes(directory: String, enabled: Boolean, selectedFolders: Set<String>) =
        !enabled || directory in selectedFolders

    fun <T> filterSnapshot(
        snapshot: Collection<T>,
        enabled: Boolean,
        selectedFolders: Set<String>,
        directory: (T) -> String,
    ): List<T> =
        if (!enabled) snapshot.toList()
        else snapshot.filter { includes(directory(it), enabled = true, selectedFolders) }
}
