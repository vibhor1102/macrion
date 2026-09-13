/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.filter.events

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportDialogTopBar
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.ReportFastScroller
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.filter.DebugReportTimelineFilter

class FilteredEventsSelectorDialog(
    private val eventsFilter: DebugReportTimelineFilter.Events,
    private val onFilteredIdsChanged: (DebugReportTimelineFilter.Events) -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    private val viewModel: FilteredEventsSelectorViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { filteredEventsSelectorViewModel() },
    )
    override fun onCreateView(): ViewGroup {
        viewModel.setEventFilter(eventsFilter)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@FilteredEventsSelectorDialog.Content() } }
        }
    }

@Composable private fun Content() {
        val items = viewModel.eventsItems.collectAsStateWithLifecycle(initialValue = emptyList()).value
        Surface(Modifier.fillMaxSize().heightIn(min = 600.dp)) {
            Column {
                ReportDialogTopBar(
                    title = "",
                    onDismiss = ::back,
                    onSave = {
                        onFilteredIdsChanged(viewModel.getFilter())
                        back()
                    },
                )
                FilteredEventsList(items, viewModel::setFilteredState, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FilteredEventsList(
    items: List<FilteredEventsSelectorItem>,
    onItemClicked: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(items, key = FilteredEventsSelectorItem::eventId) { item ->
                FilteredEventItem(item, onItemClicked)
            }
        }
        ReportFastScroller(
            state = listState,
            contentDescription = LocalContext.current.getString(
                R.string.content_desc_filtered_events_fast_scroller,
            ),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun FilteredEventItem(
    item: FilteredEventsSelectorItem,
    onItemClicked: (Long, Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clickable { onItemClicked(item.eventId, !item.eventState) }
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.eventName,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Checkbox(
                checked = item.eventState,
                onCheckedChange = { onItemClicked(item.eventId, !item.eventState) },
            )
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}
