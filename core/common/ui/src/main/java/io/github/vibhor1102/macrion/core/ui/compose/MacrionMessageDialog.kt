/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext

/** A Compose message surface in an AlertDialog window, for callers that need Android dialog lifecycle hooks. */
fun Context.createMacrionMessageDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes confirmLabel: Int = android.R.string.ok,
    @StringRes cancelLabel: Int? = null,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
): Dialog = createMacrionMessageDialog(
    title = getString(title),
    message = getString(message),
    confirmLabel = confirmLabel,
    cancelLabel = cancelLabel,
    onConfirm = onConfirm,
    onCancel = onCancel,
    onDismiss = onDismiss,
)

fun Context.createMacrionMessageDialog(
    title: String,
    message: String,
    @StringRes confirmLabel: Int = android.R.string.ok,
    @StringRes cancelLabel: Int? = null,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
): Dialog {
    val themedContext = getDynamicColorsContext(R.style.AppTheme)
    lateinit var dialog: Dialog
    val content = ComposeView(themedContext).apply {
        setContent {
            MacrionTheme {
                MacrionDialogSurface {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (cancelLabel != null) {
                                TextButton(onClick = {
                                    try {
                                        onCancel?.invoke()
                                    } finally {
                                        dialog.dismiss()
                                    }
                                }) { Text(themedContext.getString(cancelLabel)) }
                            }
                            TextButton(onClick = {
                                try {
                                    onConfirm()
                                } finally {
                                    dialog.dismiss()
                                }
                            }) { Text(themedContext.getString(confirmLabel)) }
                        }
                    }
                }
            }
        }
    }
    dialog = MaterialAlertDialogBuilder(themedContext)
        .setView(content)
        .setOnCancelListener { onCancel?.invoke() }
        .setOnDismissListener { onDismiss?.invoke() }
        .create()
    return dialog
}
