/*
 * Copyright (c) 2021 Auxio Project
 * PlaybackPanelFragment.kt is part of Auxio.
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
 
package org.oxycblt.auxio.playback

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import org.oxycblt.auxio.R
import org.oxycblt.auxio.audiobooks.AudiobookBookmark
import org.oxycblt.auxio.audiobooks.AudiobookBookmarkRepository
import org.oxycblt.auxio.audiobooks.AudiobookCatalog
import org.oxycblt.auxio.audiobooks.AudiobookClassifier
import org.oxycblt.auxio.audiobooks.AudiobookPlaybackController
import org.oxycblt.auxio.audiobooks.AudiobookSettings
import org.oxycblt.auxio.databinding.FragmentPlaybackPanelBinding
import org.oxycblt.auxio.detail.DetailViewModel
import org.oxycblt.auxio.list.ListViewModel
import org.oxycblt.auxio.music.resolve
import org.oxycblt.auxio.music.resolveNames
import org.oxycblt.auxio.playback.queue.QueueViewModel
import org.oxycblt.auxio.playback.state.PlaybackCommand
import org.oxycblt.auxio.playback.state.PlaybackStateManager
import org.oxycblt.auxio.playback.state.RepeatMode
import org.oxycblt.auxio.playback.state.ShuffleMode
import org.oxycblt.auxio.playback.ui.StyledSeekBar
import org.oxycblt.auxio.playback.ui.stepper.Direction
import org.oxycblt.auxio.playback.ui.stepper.StepperOverlay
import org.oxycblt.auxio.playback.ui.swiper.CarouselTransformer
import org.oxycblt.auxio.playback.ui.swiper.CoverPagerAdapter
import org.oxycblt.auxio.playback.ui.swiper.UserAwarePagerCallback
import org.oxycblt.auxio.ui.ViewBindingFragment
import org.oxycblt.auxio.util.collectImmediately
import org.oxycblt.auxio.util.dampen
import org.oxycblt.auxio.util.recycler
import org.oxycblt.auxio.util.showToast
import org.oxycblt.auxio.util.smoothScrollByPageTo
import org.oxycblt.auxio.util.systemBarInsetsCompat
import org.oxycblt.musikr.MusicParent
import org.oxycblt.musikr.Song
import timber.log.Timber as L

/**
 * A [ViewBindingFragment] more information about the currently playing song, alongside all
 * available controls.
 *
 * @author Alexander Capehart (OxygenCobalt)
 *
 * TODO: Improve flickering situation on play button
 */
@AndroidEntryPoint
class PlaybackPanelFragment :
    ViewBindingFragment<FragmentPlaybackPanelBinding>(),
    Toolbar.OnMenuItemClickListener,
    StyledSeekBar.Listener,
    StepperOverlay.Listener {
    private val coverPagerAdapter = CoverPagerAdapter(this)
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val detailModel: DetailViewModel by activityViewModels()
    private val listModel: ListViewModel by activityViewModels()
    private val queueModel: QueueViewModel by viewModels()
    @Inject lateinit var playbackManager: PlaybackStateManager
    @Inject lateinit var audiobookPlaybackController: AudiobookPlaybackController
    @Inject lateinit var audiobookSettings: AudiobookSettings
    @Inject lateinit var audiobookBookmarkRepository: AudiobookBookmarkRepository
    @Inject lateinit var commandFactory: PlaybackCommand.Factory
    private var equalizerLauncher: ActivityResultLauncher<Intent>? = null
    private var userAwarePagerCallback: UserAwarePagerCallback? = null
    private var currentPagerPosition = 0
    private var audiobookActionRow: LinearLayout? = null
    private var audiobookSpeedButton: MaterialButton? = null
    private var audiobookSessionActive = false

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentPlaybackPanelBinding.inflate(inflater)

    override fun onBindingCreated(
        binding: FragmentPlaybackPanelBinding,
        savedInstanceState: Bundle?,
    ) {
        super.onBindingCreated(binding, savedInstanceState)

        // AudioEffect expects you to use startActivityForResult with the panel intent. There is no
        // contract analogue for this intent, so the generic contract is used instead.
        equalizerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // Nothing to do
            }

        // --- UI SETUP ---
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.systemBarInsetsCompat
            view.updatePadding(bottom = bars.bottom)
            insets
        }

        binding.playbackToolbar.apply {
            setNavigationOnClickListener { playbackModel.openMain() }
            setOnMenuItemClickListener(this@PlaybackPanelFragment)
        }

        binding.playbackPager?.apply {
            adapter = coverPagerAdapter
            userAwarePagerCallback =
                UserAwarePagerCallback(this) {
                        // Posting the queue goto command prevents the seekbar pos from desyncing
                        // from the song's duration, which creates a visual flicker in the seekbar.
                        post { queueModel.goto(it) }
                    }
                    .also { it.attach() }
            setPageTransformer(CarouselTransformer())
            recycler().apply {
                // Make it possible to collapse the bottom sheet from the ViewPager's touch area.
                isNestedScrollingEnabled = false
                // Visual effect consistency
                // TODO: Custom overscroll?
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            // Make it easier to collapse the bottom sheet
            dampen()
            offscreenPageLimit = 1
        }

        // Set up fast seek overlay
        binding.playbackSong.apply {
            isSelected = true
            setOnClickListener { navigateToCurrentSong() }
        }
        binding.playbackArtist.apply {
            isSelected = true
            setOnClickListener { navigateToCurrentArtist() }
        }
        binding.playbackAlbum?.apply {
            isSelected = true
            setOnClickListener { navigateToCurrentAlbum() }
        }

        binding.playbackSeekBar?.listener = this
        audiobookActionRow = createAudiobookActionRow()
        binding.playbackInfoContainer.addView(audiobookActionRow)

        // Set up actions
        // TODO: Add better playback button accessibility
        binding.playbackRepeat.setOnClickListener { playbackModel.toggleRepeatMode() }
        binding.playbackSkipPrev.setOnClickListener { playbackModel.prev() }
        binding.playbackPlayPause.apply {
            @SuppressLint("RestrictedApi")
            setCornerSpringForce(
                SpringForce().apply {
                    stiffness = 700f
                    dampingRatio = 0.9f
                }
            )
            setOnClickListener { playbackModel.togglePlaying() }
        }
        binding.playbackSkipNext.setOnClickListener { playbackModel.next() }
        binding.playbackShuffle.setOnClickListener { playbackModel.toggleShuffled() }
        binding.playbackMore?.setOnClickListener {
            playbackModel.song.value?.let {
                listModel.openMenu(R.menu.playback_song, it, PlaySong.ByItself)
            }
        }

        // --- VIEWMODEL SETUP --
        collectImmediately(playbackModel.song, ::updateSong)
        collectImmediately(playbackModel.parent, ::updateParent)
        collectImmediately(playbackModel.positionDs, ::updatePosition)
        collectImmediately(playbackModel.repeatMode, ::updateRepeat)
        collectImmediately(playbackModel.isPlaying, ::updatePlaying)
        collectImmediately(playbackModel.isShuffled, ::updateShuffled)
        collectImmediately(playbackModel.pagerQueue, ::updatePager)
    }

    override fun onDestroyBinding(binding: FragmentPlaybackPanelBinding) {
        equalizerLauncher = null
        binding.playbackRepeat.clearPendingIcon()
        binding.playbackSong.isSelected = false
        binding.playbackArtist.isSelected = false
        binding.playbackAlbum?.isSelected = false
        audiobookActionRow = null
        audiobookSpeedButton = null
        audiobookSessionActive = false
        binding.playbackToolbar.setOnMenuItemClickListener(null)
        userAwarePagerCallback?.release()
        binding.playbackPager?.adapter = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_audiobook_controls) {
            showAudiobookControls()
            return true
        }

        if (item.itemId == R.id.action_open_equalizer) {
            // Launch the system equalizer app, if possible.
            L.d("Launching equalizer")
            val equalizerIntent =
                Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    // Provide audio session ID so the equalizer can show options for this app
                    // in particular.
                    .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playbackModel.currentAudioSessionId)
                    // Signal music type so that the equalizer settings are appropriate for
                    // music playback.
                    .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            try {
                requireNotNull(equalizerLauncher) { "Equalizer panel launcher was not available" }
                    .launch(equalizerIntent)
            } catch (e: ActivityNotFoundException) {
                requireContext().showToast(R.string.err_no_app)
            }
            return true
        }

        return false
    }

    override fun onSeekConfirmed(positionDs: Long) {
        playbackModel.seekTo(positionDs)
    }

    private fun updateSong(song: Song?) {
        if (song == null) {
            // Nothing to do.
            return
        }

        val binding = requireBinding()
        val context = requireContext()
        L.d("Updating song display: $song")
        val isAudiobook = AudiobookClassifier.isAudiobook(song)
        if (isAudiobook) {
            // Audiobooks are chapter-led: keep the chapter as the primary line and the book
            // grouping as the secondary line instead of presenting a music artist hierarchy.
            audiobookSessionActive = true
            binding.playbackSong.text = song.name.resolve(context)
            binding.playbackArtist.text = song.album.name.resolve(context)
            binding.playbackAlbum?.text = context.getString(R.string.lbl_audiobook_chapter)
            updateAudiobookSpeed()
        } else {
            if (audiobookSessionActive) {
                playbackManager.playbackSpeed(1.0f)
            }
            audiobookSessionActive = false
            binding.playbackSong.text = song.name.resolve(context)
            binding.playbackArtist.text = song.artists.resolveNames(context)
            binding.playbackAlbum?.text = song.album.name.resolve(context)
        }
        binding.playbackSeekBar?.durationDs = song.durationMs.msToDs()
        binding.playbackToolbar.menu.findItem(R.id.action_audiobook_controls)?.isVisible =
            isAudiobook
        audiobookActionRow?.visibility = if (isAudiobook) View.VISIBLE else View.GONE
    }

    private fun createAudiobookActionRow() =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
            addView(
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        audiobookButton(
                            getString(
                                R.string.lbl_audiobook_skip_back_value,
                                audiobookSettings.skipDurationMs / 1000L,
                            ),
                            R.drawable.ic_skip_prev_24,
                        ) {
                            playbackManager.seekBy(-audiobookSettings.skipDurationMs)
                        },
                        weightedButtonParams(),
                    )
                    addView(
                        audiobookButton(
                            getString(
                                R.string.lbl_audiobook_skip_forward_value,
                                audiobookSettings.skipDurationMs / 1000L,
                            ),
                            R.drawable.ic_skip_next_24,
                        ) {
                            playbackManager.seekBy(audiobookSettings.skipDurationMs)
                        },
                        weightedButtonParams(),
                    )
                }
            )
            addView(
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    audiobookSpeedButton =
                        audiobookButton(getString(R.string.lbl_audiobook_speed), null) {
                            showSpeedPicker()
                        }
                    addView(audiobookSpeedButton, weightedButtonParams())
                    addView(
                        audiobookButton(getString(R.string.lbl_audiobook_chapters), null) {
                            showChapterPicker()
                        },
                        weightedButtonParams(),
                    )
                    addView(
                        audiobookButton(getString(R.string.lbl_audiobook_sleep), null) {
                            showSleepPicker()
                        },
                        weightedButtonParams(),
                    )
                }
            )
        }

    private fun audiobookButton(label: CharSequence, icon: Int?, action: () -> Unit) =
        MaterialButton(requireContext()).apply {
            text = label
            isAllCaps = false
            minHeight = 0
            minWidth = 0
            setPadding(8, 0, 8, 0)
            icon?.let(::setIconResource)
            setOnClickListener { action() }
        }

    private fun weightedButtonParams() =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = 2
            marginEnd = 2
        }

    private fun updateAudiobookSpeed() {
        audiobookSpeedButton?.text =
            getString(R.string.lbl_audiobook_speed_value, playbackManager.playbackSpeed)
    }

    private fun showSpeedPicker() {
        val labels =
            arrayOf(
                getString(R.string.lbl_speed_075),
                getString(R.string.lbl_speed_100),
                getString(R.string.lbl_speed_125),
                getString(R.string.lbl_speed_150),
                getString(R.string.lbl_speed_200),
            )
        val speeds = floatArrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.lbl_audiobook_speed)
            .setItems(labels) { _, which -> setPlaybackSpeed(speeds[which]) }
            .show()
    }

    private fun showSleepPicker() {
        val labels =
            arrayOf(
                getString(R.string.lbl_sleep_15),
                getString(R.string.lbl_sleep_30),
                getString(R.string.lbl_sleep_60),
                getString(R.string.lbl_sleep_end_chapter),
                getString(R.string.lbl_sleep_cancel),
            )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.lbl_audiobook_sleep)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> audiobookPlaybackController.scheduleSleepTimer(15 * 60_000L)
                    1 -> audiobookPlaybackController.scheduleSleepTimer(30 * 60_000L)
                    2 -> audiobookPlaybackController.scheduleSleepTimer(60 * 60_000L)
                    3 -> audiobookPlaybackController.scheduleSleepAtChapterEnd()
                    else -> audiobookPlaybackController.cancelSleepTimer()
                }
            }
            .show()
    }

    private fun setPlaybackSpeed(speed: Float) {
        playbackManager.playbackSpeed(speed)
        updateAudiobookSpeed()
    }

    private fun showChapterPicker() {
        val queue = playbackManager.queue
        val currentSong = playbackManager.currentSong
        if (queue.isEmpty() || currentSong == null || !AudiobookClassifier.isAudiobook(currentSong))
            return

        val bookKey = AudiobookCatalog.bookKey(currentSong)
        val dialog = BottomSheetDialog(requireContext())
        val list =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 24)
                addView(
                    TextView(requireContext()).apply {
                        text = getString(R.string.lbl_audiobook_chapters)
                        textSize = 20f
                        setTextColor(Color.WHITE)
                        setPadding(0, 0, 0, 12)
                    }
                )
                addView(
                    MaterialButton(requireContext()).apply {
                        text = getString(R.string.lbl_audiobook_add_bookmark)
                        isAllCaps = false
                        setOnClickListener {
                            audiobookBookmarkRepository.add(
                                bookKey,
                                currentSong.uid,
                                playbackManager.progression.calculateElapsedPositionMs(),
                            )
                            dialog.dismiss()
                        }
                    }
                )
                val bookmarks = audiobookBookmarkRepository.getForBook(bookKey)
                addView(
                    TextView(requireContext()).apply {
                        text = getString(R.string.lbl_audiobook_bookmarks)
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        setPadding(0, 16, 0, 4)
                    }
                )
                if (bookmarks.isEmpty()) {
                    addView(
                        TextView(requireContext()).apply {
                            text = getString(R.string.lbl_audiobook_no_bookmarks)
                            setTextColor(Color.LTGRAY)
                            setPadding(0, 4, 0, 12)
                        }
                    )
                } else {
                    bookmarks.forEach { bookmark -> addBookmarkRow(bookmark, queue, dialog) }
                }
                addView(
                    TextView(requireContext()).apply {
                        text = getString(R.string.lbl_audiobook_chapters)
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        setPadding(0, 16, 0, 4)
                    }
                )
                queue.forEachIndexed { index, chapter ->
                    addView(
                        TextView(requireContext()).apply {
                            text = buildString {
                                append(if (index == playbackManager.index) "▶ " else "")
                                append(index + 1)
                                append(". ")
                                append(chapter.name.resolve(requireContext()))
                                append("  •  ")
                                append(chapter.durationMs.formatDurationMs(false))
                            }
                            textSize = 16f
                            setTextColor(Color.WHITE)
                            setPadding(0, 14, 0, 14)
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                playbackManager.goto(index)
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }
        dialog.setContentView(ScrollView(requireContext()).apply { addView(list) })
        dialog.show()
    }

    private fun LinearLayout.addBookmarkRow(
        bookmark: AudiobookBookmark,
        queue: List<Song>,
        dialog: BottomSheetDialog,
    ) {
        val chapter = queue.firstOrNull { it.uid == bookmark.chapterUid } ?: return
        addView(
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(requireContext()).apply {
                        text =
                            getString(
                                R.string.lbl_audiobook_bookmark_at,
                                chapter.name.resolve(requireContext()),
                                bookmark.positionMs.formatDurationMs(true),
                            )
                        textSize = 16f
                        setTextColor(Color.WHITE)
                        setPadding(0, 10, 0, 10)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { jumpToBookmark(bookmark, queue, dialog) }
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                )
                addView(
                    MaterialButton(requireContext()).apply {
                        text = getString(R.string.lbl_delete)
                        isAllCaps = false
                        setOnClickListener {
                            audiobookBookmarkRepository.remove(bookmark)
                            dialog.dismiss()
                        }
                    }
                )
            }
        )
    }

    private fun jumpToBookmark(
        bookmark: AudiobookBookmark,
        queue: List<Song>,
        dialog: BottomSheetDialog,
    ) {
        val chapter = queue.firstOrNull { it.uid == bookmark.chapterUid } ?: return
        val command =
            commandFactory.songs(
                songs = queue,
                shuffle = ShuffleMode.OFF,
                startSong = chapter,
                startPositionMs = bookmark.positionMs,
            ) ?: return
        playbackManager.play(command)
        dialog.dismiss()
    }

    private fun showAudiobookControls() {
        val labels =
            arrayOf(
                getString(
                    R.string.lbl_audiobook_skip_back_value,
                    audiobookSettings.skipDurationMs / 1000L,
                ),
                getString(
                    R.string.lbl_audiobook_skip_forward_value,
                    audiobookSettings.skipDurationMs / 1000L,
                ),
                getString(R.string.lbl_speed_075),
                getString(R.string.lbl_speed_100),
                getString(R.string.lbl_speed_125),
                getString(R.string.lbl_speed_150),
                getString(R.string.lbl_speed_200),
                getString(R.string.lbl_sleep_15),
                getString(R.string.lbl_sleep_30),
                getString(R.string.lbl_sleep_60),
                getString(R.string.lbl_sleep_end_chapter),
                getString(R.string.lbl_sleep_cancel),
            )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.lbl_audiobook_controls)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> playbackManager.seekBy(-audiobookSettings.skipDurationMs)
                    1 -> playbackManager.seekBy(audiobookSettings.skipDurationMs)
                    2 -> setPlaybackSpeed(0.75f)
                    3 -> setPlaybackSpeed(1.0f)
                    4 -> setPlaybackSpeed(1.25f)
                    5 -> setPlaybackSpeed(1.5f)
                    6 -> setPlaybackSpeed(2.0f)
                    7 -> audiobookPlaybackController.scheduleSleepTimer(15 * 60_000L)
                    8 -> audiobookPlaybackController.scheduleSleepTimer(30 * 60_000L)
                    9 -> audiobookPlaybackController.scheduleSleepTimer(60 * 60_000L)
                    10 -> audiobookPlaybackController.scheduleSleepAtChapterEnd()
                    11 -> audiobookPlaybackController.cancelSleepTimer()
                }
            }
            .show()
    }

    private fun updateParent(parent: MusicParent?) {
        val binding = requireBinding()
        val context = requireContext()
        binding.playbackToolbar.subtitle =
            parent?.run { name.resolve(context) } ?: context.getString(R.string.lbl_all_songs)
    }

    private fun updatePosition(positionDs: Long) {
        requireBinding().playbackSeekBar?.positionDs = positionDs
    }

    private fun updateRepeat(repeatMode: RepeatMode) {
        val repeatButton = requireBinding().playbackRepeat
        repeatButton.isChecked = repeatMode != RepeatMode.NONE
        repeatButton.setIconResource(repeatMode.icon)
    }

    private fun updatePlaying(isPlaying: Boolean) {
        requireBinding().playbackPlayPause.isChecked = isPlaying
        requireBinding().playbackSeekBar?.setWaveEnabled(isPlaying)
    }

    private fun updateShuffled(isShuffled: Boolean) {
        requireBinding().playbackShuffle.isChecked = isShuffled
    }

    private fun updatePager(queue: PagerQueue) {
        // Defer pager transitions until the current layout pass settles. This avoids the nested
        // bottom-sheet remeasure race observed on fast next/previous navigation.
        requireBinding().playbackPager.apply {
            if (!isAttachedToWindow) {
                post { updatePagerImpl(queue) }
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isHardwareAccelerated) {
                // New version using post-Q frame hooks
                viewTreeObserver.registerFrameCommitCallback {
                    post { postOnAnimation { updatePagerImpl(queue) } }
                }
                postInvalidateOnAnimation()
            } else {
                // Let current layout happen, then wait for the next to conclude
                postOnAnimation { postOnAnimation { updatePagerImpl(queue) } }
            }
        }
    }

    private fun updatePagerImpl(queue: PagerQueue) {
        // Android insanity means this may be executed after view destruction
        // but only on some devices.
        val binding = binding ?: return

        val command = playbackModel.pagerCommand.consume()
        if (command == null) {
            // This probably shouldn't happen in practice, as QueueViewModel directly
            // attaches to PlaybackStateManager and will basically always initialize
            // with a command as a result.
            //
            // If it does happen we should just make sure the UI state is aligned. Don't
            // want broken UI.
            coverPagerAdapter.update(queue.queue, null)
            binding.playbackPager.setCurrentItem(queue.index, false)
            return
        }

        if (command.update != null) {
            // queue needs to be updated.
            coverPagerAdapter.update(queue.queue, command.update)
        }

        if (command.scroll != null) {
            // we need to scroll, however the smooth scroll only really looks best
            // when we are only doing next/prev due to various factors. better to
            // just not animate on outright gotos or queue updates
            val delta = binding.playbackPager.currentItem - command.scroll
            if (delta == 0) {
                // user scroll, carry on
                return
            }
            if (command.update == null && abs(delta) == 1) {
                binding.playbackPager.smoothScrollByPageTo(command.scroll)
            } else {
                binding.playbackPager.setCurrentItem(command.scroll, false)
            }
        }
    }

    private fun navigateToCurrentSong() {
        playbackModel.song.value?.let(detailModel::showAlbum)
    }

    private fun navigateToCurrentArtist() {
        playbackModel.song.value?.let(detailModel::showArtist)
    }

    private fun navigateToCurrentAlbum() {
        playbackModel.song.value?.let { detailModel.showAlbum(it.album) }
    }

    override fun seek(direction: Direction) {
        when (direction) {
            Direction.FORWARDS -> playbackModel.stepForward()
            Direction.BACKWARDS -> playbackModel.stepBackwards()
        }
    }

    private companion object {}
}
