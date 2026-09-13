/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.sort

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R

internal data class DebugReportSortOption<T>(
    val value: T,
    @field:StringRes val titleRes: Int,
    val selected: Boolean,
)

/** Compact sort chooser which keeps the report visible behind it. */
@Composable
internal fun <T> DebugReportSortMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: List<DebugReportSortOption<T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 200.dp, max = 280.dp),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(vertical = SORT_MENU_VERTICAL_PADDING_DP.dp)) {
            options.forEach { option ->
                SortOption(option) { value ->
                    onDismissRequest()
                    onSelected(value)
                }
            }
        }
    }
}

@Composable
private fun <T> SortOption(option: DebugReportSortOption<T>, onSelected: (T) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(SORT_OPTION_HEIGHT_DP.dp)
            .selectable(
                selected = option.selected,
                onClick = { onSelected(option.value) },
                role = Role.RadioButton,
            ),
        color = if (option.selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(option.titleRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (option.selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            if (option.selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_debug_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private const val SORT_OPTION_HEIGHT_DP = 56
private const val SORT_MENU_VERTICAL_PADDING_DP = 8
