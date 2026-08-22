/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookPreferenceFragment.kt is part of Auralis.
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

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.auralis.player.R
import com.auralis.player.audiobooks.AudiobookFolderOrganization
import com.auralis.player.audiobooks.AudiobookRepository
import com.auralis.player.audiobooks.AudiobookSettings
import com.auralis.player.settings.BasePreferenceFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AudiobookPreferenceFragment : BasePreferenceFragment(R.xml.preferences_audiobooks) {
    @Inject lateinit var audiobookRepository: AudiobookRepository
    @Inject lateinit var audiobookSettings: AudiobookSettings

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val selectedFolders =
            findPreference<MultiSelectListPreference>(
                getString(R.string.set_key_audiobook_selected_folders)
            ) ?: return
        val folders = audiobookRepository.folders().toTypedArray()
        selectedFolders.entries = folders
        selectedFolders.entryValues = folders
        fun updateSelectedFoldersVisibility() {
            selectedFolders.isVisible =
                audiobookSettings.useSelectedFolders ||
                    audiobookSettings.folderOrganization.requiresSelectedRoots
        }
        updateSelectedFoldersVisibility()
        findPreference<SwitchPreferenceCompat>(
                getString(R.string.set_key_audiobook_selected_folders_enabled)
            )
            ?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                selectedFolders.isVisible =
                    (value as Boolean) || audiobookSettings.folderOrganization.requiresSelectedRoots
                true
            }
        findPreference<ListPreference>(getString(R.string.set_key_audiobook_folder_organization))
            ?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, value ->
                selectedFolders.isVisible =
                    audiobookSettings.useSelectedFolders ||
                        AudiobookFolderOrganization.fromPreference(value.toString().toInt())
                            .requiresSelectedRoots
                true
            }
    }
}
