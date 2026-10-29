/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay
import io.github.vibhor1102.macrion.core.ui.compose.createMacrionMessageDialog

/**
 * A message window owned by an overlay composition. Compose's AlertDialog creates an
 * application window, which cannot use an accessibility service's overlay token.
 */
@Composable
fun OverlayMessageDialog(
    title: String,
    message: String,
    @StringRes confirmLabel: Int = android.R.string.ok,
    @StringRes cancelLabel: Int? = null,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val confirm by rememberUpdatedState(onConfirm)
    val dismiss by rememberUpdatedState(onDismissRequest)

    DisposableEffect(context, lifecycle, title, message, confirmLabel, cancelLabel) {
        val dialog = context.createMacrionMessageDialog(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            cancelLabel = cancelLabel,
            onConfirm = { confirm() },
            onCancel = { dismiss() },
        )
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> dialog.showAsOverlay()
                Lifecycle.Event.ON_STOP -> dialog.hide()
                Lifecycle.Event.ON_DESTROY -> dialog.dismiss()
                else -> Unit
            }
        }
        // addObserver also delivers ON_START when the parent is already started.
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            dialog.dismiss()
        }
    }
}
