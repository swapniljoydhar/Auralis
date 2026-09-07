/*
 * Copyright (c) 2023 Auralis Contributors
 * ErrorDetailsDialog.kt is part of Auralis.
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
 
package com.auralis.player.home

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.auralis.player.R
import com.auralis.player.databinding.DialogErrorDetailsBinding
import com.auralis.player.music.MusicViewModel
import com.auralis.player.ui.ViewBindingMaterialDialogFragment
import com.auralis.player.util.getSystemServiceCompat
import com.auralis.player.util.openInBrowser
import com.auralis.player.util.showToast

/**
 * A dialog that shows a stack trace for a music loading error.
 *
 * @author Auralis Contributors
 *
 * TODO: Extend to other errors
 */
class ErrorDetailsDialog : ViewBindingMaterialDialogFragment<DialogErrorDetailsBinding>() {
    private val args: ErrorDetailsDialogArgs by navArgs()
    private var clipboardManager: ClipboardManager? = null
    private val musicModel: MusicViewModel by viewModels()

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.lbl_error_info)
            .setNeutralButton(R.string.lbl_retry) { _, _ -> musicModel.refresh() }
            .setPositiveButton(R.string.lbl_report) { _, _ ->
                requireContext().openInBrowser(LINK_ISSUES)
            }
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogErrorDetailsBinding.inflate(inflater)

    override fun onBindingCreated(binding: DialogErrorDetailsBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        clipboardManager = requireContext().getSystemServiceCompat(ClipboardManager::class)

        // --- UI SETUP ---
        binding.errorStackTrace.text = args.error.stackTraceToString().trimEnd('\n')
        binding.errorCopy.setOnClickListener { copyStackTrace() }
    }

    override fun onDestroyBinding(binding: DialogErrorDetailsBinding) {
        super.onDestroyBinding(binding)
        clipboardManager = null
    }

    private fun copyStackTrace() {
        requireNotNull(clipboardManager) { "Clipboard was unavailable" }
            .setPrimaryClip(
                ClipData.newPlainText("Exception Stack Trace", args.error.stackTraceToString())
            )
        // A copy notice is shown by the system from Android 13 onwards
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requireContext().showToast(R.string.lbl_copied)
        }
    }

    private companion object {
        /** The URL to the bug report issue form */
        const val LINK_ISSUES =
            "https://github.com/swapniljoydhar/Auralis/issues/new" +
                "?assignees=Auralis&labels=bug&projects=&template=bug-crash-report.yml"
    }
}
