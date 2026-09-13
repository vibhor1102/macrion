/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.DialogChoice
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.R

@Composable
fun <T : DialogChoice> TutorialChoiceList(
    @StringRes title: Int,
    choices: List<T>,
    onDismiss: () -> Unit,
    onChoiceSelected: (T) -> Unit,
    monitoredType: ((T) -> MonitoredViewType?)? = null,
) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_cancel), null) }
                Text(stringResource(title), Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                items(choices, key = { it::class.qualifiedName.orEmpty() }) { choice ->
                    val type = monitoredType?.invoke(choice)
                    Card(
                        onClick = { onChoiceSelected(choice) },
                        enabled = choice.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .then(
                                if (type != null) {
                                    Modifier.tutorialAnchor(
                                        type = type,
                                        onClick = { onChoiceSelected(choice) },
                                        enabled = choice.enabled,
                                    )
                                } else Modifier
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp)
                                .alpha(if (choice.enabled) 1f else 0.5f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            choice.iconId?.let { Icon(painterResource(it), null, Modifier.size(32.dp)) }
                            Column(Modifier.weight(1f).padding(start = if (choice.iconId != null) 16.dp else 0.dp)) {
                                Text(stringResource(choice.title), style = MaterialTheme.typography.titleSmall)
                                choice.description?.let {
                                    Text(
                                        stringResource(it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            val trailingIcon = if (choice.enabled) R.drawable.ic_chevron_right else choice.disabledIconId
                            trailingIcon?.let { Icon(painterResource(it), null, Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}
