/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.event

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.smart.debugging.domain.model.report.DebugReportEventOccurrence
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportFastScroller
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportLoading
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportIconTransition
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportNameValueRow
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportSectionHeader

class DebugEventsStateContent(
    appContext: Context,
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
) : NavBarDialogContent(appContext) {
    private val viewModel: DebugEventsStateContentViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugEventStateContentViewModel() },
    )
    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setOccurrence(scenarioId, eventOccurrence)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DebugEventsStateContent.Content() } }
        }
    }
    override fun onViewCreated() = Unit

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        when (state) {
            DebugEventsStateContentUiState.Loading -> ReportLoading()
            DebugEventsStateContentUiState.Empty -> Box(Modifier.fillMaxSize())
            is DebugEventsStateContentUiState.Available -> EventStateList(state.eventsState)
        }
    }
}

@Composable
private fun EventStateList(items: List<DebugEventStateItem>) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items, key = {
                when (it) {
                    is DebugEventStateItem.Header -> "header-${it.title}"
                    is DebugEventStateItem.EventState -> "event-${it.eventId}"
                }
            }) { item ->
                when (item) {
                    is DebugEventStateItem.Header -> ReportSectionHeader(
                        title = context.getString(item.title),
                        iconRes = item.icon,
                    )
                    is DebugEventStateItem.EventState -> ReportNameValueRow(item.eventName, "") {
                        ReportIconTransition(
                            startIcon = if (item.haveChanged) (!item.isEnabled).toEventStateIcon() else null,
                            endIcon = item.isEnabled.toEventStateIcon(),
                            separator = context.getString(R.string.event_state_changed_separator),
                        )
                    }
                }
            }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = context.getString(R.string.content_desc_event_occurrence_fast_scroller),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private fun Boolean.toEventStateIcon(): Int =
    if (this) R.drawable.ic_confirm else R.drawable.ic_cancel
