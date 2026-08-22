/*
 * Copyright (c) 2023 Auralis Contributors
 * MusicPreferenceFragment.kt is part of Auralis.
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
 
package com.auralis.player.settings.categories

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import coil3.ImageLoader
import com.auralis.player.R
import com.auralis.player.music.MusicViewModel
import com.auralis.player.settings.BasePreferenceFragment
import com.auralis.player.settings.ui.WrappedDialogPreference
import com.auralis.player.util.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber as L

/**
 * "Content" settings.
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class MusicPreferenceFragment : BasePreferenceFragment(R.xml.preferences_music) {
    private val musicModel: MusicViewModel by viewModels()
    @Inject lateinit var imageLoader: ImageLoader

    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_separators)) {
            L.d("Navigating to separator dialog")
            findNavController().navigateSafe(MusicPreferenceFragmentDirections.separatorsSettings())
        }
    }

    override fun onSetupPreference(preference: Preference) {
        if (preference.key == getString(R.string.set_key_cover_mode)) {
            L.d("Configuring cover mode setting")
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    L.d("Cover mode changed, reloading music")
                    musicModel.refresh()
                    true
                }
        }
        if (preference.key == getString(R.string.set_key_square_covers)) {
            L.d("Configuring square cover setting")
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    L.d("Cover mode changed, resetting image memory cache")
                    imageLoader.memoryCache?.clear()
                    true
                }
        }
        if (preference.key == getString(R.string.set_key_with_hidden)) {
            L.d("Configuring ignore hidden files setting")
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    L.d("Ignore hidden files setting changed, reloading music")
                    musicModel.refresh()
                    true
                }
        }
    }
}
