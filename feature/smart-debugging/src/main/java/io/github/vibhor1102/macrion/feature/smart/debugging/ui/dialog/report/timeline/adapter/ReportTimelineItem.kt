/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.adapter

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineEventOccurrenceItem

@Composable
internal fun ReportTimelineItem(item: DebugReportTimelineEventOccurrenceItem, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    text = item.eventName,
                    modifier = Modifier.weight(1f).padding(end = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    item.legacyTimeText != null -> Text(
                        text = item.legacyTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primary,
                        maxLines = 1,
                    )
                    item.detectingDurationValue != null -> Column(horizontalAlignment = Alignment.End) {
                        TimingLine(item.detectingDurationValue, stringResource(R.string.item_event_occurrence_detecting_label))
                        item.actionsDurationValue?.let { value ->
                            TimingLine(
                                value,
                                stringResource(R.string.item_event_occurrence_actions_label),
                                Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
            Text(item.occurrenceText, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = secondary)
            Text(item.conditionsText, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = secondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.actions.forEach { action ->
                    Image(
                        painter = painterResource(action.iconRes),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp, bottom = 2.dp).size(26.dp),
                        colorFilter = ColorFilter.tint(secondary),
                    )
                }
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(primary),
                )
            }
        }
    }
}

@Composable
private fun TimingLine(value: String, label: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier) {
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
        Text(
            stringResource(R.string.item_event_occurrence_milliseconds),
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        Text(label, modifier = Modifier.padding(start = 2.dp), style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
internal fun ReportOccurrenceMetadata(timestamp: String, occurrence: String) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(timestamp, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = color)
        Text(occurrence, modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodySmall, color = color)
    }
}
