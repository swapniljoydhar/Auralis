/*
 * Copyright (c) 2026 Auralis Project
 * AudiobookClassifierTest.kt is part of Auralis.
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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookClassifierTest {
    @Test
    fun m4bIsAudiobookRegardlessOfFolderName() {
        assertTrue(
            AudiobookClassifier.isAudiobook(
                AudiobookFileSignals(
                    extension = "M4B",
                    path = "Music/Unknown/track.m4b",
                    album = "Unknown Album",
                    genres = emptyList(),
                )
            )
        )
    }

    @Test
    fun mp3InAudiobookFolderIsAudiobook() {
        assertTrue(
            AudiobookClassifier.isAudiobook(
                AudiobookFileSignals(
                    extension = "mp3",
                    path = "Audiobooks/The Hobbit/01 - An Unexpected Party.mp3",
                    album = "The Hobbit",
                    genres = emptyList(),
                )
            )
        )
    }

    @Test
    fun audiobookGenreClassifiesMp3WithoutSpecialFolder() {
        assertTrue(
            AudiobookClassifier.isAudiobook(
                AudiobookFileSignals(
                    extension = "mp3",
                    path = "Books/The Hobbit/01.mp3",
                    album = "The Hobbit",
                    genres = listOf("Audiobooks"),
                )
            )
        )
    }

    @Test
    fun ordinaryMusicMp3IsNotAudiobook() {
        assertFalse(
            AudiobookClassifier.isAudiobook(
                AudiobookFileSignals(
                    extension = "mp3",
                    path = "Music/Radiohead/In Rainbows/01.mp3",
                    album = "In Rainbows",
                    genres = listOf("Alternative Rock"),
                )
            )
        )
    }
}
