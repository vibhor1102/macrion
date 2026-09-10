/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportEmptyMessage
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportFastScroller
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportLoading
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.DebugReportEventOccurrenceDetailsDialog
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.filter.DebugReportTimelineFiltersDialog
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.adapter.ReportTimelineItem

class DebugReportTimelineContent(appContext: Context) : NavBarDialogContent(appContext) {
    private val viewModel: DebugReportTimelineViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportTimelineViewModel() },
    )
    override fun floatingActionButtonsAreAvailable() = true
    override fun primaryFloatingActionButtonIcon() = R.drawable.ic_filter

    override fun onCreateView(container: ViewGroup): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@DebugReportTimelineContent.Content() } }
    }
    override fun onViewCreated() = Unit

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        LaunchedEffect(state) {
            updateFiltersBadge(state.activeFilterCount())
            if (state == DebugReportTimelineUiState.NotAvailable) dialogController.back()
        }
        when (state) {
                DebugReportTimelineUiState.Loading -> ReportLoading()
                DebugReportTimelineUiState.NotAvailable -> Unit
                DebugReportTimelineUiState.Empty -> ReportEmptyMessage(
                    context.getString(R.string.title_event_occurrence_empty),
                )
                is DebugReportTimelineUiState.FilteredEmpty -> ReportEmptyMessage(
                    context.getString(R.string.title_event_occurrence_filtered_empty),
                    context.getString(R.string.desc_event_occurrence_filtered_empty),
                ) {
                    Button(onClick = viewModel::clearFilters, modifier = Modifier.padding(top = 8.dp)) {
                        Text(context.getString(R.string.button_clear_timeline_filters))
                    }
                }
                is DebugReportTimelineUiState.Available -> TimelineList(
                    items = state.eventsOccurrences,
                    onItemClicked = ::onEventOccurrenceClicked,
                )
        }
    }

    override fun onStop() {
        dialogController.floatingActionButtons.setBadge(null)
    }

    private fun updateFiltersBadge(count: Int) {
        dialogController.floatingActionButtons.setBadge(
            text = count.takeIf { it > 0 }?.toString(),
            description = context.getString(R.string.content_desc_timeline_filters_active, count),
        )
    }

    private fun DebugReportTimelineUiState.activeFilterCount() = when (this) {
        is DebugReportTimelineUiState.Available -> activeFilterCount
        is DebugReportTimelineUiState.FilteredEmpty -> activeFilterCount
        else -> 0
    }

    private fun onEventOccurrenceClicked(item: DebugReportTimelineEventOccurrenceItem) {
        dialogController.overlayManager.navigateTo(
            context,
            DebugReportEventOccurrenceDetailsDialog(item.scenarioId, item.occurrence),
            hideCurrent = false,
        )
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        val duration = when (val state = viewModel.uiState.value) {
            is DebugReportTimelineUiState.Available -> state.durationMs
            is DebugReportTimelineUiState.FilteredEmpty -> state.durationMs
            else -> 0L
        }
        dialogController.overlayManager.navigateTo(
            context,
            DebugReportTimelineFiltersDialog(duration, viewModel.getFilters(), viewModel::setFilters),
            hideCurrent = false,
        )
    }
}

@Composable
private fun TimelineList(
    items: List<DebugReportTimelineEventOccurrenceItem>,
    onItemClicked: (DebugReportTimelineEventOccurrenceItem) -> Unit,
) {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items, key = DebugReportTimelineEventOccurrenceItem::id) { item ->
                ReportTimelineItem(item) { onItemClicked(item) }
            }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = LocalContext.current.getString(R.string.content_desc_timeline_fast_scroller),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
