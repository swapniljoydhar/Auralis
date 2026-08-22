/*
 * Copyright (c) 2022 Auralis Contributors
 * MediaButtonReceiver.kt is part of Auralis.
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
 
package com.auralis.player.playback.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.auralis.player.AuralisService
import com.auralis.player.IntegerTable
import com.auralis.player.playback.state.PlaybackStateManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber as L

/**
 * A [BroadcastReceiver] that forwards [Intent.ACTION_MEDIA_BUTTON] [Intent]s to
 * [PlaybackServiceFragment].
 *
 * @author Auralis Contributors
 */
@AndroidEntryPoint
class MediaButtonReceiver : BroadcastReceiver() {
    @Inject lateinit var playbackManager: PlaybackStateManager

    // TODO: Figure this out
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON || playbackManager.currentSong == null) {
            return
        }

        val keyEvent =
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                ?: return

        // We have a song, so we can assume that the service will start a foreground state.
        // At least, I hope. Again, *this is why we don't do this*. I cannot describe how
        // stupid this is with the state of foreground services on modern android. One
        // wrong action at the wrong time will result in the app crashing, and there is
        // nothing I can do about it.
        // TODO: Think I finally have an alternative with the changes I made to accomodate
        // tasker
        L.d("Delivering media button intent $intent")
        val safeIntent =
            Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(context, AuralisService::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
                .putExtra(AuralisService.INTENT_KEY_START_ID, IntegerTable.START_ID_MEDIA_BUTTON)
        ContextCompat.startForegroundService(context, safeIntent)
    }
}
