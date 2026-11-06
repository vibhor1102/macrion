package io.github.vibhor1102.macrion.feature.smart.config.ui.action

import android.graphics.Point
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.domain.model.action.PositionCounters
import io.github.vibhor1102.macrion.feature.smart.config.R

/** Axis overrides share the fixed position picker; choosing a counter never changes the literal. */
@Composable
internal fun PositionCountersEditor(
    title: String,
    value: PositionCounters?,
    fixed: Point?,
    counterNames: List<String>,
    onChanged: (PositionCounters?) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.coordinate_counters_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true, false).forEach { xAxis ->
                val name = if (xAxis) value?.x else value?.y
                val literal = if (xAxis) fixed?.x else fixed?.y
                CoordinateChoice(
                    label = stringResource(if (xAxis) R.string.coordinate_axis_x else R.string.coordinate_axis_y),
                    selected = name ?: stringResource(R.string.coordinate_fixed_value, literal?.toString() ?: "?"),
                    options = listOf(null to stringResource(R.string.coordinate_fixed_value, literal?.toString() ?: "?")) +
                        counterNames.map { it to it },
                    onSelected = { selected ->
                        val next = if (xAxis) (value ?: PositionCounters()).copy(x = selected)
                            else (value ?: PositionCounters()).copy(y = selected)
                        onChanged(next.takeIf { it.x != null || it.y != null })
                    },
                    isError = name != null && name !in counterNames,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> CoordinateChoice(
    label: String, selected: String, options: List<Pair<T, String>>, onSelected: (T) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }, modifier) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, enabled = enabled, isError = isError,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}
