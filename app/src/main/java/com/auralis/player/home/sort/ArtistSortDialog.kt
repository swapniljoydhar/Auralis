/*
 * Copyright (c) 2023 Auralis Contributors
 * ArtistSortDialog.kt is part of Auralis.
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
 
package com.auralis.player.home.sort

import androidx.fragment.app.activityViewModels
import com.auralis.player.home.HomeViewModel
import com.auralis.player.list.sort.Sort
import com.auralis.player.list.sort.SortDialog
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [SortDialog] that controls the [Sort] of [HomeViewModel.artistList].
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class ArtistSortDialog : SortDialog() {
    private val homeModel: HomeViewModel by activityViewModels()

    override fun getInitialSort() = homeModel.artistSort

    override fun applyChosenSort(sort: Sort) {
        homeModel.applyArtistSort(sort)
    }

    override fun getModeChoices() =
        listOf(Sort.Mode.ByName, Sort.Mode.ByDuration, Sort.Mode.ByCount)
}
