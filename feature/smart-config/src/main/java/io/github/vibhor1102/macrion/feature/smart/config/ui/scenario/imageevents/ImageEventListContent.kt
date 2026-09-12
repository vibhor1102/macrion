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
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.event.EventDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.event.EventCopyDialog
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.LocalMonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common.EventListRow

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

import kotlinx.coroutines.launch

class ImageEventListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: ImageEventListViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageEventListViewModel() },
    )

    override fun floatingActionButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@ImageEventListContent.Content() } }
        }
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.copyButtonIsVisible.collect(::updateCopyButtonVisibility) }
            }
        }
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        debounceUserInteraction {
            showEventConfigDialog(viewModel.createNewEvent(context))
        }
    }

    override fun onSecondaryFloatingActionButtonClicked() {
        debounceUserInteraction {
            showEventCopyDialog()
        }
    }

    private fun onEventItemClicked(event: ScreenEvent) {
        debounceUserInteraction {
            showEventConfigDialog(event)
        }
    }

    @Composable private fun Content() {
        CompositionLocalProvider(LocalMonitoredViewsManager provides viewModel.monitoredViewsManager) {
            val sourceItems = viewModel.eventsItems.collectAsStateWithLifecycle(null).value
            var displayedItems by remember { mutableStateOf(emptyList<UiImageEvent>()) }
            var isReordering by remember { mutableStateOf(false) }
            val accessibilityManager = remember(context) {
                context.getSystemService(AccessibilityManager::class.java)
            }
            var touchExplorationEnabled by remember(accessibilityManager) {
                mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
            }

            DisposableEffect(accessibilityManager) {
                val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                    touchExplorationEnabled = enabled
                }
                accessibilityManager?.addTouchExplorationStateChangeListener(listener)
                onDispose { accessibilityManager?.removeTouchExplorationStateChangeListener(listener) }
            }

            LaunchedEffect(sourceItems) {
                if (!isReordering) displayedItems = sourceItems ?: emptyList()
            }

            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                displayedItems = displayedItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }

            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                when {
                    sourceItems == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    displayedItems.isEmpty() -> EmptyState(R.string.message_empty_screen_event_title, R.string.message_empty_screen_event_desc)
                    else -> LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
                        itemsIndexed(displayedItems, key = { _, item -> item.event.id.toLazyListKey() }) { index, item ->
                            val anchorType = when (index) {
                                0 -> MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT
                                1 -> MonitoredViewType.SCENARIO_DIALOG_ITEM_SECOND_EVENT
                                2 -> MonitoredViewType.SCENARIO_DIALOG_ITEM_THIRD_EVENT
                                3 -> MonitoredViewType.SCENARIO_DIALOG_ITEM_FOURTH_EVENT
                                else -> null
                            }
                            val anchorModifier = if (anchorType != null) {
                                Modifier.tutorialAnchor(
                                    type = anchorType,
                                    onClick = { onEventItemClicked(item.event) },
                                )
                            } else Modifier

                            ReorderableItem(reorderableState, item.event.id.toLazyListKey()) { isBeingDragged ->
                                Column(modifier = anchorModifier) {
                                    EventListRow(
                                        name = item.name,
                                        conditionsCount = item.conditionsCountText,
                                        actionsCount = item.actionsCountText,
                                        enabledTextRes = item.enabledOnStartTextRes,
                                        enabledIconRes = item.enabledOnStartIconRes,
                                        conditionIconRes = R.drawable.ic_condition,
                                        actionsInError = item.haveError,
                                        showReorderHandle = true,
                                        onClick = { onEventItemClicked(item.event) },
                                        isBeingDragged = isBeingDragged,
                                        reorderHandleModifier = Modifier
                                            .longPressDraggableHandle(
                                                onDragStarted = { isReordering = true },
                                                onDragStopped = {
                                                    viewModel.updateEventsPriority(displayedItems)
                                                    isReordering = false
                                                },
                                            )
                                            .clearAndSetSemantics { },
                                        accessibilityActions = if (touchExplorationEnabled) {
                                            eventAccessibilityActions(index, displayedItems) { from, to ->
                                                displayedItems = displayedItems.move(from, to)
                                                viewModel.updateEventsPriority(displayedItems)
                                            }
                                        } else emptyList(),
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun eventAccessibilityActions(
        index: Int,
        items: List<UiImageEvent>,
        moveEvent: (Int, Int) -> Unit,
    ): List<CustomAccessibilityAction> = buildList {
        if (index > 0) add(CustomAccessibilityAction(context.getString(R.string.action_move_event_up)) {
            moveEvent(index, index - 1)
            true
        })
        if (index < items.lastIndex) add(CustomAccessibilityAction(context.getString(R.string.action_move_event_down)) {
            moveEvent(index, index + 1)
            true
        })
    }

    private fun List<UiImageEvent>.move(from: Int, to: Int): List<UiImageEvent> =
        toMutableList().apply { add(to, removeAt(from)) }

    /**
     * Lazy layouts persist item state in a Bundle, so keys must be Bundle-saveable.
     * Database ids are positive and temporary ids are positive but have a database id of zero;
     * making temporary ids negative yields a stable, allocation-free key for both cases.
     */
    private fun io.github.vibhor1102.macrion.core.base.identifier.Identifier.toLazyListKey(): Long =
        if (databaseId != 0L) databaseId else -requireNotNull(tempId)

    @Composable private fun EmptyState(title: Int, description: Int) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(context.getString(title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(context.getString(description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.floatingActionButtons.setSecondaryVisible(isVisible)
    }

    /** Opens the dialog allowing the user to copy an event. */
    private fun showEventCopyDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventCopyDialog(
                requestTriggerEvents = false,
                onEventsSelected = { events ->
                    if (events.size != 1) return@EventCopyDialog
                    (events[0] as? ScreenEvent)?.let { screenEvent ->  showEventConfigDialog(screenEvent) }
                },
            ),
        )
    }

    /** Opens the dialog allowing the user to add a new event. */
    private fun showEventConfigDialog(item: ScreenEvent) {
        viewModel.startEventEdition(item)

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventDialog(
                onConfigComplete = viewModel::saveEventEdition,
                onDelete = viewModel::deleteEditedEvent,
                onDismiss = viewModel::dismissEditedEvent,
            ),
            hideCurrent = true,
        )
    }
}
