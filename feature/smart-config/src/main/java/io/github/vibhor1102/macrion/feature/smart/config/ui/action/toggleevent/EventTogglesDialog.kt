/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.toggleevent

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.EventToggle
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class EventTogglesDialog(
    private val toggleEventAction: ToggleEvent,
    private val scenarioEvents: List<Event>,
    private val onConfirmClicked: (List<EventToggle>) -> Unit,
    private val onDismissed: (() -> Unit)? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.EVENT_TOGGLES.name
    private val viewModel: EventTogglesViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventTogglesViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setDialogArgs(toggleEventAction, scenarioEvents)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@EventTogglesDialog.Content() } }
        }
    }
override fun onDestroy() { onDismissed?.invoke(); super.onDestroy() }

    @Composable private fun Content() {
        val listItems by viewModel.currentItems.collectAsStateWithLifecycle(initialValue = emptyList())
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar()
                if (listItems.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(context.getString(R.string.message_empty_screen_event_title), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(listItems, key = { item -> when (item) {
                            is EventTogglesListItem.Header -> "header:${item.title}"
                            is EventTogglesListItem.Item -> item.event.id.let { "event:${it.databaseId}:${it.tempId ?: ""}" }
                        } }, contentType = { item -> when (item) {
                            is EventTogglesListItem.Header -> "header"
                            is EventTogglesListItem.Item -> "event"
                        } }) { item -> when (item) {
                            is EventTogglesListItem.Header -> Header(item.title)
                            is EventTogglesListItem.Item -> EventRow(item)
                        } }
                    }
                }
            }
        }
    }

    @Composable private fun TopBar() {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_events_toggle), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledIconButton(onClick = ::save) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @Composable private fun Header(title: String) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 8.dp))
        }
    }

    @Composable private fun EventRow(item: EventTogglesListItem.Item) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Clip)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EventCount(R.drawable.ic_condition, item.conditionsCount)
                    EventCount(R.drawable.ic_click, item.actionsCount)
                }
            }
            EventToggleButtons(when (item.toggleState) {
                ToggleEvent.ToggleType.ENABLE -> BUTTON_ENABLE_EVENT
                ToggleEvent.ToggleType.TOGGLE -> BUTTON_TOGGLE_EVENT
                ToggleEvent.ToggleType.DISABLE -> BUTTON_DISABLE_EVENT
                null -> null
            }) { checked -> viewModel.changeEventToggleState(item.event, when (checked) {
                BUTTON_ENABLE_EVENT -> ToggleEvent.ToggleType.ENABLE
                BUTTON_TOGGLE_EVENT -> ToggleEvent.ToggleType.TOGGLE
                BUTTON_DISABLE_EVENT -> ToggleEvent.ToggleType.DISABLE
                else -> null
            }) }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }

    @Composable private fun EventCount(icon: Int, count: Int) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    private fun save() { onConfirmClicked(viewModel.getEditedEventToggleList()); back() }
}
