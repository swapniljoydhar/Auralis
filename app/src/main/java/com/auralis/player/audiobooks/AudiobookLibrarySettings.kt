/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookLibrarySettings.kt is part of Auralis.
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

/** The presentation used only by the Audiobooks listening library. */
enum class AudiobookLibraryPresentation(private val preferenceValue: Int) {
    COMPACT(0),
    GRID(1);

    companion object {
        fun fromPreference(value: Int) =
            entries.firstOrNull { it.preferenceValue == value } ?: COMPACT
    }
}

/**
 * The local folder model used only to group Audiobooks after the shared Music snapshot is copied.
 */
enum class AudiobookFolderOrganization(private val preferenceValue: Int) {
    SEPARATE_BOOK_FOLDERS(0),
    SELECTED_FOLDER_AS_BOOK(1),
    AUTHOR_BOOK_HIERARCHY(2);

    val requiresSelectedRoots: Boolean
        get() = this != SEPARATE_BOOK_FOLDERS

    companion object {
        fun fromPreference(value: Int) =
            entries.firstOrNull { it.preferenceValue == value } ?: SEPARATE_BOOK_FOLDERS
    }
}

/** A deterministic local path grouping result that does not mutate any shared Music snapshot. */
data class AudiobookFolderGroup(
    val key: String,
    val title: String? = null,
    val author: String? = null,
)

/** Pure resolver for the three supported local Audiobooks folder models. */
object AudiobookFolderOrganizationResolver {
    fun group(
        volume: String,
        directory: String,
        organization: AudiobookFolderOrganization,
        selectedFolders: Set<String>,
    ): AudiobookFolderGroup {
        val normalizedDirectory = directory.trimEnd('/')
        val selectedRoot =
            selectedFolders
                .asSequence()
                .map { it.trimEnd('/') }
                .filter { it.isNotEmpty() }
                .filter { normalizedDirectory == it || normalizedDirectory.startsWith("$it/") }
                .maxByOrNull(String::length)
        return when (organization) {
            AudiobookFolderOrganization.SEPARATE_BOOK_FOLDERS ->
                separate(volume, normalizedDirectory)
            AudiobookFolderOrganization.SELECTED_FOLDER_AS_BOOK ->
                selectedRoot?.let { root ->
                    AudiobookFolderGroup(
                        key = "$volume:selected:$root",
                        title = root.substringAfterLast('/').ifBlank { null },
                    )
                } ?: separate(volume, normalizedDirectory)
            AudiobookFolderOrganization.AUTHOR_BOOK_HIERARCHY ->
                selectedRoot?.let { root -> authorBook(volume, normalizedDirectory, root) }
                    ?: separate(volume, normalizedDirectory)
        }
    }

    private fun separate(volume: String, directory: String) =
        AudiobookFolderGroup(key = "$volume:folder:$directory")

    private fun authorBook(volume: String, directory: String, root: String): AudiobookFolderGroup {
        val relative = directory.removePrefix(root).trim('/').split('/').filter(String::isNotBlank)
        if (relative.size < 2) return separate(volume, directory)
        val author = relative.first()
        val book = relative[1]
        return AudiobookFolderGroup(
            key = "$volume:author-book:$root/$author/$book",
            title = book,
            author = author,
        )
    }
}

/** Pure folder-scoping contract for the Audiobooks projection over the shared local media index. */
object AudiobookFolderScope {
    /**
     * A directory is included when scoping is disabled, or when it is a selected folder or nested
     * below one. Recursion matters: book folders almost always live *under* the folder the listener
     * picks, and the [AudiobookFolderOrganizationResolver] grouping models resolve nested chapters
     * against the same selected roots.
     */
    fun includes(directory: String, enabled: Boolean, selectedFolders: Set<String>): Boolean {
        if (!enabled) return true
        val normalized = directory.trimEnd('/')
        return selectedFolders.any { selected ->
            val root = selected.trimEnd('/')
            root.isNotEmpty() && (normalized == root || normalized.startsWith("$root/"))
        }
    }

    fun <T> filterSnapshot(
        snapshot: Collection<T>,
        enabled: Boolean,
        selectedFolders: Set<String>,
        directory: (T) -> String,
    ): List<T> =
        if (!enabled) snapshot.toList()
        else snapshot.filter { includes(directory(it), enabled = true, selectedFolders) }
}
