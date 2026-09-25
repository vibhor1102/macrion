/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R

data class ActionEditorMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@Composable
fun ActionEditorOverflowMenu(items: List<ActionEditorMenuItem>) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more), stringResource(R.string.action_editor_more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    enabled = item.enabled,
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

data class ExistingActionOption<T>(
    val value: T,
    val title: String,
    val subtitle: String,
    @DrawableRes val icon: Int,
)

/** Replaces the editor content while choosing a second action; no stacked modal is needed. */
@Composable
fun <T> ExistingActionPicker(
    options: List<ExistingActionOption<T>>,
    onBack: () -> Unit,
    onSelected: (T) -> Unit,
) {
    Surface(
        shape = OverlayDialogShape,
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.content_desc_go_back))
                }
                Text(
                    text = stringResource(R.string.action_combine_choose_existing),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.action_combine_choose_help),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                items(options) { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                            .clickable { onSelected(option.value) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(option.icon), null, Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(option.title, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(option.subtitle, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
