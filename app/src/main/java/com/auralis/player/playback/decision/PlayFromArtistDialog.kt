/*
 * Copyright (c) 2022 Auralis Contributors
 * PlayFromArtistDialog.kt is part of Auralis.
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
 
package com.auralis.player.playback.decision

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.auralis.player.R
import com.auralis.player.databinding.DialogMusicChoicesBinding
import com.auralis.player.list.ClickableListListener
import com.auralis.player.list.adapter.UpdateInstructions
import com.auralis.player.playback.PlaybackViewModel
import com.auralis.player.ui.ViewBindingMaterialDialogFragment
import com.auralis.player.util.collectImmediately
import com.auralis.player.util.unlikelyToBeNull
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.musikr.Artist
import org.oxycblt.musikr.Song
import timber.log.Timber as L

/**
 * A picker [ViewBindingMaterialDialogFragment] intended for when [Artist] playback is ambiguous.
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class PlayFromArtistDialog :
    ViewBindingMaterialDialogFragment<DialogMusicChoicesBinding>(), ClickableListListener<Artist> {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val pickerModel: PlaybackPickerViewModel by viewModels()
    // Information about what Song to show choices for is initially within the navigation arguments
    // as UIDs, as that is the only safe way to parcel a Song.
    private val args: PlayFromArtistDialogArgs by navArgs()
    private val choiceAdapter = ArtistPlaybackChoiceAdapter(this)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.lbl_artists).setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogMusicChoicesBinding.inflate(inflater)

    override fun onBindingCreated(binding: DialogMusicChoicesBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.choiceRecycler.apply {
            itemAnimator = null
            adapter = choiceAdapter
        }

        playbackModel.playbackDecision.consume()
        pickerModel.setPickerSongUid(args.songUid)
        collectImmediately(pickerModel.currentPickerSong, ::updateSong)
    }

    override fun onDestroyBinding(binding: DialogMusicChoicesBinding) {
        super.onDestroyBinding(binding)
        binding.choiceRecycler.adapter = null
    }

    override fun onClick(item: Artist, viewHolder: RecyclerView.ViewHolder) {
        // User made a choice, play the given song from that artist.
        val song = unlikelyToBeNull(pickerModel.currentPickerSong.value)
        playbackModel.playFromArtist(song, item)
        findNavController().navigateUp()
    }

    private fun updateSong(song: Song?) {
        if (song == null) {
            L.d("No song to show choices for, navigating away")
            findNavController().navigateUp()
            return
        }
        choiceAdapter.update(song.artists, UpdateInstructions.Replace(0))
    }
}
