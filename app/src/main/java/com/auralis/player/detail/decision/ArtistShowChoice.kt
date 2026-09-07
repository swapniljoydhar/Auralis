/*
 * Copyright (c) 2023 Auralis Contributors
 * ArtistShowChoice.kt is part of Auralis.
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
 
package com.auralis.player.detail.decision

import android.view.ViewGroup
import com.auralis.musikr.Artist
import com.auralis.player.databinding.ItemPickerChoiceBinding
import com.auralis.player.list.ClickableListListener
import com.auralis.player.list.adapter.FlexibleListAdapter
import com.auralis.player.list.adapter.SimpleDiffCallback
import com.auralis.player.list.recycler.DialogRecyclerView
import com.auralis.player.music.resolve
import com.auralis.player.util.context
import com.auralis.player.util.inflater

/**
 * A [FlexibleListAdapter] that displays a list of [Artist] navigation choices, for use with
 * [ShowArtistDialog].
 *
 * @param listener A [ClickableListListener] to bind interactions to.
 */
class ArtistShowChoice(private val listener: ClickableListListener<Artist>) :
    FlexibleListAdapter<Artist, ArtistNavigationChoiceViewHolder>(
        ArtistNavigationChoiceViewHolder.DIFF_CALLBACK
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ArtistNavigationChoiceViewHolder.from(parent)

    override fun onBindViewHolder(holder: ArtistNavigationChoiceViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }
}

/**
 * A [DialogRecyclerView.ViewHolder] that displays a smaller variant of a typical [Artist] item, for
 * use [ArtistShowChoice]. Use [from] to create an instance.
 *
 * @author Auralis Contributors
 */
class ArtistNavigationChoiceViewHolder
private constructor(private val binding: ItemPickerChoiceBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     *
     * @param artist The new [Artist] to bind.
     * @param listener A [ClickableListListener] to bind interactions to.
     */
    fun bind(artist: Artist, listener: ClickableListListener<Artist>) {
        listener.bind(artist, this)
        binding.pickerImage.bind(artist)
        binding.pickerName.text = artist.name.resolve(binding.context)
    }

    companion object {

        /**
         * Create a new instance.
         *
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: ViewGroup) =
            ArtistNavigationChoiceViewHolder(
                ItemPickerChoiceBinding.inflate(parent.context.inflater, parent, false)
            )

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Artist>() {
                override fun areContentsTheSame(oldItem: Artist, newItem: Artist) =
                    oldItem.name.compareTo(newItem.name) == 0
            }
    }
}
