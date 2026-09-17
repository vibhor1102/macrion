/*
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateLimitUiState
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.allComputeRateUnitDropdownItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionExecutionLimiterCard(
    state: ComputeRateLimitUiState,
    onToggle: () -> Unit,
    onRateChanged: (Double) -> Unit,
    onUnitChanged: (ComputeRateUnitDropdownItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by rememberSaveable(state.value) {
        mutableStateOf(state.value.toNaturalDisplayString())
    }
    var focused by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.value, state.unit, state.isEnabled, focused) {
        if (!focused && state.isEnabled) {
            value = state.value.toNaturalDisplayString()
        }
    }

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.field_scenario_fps_limit_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.field_condition_fps_limit_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalDivider(Modifier.height(48.dp))
                Spacer(Modifier.width(12.dp))
                Switch(state.isEnabled, { onToggle() })
            }
            AnimatedVisibility(
                visible = state.isEnabled,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    expandFrom = Alignment.Top,
                ) + fadeIn(
                    animationSpec = tween(150),
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(
                    animationSpec = tween(100),
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value,
                        { input ->
                            if (input.matches(Regex("\\d*(\\.\\d*)?"))) {
                                value = input
                                input.toDoubleOrNull()?.takeIf { it > 0.0 && it <= state.maxValue }
                                    ?.let(onRateChanged)
                            }
                        },
                        Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                        label = { Text(stringResource(R.string.field_scenario_fps_rate_label)) },
                        singleLine = true,
                        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
                        keyboardActions = macrionDoneKeyboardActions(),
                    )
                    Text("/", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineMedium)
                    ExposedDropdownMenuBox(menuExpanded, { menuExpanded = !menuExpanded }, Modifier.weight(.8f)) {
                        OutlinedTextField(
                            stringResource(state.unit.title), {}, readOnly = true,
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            label = { Text(stringResource(R.string.field_scenario_fps_rate_unit_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuExpanded) },
                            singleLine = true,
                        )
                        ExposedDropdownMenu(menuExpanded, { menuExpanded = false }) {
                            allComputeRateUnitDropdownItems().forEach { item ->
                                DropdownMenuItem(
                                    { Text(stringResource(item.title)) },
                                    { onUnitChanged(item); menuExpanded = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
