/*
 * Copyright (c) 2025 Auralis Contributors
 * CoverProvider.kt is part of Auralis.
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
 
package com.auralis.player.image

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.auralis.player.BuildConfig
import com.auralis.player.image.covers.SettingCovers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.oxycblt.musikr.covers.CoverResult

class CoverProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r" || uriMatcher.match(uri) != 1) {
            return null
        }
        val id = uri.lastPathSegment ?: return null
        if (id.contains("..")) {
            return null
        }
        return openPipeHelper(uri, "image/*", null, id) { output, _, _, _, coverId ->
            ParcelFileDescriptor.AutoCloseOutputStream(output).use { outputStream ->
                coverId?.let { requestedId ->
                    runBlocking(Dispatchers.IO) {
                        when (
                            val result =
                                SettingCovers.immutable(requireNotNull(context)).obtain(requestedId)
                        ) {
                            is CoverResult.Hit ->
                                result.cover.open()?.use { it.copyTo(outputStream) }
                            else -> Unit
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
        private val uriMatcher =
            UriMatcher(UriMatcher.NO_MATCH).apply { addURI(AUTHORITY, "$IMAGES_PATH/*", 1) }

        val CONTENT_URI: Uri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY)
                .appendPath(IMAGES_PATH)
                .build()
    }
}
