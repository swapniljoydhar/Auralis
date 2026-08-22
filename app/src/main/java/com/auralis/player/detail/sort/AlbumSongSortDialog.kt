/*
 * Copyright (c) 2023 Auralis Contributors
 * AlbumSongSortDialog.kt is part of Auralis.
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
 
package com.auralis.player.detail.sort

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.auralis.player.databinding.DialogSortBinding
import com.auralis.player.detail.DetailViewModel
import com.auralis.player.list.sort.Sort
import com.auralis.player.list.sort.SortDialog
import com.auralis.player.util.collectImmediately
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.musikr.Album
import timber.log.Timber as L

/**
 * A [SortDialog] that controls the [Sort] of [DetailViewModel.albumSongSort].
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class AlbumSongSortDialog : SortDialog() {
    private val detailModel: DetailViewModel by activityViewModels()

    override fun onBindingCreated(binding: DialogSortBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- VIEWMODEL SETUP ---
        collectImmediately(detailModel.currentAlbum, ::updateAlbum)
    }

    override fun getInitialSort() = detailModel.albumSongSort

    override fun applyChosenSort(sort: Sort) {
        detailModel.applyAlbumSongSort(sort)
    }

    override fun getModeChoices() = listOf(Sort.Mode.ByDisc, Sort.Mode.ByTrack)

    private fun updateAlbum(album: Album?) {
        if (album == null) {
            L.d("No album to sort, navigating away")
            findNavController().navigateUp()
        }
    }
}
