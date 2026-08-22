/*
 * Copyright (c) 2023 Auralis Contributors
 * MenuItemAdapter.kt is part of Auralis.
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
 
package com.auralis.player.list.menu

import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.auralis.player.databinding.ItemMenuOptionBinding
import com.auralis.player.list.ClickableListListener
import com.auralis.player.list.adapter.FlexibleListAdapter
import com.auralis.player.list.recycler.DialogRecyclerView
import com.auralis.player.util.inflater

/**
 * Displays a list of [MenuItem]s as custom list items.
 *
 * @param listener A [ClickableListListener] to bind interactions to.
 * @author Auralis Contributors
 */
class MenuItemAdapter(private val listener: ClickableListListener<MenuItem>) :
    FlexibleListAdapter<MenuItem, MenuItemViewHolder>(MenuItemViewHolder.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MenuItemViewHolder.from(parent)

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }
}

/**
 * A [DialogRecyclerView.ViewHolder] that displays a [MenuItem].
 *
 * @author Auralis Contributors
 */
class MenuItemViewHolder private constructor(private val binding: ItemMenuOptionBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     *
     * @param item The new [MenuItem] to bind.
     * @param listener A [ClickableListListener] to bind interactions to.
     */
    fun bind(item: MenuItem, listener: ClickableListListener<MenuItem>) {
        listener.bind(item, this)
        binding.title.apply {
            text = item.title
            setCompoundDrawablesRelativeWithIntrinsicBounds(item.icon, null, null, null)
            isEnabled = item.isEnabled
        }
    }

    companion object {
        /**
         * Create a new instance.
         *
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: ViewGroup) =
            MenuItemViewHolder(ItemMenuOptionBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<MenuItem>() {
                override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem) =
                    oldItem == newItem

                override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem) =
                    oldItem.title.toString() == newItem.title.toString()
            }
    }
}
