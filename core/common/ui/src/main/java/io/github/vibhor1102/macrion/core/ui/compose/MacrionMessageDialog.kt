/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** A Compose message surface in an AlertDialog window, for callers that need Android dialog lifecycle hooks. */
fun Context.createMacrionMessageDialog(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes confirmLabel: Int = android.R.string.ok,
    @StringRes cancelLabel: Int? = null,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
): AlertDialog {
    lateinit var dialog: AlertDialog
    val content = ComposeView(this).apply {
        setContent {
            MacrionTheme {
                MacrionDialogSurface {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                        Text(
                            text = stringResource(title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        Text(
                            text = stringResource(message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (cancelLabel != null) {
                                TextButton(onClick = { dialog.dismiss() }) { Text(stringResource(cancelLabel)) }
                            }
                            TextButton(onClick = {
                                try {
                                    onConfirm()
                                } finally {
                                    dialog.dismiss()
                                }
                            }) { Text(stringResource(confirmLabel)) }
                        }
                    }
                }
            }
        }
    }
    dialog = MaterialAlertDialogBuilder(this)
        .setView(content)
        .setOnCancelListener { onCancel?.invoke() }
        .setOnDismissListener { onDismiss?.invoke() }
        .create()
    return dialog
}
