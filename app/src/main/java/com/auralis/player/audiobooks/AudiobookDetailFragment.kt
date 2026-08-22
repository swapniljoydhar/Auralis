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
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.R as AR
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.auralis.player.R
import com.auralis.player.image.CoverView
import com.auralis.player.playback.formatDurationMs
import com.auralis.player.playback.state.PlaybackCommand
import com.auralis.player.playback.state.PlaybackStateManager
import com.auralis.player.playback.state.ShuffleMode
import com.google.android.material.R as MR
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
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

    private var content: LinearLayout? = null
    private var currentBook: AudiobookBook? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20.dp(), 24.dp(), 20.dp(), 32.dp())
            }
        content = root
        return ScrollView(requireContext()).apply {
            clipToPadding = false
            addView(root)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val book = audiobookRepository.book(args.bookKey) ?: return@launch
            currentBook = book
            render(book)
        }
    }

    override fun onDestroyView() {
        currentBook = null
        content = null
        super.onDestroyView()
    }

    private suspend fun render(book: AudiobookBook) {
        val root = content ?: return
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
        val completed = listeningSummary.completedChapters
        val resumeChapter = book.chapters.firstOrNull { progress[it.uid]?.completed != true }
        val hasSavedProgress = progress.values.any { it.positionMs > 0L || it.completed }
        val colors = requireContext()
        root.removeAllViews()

        val cover =
            LayoutInflater.from(colors).inflate(R.layout.view_audiobook_cover, root, false)
                as CoverView
        cover.bind(
            book.chapters.map { it.song },
            getString(R.string.desc_audiobook_cover),
            R.drawable.ic_album_24,
            book.key.hashCode(),
        )
        root.addView(cover)

        root.addView(
            TextView(colors).apply {
                text = book.title
                textSize = 26f
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                contentDescription = book.title
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = fullWidthParams(top = 16, bottom = 4)
            }
        )

        book.author
            ?.takeIf { it.isNotBlank() }
            ?.let { author ->
                root.addView(
                    TextView(colors).apply {
                        text = author
                        textSize = 16f
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        layoutParams = fullWidthParams(bottom = 4)
                    }
                )
            }

        root.addView(
            TextView(colors).apply {
                text =
                    getString(
                        R.string.lbl_audiobook_summary,
                        book.totalDurationMs.formatDurationMs(false),
                        book.chapterCount,
                    )
                textSize = 14f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(
                    MaterialColors.getColor(colors, MR.attr.colorOnSurfaceVariant, Color.GRAY)
                )
                layoutParams = fullWidthParams(bottom = 16)
            }
        )

        root.addView(
            LinearProgressIndicator(colors).apply {
                max = 100
                this.progress = listeningSummary.percentage
                isIndeterminate = false
                contentDescription =
                    getString(
                        R.string.desc_audiobook_progress_percent,
                        listeningSummary.percentage,
                        listeningSummary.remainingMs.formatDurationMs(false),
                    )
                layoutParams = fullWidthParams(bottom = 4)
            }
        )

        root.addView(
            TextView(colors).apply {
                text =
                    getString(
                        R.string.lbl_audiobook_progress_percent,
                        listeningSummary.percentage,
                        listeningSummary.listenedMs.formatDurationMs(false),
                        listeningSummary.remainingMs.formatDurationMs(false),
                    )
                textSize = 14f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(
                    MaterialColors.getColor(colors, MR.attr.colorOnSurfaceVariant, Color.GRAY)
                )
                layoutParams = fullWidthParams(bottom = 16)
            }
        )

        root.addView(
            MaterialButton(colors).apply {
                text =
                    when {
                        resumeChapter == null -> getString(R.string.lbl_audiobook_restart)
                        hasSavedProgress -> getString(R.string.lbl_audiobook_continue)
                        else -> getString(R.string.lbl_audiobook_start)
                    }
                isAllCaps = false
                setOnClickListener { startBook(book, progress) }
                layoutParams = fullWidthParams(bottom = 8)
            }
        )

        root.addView(
            MaterialButton(colors).apply {
                text = getString(R.string.lbl_audiobook_bookmarks)
                isAllCaps = false
                setOnClickListener {
                    findNavController()
                        .navigate(
                            AudiobookDetailFragmentDirections.showAudiobookBookmarks(book.key)
                        )
                }
                layoutParams = fullWidthParams(bottom = 8)
            }
        )

        if (hasSavedProgress) {
            root.addView(
                MaterialButton(colors).apply {
                    text = getString(R.string.lbl_audiobook_reset_progress)
                    isAllCaps = false
                    setTextColor(
                        MaterialColors.getColor(colors, MR.attr.colorOnPrimary, Color.WHITE)
                    )
                    setOnClickListener {
                        lifecycleScope.launch {
                            progressRepository.clearBook(book.key)
                            render(book)
                        }
                    }
                    layoutParams = fullWidthParams(bottom = 20)
                }
            )
        }

        root.addView(
            TextView(colors).apply {
                text = getString(R.string.lbl_audiobook_chapters)
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = fullWidthParams(bottom = 8)
            }
        )

        book.chapters.forEach { chapter ->
            val saved = progress[chapter.uid]
            val isComplete = saved?.completed == true
            root.addView(
                MaterialCardView(colors).apply {
                    isClickable = true
                    isFocusable = true
                    setCardBackgroundColor(
                        MaterialColors.getColor(colors, MR.attr.colorSurfaceContainer, Color.DKGRAY)
                    )
                    setOnClickListener { startChapter(book, chapter, progress) }
                    layoutParams = fullWidthParams(bottom = 8)
                    addView(
                        LinearLayout(colors).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(16.dp())
                            addView(
                                TextView(colors).apply {
                                    text = chapter.number.toString()
                                    textSize = 16f
                                    setTextColor(
                                        MaterialColors.getColor(
                                            colors,
                                            AR.attr.colorPrimary,
                                            Color.WHITE,
                                        )
                                    )
                                    gravity = Gravity.CENTER
                                    layoutParams = LinearLayout.LayoutParams(32.dp(), 32.dp())
                                }
                            )
                            addView(
                                LinearLayout(colors).apply {
                                    orientation = LinearLayout.VERTICAL
                                    layoutParams =
                                        LinearLayout.LayoutParams(
                                                0,
                                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                                1f,
                                            )
                                            .apply { marginStart = 12.dp() }
                                    addView(
                                        TextView(colors).apply {
                                            text = chapter.title
                                            textSize = 16f
                                        }
                                    )
                                    addView(
                                        TextView(colors).apply {
                                            text =
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
                                            textSize = 14f
                                            setTextColor(
                                                MaterialColors.getColor(
                                                    colors,
                                                    MR.attr.colorOnSurfaceVariant,
                                                    Color.GRAY,
                                                )
                                            )
                                            layoutParams = fullWidthParams(top = 4)
                                        }
                                    )
                                }
                            )
                            addView(
                                ImageView(colors).apply {
                                    setImageResource(R.drawable.ic_check_24)
                                    imageTintList =
                                        ColorStateList.valueOf(
                                            MaterialColors.getColor(
                                                colors,
                                                AR.attr.colorPrimary,
                                                Color.WHITE,
                                            )
                                        )
                                    contentDescription =
                                        if (isComplete) {
                                            getString(R.string.lbl_audiobook_chapter_complete_short)
                                        } else null
                                    visibility = if (isComplete) View.VISIBLE else View.INVISIBLE
                                    layoutParams = LinearLayout.LayoutParams(24.dp(), 24.dp())
                                }
                            )
                        }
                    )
                }
            )
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
                    domain = com.auralis.player.playback.state.PlaybackDomain.AUDIOBOOKS,
                ) ?: return@launch
            playbackManager.play(command)
        }
    }

    private fun fullWidthParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            .apply {
                topMargin = top.dp()
                bottomMargin = bottom.dp()
            }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
