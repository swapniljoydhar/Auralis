/*
 * Copyright (c) 2023 Auralis Contributors
 * PlaylistSortDialog.kt is part of Auralis.
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
 
package com.auralis.player.home.sort

import androidx.fragment.app.activityViewModels
import com.auralis.player.home.HomeViewModel
import com.auralis.player.list.sort.Sort
import com.auralis.player.list.sort.SortDialog
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [SortDialog] that controls the [Sort] of [HomeViewModel.playlistList].
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class PlaylistSortDialog : SortDialog() {
    private val homeModel: HomeViewModel by activityViewModels()

    override fun getInitialSort() = homeModel.playlistSort

    override fun applyChosenSort(sort: Sort) {
        homeModel.applyPlaylistSort(sort)
    }

    override fun getModeChoices() =
        listOf(Sort.Mode.ByName, Sort.Mode.ByDuration, Sort.Mode.ByCount)
}
