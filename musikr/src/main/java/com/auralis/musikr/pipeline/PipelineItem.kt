/*
 * Copyright (c) 2025 Auralis Contributors
 * PipelineItem.kt is part of Auralis.
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
 
package com.auralis.musikr.pipeline

import com.auralis.musikr.covers.Cover
import com.auralis.musikr.fs.File
import com.auralis.musikr.metadata.Properties
import com.auralis.musikr.playlist.PlaylistFile
import com.auralis.musikr.tag.parse.ParsedTags

internal sealed interface PipelineItem

internal sealed interface Incomplete : PipelineItem

internal sealed interface Complete : PipelineItem

internal sealed interface Explored : PipelineItem {
    sealed interface New : Explored, Incomplete

    sealed interface Known : Explored, Complete
}

internal data class NewSong(val file: File) : Explored.New

internal sealed interface Extracted : PipelineItem {
    sealed interface Valid : Complete, Extracted

    sealed interface Invalid : Extracted
}

internal data object InvalidSong : Extracted.Invalid

internal data object NotAudio : Explored.Known, Extracted.Valid

internal data class RawPlaylist(val file: PlaylistFile) : Explored.Known, Extracted.Valid

internal data class RawSong(
    val file: File,
    val properties: Properties,
    val tags: ParsedTags,
    val cover: Cover?,
    val addedMs: Long,
) : Explored.Known, Extracted.Valid
