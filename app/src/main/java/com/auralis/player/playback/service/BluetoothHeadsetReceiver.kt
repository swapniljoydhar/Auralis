/*
 * Copyright (c) 2022 Auralis Contributors
 * BluetoothHeadsetReceiver.kt is part of Auralis.
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

import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A [BroadcastReceiver] that starts music playback when a bluetooth headset is connected.
 *
 * @author seijikun, Auralis
 */
class BluetoothHeadsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val newState =
                intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED,
                )
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // TODO: Initialize the service (Permission workflow must be figured out)
                //  Perhaps move this to the internal receivers?
            }
        }
    }
}
