/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
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
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.adapter.ConditionPerformanceFooter
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.adapter.ConditionPerformanceRow
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.adapter.ConditionPerformanceRowState
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.sort.DebugReportSortOption
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.sort.DebugReportSortPopup
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import kotlinx.coroutines.Job

class ConditionPerformanceContent(appContext: Context) : NavBarDialogContent(appContext) {
    private val viewModel: ConditionPerformanceViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { conditionPerformanceViewModel() },
    )
    private var sortPopup: DebugReportSortPopup<ConditionPerformanceSort>? = null

    override fun floatingActionButtonsAreAvailable() = true
    override fun primaryFloatingActionButtonIcon() = R.drawable.ic_sort
    override fun onCreateView(container: ViewGroup): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ConditionPerformanceContent.Content() } }
    }
    override fun onViewCreated() = Unit
    override fun onStart() {
        dialogController.floatingActionButtons.primary.contentDescription =
            context.getString(R.string.content_desc_condition_performance_sort)
    }
    override fun onStop() { sortPopup?.dismiss(); sortPopup = null }

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        LaunchedEffect(state) {
            dialogController.floatingActionButtons.root.visibility =
                if (state is ConditionPerformanceUiState.Available) View.VISIBLE else View.GONE
        }
        when (state) {
            ConditionPerformanceUiState.Loading -> ReportLoading()
            ConditionPerformanceUiState.NotAvailable -> ReportEmptyMessage(
                context.getString(R.string.title_condition_performance_unavailable),
            )
            is ConditionPerformanceUiState.Available -> ConditionPerformanceList(
                entries = state.entries,
                bitmapProvider = viewModel::getConditionBitmap,
            )
        }
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        val selected = viewModel.getSort()
        sortPopup?.dismiss()
        sortPopup = DebugReportSortPopup(
            dialogController.floatingActionButtons.primary,
            ConditionPerformanceSort.entries.map { sort -> DebugReportSortOption(
                sort,
                when (sort) {
                    ConditionPerformanceSort.TOTAL_TIME -> R.string.condition_performance_sort_total_time
                    ConditionPerformanceSort.AVERAGE_PER_CHECK -> R.string.condition_performance_sort_average
                    ConditionPerformanceSort.CHECKS -> R.string.condition_performance_sort_checks
                    ConditionPerformanceSort.SCENARIO_ORDER -> R.string.condition_performance_sort_scenario_order
                },
                sort == selected,
            ) },
            viewModel::setSort,
        ).also { it.show() }
    }
}

@Composable
private fun ConditionPerformanceList(
    entries: List<ConditionPerformanceEntry>,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job,
) {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            items(entries, key = { it.condition.id.databaseId }) { entry ->
                ConditionPerformanceItem(entry, bitmapProvider)
            }
            item { ConditionPerformanceFooter() }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = LocalContext.current.getString(
                R.string.content_desc_condition_performance_fast_scroller,
            ),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ConditionPerformanceItem(
    entry: ConditionPerformanceEntry,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job,
) {
    val context = LocalContext.current
    var bitmap by remember(entry.condition.id) { mutableStateOf<Bitmap?>(null) }
    var bitmapFailed by remember(entry.condition.id) { mutableStateOf(false) }
    DisposableEffect(entry.condition) {
        val job = (entry.condition as? ScreenCondition.Image)?.let { condition ->
            bitmapProvider(condition) { loadedBitmap ->
                bitmap = loadedBitmap
                bitmapFailed = loadedBitmap == null
            }
        }
        onDispose { job?.cancel() }
    }
    val fulfilledCount = formatCount(entry.fulfilledCount)
    val checkCount = formatCount(entry.checkCount)
    val average = formatAverageDuration(entry.totalDurationNs, entry.checkCount)?.let { value ->
        context.getString(R.string.item_condition_performance_average, value)
    } ?: context.getString(R.string.item_condition_performance_average_unavailable)
    ConditionPerformanceRow(
        ConditionPerformanceRowState(
            entry = entry,
            totalTime = context.getString(
                R.string.item_condition_performance_total_time,
                formatTotalDuration(entry.totalDurationNs),
            ),
            fulfilled = context.getString(
                R.string.item_condition_performance_fulfilled,
                fulfilledCount,
                context.resources.getQuantityString(
                    R.plurals.item_condition_performance_time,
                    entry.fulfilledCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                ),
                checkCount,
                context.resources.getQuantityString(
                    R.plurals.item_condition_performance_check,
                    entry.checkCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                ),
            ),
            average = average,
            percentage = formatPercentage(entry.totalDurationNs, entry.totalMeasuredDurationNs),
            bitmap = bitmap,
            bitmapFailed = bitmapFailed,
        ),
    )
}
