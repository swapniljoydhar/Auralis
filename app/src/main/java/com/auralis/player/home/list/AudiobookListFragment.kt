/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookListFragment.kt is part of Auralis.
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
 
package com.auralis.player.home.list

import android.graphics.Typeface
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
import com.auralis.player.R
import com.auralis.player.audiobooks.AudiobookBook
import com.auralis.player.audiobooks.AudiobookProgress
import com.auralis.player.audiobooks.AudiobookProgressRepository
import com.auralis.player.databinding.FragmentHomeListBinding
import com.auralis.player.home.HomeFragmentDirections
import com.auralis.player.home.HomeViewModel
import com.auralis.player.image.CoverView
import com.auralis.player.music.IndexingState
import com.auralis.player.music.MusicViewModel
import com.auralis.player.playback.formatDurationMs
import com.auralis.player.util.collectImmediately
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Music

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
        current.homeNoMusicMsg.text = getString(R.string.msg_no_audiobooks)
        current.homeNoMusicAction.isVisible = false

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
        adapter.setBooks(books)
        current.homeRecycler.isInvisible = books.isEmpty()
        current.homeNoMusic.isVisible = books.isEmpty()
        current.homeNoMusicAction.isVisible = false
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
        val booksEmpty = adapter.bookCount == 0
        current.homeRecycler.isInvisible = empty || booksEmpty
        current.homeNoMusic.isVisible = empty || booksEmpty
        current.homeNoMusicAction.isVisible = false
    }

    private fun openBook(book: AudiobookBook) {
        findNavController().navigate(HomeFragmentDirections.showAudiobook(book.key))
    }

    private sealed interface AudiobookRow {
        data class Header(val title: String) : AudiobookRow

        data class Book(val book: AudiobookBook, val progress: Map<Music.UID, AudiobookProgress>) :
            AudiobookRow
    }

    private class AudiobookAdapter(private val onClick: (AudiobookBook) -> Unit) :
        ListAdapter<AudiobookRow, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
        private var books = emptyList<AudiobookBook>()
        private var progress = emptyMap<String, Map<Music.UID, AudiobookProgress>>()

        val bookCount: Int
            get() = books.size

        fun setBooks(value: List<AudiobookBook>) {
            books = value
            submitRows()
        }

        fun setProgress(value: Map<String, Map<Music.UID, AudiobookProgress>>) {
            progress = value
            submitRows()
        }

        override fun getItemViewType(position: Int) =
            when (getItem(position)) {
                is AudiobookRow.Header -> VIEW_TYPE_HEADER
                is AudiobookRow.Book -> VIEW_TYPE_BOOK
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                VIEW_TYPE_HEADER -> HeaderViewHolder(parent)
                VIEW_TYPE_BOOK -> AudiobookViewHolder(parent, onClick)
                else -> error("Unknown audiobook row type: $viewType")
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = getItem(position)) {
                is AudiobookRow.Header -> (holder as HeaderViewHolder).bind(row)
                is AudiobookRow.Book -> (holder as AudiobookViewHolder).bind(row)
            }
        }

        private fun submitRows() {
            val current =
                books.filter { book ->
                    progress[book.key].orEmpty().values.any { it.positionMs > 0L || it.completed }
                }
            val notStarted = books - current.toSet()
            submitList(
                buildList {
                    if (current.isNotEmpty()) {
                        add(AudiobookRow.Header("Current"))
                        current.forEach { book ->
                            add(AudiobookRow.Book(book, progress[book.key].orEmpty()))
                        }
                    }
                    if (notStarted.isNotEmpty()) {
                        add(AudiobookRow.Header("Not started"))
                        notStarted.forEach { book ->
                            add(AudiobookRow.Book(book, progress[book.key].orEmpty()))
                        }
                    }
                }
            )
        }

        private class HeaderViewHolder(parent: ViewGroup) :
            RecyclerView.ViewHolder(
                TextView(parent.context).apply {
                    setPadding(24.dp(), 24.dp(), 24.dp(), 8.dp())
                    textSize = 20f
                    setTypeface(typeface, Typeface.BOLD)
                }
            ) {
            fun bind(row: AudiobookRow.Header) {
                (itemView as TextView).text = row.title
            }
        }

        private class AudiobookViewHolder(
            parent: ViewGroup,
            private val onClick: (AudiobookBook) -> Unit,
        ) :
            RecyclerView.ViewHolder(
                LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    minimumHeight = 88.dp()
                    setPadding(24.dp(), 8.dp(), 24.dp(), 8.dp())
                    isClickable = true
                    isFocusable = true
                }
            ) {
            private val root = itemView as LinearLayout
            private val cover = CoverView(parent.context)
            private val title = TextView(parent.context).apply { textSize = 17f }
            private val subtitle = TextView(parent.context).apply { textSize = 14f }

            init {
                root.addView(
                    cover,
                    LinearLayout.LayoutParams(64.dp(), 64.dp()).apply { marginEnd = 16.dp() },
                )
                root.addView(
                    LinearLayout(parent.context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(title)
                        addView(subtitle)
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
            }

            fun bind(row: AudiobookRow.Book) {
                val context = root.context
                val book = row.book
                cover.bind(
                    book.chapters.map { it.song },
                    context.getString(R.string.desc_audiobook_cover),
                    R.drawable.ic_album_24,
                    book.key.hashCode(),
                )
                title.text = book.title
                val author = book.author?.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
                val isCurrent = row.progress.values.any { it.positionMs > 0L || it.completed }
                val progress = row.progress.values.count { it.completed }
                subtitle.text =
                    if (isCurrent) {
                        "Continue · $progress of ${book.chapterCount} complete"
                    } else {
                        "$author${book.chapterCount} chapters · ${book.totalDurationMs.formatDurationMs(false)}"
                    }
                root.setOnClickListener { onClick(book) }
                root.contentDescription = book.title
            }
        }

        private companion object {
            const val VIEW_TYPE_HEADER = 0
            const val VIEW_TYPE_BOOK = 1
            val DIFF_CALLBACK =
                object : DiffUtil.ItemCallback<AudiobookRow>() {
                    override fun areItemsTheSame(old: AudiobookRow, new: AudiobookRow) =
                        when {
                            old is AudiobookRow.Header && new is AudiobookRow.Header ->
                                old.title == new.title
                            old is AudiobookRow.Book && new is AudiobookRow.Book ->
                                old.book.key == new.book.key
                            else -> false
                        }

                    override fun areContentsTheSame(old: AudiobookRow, new: AudiobookRow) =
                        old == new
                }
        }
    }
}

private fun Int.dp() =
    (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
