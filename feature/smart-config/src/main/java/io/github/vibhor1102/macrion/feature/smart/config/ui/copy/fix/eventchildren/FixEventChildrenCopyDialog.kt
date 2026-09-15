/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.eventchildren

import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.EventToggle
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.toggleevent.EventTogglesDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection.CounterSelectionDialog

class FixEventChildrenCopyDialog(
    private val dialogArguments: Arguments, private val onFixConfirmed: (Event) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    data class Arguments(val resultingEventList: List<Event>, val parent: Event, val showHelpMessage: Boolean)
    private val viewModel: FixEventChildrenCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java, creator = { fixEventChildrenCopyViewModel() })
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.FIX_EVENT_CHILDREN_COPY.name
    override fun onCreateView(): ViewGroup {
        viewModel.setDialogArguments(dialogArguments)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@FixEventChildrenCopyDialog.Content() } }
        }
    }
@Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        FixCopyContent(context.getString(R.string.dialog_title_copy_fix), state == null, state?.canBeCopied == true,
            ::back, ::onSaveClicked) {
            itemsIndexed(state?.items.orEmpty(), key = { index, item -> when (item) {
                is FixCopyUiItem.Header -> "header:${item.message}:$index"
                is FixCopyUiItem.Item.EventChildren.ActionItem -> item.uiAction.action.id.let { "action:${it.databaseId}:${it.tempId ?: ""}" }
                is FixCopyUiItem.Item.EventChildren.ConditionItem -> item.uiCondition.condition.id.let { "condition:${it.databaseId}:${it.tempId ?: ""}" }
                else -> "item:$index"
            } }) { _, item -> when (item) {
                is FixCopyUiItem.Header -> FixMessageHeader(context.getString(item.message))
                is FixCopyUiItem.Item.EventChildren -> ChildCard(item)
                else -> Unit
            } }
        }
    }

    @Composable private fun ChildCard(item: FixCopyUiItem.Item.EventChildren) {
        val isAction = item is FixCopyUiItem.Item.EventChildren.ActionItem
        val color = colorResource(if (isAction) R.color.event_actions_color else R.color.event_conditions_color)
        val icon = if (item is FixCopyUiItem.Item.EventChildren.ActionItem) item.uiAction.icon
            else (item as FixCopyUiItem.Item.EventChildren.ConditionItem).uiCondition.iconRes
        val name = if (item is FixCopyUiItem.Item.EventChildren.ActionItem) item.uiAction.name
            else (item as FixCopyUiItem.Item.EventChildren.ConditionItem).uiCondition.name
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).border(2.dp, color, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.stateText, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(if (item.isValidForCopy) R.drawable.ic_confirm else R.drawable.ic_warning), null,
                    Modifier.size(28.dp), tint = if (item.isValidForCopy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            item.itemWithMissingReferences.missingReferences.forEach { MissingReferenceRow(item, it, color) }
        }
    }

    @Composable private fun MissingReferenceRow(
        item: FixCopyUiItem.Item.EventChildren, reference: MissingCopyReference, color: Color,
    ) {
        val icon = when (reference) {
            is MissingCopyReference.EventToggleReference -> R.drawable.ic_toggle_event
            is MissingCopyReference.ScreenConditionReference -> R.drawable.ic_image_condition
            is MissingCopyReference.CounterReference -> R.drawable.ic_change_counter
        }
        Row(Modifier.fillMaxWidth().height(56.dp).clickable { onMissingReferenceClicked(item, reference) },
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(12.dp).fillMaxHeight().background(color))
            Icon(painterResource(icon), null, Modifier.padding(start = 12.dp).size(24.dp))
            Text(reference.name, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.padding(end = 16.dp).size(20.dp))
        }
    }

    private fun onSaveClicked() {
        if (viewModel.uiState.value?.canBeCopied != true) return
        viewModel.getFixedEventToCopy()?.let { back(); onFixConfirmed(it) }
    }
    private fun onMissingReferenceClicked(item: FixCopyUiItem.Item.EventChildren, reference: MissingCopyReference) {
        when (reference) {
            is MissingCopyReference.EventToggleReference -> {
                val action = (item as? FixCopyUiItem.Item.EventChildren.ActionItem)?.uiAction?.action as? ToggleEvent ?: return
                showReplaceEventToggleDialog(action) { viewModel.updateEventToggles(item, reference, it) }
            }
            is MissingCopyReference.ScreenConditionReference -> showReplaceScreenConditionDialog { viewModel.updateScreenCondition(item, reference, it) }
            is MissingCopyReference.CounterReference -> showReplaceCounterDialog { viewModel.updateCounter(item, reference, it) }
        }
    }
    private fun showReplaceScreenConditionDialog(selected: (ScreenCondition) -> Unit) = overlayManager.navigateTo(
        context, ScreenConditionSelectionDialog(viewModel.getScreenConditionReplacementCandidates(context), selected), true)
    private fun showReplaceEventToggleDialog(action: ToggleEvent, selected: (List<EventToggle>) -> Unit) {
        viewModel.startActionEdition(action)
        overlayManager.navigateTo(context, EventTogglesDialog(action, dialogArguments.resultingEventList,
            { viewModel.stopActionEdition(); selected(it) }, { viewModel.stopActionEdition() }), true)
    }
    private fun showReplaceCounterDialog(selected: (String) -> Unit) =
        overlayManager.navigateTo(context, CounterSelectionDialog(selected), true)
}
