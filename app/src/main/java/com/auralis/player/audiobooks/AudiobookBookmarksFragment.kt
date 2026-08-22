/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookBookmarksFragment.kt is part of Auralis.
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
 
package com.auralis.player.audiobooks

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.auralis.player.R
import com.auralis.player.playback.formatDurationMs
import com.auralis.player.playback.state.PlaybackCommand
import com.auralis.player.playback.state.PlaybackDomain
import com.auralis.player.playback.state.PlaybackStateManager
import com.auralis.player.playback.state.ShuffleMode
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AudiobookBookmarksFragment : Fragment() {
    private val args: AudiobookBookmarksFragmentArgs by navArgs()

    @Inject lateinit var audiobookRepository: AudiobookRepository
    @Inject lateinit var bookmarkRepository: AudiobookBookmarkRepository
    @Inject lateinit var commandFactory: PlaybackCommand.Factory
    @Inject lateinit var playbackManager: PlaybackStateManager

    private lateinit var title: TextView
    private lateinit var empty: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: BookmarkAdapter
    private var currentBook: AudiobookBook? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = requireContext()
        adapter = BookmarkAdapter(::resumeBookmark, ::removeBookmark)
        title = TextView(context).apply { textSize = 24f }
        empty = TextView(context).apply { text = getString(R.string.msg_audiobook_no_bookmarks) }
        recycler =
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@AudiobookBookmarksFragment.adapter
            }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 24.dp(), 20.dp(), 24.dp())
            addView(title)
            addView(empty)
            addView(recycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val book = audiobookRepository.book(args.bookKey) ?: return@launch
            currentBook = book
            title.text = getString(R.string.lbl_audiobook_bookmarks_for, book.title)
            refresh()
        }
    }

    private fun refresh() {
        val book = currentBook ?: return
        val rows =
            bookmarkRepository.getForBook(book.key).map { bookmark ->
                val chapter = book.chapters.firstOrNull { it.uid == bookmark.chapterUid }
                BookmarkRow(bookmark, chapter)
            }
        adapter.submit(rows)
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun resumeBookmark(row: BookmarkRow) {
        val book = currentBook ?: return
        val chapter = row.chapter ?: return
        commandFactory
            .songs(
                songs = book.chapters.map { it.song },
                shuffle = ShuffleMode.OFF,
                startSong = chapter.song,
                startPositionMs = row.bookmark.positionMs,
                domain = PlaybackDomain.AUDIOBOOKS,
            )
            ?.let(playbackManager::play)
    }

    private fun removeBookmark(row: BookmarkRow) {
        bookmarkRepository.remove(row.bookmark)
        refresh()
    }

    private data class BookmarkRow(val bookmark: AudiobookBookmark, val chapter: AudiobookChapter?)

    private class BookmarkAdapter(
        private val onResume: (BookmarkRow) -> Unit,
        private val onRemove: (BookmarkRow) -> Unit,
    ) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {
        private var rows = emptyList<BookmarkRow>()

        fun submit(value: List<BookmarkRow>) {
            rows = value
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    minimumHeight = 64.dp(parent.context)
                    setPadding(0, 12.dp(parent.context), 0, 12.dp(parent.context))
                }
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val row = rows[position]
            val context = holder.root.context
            val chapterTitle =
                row.chapter?.title ?: context.getString(R.string.lbl_audiobook_bookmark_unavailable)
            val embedded =
                row.bookmark.embeddedChapterStartMs
                    ?.let {
                        " · ${context.getString(R.string.lbl_audiobook_embedded_chapter)} ${it.formatDurationMs(true)}"
                    }
                    .orEmpty()
            holder.title.text = chapterTitle
            holder.subtitle.text = "${row.bookmark.positionMs.formatDurationMs(true)}$embedded"
            holder.resume.isEnabled = row.chapter != null
            holder.resume.text = context.getString(R.string.lbl_audiobook_resume)
            holder.resume.setOnClickListener { onResume(row) }
            holder.remove.text = context.getString(R.string.lbl_delete)
            holder.remove.setOnClickListener { onRemove(row) }
        }

        override fun getItemCount() = rows.size

        private class ViewHolder(val root: LinearLayout) : RecyclerView.ViewHolder(root) {
            val title = TextView(root.context).apply { textSize = 17f }
            val subtitle = TextView(root.context).apply { textSize = 14f }
            val resume = MaterialButton(root.context).apply { isAllCaps = false }
            val remove = MaterialButton(root.context).apply { isAllCaps = false }

            init {
                root.addView(title)
                root.addView(subtitle)
                root.addView(
                    LinearLayout(root.context).apply {
                        gravity = Gravity.END
                        addView(resume)
                        addView(remove)
                    }
                )
            }
        }

        private fun Int.dp(context: Context) =
            (this * context.resources.displayMetrics.density).toInt()
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
