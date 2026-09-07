/*
 * Copyright (c) 2025 Auralis Contributors
 * ChainedCovers.kt is part of Auralis.
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
 
package com.auralis.musikr.covers.chained

import com.auralis.musikr.covers.Cover
import com.auralis.musikr.covers.CoverResult
import com.auralis.musikr.covers.Covers
import com.auralis.musikr.covers.MutableCovers
import com.auralis.musikr.fs.File
import com.auralis.musikr.metadata.Metadata

/**
 * A [Covers] implementation that chains multiple [Covers] together as fallbacks.
 *
 * This is useful for when you want to try multiple sources for a cover, such as first embedded and
 * then filesystem-based covers.
 *
 * This implementation will return the first hit from the provided [Covers] instances. It's assumed
 * that there is no ID overlap between [Covers] outputs.
 *
 * See [MutableChainedCovers] for the mutable variant.
 *
 * @param many The [Covers] instances to chain together.
 */
class ChainedCovers<R : Cover, T : R>(vararg many: Covers<out T>) : Covers<R> {
    private val _many = many

    override suspend fun obtain(id: String): CoverResult<R> {
        for (covers in _many) {
            val result = covers.obtain(id)
            if (result is CoverResult.Hit) {
                return CoverResult.Hit(result.cover)
            }
        }
        return CoverResult.Miss()
    }
}

/**
 * A [MutableCovers] implementation that chains multiple [MutableCovers] together as fallbacks.
 *
 * This is useful for when you want to try multiple sources for a cover, such as first embedded and
 * then filesystem-based covers.
 *
 * This implementation will use the first hit from the provided [MutableCovers] instances, and
 * propagate cleanup across all [MutableCovers] instances. It's assumed that there is no ID overlap
 * between [MutableCovers] outputs.
 *
 * See [ChainedCovers] for the immutable variant.
 *
 * @param many The [MutableCovers] instances to chain together.
 */
class MutableChainedCovers<R : Cover, T : R>(vararg many: MutableCovers<out T>) : MutableCovers<R> {
    private val inner = ChainedCovers<R, T>(*many)
    private val _many = many

    override suspend fun obtain(id: String): CoverResult<R> = inner.obtain(id)

    override suspend fun create(file: File, metadata: Metadata): CoverResult<R> {
        for (cover in _many) {
            val result = cover.create(file, metadata)
            if (result is CoverResult.Hit) {
                return CoverResult.Hit(result.cover)
            }
        }
        return CoverResult.Miss()
    }

    override suspend fun cleanup(excluding: Collection<Cover>) {
        for (cover in _many) {
            cover.cleanup(excluding)
        }
    }
}
