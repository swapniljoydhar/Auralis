/*
 * Copyright (c) 2024 Auralis Contributors
 * NullCovers.kt is part of Auralis.
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
 
package com.auralis.player.image.covers

import com.auralis.musikr.covers.Cover
import com.auralis.musikr.covers.CoverResult
import com.auralis.musikr.covers.MutableCovers
import com.auralis.musikr.covers.stored.CoverStorage
import com.auralis.musikr.fs.File
import com.auralis.musikr.metadata.Metadata

class NullCovers(private val storage: CoverStorage) : MutableCovers<NullCover> {
    override suspend fun obtain(id: String) = CoverResult.Hit(NullCover)

    override suspend fun create(file: File, metadata: Metadata) = CoverResult.Hit(NullCover)

    override suspend fun cleanup(excluding: Collection<Cover>) {
        storage.ls(setOf()).map { storage.rm(it) }
    }
}

data object NullCover : Cover {
    override val id = "null"

    override suspend fun open() = null
}
