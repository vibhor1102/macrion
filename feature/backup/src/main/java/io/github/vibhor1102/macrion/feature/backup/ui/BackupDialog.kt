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
package io.github.vibhor1102.macrion.feature.backup.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.feature.backup.R

private const val TAG = "BackupDialog"

@Composable
fun BackupDialog(
    isImport: Boolean,
    exportSmartScenarios: List<Long> = emptyList(),
    exportDumbScenarios: List<Long> = emptyList(),
    viewModel: BackupViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(isImport) {
        viewModel.initialize(context, isImport)
    }

    val backupActivityResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                viewModel.startBackup(
                    context = context,
                    uri = uri,
                    isImport = isImport,
                    dumbScenarios = exportDumbScenarios,
                    smartScenarios = exportSmartScenarios,
                )
            }
        }
    }

    fun launchDocumentPicker(): Boolean =
        try {
            backupActivityResult.launch(
                if (isImport) viewModel.createBackupRestorationFileSelectionIntent()
                else viewModel.createBackupFileCreationIntent()
            )
            true
        } catch (anfex: ActivityNotFoundException) {
            Log.e(TAG, "No application found to load/save a zip file.", anfex)
            false
        }

    fun launchDocumentPickerOrShowError() {
        if (!launchDocumentPicker()) {
            Toast.makeText(context, R.string.message_backup_error_no_zip_app, Toast.LENGTH_LONG).show()
        }
    }

    fun dismiss() {
        viewModel.reset()
        onDismiss()
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                BackupDialogContent(
                    title = stringResource(
                        if (isImport) R.string.dialog_title_import_backup
                        else R.string.dialog_title_create_backup,
                    ),
                    stateFlow = viewModel.backupState,
                    onFileSelection = { state ->
                        if (state.requiresCompatibilityPreparation) {
                            viewModel.prepareKlickrCompatibleExport(
                                context,
                                exportDumbScenarios,
                                exportSmartScenarios,
                            )
                        } else {
                            launchDocumentPickerOrShowError()
                        }
                    },
                    onKlickrCompatibleChanged = {
                        viewModel.setKlickrCompatibleExport(context, it)
                    },
                    onConfirm = { state ->
                        if (state.compatibilityReviewReady) launchDocumentPickerOrShowError()
                        else dismiss()
                    },
                    onCancel = ::dismiss,
                )
            }
        }
    }
}
