/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.quality.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vibhor1102.macrion.core.base.extensions.safeStartWebBrowserActivity
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface

@Composable
fun BackgroundLaunchTroubleshootingDialog(
    title: String,
    message: String,
    helpUrl: String,
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
                    title = title,
                    message = message,
                    onOpenWebsite = {
                        context.safeStartWebBrowserActivity(helpUrl)
                    },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}
