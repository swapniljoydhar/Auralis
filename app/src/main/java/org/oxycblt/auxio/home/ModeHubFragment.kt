/*
 * Copyright (c) 2026 Auxio Project
 * ModeHubFragment.kt is part of Auxio.
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
 
package org.oxycblt.auxio.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.MusicType

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
    }

    private fun open(mode: MusicType) {
        homeModel.selectMode(mode)
        findNavController().navigate(ModeHubFragmentDirections.openMain())
    }
}
