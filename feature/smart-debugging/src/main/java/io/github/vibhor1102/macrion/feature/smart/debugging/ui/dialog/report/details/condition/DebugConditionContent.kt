/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.condition

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportKeyValueCard
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportTriggerConditionCard
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.condition.adapter.ScreenConditionResultRow
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.details.condition.adapter.ScreenConditionResultState
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import kotlinx.coroutines.Job

class DebugConditionContent(
    appContext: Context,
    private val scenarioId: Long,
    private val eventOccurrence: DebugReportEventOccurrence,
) : NavBarDialogContent(appContext) {
    private val viewModel: DebugConditionContentViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugConditionContentViewModel() },
    )
    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setOccurrence(scenarioId, eventOccurrence)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DebugConditionContent.Content() } }
        }
    }
    override fun onViewCreated() = Unit

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        when (state) {
            DebugConditionContentUiState.Loading -> ReportLoading()
            is DebugConditionContentUiState.Available -> ConditionOccurrenceList(
                state.items,
                viewModel::getConditionBitmap,
            )
        }
    }
}

@Composable
private fun ConditionOccurrenceList(
    items: List<EventOccurrenceItem>,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items, key = {
                when (it) {
                    is EventOccurrenceItem.Header -> "header"
                    is EventOccurrenceItem.Screen -> "screen-${it.id}"
                    is EventOccurrenceItem.Trigger -> "trigger-${it.id}"
                }
            }) { item ->
                when (item) {
                    is EventOccurrenceItem.Header -> ReportKeyValueCard(
                        title = context.getString(R.string.item_event_occurrence_details_header_title),
                        value = item.conditionOperatorValueText,
                    )
                    is EventOccurrenceItem.Screen -> ScreenConditionOccurrenceItem(item, bitmapProvider)
                    is EventOccurrenceItem.Trigger -> ReportTriggerConditionCard(
                        item.conditionName,
                        item.description,
                        item.iconRes,
                    )
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

@Composable
private fun ScreenConditionOccurrenceItem(
    item: EventOccurrenceItem.Screen,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
) {
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    var bitmapFailed by remember(item.id) { mutableStateOf(false) }
    DisposableEffect(item) {
        val job = (item.condition as? ScreenCondition.Image)?.let { condition ->
            bitmapProvider(condition) { loadedBitmap ->
                bitmap = loadedBitmap
                bitmapFailed = loadedBitmap == null
            }
        }
        onDispose { job?.cancel() }
    }
    ScreenConditionResultRow(ScreenConditionResultState(item, bitmap, bitmapFailed))
}
