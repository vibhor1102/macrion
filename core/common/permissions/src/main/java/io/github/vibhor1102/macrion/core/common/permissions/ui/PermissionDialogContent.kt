/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.permissions.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.common.permissions.R

@Composable
internal fun PermissionDialogContent(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        HorizontalDivider()
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(descriptionRes),
                modifier = Modifier.padding(bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_request_permission))
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_deny))
            }
        }
    }
}
