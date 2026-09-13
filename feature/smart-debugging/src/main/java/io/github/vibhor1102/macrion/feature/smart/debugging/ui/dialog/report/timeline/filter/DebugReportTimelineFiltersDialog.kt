/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.filter

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportDialogTopBar
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.filter.events.FilteredEventsSelectorDialog

class DebugReportTimelineFiltersDialog(
    private val reportDurationMs: Long,
    private val currentFilters: List<DebugReportTimelineFilter>,
    private val onFiltersApplied: (List<DebugReportTimelineFilter>) -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    private val viewModel: DebugReportTimelineFiltersViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportTimelineFiltersViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setupUserValues(context, reportDurationMs, currentFilters)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DebugReportTimelineFiltersDialog.Content() } }
        }
    }
@Composable private fun Content() {
        val time = viewModel.timeUiState.collectAsStateWithLifecycle(initialValue = null).value
        val image = viewModel.imageEventsUiState.collectAsStateWithLifecycle(initialValue = null).value
        val trigger = viewModel.triggerEventsUiState.collectAsStateWithLifecycle(initialValue = null).value
        Surface(Modifier.fillMaxSize().heightIn(min = 600.dp)) {
            Column {
                ReportDialogTopBar(
                    context.getString(R.string.dialog_overlay_title_timeline_filters),
                    ::back,
                ) {
                    onFiltersApplied(viewModel.getAllFilters())
                    back()
                }
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    time?.let { TimeFilter(it) }
                    image?.let { state -> EventFilterCard(
                        title = context.getString(R.string.field_debug_filter_image_events_title),
                        description = context.getString(if (state.checkboxState)
                            R.string.field_debug_filter_image_events_show_desc_on
                        else R.string.field_debug_filter_image_events_show_desc_off),
                        checked = state.checkboxState,
                        selectorDescription = state.filteredIdsText,
                        onToggle = viewModel::toggleShowImageEvents,
                        onSelect = { showEventSelectionDialog(viewModel.getImageEventsFilter()) },
                    ) }
                    trigger?.let { state -> EventFilterCard(
                        title = context.getString(R.string.field_debug_filter_trigger_events_title),
                        description = context.getString(if (state.checkboxState)
                            R.string.field_debug_filter_trigger_events_show_desc_on
                        else R.string.field_debug_filter_trigger_events_show_desc_off),
                        checked = state.checkboxState,
                        selectorDescription = state.filteredIdsText,
                        onToggle = viewModel::toggleShowTriggerEvents,
                        onSelect = { showEventSelectionDialog(viewModel.getTriggerEventsFilter()) },
                    ) }
                }
            }
        }
    }

    @Composable private fun TimeFilter(state: DebugReportTimeFilterUiState) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TimeValue(state.lowerValueText)
                    TimeValue(state.upperValueText)
                }
                if (state.lowerBoundMs < state.upperBoundMs) {
                    RangeSlider(
                        value = state.lowerValueMs.toFloat()..state.upperValueMs.toFloat(),
                        onValueChange = { range ->
                            viewModel.setUserTimeLowerBound(range.start.toLong())
                            viewModel.setUserTimeUpperBound(range.endInclusive.toLong())
                        },
                        valueRange = state.lowerBoundMs.toFloat()..state.upperBoundMs.toFloat(),
                    )
                }
            }
        }
    }

    @Composable private fun TimeValue(value: String) {
        ElevatedCard { Text(
            value,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        ) }
    }

    @Composable private fun EventFilterCard(
        title: String,
        description: String,
        checked: Boolean,
        selectorDescription: String,
        onToggle: () -> Unit,
        onSelect: () -> Unit,
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(description, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked, onCheckedChange = { onToggle() })
                }
                HorizontalDivider()
                Column(
                    Modifier.fillMaxWidth().clickable(enabled = checked, onClick = onSelect).padding(vertical = 12.dp),
                ) {
                    Text(context.getString(R.string.field_debug_filter_events_show_title),
                        color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                    Text(selectorDescription, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (checked) 1f else 0.38f))
                }
            }
        }
    }

    private fun showEventSelectionDialog(filter: DebugReportTimelineFilter.Events) {
        overlayManager.navigateTo(
            context,
            FilteredEventsSelectorDialog(filter) { viewModel.setEventsFilter(context, it) },
            hideCurrent = false,
        )
    }
}
