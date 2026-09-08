/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.common.quality.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.vibhor1102.macrion.core.base.extensions.safeStartWebBrowserActivity
import io.github.vibhor1102.macrion.core.common.quality.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

class AccessibilityTroubleshootingDialog : DialogFragment() {

    internal companion object {
        /** Tag for dialog fragment. */
        internal const val FRAGMENT_TAG_TROUBLESHOOTING_DIALOG = "AccessibilityTroubleshootingDialog"
        /** Fragment result key for notifying the dialog is closed. */
        internal const val FRAGMENT_RESULT_KEY_TROUBLESHOOTING = ":$FRAGMENT_TAG_TROUBLESHOOTING_DIALOG:result"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = ComposeView(requireContext()).apply {
            setContent {
                MacrionTheme {
                    MacrionDialogSurface {
                        TroubleshootingContent(
                            title = context.getString(R.string.dialog_title_permission_issue),
                            message = context.getString(R.string.message_accessibility_issues),
                            onOpenWebsite = ::showDontKillMyApp,
                            onDismiss = ::dismiss,
                        )
                    }
                }
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(content)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(
            requestKey = FRAGMENT_RESULT_KEY_TROUBLESHOOTING,
            result = Bundle.EMPTY,
        )
    }

    private fun showDontKillMyApp() {
        context?.safeStartWebBrowserActivity("https://dontkillmyapp.com?app=macrion")
    }
}
