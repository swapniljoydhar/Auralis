/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookClassifierTest.kt is part of Auralis.
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
    fun supportedNonM4bFormatsClassifyWithAudiobookMarker() {
        listOf("m4a", "ogg", "oga", "opus").forEach { extension ->
            assertTrue(
                "Expected .$extension in an Audiobooks folder to classify as an audiobook",
                AudiobookClassifier.isAudiobook(
                    AudiobookFileSignals(
                        extension = extension,
                        path = "Audiobooks/The Hobbit/01.$extension",
                        album = "The Hobbit",
                        genres = emptyList(),
                    )
                ),
            )
        }
    }

    @Test
    fun supportedNonM4bFormatsDoNotClassifyOrdinaryMusicWithoutMarker() {
        listOf("m4a", "ogg", "oga", "opus").forEach { extension ->
            assertFalse(
                "Expected ordinary .$extension music to remain outside Audiobooks",
                AudiobookClassifier.isAudiobook(
                    AudiobookFileSignals(
                        extension = extension,
                        path = "Music/Radiohead/In Rainbows/01.$extension",
                        album = "In Rainbows",
                        genres = listOf("Alternative Rock"),
                    )
                ),
            )
        }
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

    @Test
    fun longFormChapterSetIsRecognizedWithoutManualAssignment() {
        assertTrue(
            AudiobookCatalog.isLongFormBook(listOf(45 * 60_000L, 42 * 60_000L, 39 * 60_000L))
        )
    }

    @Test
    fun ordinaryLengthMusicAlbumIsNotLongFormBook() {
        assertFalse(AudiobookCatalog.isLongFormBook(List(12) { 4 * 60_000L }))
    }
}
