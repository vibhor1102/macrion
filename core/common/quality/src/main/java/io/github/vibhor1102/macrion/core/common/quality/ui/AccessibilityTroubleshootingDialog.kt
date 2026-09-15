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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vibhor1102.macrion.core.base.extensions.safeStartWebBrowserActivity
import io.github.vibhor1102.macrion.core.common.quality.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface

@Composable
fun AccessibilityTroubleshootingDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                TroubleshootingContent(
                    title = stringResource(R.string.dialog_title_permission_issue),
                    message = stringResource(R.string.message_accessibility_issues),
                    onOpenWebsite = {
                        context.safeStartWebBrowserActivity("https://dontkillmyapp.com?app=Macrion")
                    },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
