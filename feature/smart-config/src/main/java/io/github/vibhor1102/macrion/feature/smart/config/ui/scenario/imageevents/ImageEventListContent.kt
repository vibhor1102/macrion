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
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common.DualDragGestureDetector
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
            val sourceItems by viewModel.eventsItems.collectAsStateWithLifecycle(null)
            var reorderedItems by remember { mutableStateOf<List<UiImageEvent>?>(null) }
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
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

            val itemsToDisplay = reorderedItems ?: sourceItems ?: emptyList()
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val current = (reorderedItems ?: sourceItems ?: emptyList()).toMutableList()
                if (from.index in current.indices && to.index in current.indices) {
                    reorderedItems = current.apply { add(to.index, removeAt(from.index)) }
                }
            }

            LaunchedEffect(sourceItems) {
                if (!reorderableState.isAnyItemDragging) {
                    reorderedItems = null
                }
            }

            val onDragStarted: (androidx.compose.ui.geometry.Offset) -> Unit = remember(haptic) {
                {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }
            val onDragStopped: () -> Unit = remember(viewModel, sourceItems) {
                {
                    val currentReordered = reorderedItems
                    if (currentReordered != null && currentReordered != sourceItems) {
                        viewModel.updateEventsPriority(currentReordered)
                    } else {
                        reorderedItems = null
                    }
                }
            }
            val onMoveEvent: (Int, Int) -> Unit = remember(viewModel, sourceItems) {
                { from, to ->
                    val current = (sourceItems ?: emptyList()).toMutableList()
                    if (from in current.indices && to in current.indices) {
                        val updated = current.apply { add(to, removeAt(from)) }
                        viewModel.updateEventsPriority(updated)
                    }
                }
            }

            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                when {
                    sourceItems == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    sourceItems?.isEmpty() == true -> EmptyState(R.string.message_empty_screen_event_title, R.string.message_empty_screen_event_desc)
                    else -> LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
                        itemsIndexed(
                            items = itemsToDisplay,
                            key = { _, item -> item.event.id.toLazyListKey() },
                            contentType = { _, _ -> "image_event_item" },
                        ) { index, item ->
                            ImageEventListItem(
                                item = item,
                                index = index,
                                isLastIndex = index == itemsToDisplay.lastIndex,
                                touchExplorationEnabled = touchExplorationEnabled,
                                reorderableState = reorderableState,
                                onEventClick = remember(item.event) { { onEventItemClicked(item.event) } },
                                onDragStarted = onDragStarted,
                                onDragStopped = onDragStopped,
                                onMoveEvent = onMoveEvent,
                                context = context,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun androidx.compose.foundation.lazy.LazyItemScope.ImageEventListItem(
        item: UiImageEvent,
        index: Int,
        isLastIndex: Boolean,
        touchExplorationEnabled: Boolean,
        reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
        onEventClick: () -> Unit,
        onDragStarted: (androidx.compose.ui.geometry.Offset) -> Unit,
        onDragStopped: () -> Unit,
        onMoveEvent: (Int, Int) -> Unit,
        context: Context,
    ) {
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
                onClick = onEventClick,
            )
        } else Modifier

        val accessibilityActions = if (touchExplorationEnabled) {
            remember(index, isLastIndex, onMoveEvent) {
                buildList {
                    if (index > 0) add(CustomAccessibilityAction(context.getString(R.string.action_move_event_up)) {
                        onMoveEvent(index, index - 1)
                        true
                    })
                    if (!isLastIndex) add(CustomAccessibilityAction(context.getString(R.string.action_move_event_down)) {
                        onMoveEvent(index, index + 1)
                        true
                    })
                }
            }
        } else emptyList()

        ReorderableItem(
            state = reorderableState,
            key = item.event.id.toLazyListKey(),
            animateItemModifier = if (reorderableState.isAnyItemDragging) Modifier.animateItem() else Modifier,
        ) { isBeingDragged ->
            val reorderHandleModifier = Modifier
                .draggableHandle(
                    onDragStarted = onDragStarted,
                    onDragStopped = onDragStopped,
                    dragGestureDetector = DualDragGestureDetector,
                )
                .clearAndSetSemantics { }

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
                    onClick = onEventClick,
                    isBeingDragged = isBeingDragged,
                    reorderHandleModifier = reorderHandleModifier,
                    accessibilityActions = accessibilityActions,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
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
