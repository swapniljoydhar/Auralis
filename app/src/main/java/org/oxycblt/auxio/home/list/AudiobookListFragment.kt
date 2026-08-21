/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookListFragment.kt is part of Auxio.
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
 
package org.oxycblt.auxio.home.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.oxycblt.auxio.R
import org.oxycblt.auxio.audiobooks.AudiobookBook
import org.oxycblt.auxio.audiobooks.AudiobookProgressRepository
import org.oxycblt.auxio.databinding.FragmentHomeListBinding
import org.oxycblt.auxio.home.HomeFragmentDirections
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.music.IndexingState
import org.oxycblt.auxio.music.MusicType
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.util.collectImmediately
import org.oxycblt.auxio.util.showToast

@AndroidEntryPoint
class AudiobookListFragment : Fragment() {
    private val homeModel: HomeViewModel by activityViewModels()
    private val musicModel: MusicViewModel by activityViewModels()

    @Inject lateinit var progressRepository: AudiobookProgressRepository

    private var binding: FragmentHomeListBinding? = null
    private val adapter = AudiobookAdapter(::openBook)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val created = FragmentHomeListBinding.inflate(inflater, container, false)
        binding = created
        return created.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val current = requireNotNull(binding)
        current.homeRecycler.apply {
            id = R.id.home_audiobook_recycler
            adapter = this@AudiobookListFragment.adapter
            setHasFixedSize(true)
        }
        current.homeNoMusicPlaceholder.apply {
            setImageResource(R.drawable.ic_album_48)
            contentDescription = getString(R.string.lbl_audiobooks)
        }
        current.homeNoMusicMsg.text = getString(R.string.msg_add_audiobook_chapters)
        current.homeNoMusicAction.text = getString(R.string.lbl_add_audiobook_chapters)
        current.homeNoMusicAction.setOnClickListener {
            homeModel.selectMode(MusicType.SONGS)
            requireContext().showToast(R.string.msg_add_audiobook_chapters)
        }

        collectImmediately(homeModel.audiobookList, ::updateBooks)
        collectImmediately(homeModel.empty, musicModel.indexingState, ::updateEmpty)
    }

    override fun onDestroyView() {
        binding?.homeRecycler?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun updateBooks(books: List<AudiobookBook>) {
        val current = binding ?: return
        adapter.submitList(books)
        current.homeRecycler.isInvisible = books.isEmpty()
        current.homeNoMusic.isVisible = books.isEmpty()
        current.homeNoMusicAction.isVisible = books.isEmpty()
        lifecycleScope.launch {
            val progress =
                books.associate { book ->
                    book.key to
                        progressRepository.getForBook(book.key).associateBy { it.chapterUid }
                }
            adapter.setProgress(progress)
        }
    }

    private fun updateEmpty(empty: Boolean, indexingState: IndexingState?) {
        val current = binding ?: return
        val booksEmpty = adapter.itemCount == 0
        current.homeRecycler.isInvisible = empty || booksEmpty
        current.homeNoMusic.isVisible = empty || booksEmpty
        current.homeNoMusicAction.isVisible = empty && indexingState != null
    }

    private fun openBook(book: AudiobookBook) {
        findNavController().navigate(HomeFragmentDirections.showAudiobook(book.key))
    }

    private class AudiobookAdapter(private val onClick: (AudiobookBook) -> Unit) :
        ListAdapter<AudiobookBook, AudiobookViewHolder>(DIFF_CALLBACK) {
        private var progress:
            Map<
                String,
                Map<org.oxycblt.musikr.Music.UID, org.oxycblt.auxio.audiobooks.AudiobookProgress>,
            > =
            emptyMap()

        fun setProgress(
            value:
                Map<
                    String,
                    Map<
                        org.oxycblt.musikr.Music.UID,
                        org.oxycblt.auxio.audiobooks.AudiobookProgress,
                    >,
                >
        ) {
            progress = value
            notifyItemRangeChanged(0, itemCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudiobookViewHolder =
            AudiobookViewHolder(parent, onClick)

        override fun onBindViewHolder(holder: AudiobookViewHolder, position: Int) {
            val book = getItem(position)
            holder.bind(book, progress[book.key].orEmpty())
        }

        private companion object {
            val DIFF_CALLBACK =
                object : DiffUtil.ItemCallback<AudiobookBook>() {
                    override fun areItemsTheSame(old: AudiobookBook, new: AudiobookBook) =
                        old.key == new.key

                    override fun areContentsTheSame(old: AudiobookBook, new: AudiobookBook) =
                        old == new
                }
        }
    }

    private class AudiobookViewHolder(
        parent: ViewGroup,
        private val onClick: (AudiobookBook) -> Unit,
    ) :
        RecyclerView.ViewHolder(
            LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                isClickable = true
                isFocusable = true
            }
        ) {
        private val root = itemView as LinearLayout
        private val title = TextView(parent.context).apply { textSize = 18f }
        private val subtitle = TextView(parent.context).apply { textSize = 14f }

        init {
            root.addView(title)
            root.addView(subtitle)
        }

        fun bind(
            book: AudiobookBook,
            progress:
                Map<org.oxycblt.musikr.Music.UID, org.oxycblt.auxio.audiobooks.AudiobookProgress>,
        ) {
            title.text = book.title
            val author = book.author?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            val completed = progress.values.count { it.completed }
            subtitle.text = "${book.chapterCount} chapters$author · $completed complete"
            root.setOnClickListener { onClick(book) }
            root.contentDescription = book.title
        }
    }
}
