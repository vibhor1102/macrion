/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.event

import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

class FixEventsCopyDialog(
    private val eventsToCopy: List<Event>, private val onFixConfirmed: (List<Event>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    private val viewModel: FixEventsCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java, creator = { fixEventsCopyViewModel() })
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.FIX_EVENTS_COPY.name
    override fun onCreateView(): ViewGroup {
        viewModel.setEventsToCopy(eventsToCopy)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@FixEventsCopyDialog.Content() } }
        }
    }
@Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        FixCopyContent(context.getString(R.string.dialog_title_copy_fix), state == null, state?.canBeCopied == true,
            ::back, ::onSaveClicked) {
            itemsIndexed(state?.items.orEmpty(), key = { index, item -> when (item) {
                is FixCopyUiItem.Header -> "header:${item.message}:$index"
                is FixCopyUiItem.Item.EventItem -> item.uiEvent.event.id.let { "event:${it.databaseId}:${it.tempId ?: ""}" }
                else -> "item:$index"
            } }) { _, item -> when (item) {
                is FixCopyUiItem.Header -> FixMessageHeader(context.getString(item.message))
                is FixCopyUiItem.Item.EventItem -> EventRow(item)
                else -> Unit
            } }
        }
    }

    @Composable private fun EventRow(item: FixCopyUiItem.Item.EventItem) {
        val image = item.uiEvent as? UiImageEvent
        val trigger = item.uiEvent as? UiTriggerEvent
        val icon = if (image != null) R.drawable.ic_screen_event else R.drawable.ic_trigger_event
        val name = image?.name ?: trigger?.name.orEmpty()
        val actions = image?.actionsCountText ?: trigger?.actionsCountText.orEmpty()
        val conditions = image?.conditionsCountText ?: trigger?.conditionsCountText.orEmpty()
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
            .clickable(enabled = !item.isValidForCopy) { showFixChildrenDialog(item.uiEvent.event) }
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text("$actions  •  $conditions", style = MaterialTheme.typography.bodySmall,
                    color = if (item.isValidForCopy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
            Icon(painterResource(if (item.isValidForCopy) R.drawable.ic_confirm else R.drawable.ic_cancel), null,
                Modifier.size(28.dp), tint = if (item.isValidForCopy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }

    private fun onSaveClicked() {
        if (viewModel.uiState.value?.canBeCopied != true) return
        back(); onFixConfirmed(viewModel.getFixedEventsToCopy())
    }
    private fun showFixChildrenDialog(event: Event) {
        overlayManager.navigateTo(context, FixEventChildrenCopyDialog(
            FixEventChildrenCopyDialog.Arguments(viewModel.getResultingEventList(), event, false), viewModel::updateEvent), true)
    }
}
