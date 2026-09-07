/*
 * Copyright (c) 2026 Auralis Contributors
 * FfmpegRendererCompat.kt is part of Auralis.
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
 
package com.auralis.player.playback.service

import android.os.Handler
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.BaseRenderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Creates the audio renderer array for ExoPlayer, optionally including the FFmpeg audio renderer
 * when the `media-lib-decoder-ffmpeg` module is present at runtime.
 */
object FfmpegRendererCompat {
    fun createAudioRenderers(
        context: android.content.Context,
        handler: Handler,
        audioRendererEventListener: AudioRendererEventListener,
        replayGainProcessor: AudioProcessor,
    ): Array<BaseRenderer> {
        val baseRenderer =
            MediaCodecAudioRenderer(
                context,
                MediaCodecSelector.DEFAULT,
                handler,
                audioRendererEventListener,
                DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(replayGainProcessor))
                    .build(),
            )

        // FFmpeg decoder module is optional and excluded on Windows.
        // Attempt to load FfmpegAudioRenderer via reflection when available.
        val ffmpegRenderers =
            try {
                val clazz = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
                val ctor =
                    clazz.getConstructor(
                        Handler::class.java,
                        AudioRendererEventListener::class.java,
                        AudioProcessor::class.java,
                    )
                @Suppress("UNCHECKED_CAST")
                ctor.newInstance(handler, audioRendererEventListener, replayGainProcessor)
                    as BaseRenderer
            } catch (_: ReflectiveOperationException) {
                null
            } catch (_: LinkageError) {
                null
            }

        return if (ffmpegRenderers != null) {
            arrayOf(ffmpegRenderers, baseRenderer)
        } else {
            arrayOf(baseRenderer)
        }
    }
}
