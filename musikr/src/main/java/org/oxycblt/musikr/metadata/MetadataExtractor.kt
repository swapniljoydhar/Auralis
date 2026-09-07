/*
 * Copyright (c) 2024 Auralis Contributors
 * MetadataExtractor.kt is part of Auralis.
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
 
package org.oxycblt.musikr.metadata

import android.content.ContentResolver
import android.content.Context
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.fs.File

internal interface MetadataExtractor {
    suspend fun extract(deviceFile: File): MetadataResult

    companion object {
        fun from(context: Context): MetadataExtractor =
            MetadataExtractorImpl(context.contentResolver)
    }
}

sealed interface MetadataResult {
    data class Success(val metadata: Metadata?) : MetadataResult

    data object NoMetadata : MetadataResult

    data object NotAudio : MetadataResult

    data object ProviderFailed : MetadataResult
}

private class MetadataExtractorImpl(private val contentResolver: ContentResolver) :
    MetadataExtractor {
    override suspend fun extract(deviceFile: File): MetadataResult =
        withContext(Dispatchers.IO) {
            contentResolver.openFileDescriptor(deviceFile.uri, "r")?.use { fd ->
                val fis = FileInputStream(fd.fileDescriptor)
                TagLibJNI.open(deviceFile, fis).also { fis.close() }
            } ?: MetadataResult.ProviderFailed
        }
}
