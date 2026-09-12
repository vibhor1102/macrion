/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.TimeUnitDropDownItem
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.timeUnitDropdownItems

@Composable
fun MacrionPauseEditor(
    title: String,
    name: String,
    duration: String,
    selectedUnit: TimeUnitDropDownItem,
    nameLabel: String,
    durationLabel: String,
    unitLabel: String,
    nameError: Boolean,
    durationError: Boolean,
    saveEnabled: Boolean,
    maxNameLength: Int,
    onNameChanged: (String) -> Unit,
    onDurationChanged: (String) -> Unit,
    onUnitChanged: (TimeUnitDropDownItem) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                FilledTonalIconButton(onClick = onDelete) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSave, enabled = saveEnabled) {
                    Icon(painterResource(R.drawable.ic_save_filled), contentDescription = null)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                MacrionTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = nameLabel,
                    isError = nameError,
                    maxLength = maxNameLength,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { onDurationChanged(it.filter(Char::isDigit)) },
                        label = { Text(durationLabel) },
                        modifier = Modifier.weight(0.7f),
                        isError = durationError,
                        singleLine = true,
                        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
                        keyboardActions = macrionDoneKeyboardActions(),
                    )
                    Spacer(Modifier.width(16.dp))
                    TimeUnitDropdown(
                        selected = selectedUnit,
                        label = unitLabel,
                        onSelected = onUnitChanged,
                        modifier = Modifier.weight(0.3f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeUnitDropdown(
    selected: TimeUnitDropDownItem,
    label: String,
    onSelected: (TimeUnitDropDownItem) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = stringResource(selected.title),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            timeUnitDropdownItems.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(stringResource(unit.title)) },
                    onClick = { onSelected(unit); expanded = false },
                )
            }
        }
    }
}
