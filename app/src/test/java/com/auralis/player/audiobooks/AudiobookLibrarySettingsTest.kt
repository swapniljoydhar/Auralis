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

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
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

    @Test
    fun concurrentFolderProjectionsNeverMutateMusicSnapshot() {
        val musicSnapshot =
            List(128) { index ->
                "/library/${if (index % 2 == 0) "Audiobooks" else "Music"}/$index"
            }
        val expectedSnapshot = musicSnapshot.toList()
        val selectedFolders = musicSnapshot.filter { it.contains("/Audiobooks/") }.toSet()
        val start = CountDownLatch(1)
        val complete = CountDownLatch(8)
        val failures = ConcurrentLinkedQueue<String>()

        repeat(8) {
            thread(start = true) {
                start.await()
                repeat(200) {
                    val projection =
                        AudiobookFolderScope.filterSnapshot(
                            snapshot = musicSnapshot,
                            enabled = true,
                            selectedFolders = selectedFolders,
                        ) {
                            it
                        }
                    if (projection.any { it !in selectedFolders }) {
                        failures += "Audiobooks projection included an unselected Music entry"
                    }
                    if (projection === musicSnapshot) {
                        failures += "Audiobooks projection reused the Music snapshot"
                    }
                }
                complete.countDown()
            }
        }

        start.countDown()
        assertTrue("Concurrent projections did not finish", complete.await(5, TimeUnit.SECONDS))
        assertTrue(failures.joinToString(), failures.isEmpty())
        assertEquals(expectedSnapshot, musicSnapshot)
    }
}
