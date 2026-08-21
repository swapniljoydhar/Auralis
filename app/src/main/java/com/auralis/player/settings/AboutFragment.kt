/*
 * Copyright (c) 2021 Auxio Project
 * AboutFragment.kt is part of Auralis.
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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.auralis.player.BuildConfig
import com.auralis.player.R
import com.auralis.player.databinding.FragmentAboutBinding
import com.auralis.player.music.MusicViewModel
import com.auralis.player.playback.formatDurationMs
import com.auralis.player.ui.ViewBindingFragment
import com.auralis.player.util.collectImmediately
import com.auralis.player.util.openInBrowser
import com.auralis.player.util.startIntent
import com.auralis.player.util.systemBarInsetsCompat
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [ViewBindingFragment] that displays information about the app and the current music library.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
@AndroidEntryPoint
class AboutFragment : ViewBindingFragment<FragmentAboutBinding>() {
    private val musicModel: MusicViewModel by activityViewModels()

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
        binding.aboutWiki.setOnClickListener { requireContext().openInBrowser(LINK_WIKI) }
        binding.aboutLicenses.setOnClickListener { requireContext().openInBrowser(LINK_LICENSES) }
        binding.aboutProfile.setOnClickListener { requireContext().openInBrowser(LINK_PROFILE) }
        binding.aboutDonate.setOnClickListener { requireContext().openInBrowser(LINK_DONATE) }
        binding.aboutFeedbackGithub.setOnClickListener {
            requireContext().openInBrowser(LINK_NEW_ISSUE)
        }
        binding.aboutFeedbackEmail.setOnClickListener {
            requireContext().sendEmail("feedback@auralis.app")
        }

        binding.aboutSupportersBkkellyh.setOnClickListener {
            requireContext().openInBrowser(LINK_BKKELLYH)
        }
        binding.aboutSupportersGruntstumper.setOnClickListener {
            requireContext().openInBrowser(LINK_GRUNTSTUMPER)
        }

        binding.aboutSupportersPromo.setOnClickListener {
            requireContext().openInBrowser(LINK_DONATE)
        }

        // VIEWMODEL SETUP
        collectImmediately(musicModel.statistics, ::updateStatistics)
    }

    private fun updateStatistics(statistics: MusicViewModel.Statistics?) {
        val binding = requireBinding()
        binding.aboutSongCount.text = getString(R.string.fmt_lib_song_count, statistics?.songs ?: 0)
        requireBinding().aboutAlbumCount.text =
            getString(R.string.fmt_lib_album_count, statistics?.albums ?: 0)
        requireBinding().aboutArtistCount.text =
            getString(R.string.fmt_lib_artist_count, statistics?.artists ?: 0)
        requireBinding().aboutGenreCount.text =
            getString(R.string.fmt_lib_genre_count, statistics?.genres ?: 0)
        binding.aboutTotalDuration.text =
            getString(
                R.string.fmt_lib_total_duration,
                (statistics?.durationMs ?: 0).formatDurationMs(false),
            )

        binding.aboutTotalSize.text =
            getString(
                R.string.fmt_lib_total_size,
                Formatter.formatFileSize(context, statistics?.totalSizeBytes ?: 0L),
            )
    }

    private fun Context.sendEmail(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply { data = "mailto:$recipient".toUri() }
        startIntent(intent)
    }

    private companion object {
        const val LINK_SOURCE = "https://github.com/OxygenCobalt/Auralis"
        const val LINK_WIKI = "$LINK_SOURCE/wiki"
        const val LINK_LICENSES = "$LINK_WIKI/Licenses"
        const val LINK_NEW_ISSUE = "$LINK_SOURCE/issues/new"
        const val LINK_PROFILE = "https://github.com/OxygenCobalt"
        const val LINK_BKKELLYH = "https://github.com/bkkellyh"
        const val LINK_GRUNTSTUMPER = "https://github.com/gruntstumper"
        const val LINK_DONATE = "https://github.com/sponsors/OxygenCobalt"
    }
}
