/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.scenarios.list.copy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField

@Composable
fun ScenarioCopyDialog(
    scenarioId: Long,
    isSmart: Boolean,
    defaultName: String = "",
    viewModel: ScenarioCopyViewModel,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(scenarioId, defaultName) {
        viewModel.initializeCopyName(defaultName)
    }

    val name by viewModel.copyName.collectAsStateWithLifecycle()
    val isError by viewModel.copyNameError.collectAsStateWithLifecycle(false)
    var isSubmitting by remember { mutableStateOf(false) }

    fun dismiss() {
        viewModel.reset()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text(stringResource(R.string.dialog_title_copy_scenario)) },
        text = {
            MacrionDialogSurface {
                val focusRequester = remember { FocusRequester() }
                MacrionTextField(
                    value = name.orEmpty(),
                    onValueChange = viewModel::setCopyName,
                    label = stringResource(R.string.default_click_name),
                    isError = isError,
                    maxLength = 60,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .focusRequester(focusRequester),
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!name.isNullOrEmpty() && !isSubmitting) {
                        isSubmitting = true
                        viewModel.copyScenario(scenarioId, isSmart) {
                            isSubmitting = false
                            dismiss()
                        }
                    }
                },
                enabled = !name.isNullOrEmpty() && !isSubmitting,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = ::dismiss,
                enabled = !isSubmitting,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
