/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.R

private val AUTO_HIDE_DELAY_STOPS = listOf(3, 5, 10, 15, 30, 60, 120, 180, 300, 600)

@Composable
internal fun formatAutoHideDelay(seconds: Int): String = when {
    seconds == 120 -> pluralStringResource(R.plurals.settings_toolbar_auto_hide_delay_minutes_default, 2, 2)
    seconds == 60 -> stringResource(R.string.settings_toolbar_auto_hide_delay_1_minute)
    seconds >= 60 && seconds % 60 == 0 -> {
        val minutes = seconds / 60
        pluralStringResource(R.plurals.settings_toolbar_auto_hide_delay_minutes, minutes, minutes)
    }
    else -> pluralStringResource(R.plurals.settings_toolbar_auto_hide_delay_seconds, seconds, seconds)
}

@Composable
internal fun ToolbarAutoHideDelayDialog(
    currentDelaySeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selectedDelay by remember(currentDelaySeconds) { mutableIntStateOf(currentDelaySeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_toolbar_auto_hide_delay_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                AUTO_HIDE_DELAY_STOPS.forEach { stop ->
                    val isSelected = stop == selectedDelay
                    val label = formatAutoHideDelay(stop)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedDelay = stop },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDelay) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
