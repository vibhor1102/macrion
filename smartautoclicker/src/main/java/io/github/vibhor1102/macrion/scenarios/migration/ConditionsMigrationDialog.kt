/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.scenarios.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.ui.bindings.buttons.LoadableButtonState
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionLoadableButton

@Composable
fun ConditionsMigrationDialog(
    viewModel: ConditionsMigrationViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                ConditionsMigrationContent(
                    viewModel = viewModel,
                    onFinished = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ConditionsMigrationContent(viewModel: ConditionsMigrationViewModel, onFinished: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle(null)
    val buttonState = state?.buttonState
    Column(
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.dialog_condition_migration_title), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        Text(stringResource(R.string.message_condition_migration_desc), modifier = Modifier.padding(horizontal = 16.dp), textAlign = TextAlign.Center)
        state?.let { Text(it.textState, modifier = Modifier.padding(horizontal = 16.dp), textAlign = TextAlign.Center) }
        MacrionLoadableButton(
            text = buttonState?.text.orEmpty(),
            loading = buttonState is LoadableButtonState.Loading,
            enabled = buttonState is LoadableButtonState.Loaded.Enabled,
        ) {
            when (state?.migrationState) {
                MigrationState.NOT_STARTED -> viewModel.startMigration()
                MigrationState.FINISHED, MigrationState.FINISHED_WITH_ERROR -> onFinished()
                else -> Unit
            }
        }
    }
}
