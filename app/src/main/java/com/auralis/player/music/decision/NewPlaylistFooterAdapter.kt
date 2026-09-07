/*
 * Copyright (c) 2023 Auralis Contributors
 * NewPlaylistFooterAdapter.kt is part of Auralis.
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
 
package com.auralis.player.music.decision

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.auralis.player.databinding.ItemNewPlaylistChoiceBinding
import com.auralis.player.list.recycler.DialogRecyclerView
import com.auralis.player.util.inflater

/**
 * A purely-visual [RecyclerView.Adapter] that acts as a footer providing a "New Playlist" choice in
 * [AddToPlaylistDialog].
 *
 * @author Auralis Contributors
 */
class NewPlaylistFooterAdapter(private val listener: Listener) :
    RecyclerView.Adapter<NewPlaylistFooterViewHolder>() {
    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        NewPlaylistFooterViewHolder.from(parent)

    override fun onBindViewHolder(holder: NewPlaylistFooterViewHolder, position: Int) {
        holder.bind(listener)
    }

    /** A listener for [NewPlaylistFooterAdapter] interactions. */
    interface Listener {
        /**
         * Called when the footer has been pressed, requesting to create a new playlist to add to.
         */
        fun onNewPlaylist()
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays a "New Playlist" choice in [NewPlaylistFooterAdapter].
 * Use [from] to create an instance.
 *
 * @author Auralis Contributors
 */
class NewPlaylistFooterViewHolder
private constructor(private val binding: ItemNewPlaylistChoiceBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     *
     * @param listener A [NewPlaylistFooterAdapter.Listener] to bind interactions to.
     */
    fun bind(listener: NewPlaylistFooterAdapter.Listener) {
        binding.root.setOnClickListener { listener.onNewPlaylist() }
    }

    companion object {
        /**
         * Create a new instance.
         *
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            NewPlaylistFooterViewHolder(
                ItemNewPlaylistChoiceBinding.inflate(parent.context.inflater)
            )
    }
}
