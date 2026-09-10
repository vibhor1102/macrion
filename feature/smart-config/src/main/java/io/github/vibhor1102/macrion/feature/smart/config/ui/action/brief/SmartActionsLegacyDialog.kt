/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.selection.ActionTypeSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction

import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


class SmartActionsLegacyDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SMART_ACTIONS_LEGACY.name


    /** View model for this content. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    override fun onCreateView(): ViewGroup {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@SmartActionsLegacyDialog.Content() } }
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    private fun onCreateButtonClicked() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ActionTypeSelectionDialog(
                choices = viewModel.actionTypeChoices.value,
                onChoiceSelectedListener = { choiceClicked ->
                    showActionConfigDialog(viewModel, viewModel.createAction(context, choiceClicked))
                },
            ),
        )
    }

    private fun onCopyButtonClicked() {
        showActionCopyDialog(viewModel)
    }

    private fun onActionClicked(item: ItemBrief) {
        debounceUserInteraction { showActionConfigDialog(viewModel, (item.data as UiAction).action) }
    }

    @Composable private fun Content() {
        val canCopy = viewModel.canCopyActions.collectAsStateWithLifecycle(false).value
        val items = viewModel.actionBriefList.collectAsStateWithLifecycle(null).value
        var displayedItems by remember { mutableStateOf(emptyList<ItemBrief>()) }
        var isReordering by remember { mutableStateOf(false) }
        LaunchedEffect(items) { if (!isReordering) displayedItems = items.orEmpty() }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            displayedItems = displayedItems.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
                    Text(context.getString(R.string.menu_item_title_actions), Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleLarge)
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        items == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        items.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(context.getString(R.string.message_empty_action_list_title), style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(context.getString(R.string.message_empty_action_list_desc), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                            items(displayedItems, key = { it.id.databaseId.takeIf { id -> id != 0L } ?: -requireNotNull(it.id.tempId) }) { item ->
                                val key = item.id.databaseId.takeIf { it != 0L } ?: -requireNotNull(item.id.tempId)
                                ReorderableItem(reorderState, key) { dragging ->
                                    ActionRow(
                                        item = item,
                                        isBeingDragged = dragging,
                                        reorderHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { isReordering = true },
                                            onDragStopped = {
                                                viewModel.updateActionOrder(displayedItems)
                                                isReordering = false
                                            },
                                        ),
                                        onClick = { onActionClicked(item) },
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                    Column(Modifier.align(Alignment.BottomEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (canCopy) FloatingActionButton(onClick = ::onCopyButtonClicked,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(painterResource(R.drawable.ic_copy), context.getString(R.string.content_desc_copy_button))
                        }
                        FloatingActionButton(onClick = ::onCreateButtonClicked) {
                            Icon(painterResource(R.drawable.ic_add), context.getString(R.string.content_desc_add_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    item: ItemBrief,
    isBeingDragged: Boolean,
    reorderHandleModifier: Modifier,
    onClick: () -> Unit,
) {
    val details = item.data as UiAction
    Row(
        Modifier.fillMaxWidth().height(80.dp).clickable(onClick = onClick).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp)
                .background(if (isBeingDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .then(reorderHandleModifier),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_reorder), null, Modifier.size(24.dp)) }
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 12.dp)) {
            Text(details.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(details.description, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(32.dp)) {
            Icon(painterResource(details.icon), null, Modifier.matchParentSize())
            if (details.haveError) Box(Modifier.align(Alignment.TopEnd).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
        }
    }
}
