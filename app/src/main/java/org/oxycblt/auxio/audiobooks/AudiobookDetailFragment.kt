/*
 * Copyright (c) 2026 Auxio Project
 * AudiobookDetailFragment.kt is part of Auxio.
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
 
package org.oxycblt.auxio.audiobooks

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.oxycblt.auxio.R
import org.oxycblt.auxio.playback.state.PlaybackCommand
import org.oxycblt.auxio.playback.state.PlaybackStateManager
import org.oxycblt.auxio.playback.state.ShuffleMode

@AndroidEntryPoint
class AudiobookDetailFragment : Fragment() {
    private val args: AudiobookDetailFragmentArgs by navArgs()

    @Inject lateinit var audiobookRepository: AudiobookRepository
    @Inject lateinit var progressRepository: AudiobookProgressRepository
    @Inject lateinit var playbackManager: PlaybackStateManager
    @Inject lateinit var commandFactory: PlaybackCommand.Factory

    private var content: LinearLayout? = null
    private var currentBook: AudiobookBook? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }
        content = root
        return ScrollView(requireContext()).apply { addView(root) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val book = audiobookRepository.book(args.bookKey) ?: return@launch
            currentBook = book
            render(book)
        }
    }

    override fun onDestroyView() {
        content = null
        super.onDestroyView()
    }

    private suspend fun render(book: AudiobookBook) {
        val root = content ?: return
        val progress = progressRepository.getForBook(book.key).associateBy { it.chapterUid }
        root.removeAllViews()

        root.addView(
            TextView(requireContext()).apply {
                text = book.title
                textSize = 24f
                contentDescription = book.title
            }
        )
        book.author
            ?.takeIf { it.isNotBlank() }
            ?.let { author ->
                root.addView(
                    TextView(requireContext()).apply {
                        text = author
                        textSize = 16f
                    }
                )
            }

        root.addView(
            MaterialButton(requireContext()).apply {
                text = getString(R.string.lbl_play)
                setOnClickListener { startBook(book, progress) }
            }
        )
        root.addView(
            MaterialButton(requireContext()).apply {
                text = getString(R.string.lbl_reset)
                setOnClickListener {
                    lifecycleScope.launch {
                        progressRepository.clearBook(book.key)
                        render(book)
                    }
                }
            }
        )

        book.chapters.forEach { chapter ->
            val saved = progress[chapter.uid]
            root.addView(
                TextView(requireContext()).apply {
                    text = buildString {
                        append(chapter.number)
                        append(". ")
                        append(chapter.title)
                        if (saved != null) {
                            append("  ·  ")
                            append(
                                if (saved.completed) "Complete"
                                else formatPosition(saved.positionMs)
                            )
                        }
                    }
                    textSize = 16f
                    setPadding(0, 18, 0, 18)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { startChapter(book, chapter, progress) }
                }
            )
        }
    }

    private fun startBook(
        book: AudiobookBook,
        progress: Map<org.oxycblt.musikr.Music.UID, AudiobookProgress>,
    ) {
        val chapter =
            book.chapters.firstOrNull { progress[it.uid]?.completed != true }
                ?: book.chapters.firstOrNull()
                ?: return
        startChapter(book, chapter, progress)
    }

    private fun startChapter(
        book: AudiobookBook,
        chapter: AudiobookChapter,
        progress: Map<org.oxycblt.musikr.Music.UID, AudiobookProgress>,
    ) {
        lifecycleScope.launch {
            val saved = progress[chapter.uid]
            val position = if (saved?.completed == true) 0L else saved?.positionMs ?: 0L
            val command =
                commandFactory.songs(
                    songs = book.chapters.map { it.song },
                    shuffle = ShuffleMode.OFF,
                    startSong = chapter.song,
                    startPositionMs = position,
                ) ?: return@launch
            playbackManager.play(command)
        }
    }

    private fun formatPosition(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
        return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }
}
