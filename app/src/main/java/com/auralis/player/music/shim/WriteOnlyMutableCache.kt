/*
 * Copyright (c) 2025 Auralis Contributors
 * WriteOnlyMutableCache.kt is part of Auralis.
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
 
package com.auralis.player.music.shim

import com.auralis.musikr.cache.CacheResult
import com.auralis.musikr.cache.CachedFile
import com.auralis.musikr.cache.MutableCache
import com.auralis.musikr.fs.File

class WriteOnlyMutableCache(private val inner: MutableCache) : MutableCache {
    override suspend fun read(file: File): CacheResult {
        return when (val result = inner.read(file)) {
            is CacheResult.Hit -> CacheResult.Stale(file, result.file.addedMs)
            else -> result
        }
    }

    override suspend fun write(cachedFile: CachedFile) {
        inner.write(cachedFile)
    }

    override suspend fun cleanup(excluding: List<CachedFile>) {
        inner.cleanup(excluding)
    }
}
