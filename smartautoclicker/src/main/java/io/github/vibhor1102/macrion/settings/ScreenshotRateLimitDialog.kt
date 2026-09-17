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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.R

@Composable
internal fun formatScreenshotRateLimit(limit: Int): String = when {
    limit <= 0 -> stringResource(R.string.settings_screenshot_rate_limit_item_disabled)
    limit == 10 -> stringResource(R.string.settings_screenshot_rate_limit_item_default, limit)
    else -> stringResource(R.string.settings_screenshot_rate_limit_item, limit)
}

@Composable
internal fun ScreenshotRateLimitDialog(
    currentLimit: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var textValue by remember(currentLimit) { mutableStateOf(currentLimit.toString()) }
    val parsed = textValue.trim().toIntOrNull()
    val isValid = parsed != null && parsed >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_screenshot_rate_limit_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.settings_screenshot_rate_limit_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        // Only allow digits
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            textValue = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.settings_screenshot_rate_limit_dialog_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text(
                                text = stringResource(R.string.settings_screenshot_rate_limit_invalid),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (parsed == 0) {
                            Text(
                                text = stringResource(R.string.settings_screenshot_rate_limit_item_disabled),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid && parsed != null) onConfirm(parsed) },
                enabled = isValid,
            ) {
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
