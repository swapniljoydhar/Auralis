/*
 * Copyright (c) 2022 Auralis Contributors
 * SelectionFragment.kt is part of Auralis.
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
 
package com.auralis.player.list

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import com.auralis.player.R
import com.auralis.player.music.MusicViewModel
import com.auralis.player.playback.PlaybackViewModel
import com.auralis.player.ui.AuralisToolbar
import com.auralis.player.ui.ViewBindingFragment
import com.auralis.player.util.showToast

/**
 * A subset of ListFragment that implements aspects of the selection UI.
 *
 * @author Auralis Contributors
 */
abstract class SelectionFragment<VB : ViewBinding> :
    ViewBindingFragment<VB>(), Toolbar.OnMenuItemClickListener {
    protected abstract val listModel: ListViewModel
    protected abstract val musicModel: MusicViewModel
    protected abstract val playbackModel: PlaybackViewModel

    open fun getSelectionToolbar(binding: VB): AuralisToolbar? = null

    override fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)
        getSelectionToolbar(binding)?.apply {
            // Add cancel and menu item listeners to manage what occurs with the selection.
            setNavigationOnClickListener { listModel.dropSelection() }
            setOnMenuItemClickListener(this@SelectionFragment)
            setOnOverflowMenuClick {
                listModel.openMenu(R.menu.selection, listModel.peekSelection())
            }
        }
    }

    override fun onDestroyBinding(binding: VB) {
        super.onDestroyBinding(binding)
        getSelectionToolbar(binding)?.setOnMenuItemClickListener(null)
    }

    override fun onMenuItemClick(item: MenuItem) =
        when (item.itemId) {
            R.id.action_selection_play_next -> {
                playbackModel.playNext(listModel.takeSelection())
                requireContext().showToast(R.string.lng_play_next)
                true
            }
            R.id.action_selection_playlist_add -> {
                musicModel.addToPlaylist(listModel.takeSelection())
                true
            }
            else -> false
        }

    // TODO: Re-add the automatic selection handling
}
