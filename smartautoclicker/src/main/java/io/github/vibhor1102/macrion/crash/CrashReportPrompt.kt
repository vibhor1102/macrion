/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.crash

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CrashReportDialog(
    reportId: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    fun discardReport() {
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            val store = context.applicationContext.crashReportStore()
            val deleted = withContext(Dispatchers.IO) {
                runCatching { store.delete(reportId) }.isSuccess
            }
            if (!deleted) {
                Toast.makeText(context, R.string.crash_reports_error, Toast.LENGTH_LONG).show()
            }
            isSubmitting = false
            onDismiss()
        }
    }

    fun sendReport() {
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            val appContext = context.applicationContext
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val store = appContext.crashReportStore()
                    val report = store.pending().firstOrNull { it.id == reportId } ?: return@runCatching null
                    CrashReportUploader().send(report.body).also { outcome ->
                        if (outcome == CrashReportUploader.Result.SENT || outcome == CrashReportUploader.Result.ALREADY_SENT) {
                            store.delete(reportId)
                        }
                    }
                }
            }.getOrNull()
            val message = when (result) {
                CrashReportUploader.Result.SENT, CrashReportUploader.Result.ALREADY_SENT -> R.string.crash_report_sent
                CrashReportUploader.Result.REJECTED -> R.string.crash_report_rejected
                else -> R.string.crash_report_send_failed
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            isSubmitting = false
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.crash_report_prompt_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.crash_report_prompt_message),
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = ::discardReport,
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isSubmitting,
                        ) {
                            Text(stringResource(R.string.crash_report_discard))
                        }
                        Button(
                            onClick = ::sendReport,
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = !isSubmitting,
                        ) {
                            Text(stringResource(R.string.crash_report_send))
                        }
                    }
                }
            }
        }
    }
}
