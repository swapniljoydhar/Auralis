/*
 * Copyright (c) 2021 Auralis Contributors
 * RepeatMode.kt is part of Auralis.
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
 
package com.auralis.player.playback.state

import com.auralis.player.IntegerTable
import com.auralis.player.R

/**
 * Represents the current repeat mode of the player.
 *
 * @author Auralis Contributors
 */
enum class RepeatMode {
    /**
     * Do not repeat. Songs are played immediately, and playback is paused when the queue repeats.
     */
    NONE,

    /**
     * Repeat the whole queue. Songs are played immediately, and playback continues when the queue
     * repeats.
     */
    ALL,

    /**
     * Repeat the current song. A Song will be continuously played until skipped. If configured,
     * playback may pause when a Song repeats.
     */
    TRACK;

    /**
     * Increment the mode.
     *
     * @return If [NONE], [ALL]. If [ALL], [TRACK]. If [TRACK], [NONE].
     */
    fun increment() =
        when (this) {
            NONE -> ALL
            ALL -> TRACK
            TRACK -> NONE
        }

    /**
     * The integer representation of this instance.
     *
     * @see fromIntCode
     */
    val icon: Int
        get() =
            when (this) {
                NONE -> R.drawable.ic_repeat_off_24
                ALL -> R.drawable.ic_repeat_on_24
                TRACK -> R.drawable.ic_repeat_one_24
            }

    /** The integer code representing this particular mode. */
    val intCode: Int
        get() =
            when (this) {
                NONE -> IntegerTable.REPEAT_MODE_NONE
                ALL -> IntegerTable.REPEAT_MODE_ALL
                TRACK -> IntegerTable.REPEAT_MODE_TRACK
            }

    companion object {
        /**
         * Convert a [RepeatMode] integer representation into an instance.
         *
         * @param intCode An integer representation of a [RepeatMode]
         * @return The corresponding [RepeatMode], or null if the [RepeatMode] is invalid.
         * @see RepeatMode.intCode
         */
        fun fromIntCode(intCode: Int) =
            when (intCode) {
                IntegerTable.REPEAT_MODE_NONE -> NONE
                IntegerTable.REPEAT_MODE_ALL -> ALL
                IntegerTable.REPEAT_MODE_TRACK -> TRACK
                else -> null
            }
    }
}
