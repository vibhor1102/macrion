/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.backup.ui

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.feature.backup.R
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun BackupDialogContent(
    title: String,
    stateFlow: StateFlow<BackupDialogUiState?>,
    onFileSelection: (BackupDialogUiState) -> Unit,
    onKlickrCompatibleChanged: (Boolean) -> Unit,
    onConfirm: (BackupDialogUiState) -> Unit,
    onCancel: () -> Unit,
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val currentState = state ?: return

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(24.dp))

        if (currentState.loadingVisibility == View.VISIBLE) {
            CircularProgressIndicator(Modifier.size(72.dp))
        } else if (currentState.iconStatusVisibility == View.VISIBLE && currentState.iconStatus != null) {
            Image(
                painter = painterResource(currentState.iconStatus),
                contentDescription = stringResource(R.string.content_desc_backup_state),
                modifier = Modifier.size(72.dp),
                colorFilter = currentState.iconTint?.let { ColorFilter.tint(Color(it)) }
                    ?: ColorFilter.tint(MaterialTheme.colorScheme.primary),
            )
        }

        if (currentState.fileSelectionVisibility == View.VISIBLE) {
            OutlinedButton(
                onClick = { onFileSelection(currentState) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Text(currentState.fileSelectionText.orEmpty())
            }
        }

        if (currentState.klickrCheckboxVisibility == View.VISIBLE) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = currentState.klickrCompatibleChecked,
                    onCheckedChange = onKlickrCompatibleChanged,
                )
                Text(
                    text = stringResource(R.string.item_title_backup_klickr_compatible),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (currentState.klickrExportWarningVisibility == View.VISIBLE) {
            Text(
                text = stringResource(R.string.message_backup_klickr_compatibility_loss),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (currentState.textStatusVisibility == View.VISIBLE) {
            Text(
                text = currentState.textStatusText.orEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        if (currentState.compatWarningVisibility == View.VISIBLE) {
            Text(
                text = stringResource(R.string.message_backup_import_compatibility),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, enabled = currentState.dialogCancelButtonEnabled) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = { onConfirm(currentState) },
                enabled = currentState.dialogOkButtonEnabled,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}
