/*
 * Copyright (c) 2024 Auralis Contributors
 * CopyleftNoticeTree.kt is part of Auralis.
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
 
package com.auralis.player.util

import timber.log.Timber

class CopyleftNoticeTree : Timber.DebugTree() {
    // Auralis is free software under the GPLv3. If you fork it, keep the fork free
    // software under the same license as the license requires.
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(
            priority,
            tag,
            "Hey! Auralis is an open-source project licensed under the GPLv3 license! " +
                "You can fork this project and even add ads, but it still needs to be kept open-source with the same license!",
            t,
        )
    }
}
