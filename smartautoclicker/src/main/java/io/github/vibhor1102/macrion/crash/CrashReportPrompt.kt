/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.crash

import android.app.Dialog
import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.vibhor1102.macrion.R
import androidx.lifecycle.lifecycleScope
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrashReportPrompt : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = ComposeView(requireContext()).apply {
            setContent {
                MacrionTheme {
                    Surface(
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
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
                                    onClick = {
                                        discardReport()
                                        dismiss()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                ) {
                                    Text(stringResource(R.string.crash_report_discard))
                                }
                                Button(
                                    onClick = {
                                        sendReport()
                                        dismiss()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                ) {
                                    Text(stringResource(R.string.crash_report_send))
                                }
                            }
                        }
                    }
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(content).create()
    }

    private fun discardReport() {
        val store = requireContext().applicationContext.crashReportStore()
        val activity = requireActivity()
        val id = requireArguments().getString("reportId") ?: return
        activity.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) { runCatching { store.delete(id) }.isSuccess }
            if (!deleted) android.widget.Toast.makeText(activity, R.string.crash_reports_error, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun sendReport() {
        val context = requireContext().applicationContext
        val activity = requireActivity()
        val id = requireArguments().getString("reportId") ?: return
        activity.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val store = context.crashReportStore()
                    val report = store.pending().firstOrNull { it.id == id } ?: return@runCatching null
                    CrashReportUploader().send(report.body).also { outcome ->
                        if (outcome == CrashReportUploader.Result.SENT || outcome == CrashReportUploader.Result.ALREADY_SENT) {
                            store.delete(id)
                        }
                    }
                }
            }.getOrNull()
            val message = when (result) {
                CrashReportUploader.Result.SENT, CrashReportUploader.Result.ALREADY_SENT -> R.string.crash_report_sent
                CrashReportUploader.Result.REJECTED -> R.string.crash_report_rejected
                else -> R.string.crash_report_send_failed
            }
            android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val TAG = "local-crash-report"
        fun newInstance(id: String) = CrashReportPrompt().apply {
            arguments = Bundle().apply { putString("reportId", id) }
        }
    }
}
