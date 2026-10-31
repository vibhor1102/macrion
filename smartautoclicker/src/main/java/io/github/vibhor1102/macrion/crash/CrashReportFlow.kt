/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.crash

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import io.github.vibhor1102.macrion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps update suggestions separate from the report's consent and upload outcome. */
@Composable
fun CrashReportFlow(reportId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dismiss by rememberUpdatedState(onDismiss)
    var reportClosed by rememberSaveable(reportId) { mutableStateOf(false) }
    var checked by remember(reportId) { mutableStateOf(false) }
    var update by remember(reportId) { mutableStateOf<CrashUpdate?>(null) }
    var showUpdate by remember { mutableStateOf(false) }
    val history = remember(context) { context.getSharedPreferences("crash-update-notices", Context.MODE_PRIVATE) }

    LaunchedEffect(reportId) {
        update = withContext(Dispatchers.IO) { runCatching { CrashUpdateChecker().check(context.applicationContext) }.getOrNull() }
        checked = true
    }
    LaunchedEffect(reportClosed, checked) {
        if (reportClosed && checked) {
            val available = update
            if (available != null && !history.getBoolean(available.key, false)) {
                // Persist before display, so rotation/recreation never repeats an offered version.
                val recorded = withContext(Dispatchers.IO) { history.edit().putBoolean(available.key, true).commit() }
                if (recorded) showUpdate = true else dismiss()
            } else dismiss()
        }
    }

    if (!reportClosed) CrashReportDialog(reportId = reportId, onDismiss = { reportClosed = true })
    if (showUpdate) update?.let { available ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.crash_update_title)) },
            text = { Text(stringResource(R.string.crash_update_message, available.version, available.source.getLabel(context))) },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_update_later)) } },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, available.url.toUri())) }
                    onDismiss()
                }) { Text(stringResource(R.string.crash_update_view)) }
            },
        )
    }
}
