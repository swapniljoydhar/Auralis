/*
 * Copyright (c) 2026 Auralis Contributors
 * ModeHubFragment.kt is part of Auralis.
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
 
package com.auralis.player.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.auralis.player.R
import com.auralis.player.music.MusicType
import com.auralis.player.util.navigateSafe
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ModeHubFragment : Fragment(R.layout.fragment_mode_hub) {
    private val homeModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.action_music).setOnClickListener { open(MusicType.SONGS) }
        view.findViewById<View>(R.id.action_music_button).setOnClickListener {
            open(MusicType.SONGS)
        }
        view.findViewById<View>(R.id.action_audiobooks).setOnClickListener {
            open(MusicType.AUDIOBOOKS)
        }
        view.findViewById<View>(R.id.action_audiobooks_button).setOnClickListener {
            open(MusicType.AUDIOBOOKS)
        }

        // Keep the choice visible on a first launch. Later launches go straight
        // back to the library the listener last used.
        if (savedInstanceState == null) {
            homeModel.preferredMode?.let(::open)
        }
    }

    private fun open(mode: MusicType) {
        homeModel.selectMode(mode)
        findNavController().navigateSafe(ModeHubFragmentDirections.openMain())
    }
}
