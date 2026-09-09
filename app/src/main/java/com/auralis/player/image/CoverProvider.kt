/*
 * Copyright (c) 2025 Auralis Contributors
 * CoverProvider.kt is part of Auralis.
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
 
package com.auralis.player.image

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap
import com.auralis.player.BuildConfig
import com.auralis.player.image.covers.SettingCovers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.oxycblt.musikr.covers.CoverResult
import timber.log.Timber

class CoverProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r" || uriMatcher.match(uri) != 1) {
            return null
        }
        val id = uri.lastPathSegment ?: return null
        val callerUid = Binder.getCallingUid()
        if (!isSafeCoverId(id, callerUid)) {
            return null
        }
        return openPipeHelper(uri, "image/*", null, id) { output, _, _, _, coverId ->
            ParcelFileDescriptor.AutoCloseOutputStream(output).use { outputStream ->
                coverId?.let { requestedId ->
                    val cachedBytes = coverMemoryCache.get(requestedId)
                    if (cachedBytes != null) {
                        outputStream.write(cachedBytes)
                        return@use
                    }
                    // Use a bounded timeout to prevent ANR if the cover fetch hangs.
                    runBlocking(Dispatchers.IO) {
                        try {
                            withTimeoutOrNull(COVER_LOAD_TIMEOUT_MS) {
                                when (
                                    val result =
                                        SettingCovers.immutable(requireNotNull(context))
                                            .obtain(requestedId)
                                ) {
                                    is CoverResult.Hit -> {
                                        val bytes: ByteArray? =
                                            result.cover.open()?.use {
                                                it.readBoundedBytes(MAX_COVER_BYTES)
                                            }
                                        if (bytes != null) {
                                            if (bytes.isNotEmpty()) {
                                                coverMemoryCache.put(requestedId, bytes)
                                                outputStream.write(bytes)
                                            }
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to load cover for $requestedId")
                        }
                    }
                }
            }
        }
    }

    override fun getType(uri: Uri): String? {
        return if (uriMatcher.match(uri) == 1) "image/*" else null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(projection ?: emptyArray())

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.image.CoverProvider"
        private const val IMAGES_PATH = "covers"
        private const val COVER_LOAD_TIMEOUT_MS = 3000L
        private const val MAX_COVER_BYTES = 8 * 1024 * 1024
        private val uriMatcher =
            UriMatcher(UriMatcher.NO_MATCH).apply { addURI(AUTHORITY, "$IMAGES_PATH/*", 1) }

        private val coverMemoryCache =
            object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
                override fun sizeOf(key: String, value: ByteArray): Int = value.size
            }
        private val publishedFolderCovers = ConcurrentHashMap.newKeySet<String>()

        val CONTENT_URI: Uri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(IMAGES_PATH)
                .build()

        fun uriForCover(id: String): Uri {
            if (id.startsWith("mcf:")) {
                publishedFolderCovers.add(id)
            }
            return Uri.withAppendedPath(CONTENT_URI, id)
        }
    }

    private fun isSafeCoverId(id: String, callerUid: Int): Boolean {
        if (id.isBlank() || id.contains("..") || id.contains('\u0000')) {
            return false
        }
        if (!id.startsWith("mcf:")) {
            return true
        }
        // Only expose folder covers that Auralis has published in a media
        // description. This preserves Android Auto/widget artwork without
        // turning the provider into an arbitrary SAF URI reader.
        return callerUid == android.os.Process.myUid() || publishedFolderCovers.contains(id)
    }
}

private fun java.io.InputStream.readBoundedBytes(maxBytes: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) {
            return output.toByteArray()
        }
        total += read
        if (total > maxBytes) {
            return null
        }
        output.write(buffer, 0, read)
    }
}
