/*
 * Copyright (c) 2026 Auralis Contributors
 * AudiobookDetailFragment.kt is part of Auralis.
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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.R as AR
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.auralis.player.R
import com.auralis.player.databinding.FragmentAudiobookDetailBinding
import com.auralis.player.databinding.ItemAudiobookChapterBinding
import com.auralis.player.image.CoverView
import com.auralis.player.playback.formatDurationMs
import com.auralis.player.playback.state.PlaybackCommand
import com.auralis.player.playback.state.PlaybackDomain
import com.auralis.player.playback.state.PlaybackStateManager
import com.auralis.player.playback.state.ShuffleMode
import com.google.android.material.R as MR
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.oxycblt.musikr.Music

@AndroidEntryPoint
class AudiobookDetailFragment : Fragment() {
    private val args: AudiobookDetailFragmentArgs by navArgs()

    @Inject lateinit var audiobookRepository: AudiobookRepository
    @Inject lateinit var progressRepository: AudiobookProgressRepository
    @Inject lateinit var playbackManager: PlaybackStateManager
    @Inject lateinit var commandFactory: PlaybackCommand.Factory

    private var _binding: FragmentAudiobookDetailBinding? = null
    private val binding get() = _binding!!
    private var currentBook: AudiobookBook? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAudiobookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val book = audiobookRepository.book(args.bookKey) ?: return@launch
            currentBook = book
            render(book)
        }
    }

    override fun onDestroyView() {
        currentBook = null
        _binding = null
        super.onDestroyView()
    }

    private suspend fun render(book: AudiobookBook) {
        val b = _binding ?: return
        val context = requireContext()
        val progress = progressRepository.getForBook(book.key).associateBy { it.chapterUid }
        val listeningSummary =
            AudiobookListeningState.summarize(
                book.chapters.map { chapter ->
                    progress[chapter.uid]?.let {
                        AudiobookProgressInput(
                            chapter.durationMs,
                            it.positionMs,
                            it.completed,
                            it.updatedMs,
                        )
                    } ?: AudiobookProgressInput(chapter.durationMs, 0L, false, 0L)
                }
            )
        val resumeChapter = book.chapters.firstOrNull { progress[it.uid]?.completed != true }
        val hasSavedProgress = progress.values.any { it.positionMs > 0L || it.completed }

        // Cover
        b.audiobookCover.bind(
            book.chapters.map { it.song },
            getString(R.string.desc_audiobook_cover),
            R.drawable.ic_album_24,
            book.key.hashCode(),
        )

        // Title and author
        b.audiobookTitle.text = book.title
        b.audiobookAuthor.text = book.author?.takeIf { it.isNotBlank() }
        b.audiobookAuthor.visibility =
            if (book.author.isNullOrBlank()) View.GONE else View.VISIBLE

        // Summary
        b.audiobookSummary.text =
            getString(
                R.string.lbl_audiobook_summary,
                book.totalDurationMs.formatDurationMs(false),
                book.chapterCount,
            )

        // Progress
        b.audiobookProgressBar.progress = listeningSummary.percentage
        b.audiobookProgressBar.contentDescription =
            getString(
                R.string.desc_audiobook_progress_percent,
                listeningSummary.percentage,
                listeningSummary.remainingMs.formatDurationMs(false),
            )
        b.audiobookProgressText.text =
            getString(
                R.string.lbl_audiobook_progress_percent,
                listeningSummary.percentage,
                listeningSummary.listenedMs.formatDurationMs(false),
                listeningSummary.remainingMs.formatDurationMs(false),
            )

        // Primary action button
        b.audiobookActionPrimary.text =
            when {
                resumeChapter == null -> getString(R.string.lbl_audiobook_restart)
                hasSavedProgress -> getString(R.string.lbl_audiobook_continue)
                else -> getString(R.string.lbl_audiobook_start)
            }
        b.audiobookActionPrimary.setOnClickListener { startBook(book, progress) }

        // Bookmarks button
        b.audiobookActionBookmarks.setOnClickListener {
            findNavController()
                .navigate(AudiobookDetailFragmentDirections.showAudiobookBookmarks(book.key))
        }

        // Reset button
        if (hasSavedProgress) {
            b.audiobookActionReset.visibility = View.VISIBLE
            b.audiobookActionReset.setTextColor(
                MaterialColors.getColor(context, AR.attr.colorError, 0)
            )
            b.audiobookActionReset.strokeColor =
                ColorStateList.valueOf(MaterialColors.getColor(context, AR.attr.colorError, 0))
            b.audiobookActionReset.setOnClickListener {
                lifecycleScope.launch {
                    progressRepository.clearBook(book.key)
                    render(book)
                }
            }
        } else {
            b.audiobookActionReset.visibility = View.GONE
        }

        // Chapters
        b.audiobookChaptersContainer.removeAllViews()
        book.chapters.forEach { chapter ->
            val saved = progress[chapter.uid]
            val isComplete = saved?.completed == true
            val chapterBinding =
                ItemAudiobookChapterBinding.inflate(
                    LayoutInflater.from(context),
                    b.audiobookChaptersContainer,
                    false,
                )
            chapterBinding.chapterNumber.text = chapter.number.toString()
            chapterBinding.chapterTitle.text = chapter.title
            chapterBinding.chapterDuration.text =
                if (isComplete) {
                    getString(
                        R.string.lbl_audiobook_chapter_complete,
                        chapter.durationMs.formatDurationMs(false),
                    )
                } else {
                    getString(
                        R.string.lbl_audiobook_chapter_duration,
                        chapter.durationMs.formatDurationMs(false),
                    )
                }
            chapterBinding.chapterCheck.visibility = if (isComplete) View.VISIBLE else View.INVISIBLE
            (chapterBinding.root as MaterialCardView).setOnClickListener {
                startChapter(book, chapter, progress)
            }
            b.audiobookChaptersContainer.addView(chapterBinding.root)
        }
    }

    private fun startBook(book: AudiobookBook, progress: Map<Music.UID, AudiobookProgress>) {
        val chapter =
            book.chapters.firstOrNull { progress[it.uid]?.completed != true }
                ?: book.chapters.firstOrNull()
                ?: return
        startChapter(book, chapter, progress)
    }

    private fun startChapter(
        book: AudiobookBook,
        chapter: AudiobookChapter,
        progress: Map<Music.UID, AudiobookProgress>,
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
                    domain = PlaybackDomain.AUDIOBOOKS,
                ) ?: return@launch
            playbackManager.play(command)
        }
    }
}
