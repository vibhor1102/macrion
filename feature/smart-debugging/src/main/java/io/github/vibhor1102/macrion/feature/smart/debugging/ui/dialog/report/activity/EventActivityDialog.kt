/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.activity

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportDialogTopBar
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportEmptyMessage
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportFastScroller
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportLoading
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportActivityRow
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter.ReportSectionHeader

class EventActivityDialog : OverlayDialog(R.style.AppTheme) {
    private val viewModel: EventActivityViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { eventActivityViewModel() },
    )
    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@EventActivityDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    @Composable private fun Content() {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        LaunchedEffect(state) {
            if (state == EventActivityUiState.NotAvailable) back()
        }
        Surface(Modifier.fillMaxSize().heightIn(min = 600.dp)) {
            Column {
                ReportDialogTopBar(context.getString(R.string.dialog_overlay_title_event_activity), ::back)
                Box(Modifier.weight(1f)) {
                    when (state) {
                        EventActivityUiState.Loading -> ReportLoading()
                        EventActivityUiState.Empty -> ReportEmptyMessage(
                            context.getString(R.string.title_event_activity_empty),
                            context.getString(R.string.desc_event_activity_empty),
                        )
                        is EventActivityUiState.Available -> {
                            EventActivityList(state.items, viewModel.getSort(), viewModel::setSort)
                        }
                        EventActivityUiState.NotAvailable -> Unit
                    }
                }
            }
        }
    }

}

@Composable
private fun EventActivityList(
    items: List<EventActivityListItem>,
    selectedSort: EventActivitySort,
    onSortSelected: (EventActivitySort) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(bottom = 88.dp),
        ) {
            items(items, key = {
                when (it) {
                    is EventActivityListItem.Header -> "header-${it.type}"
                    is EventActivityListItem.Event -> "event-${it.activity.key}"
                }
            }) { item ->
                when (item) {
                    is EventActivityListItem.Header -> {
                        val title = when (item.type) {
                            EventActivityType.SCREEN -> R.string.item_event_activity_screen_events
                            EventActivityType.TRIGGER -> R.string.item_event_activity_trigger_events
                        }
                        val icon = if (item.type == EventActivityType.SCREEN) R.drawable.ic_screen_event else R.drawable.ic_trigger_event
                        ReportSectionHeader(context.getString(title), icon)
                    }
                    is EventActivityListItem.Event -> ReportActivityRow(
                        name = item.activity.name,
                        count = context.getString(R.string.item_event_activity_occurrence_count, item.activity.occurrenceCount),
                        reached = item.activity.occurrenceCount != 0,
                    )
                }
            }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = context.getString(R.string.content_desc_event_activity_fast_scroller),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            FloatingActionButton(onClick = { sortMenuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = context.getString(R.string.content_desc_event_activity_sort),
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                EventActivitySort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = context.getString(sort.labelRes),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            sortMenuExpanded = false
                            onSortSelected(sort)
                        },
                        trailingIcon = if (sort == selectedSort) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_debug_confirm),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

private val EventActivitySort.labelRes: Int
    get() = when (this) {
        EventActivitySort.SCENARIO_ORDER -> R.string.event_activity_sort_scenario_order
        EventActivitySort.MOST_FREQUENT -> R.string.event_activity_sort_most_frequent
        EventActivitySort.FIRST_EXECUTION -> R.string.event_activity_sort_first_execution
    }
