/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.copy.event

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.CopyPickerContent
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.CopySectionHeader
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.event.FixEventsCopyDialog

class EventCopyDialog(
    private val requestTriggerEvents: Boolean,
    private val onEventsSelected: (List<Event>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.EVENT_COPY.name
    private val viewModel: EventCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventCopyModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setCopyListType(requestTriggerEvents)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@EventCopyDialog.Content() } }
        }
    }
@Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        var query by rememberSaveable { mutableStateOf("") }
        val list = state.orEmpty()
        CopyPickerContent(
            title = context.getString(R.string.dialog_overlay_title_copy_from),
            searchHint = context.getString(R.string.search_view_hint_event_copy),
            emptyMessage = context.getString(R.string.message_empty_copy),
            query = query,
            loading = state == null,
            empty = state != null && list.isEmpty(),
            copyEnabled = list.any { it is EventCopyItem.EventItem },
            onQueryChanged = { query = it; viewModel.updateSearchQuery(it) },
            onDismiss = ::back,
            onCopy = ::onCopyClicked,
        ) {
            itemsIndexed(list, key = { index, item ->
                when (item) {
                    is EventCopyItem.Header -> "header:${item.title}:$index"
                    is EventCopyItem.EventItem -> item.uiEvent.event.id.let { "event:${it.databaseId}:${it.tempId ?: ""}" }
                }
            }) { _, item ->
                when (item) {
                    is EventCopyItem.Header -> CopySectionHeader(context.getString(item.title))
                    is EventCopyItem.EventItem -> EventRow(item)
                }
            }
        }
    }

    @Composable
    private fun EventRow(item: EventCopyItem.EventItem) {
        val ui = when (item) {
            is EventCopyItem.EventItem.Image -> EventDetails(
                R.drawable.ic_screen_event, item.uiEvent.enabledOnStartIconRes, item.uiEvent.enabledOnStartTextRes,
                item.uiEvent.actionsCountText, item.uiEvent.conditionsCountText, item.uiEvent.haveError,
            )
            is EventCopyItem.EventItem.Trigger -> EventDetails(
                R.drawable.ic_trigger_event, item.uiEvent.enabledOnStartIconRes, item.uiEvent.enabledOnStartTextRes,
                item.uiEvent.actionsCountText, item.uiEvent.conditionsCountText, item.uiEvent.haveError,
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { viewModel.toggleCheckedForCopy(item.uiEvent.event) }
                .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(ui.icon), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    EventDetail(ui.enabledIcon, context.getString(ui.enabledText))
                    EventDetail(R.drawable.ic_click, ui.actions, error = ui.error)
                    EventDetail(if (item is EventCopyItem.EventItem.Image) R.drawable.ic_condition else R.drawable.ic_trigger_condition, ui.conditions)
                }
            }
            Checkbox(item.checked, onCheckedChange = { viewModel.toggleCheckedForCopy(item.uiEvent.event) })
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }

    @Composable
    private fun EventDetail(icon: Int, text: String, error: Boolean = false) {
        val color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), tint = color)
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }

    private data class EventDetails(
        val icon: Int, val enabledIcon: Int, val enabledText: Int, val actions: String, val conditions: String, val error: Boolean,
    )

    private fun onCopyClicked() {
        val events = viewModel.getEventsCopy()
        if (viewModel.eventsCopyShouldWarnUser(events)) {
            overlayManager.navigateTo(context, FixEventsCopyDialog(events, ::notifySelectionAndDestroy), false)
        } else notifySelectionAndDestroy(events)
    }

    private fun notifySelectionAndDestroy(events: List<Event>) {
        viewModel.saveCopyEvents(events)
        back()
        onEventsSelected(events)
    }
}
