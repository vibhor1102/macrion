/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.counter

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
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportEmptyMessage
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportFastScroller
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportLoading
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportNameValueRow
import java.math.BigDecimal

class DebugCounterStateContent(
    appContext: Context,
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
) : NavBarDialogContent(appContext) {
    private val viewModel: DebugCounterStateContentViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugCounterStateContentViewModel() },
    )
    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setOccurrence(scenarioId, eventOccurrence)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DebugCounterStateContent.Content() } }
        }
    }
    override fun onViewCreated() = Unit

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        when (state) {
            DebugCounterStateContentUiState.Loading -> ReportLoading()
            DebugCounterStateContentUiState.Empty -> ReportEmptyMessage(
                context.getString(R.string.title_event_occurrence_counters_empty),
                context.getString(R.string.desc_event_occurrence_counters_empty),
            )
            is DebugCounterStateContentUiState.Available -> CounterStateList(state.countersState)
        }
    }
}

@Composable
private fun CounterStateList(items: List<CounterStateItem>) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items, key = CounterStateItem::counterName) { item ->
                ReportNameValueRow(item.counterName, item.toValueDisplayText(context))
            }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = context.getString(R.string.content_desc_event_occurrence_fast_scroller),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

private fun CounterStateItem.toValueDisplayText(context: Context): String {
    val oldValue = oldCounterValue
    return if (oldValue == null) {
        context.getString(R.string.item_counter_state_value_same, currentCounterValue.toNaturalDisplayString())
    } else {
        context.getString(
            R.string.item_counter_state_value_changed,
            oldValue.toNaturalDisplayString(),
            currentCounterValue.toNaturalDisplayString(),
        )
    }
}

private fun Double.toNaturalDisplayString(): String =
    if (!isFinite()) toString() else BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
