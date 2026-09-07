/*
 * Copyright (c) 2021 Auralis Contributors
 * AboutFragment.kt is part of Auralis.
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
 
package com.auralis.player.settings

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.auralis.player.BuildConfig
import com.auralis.player.databinding.FragmentAboutBinding
import com.auralis.player.ui.ViewBindingFragment
import com.auralis.player.util.openInBrowser
import com.auralis.player.util.systemBarInsetsCompat
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [ViewBindingFragment] that displays information about the app and the current music library.
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class AboutFragment : ViewBindingFragment<FragmentAboutBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateBinding(inflater: LayoutInflater) = FragmentAboutBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentAboutBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- UI SETUP ---
        binding.aboutToolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.aboutContents.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(bottom = insets.systemBarInsetsCompat.bottom)
            insets
        }
        binding.aboutVersion.text = BuildConfig.VERSION_NAME
        binding.aboutCode.setOnClickListener { requireContext().openInBrowser(LINK_SOURCE) }
        binding.aboutLicenses.setOnClickListener { requireContext().openInBrowser(LINK_LICENSES) }
    }

    private companion object {
        const val LINK_SOURCE = "https://github.com/swapniljoydhar/Auralis"
        const val LINK_LICENSES = "$LINK_SOURCE/blob/dev/LICENSE"
    }
}
