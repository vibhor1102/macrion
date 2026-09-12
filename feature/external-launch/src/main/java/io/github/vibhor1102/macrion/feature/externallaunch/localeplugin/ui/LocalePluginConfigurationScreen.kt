/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.externallaunch.R

@Composable
internal fun LocalePluginConfigurationScreen(
    scenarios: List<LocalePluginScenarioItem>,
    selectedScenario: LocalePluginScenarioItem?,
    restoredScenarioWasDeleted: Boolean,
    onScenarioSelected: (LocalePluginScenarioItem) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val message = when {
        scenarios.isEmpty() -> stringResource(R.string.locale_plugin_no_scenarios)
        restoredScenarioWasDeleted -> stringResource(R.string.locale_plugin_deleted_scenario)
        selectedScenario?.isSmart == true -> stringResource(R.string.locale_plugin_smart_note)
        else -> null
    }
    ConfigurationScreen(
        title = R.string.locale_plugin_title,
        subtitle = R.string.locale_plugin_description,
        message = message,
        saveEnabled = selectedScenario != null,
        onCancel = onCancel,
        onSave = onSave,
    ) {
        ScenarioDropdown(scenarios, selectedScenario, onScenarioSelected)
    }
}

@Composable
internal fun ExternalActionEventConfigurationScreen(
    names: List<String>,
    selectedName: String?,
    restoredNameIsMissing: Boolean,
    onNameSelected: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    ConfigurationScreen(
        title = R.string.external_action_event_title,
        subtitle = R.string.external_action_event_subtitle,
        message = stringResource(
            when {
                names.isEmpty() -> R.string.external_action_event_empty
                restoredNameIsMissing -> R.string.external_action_event_missing_name
                else -> R.string.external_action_event_description
            }
        ),
        saveEnabled = !selectedName.isNullOrBlank(),
        onCancel = onCancel,
        onSave = onSave,
    ) {
        StringDropdown(names, selectedName, onNameSelected)
    }
}

@Composable
private fun ConfigurationScreen(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    message: String?,
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    field: @Composable () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(subtitle),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            field()
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.locale_plugin_cancel)) }
                Button(onClick = onSave, enabled = saveEnabled, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.locale_plugin_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioDropdown(
    scenarios: List<LocalePluginScenarioItem>,
    selected: LocalePluginScenarioItem?,
    onSelected: (LocalePluginScenarioItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.locale_plugin_scenario_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            scenarios.forEach { scenario ->
                DropdownMenuItem(
                    text = { ScenarioMenuItem(scenario) },
                    onClick = { onSelected(scenario); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ScenarioMenuItem(scenario: LocalePluginScenarioItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(
                if (scenario.isSmart) io.github.vibhor1102.macrion.core.ui.R.drawable.ic_screen_event
                else io.github.vibhor1102.macrion.core.ui.R.drawable.ic_click
            ),
            contentDescription = stringResource(R.string.locale_plugin_scenario_icon),
            modifier = Modifier.size(32.dp),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(scenario.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    if (scenario.isSmart) R.string.locale_plugin_type_smart
                    else R.string.locale_plugin_type_dumb
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StringDropdown(names: List<String>, selected: String?, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.external_action_event_name_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            names.forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(name); expanded = false })
            }
        }
    }
}
