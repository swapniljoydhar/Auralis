/*
 * Copyright (c) 2024 Auralis Contributors
 * Animations.kt is part of Auralis.
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
 
package com.auralis.player.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.isInvisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.auralis.player.util.scale
import com.google.android.material.shape.MaterialShapeDrawable

private const val MIN_VISIBLE_CHANGE_DRAWABLE_ALPHA = 1f

/**
 * Spring animation presets for spatial transitions (scale, translate, elevation). Uses Material 3
 * spring constants for consistent motion.
 */
class Spatial
private constructor(
    private val dampingRatio: Float,
    private val stiffness: Float,
) {
    private fun createSpring(to: Float): SpringForce =
        SpringForce(to).apply {
            this.dampingRatio = this@Spatial.dampingRatio
            this.stiffness = this@Spatial.stiffness
        }

    fun scale(view: View, to: Float, jumpOnCancellation: Boolean = false): SpringAnimation {
        val from = view.scale
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE)
            addUpdateListener { _, value, _ -> view.scale = value }
            addEndListener { _, canceled, value, _ ->
                view.scale = if (!canceled || jumpOnCancellation) to else value
            }
            animateToFinalPosition(to)
        }
    }

    fun translateX(view: View, to: Float, jumpOnCancellation: Boolean = false): SpringAnimation {
        val from = view.translationX
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
            addUpdateListener { _, value, _ -> view.translationX = value }
            addEndListener { _, canceled, value, _ ->
                view.translationX = if (!canceled || jumpOnCancellation) to else value
            }
            animateToFinalPosition(to)
        }
    }

    fun translateZ(view: View, to: Float, jumpOnCancellation: Boolean = false): SpringAnimation {
        val from = view.translationZ
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
            addUpdateListener { _, value, _ -> view.translationZ = value }
            addEndListener { _, canceled, value, _ ->
                view.translationZ = if (!canceled || jumpOnCancellation) to else value
            }
            animateToFinalPosition(to)
        }
    }

    fun elevation(
        context: Context,
        drawable: MaterialShapeDrawable,
        to: Float,
        jumpOnCancellation: Boolean = false,
    ): SpringAnimation {
        val from = drawable.elevation
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
            addUpdateListener { _, value, _ -> drawable.elevation = value }
            addEndListener { _, canceled, value, _ ->
                drawable.elevation = if (!canceled || jumpOnCancellation) to else value
            }
            animateToFinalPosition(to)
        }
    }

    fun corners(
        context: Context,
        drawable: MaterialShapeDrawable,
        to: Float,
        jumpOnCancellation: Boolean = false,
    ): SpringAnimation {
        val from = drawable.topRightCornerResolvedSize
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
            addUpdateListener { _, value, _ -> drawable.setCornerSize(value) }
            addEndListener { _, canceled, value, _ ->
                drawable.setCornerSize(if (!canceled || jumpOnCancellation) to else value)
            }
            animateToFinalPosition(to)
        }
    }

    companion object {
        /** Fast spatial spring - for small, quick UI transitions. */
        val FAST =
            Spatial(
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY,
                stiffness = SpringForce.STIFFNESS_HIGH,
            )
        /** Default spatial spring - for general UI transitions. */
        val DEFAULT =
            Spatial(
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY,
                stiffness = SpringForce.STIFFNESS_MEDIUM,
            )
        /** Slow spatial spring - for large, dramatic transitions. */
        val SLOW =
            Spatial(
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY,
                stiffness = SpringForce.STIFFNESS_LOW,
            )
    }
}

/**
 * Spring animation presets for effect transitions (alpha, color). Uses Material 3 spring constants
 * for consistent motion.
 */
class Effect
private constructor(
    private val dampingRatio: Float,
    private val stiffness: Float,
) {
    private fun createSpring(to: Float): SpringForce =
        SpringForce(to).apply {
            this.dampingRatio = this@Effect.dampingRatio
            this.stiffness = this@Effect.stiffness
        }

    fun alpha(view: View, to: Float, jumpOnCancellation: Boolean = false): SpringAnimation {
        val from = view.alpha
        return SpringAnimation(FloatValueHolder(from)).apply {
            spring = createSpring(to)
            setStartValue(from)
            setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA)
            addUpdateListener { _, value, _ ->
                view.alpha = value
                view.isInvisible = view.alpha == 0f
            }
            addEndListener { _, canceled, value, _ ->
                view.alpha = if (!canceled || jumpOnCancellation) to else value
                view.isInvisible = view.alpha == 0f
            }
            animateToFinalPosition(to)
        }
    }

    fun alpha(
        context: Context,
        drawable: Drawable,
        to: Int,
        jumpOnCancellation: Boolean = false,
    ): SpringAnimation {
        val from = drawable.alpha
        return SpringAnimation(FloatValueHolder(from.toFloat())).apply {
            spring = createSpring(to.toFloat())
            setStartValue(from.toFloat())
            setMinimumVisibleChange(MIN_VISIBLE_CHANGE_DRAWABLE_ALPHA)
            addUpdateListener { _, value, _ -> drawable.alpha = value.toInt() }
            addEndListener { _, canceled, value, _ ->
                drawable.alpha = (if (!canceled || jumpOnCancellation) to else value).toInt()
            }
            animateToFinalPosition(to.toFloat())
        }
    }

    companion object {
        /** Default effect spring - for general effect transitions. */
        val DEFAULT =
            Effect(
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY,
                stiffness = SpringForce.STIFFNESS_MEDIUM,
            )
        /** Slow effect spring - for subtle, gradual transitions. */
        val SLOW =
            Effect(
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY,
                stiffness = SpringForce.STIFFNESS_LOW,
            )
        /** Fast effect spring - for quick feedback. */
        val FAST =
            Effect(
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY,
                stiffness = SpringForce.STIFFNESS_HIGH,
            )
    }
}
