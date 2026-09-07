/*
 * Copyright (c) 2022 Auralis Contributors
 * ErrorCrossfadeTransitionFactory.kt is part of Auralis.
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
 
package com.auralis.player.image.coil

import coil3.decode.DataSource
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.transition.CrossfadeDrawable
import coil3.transition.CrossfadeTransition
import coil3.transition.Transition
import coil3.transition.TransitionTarget

/**
 * A copy of [CrossfadeTransition.Factory] that also applies a transition to error results.
 *
 * @author Coil Team, Auralis Contributors
 */
class ErrorCrossfadeTransitionFactory : Transition.Factory {
    override fun create(target: TransitionTarget, result: ImageResult): Transition {
        // Don't animate if the request was fulfilled by the memory cache.
        if (result is SuccessResult && result.dataSource == DataSource.MEMORY_CACHE) {
            return Transition.Factory.NONE.create(target, result)
        }

        return CrossfadeTransition(target, result, CrossfadeDrawable.DEFAULT_DURATION, false)
    }
}
