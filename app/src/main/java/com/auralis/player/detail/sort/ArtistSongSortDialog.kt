/*
 * Copyright (c) 2023 Auralis Contributors
 * ArtistSongSortDialog.kt is part of Auralis.
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
import com.auralis.musikr.Artist
import timber.log.Timber as L

/**
 * A [SortDialog] that controls the [Sort] of [DetailViewModel.artistSongSort].
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class ArtistSongSortDialog : SortDialog() {
    private val detailModel: DetailViewModel by activityViewModels()

    override fun onBindingCreated(binding: DialogSortBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- VIEWMODEL SETUP ---
        collectImmediately(detailModel.currentArtist, ::updateArtist)
    }

    override fun getInitialSort() = detailModel.artistSongSort

    override fun applyChosenSort(sort: Sort) {
        detailModel.applyArtistSongSort(sort)
    }

    override fun getModeChoices() =
        listOf(Sort.Mode.ByName, Sort.Mode.ByAlbum, Sort.Mode.ByDate, Sort.Mode.ByDuration)

    private fun updateArtist(artist: Artist?) {
        if (artist == null) {
            L.d("No artist to sort, navigating away")
            findNavController().navigateUp()
        }
    }
}
