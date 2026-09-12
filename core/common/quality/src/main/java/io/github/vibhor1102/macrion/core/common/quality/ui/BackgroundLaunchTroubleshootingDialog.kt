/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.quality.ui

import android.app.Dialog
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.vibhor1102.macrion.core.base.extensions.safeStartWebBrowserActivity
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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

/** A generic, Don’t Kill My App-style explanation for an Android background-launch failure. */
class BackgroundLaunchTroubleshootingDialog : DialogFragment() {

    companion object {
        const val FRAGMENT_TAG = "BackgroundLaunchTroubleshootingDialog"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_HELP_URL = "help_url"

        fun newInstance(title: String, message: String, helpUrl: String) =
            BackgroundLaunchTroubleshootingDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_HELP_URL, helpUrl)
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val content = ComposeView(requireContext()).apply {
            setContent {
                MacrionTheme {
                    MacrionDialogSurface {
                        TroubleshootingContent(
                            title = args.getString(ARG_TITLE).orEmpty(),
                            message = args.getString(ARG_MESSAGE).orEmpty(),
                            onOpenWebsite = {
                                context?.safeStartWebBrowserActivity(args.getString(ARG_HELP_URL).orEmpty())
                            },
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
}
