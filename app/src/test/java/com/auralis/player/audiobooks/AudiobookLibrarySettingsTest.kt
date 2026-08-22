/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookLibrarySettingsTest.kt is part of Auralis.
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookLibrarySettingsTest {
    @Test
    fun folderScopeIncludesEveryDirectoryWhenDisabled() {
        assertTrue(
            AudiobookFolderScope.includes(
                directory = "/storage/emulated/0/Music",
                enabled = false,
                selectedFolders = emptySet(),
            )
        )
    }

    @Test
    fun folderScopeIncludesOnlyExplicitlySelectedDirectoriesWhenEnabled() {
        val selected = setOf("/storage/emulated/0/Audiobooks")

        assertTrue(
            AudiobookFolderScope.includes(
                directory = "/storage/emulated/0/Audiobooks",
                enabled = true,
                selectedFolders = selected,
            )
        )
        assertFalse(
            AudiobookFolderScope.includes(
                directory = "/storage/emulated/0/Music",
                enabled = true,
                selectedFolders = selected,
            )
        )
    }

    @Test
    fun unknownPresentationFallsBackToCompact() {
        assertEquals(
            AudiobookLibraryPresentation.COMPACT,
            AudiobookLibraryPresentation.fromPreference(99),
        )
        assertEquals(
            AudiobookLibraryPresentation.GRID,
            AudiobookLibraryPresentation.fromPreference(1),
        )
    }

    @Test
    fun folderScopeCreatesAnAudiobooksProjectionWithoutChangingMusicSnapshot() {
        val musicSnapshot = listOf("/storage/emulated/0/Music", "/storage/emulated/0/Audiobooks")
        val beforeFolderScope = musicSnapshot.toList()

        val audiobookProjection =
            AudiobookFolderScope.filterSnapshot(
                snapshot = musicSnapshot,
                enabled = true,
                selectedFolders = setOf("/storage/emulated/0/Audiobooks"),
            ) {
                it
            }

        assertEquals(listOf("/storage/emulated/0/Audiobooks"), audiobookProjection)
        assertEquals(beforeFolderScope, musicSnapshot)
        assertNotSame(musicSnapshot, audiobookProjection)
    }

    @Test
    fun disabledFolderScopeStillCopiesMusicSnapshotForAudiobooksProjection() {
        val musicSnapshot = listOf("/storage/emulated/0/Music", "/storage/emulated/0/Audiobooks")

        val audiobookProjection =
            AudiobookFolderScope.filterSnapshot(
                snapshot = musicSnapshot,
                enabled = false,
                selectedFolders = emptySet(),
            ) {
                it
            }

        assertEquals(musicSnapshot, audiobookProjection)
        assertNotSame(musicSnapshot, audiobookProjection)
    }
}
