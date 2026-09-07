/*
 * Copyright (c) 2024 Auralis Contributors
 * SettingCovers.kt is part of Auralis.
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

import android.content.Context
import android.graphics.Bitmap
import com.auralis.player.image.CoverMode
import com.auralis.player.image.ImageSettings
import java.util.UUID
import javax.inject.Inject
import com.auralis.musikr.covers.Cover
import com.auralis.musikr.covers.Covers
import com.auralis.musikr.covers.FDCover
import com.auralis.musikr.covers.MutableCovers
import com.auralis.musikr.covers.chained.ChainedCovers
import com.auralis.musikr.covers.chained.MutableChainedCovers
import com.auralis.musikr.covers.embedded.CoverIdentifier
import com.auralis.musikr.covers.embedded.EmbeddedCovers
import com.auralis.musikr.covers.fs.FSCovers
import com.auralis.musikr.covers.fs.MutableFSCovers
import com.auralis.musikr.covers.stored.Compress
import com.auralis.musikr.covers.stored.CoverStorage
import com.auralis.musikr.covers.stored.MutableStoredCovers
import com.auralis.musikr.covers.stored.NoTranscoding
import com.auralis.musikr.covers.stored.StoredCovers

interface SettingCovers {
    suspend fun mutate(context: Context, revision: UUID): MutableCovers<out Cover>

    companion object {
        suspend fun immutable(context: Context): Covers<FDCover> =
            ChainedCovers(StoredCovers(CoverStorage.at(context.coversDir())), FSCovers(context))
    }
}

class SettingCoversImpl @Inject constructor(private val imageSettings: ImageSettings) :
    SettingCovers {
    override suspend fun mutate(context: Context, revision: UUID): MutableCovers<out Cover> {
        val coverStorage = CoverStorage.at(context.coversDir())
        val transcoding =
            when (imageSettings.coverMode) {
                CoverMode.OFF -> return NullCovers(coverStorage)
                CoverMode.SAVE_SPACE -> Compress(Bitmap.CompressFormat.JPEG, 500, 70)
                CoverMode.BALANCED -> Compress(Bitmap.CompressFormat.JPEG, 750, 85)
                CoverMode.HIGH_QUALITY -> Compress(Bitmap.CompressFormat.JPEG, 1000, 100)
                CoverMode.AS_IS -> NoTranscoding
            }
        val revisionedTranscoding = RevisionedTranscoding(revision, transcoding)
        val storedCovers =
            MutableStoredCovers(
                EmbeddedCovers(CoverIdentifier.sha256()),
                coverStorage,
                revisionedTranscoding,
            )
        val fsCovers = MutableFSCovers(context)
        return MutableChainedCovers(storedCovers, fsCovers)
    }
}

private fun Context.coversDir() = filesDir.resolve("covers").apply { mkdirs() }
