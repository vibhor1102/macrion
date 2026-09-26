/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R

@Composable
internal fun DebugReportOverview(
    state: DebugReportOverviewUiState,
    onEventActivityClicked: () -> Unit,
) {
    if (state !is DebugReportOverviewUiState.Available) {
        if (state is DebugReportOverviewUiState.Loading) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(bottom = 16.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        listOf(
            state.scenario,
            state.totalDuration,
            state.frameCount,
            state.averageFrameProcessingDuration,
            state.executionLimiterIdleTime,
            state.imageEventFulfilledCount,
            state.triggerEventFulfilledCount,
        ).forEach { entry ->
            OverviewEntryRow(entry)
            HorizontalDivider()
        }
        OverviewEntryRow(
            title = stringResource(R.string.item_title_report_event_activity),
            description = eventActivityDescription(state.eventActivity),
            modifier = Modifier.clickable(onClick = onEventActivityClicked),
            showChevron = true,
        )
    }
}

@Composable
private fun OverviewEntryRow(entry: OverviewEntry) {
    OverviewEntryRow(
        title = stringResource(entry.titleRes),
        description = entry.value ?: stringResource(requireNotNull(entry.valueRes)),
    )
}

@Composable
private fun OverviewEntryRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.onSurface
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).padding(
                top = 4.dp,
                end = if (showChevron) 16.dp else 0.dp,
                bottom = 4.dp,
            ),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = primary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showChevron) {
            Image(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(primary),
            )
        }
    }
}

@Composable
private fun eventActivityDescription(summary: EventActivitySummary): String {
    if (summary.reachedEventCount == 0) return stringResource(R.string.item_desc_report_event_activity_empty)
    val resources = androidx.compose.ui.platform.LocalResources.current
    val counts = resources.getQuantityString(
        R.plurals.item_desc_report_event_activity_reached,
        summary.reachedEventCount,
        summary.reachedEventCount,
        summary.totalOccurrenceCount,
    )
    return "$counts\n${stringResource(
        R.string.item_desc_report_event_activity_most_frequent,
        requireNotNull(summary.mostFrequentEventName),
        requireNotNull(summary.mostFrequentEventCount),
    )}"
}
