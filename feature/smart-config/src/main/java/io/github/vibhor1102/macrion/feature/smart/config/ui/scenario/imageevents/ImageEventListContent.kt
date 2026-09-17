/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.event.EventDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.event.EventCopyDialog
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.LocalMonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common.EventListRow
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common.FolderEndBoundaryRow
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common.FolderHeaderRow

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

    private sealed class ScenarioListItem {
        abstract val key: Any

        data class FolderHeader(
            val name: String,
            val totalCount: Int,
            val enabledCount: Int,
            val isExpanded: Boolean,
        ) : ScenarioListItem() {
            override val key: Any get() = "folder_header_$name"
        }

        data class FolderEndBoundary(
            val folderName: String,
        ) : ScenarioListItem() {
            override val key: Any get() = "folder_end_$folderName"
        }

        data class EventItem(
            val item: UiImageEvent,
            val folderName: String?,
        ) : ScenarioListItem() {
            override val key: Any get() = item.event.id.toLazyListKey()
        }
    }

    @Composable private fun Content() {
        CompositionLocalProvider(LocalMonitoredViewsManager provides viewModel.monitoredViewsManager) {
            val sourceItems by viewModel.eventsItems.collectAsStateWithLifecycle(null)
            var customFolders by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
            var collapsedFolders by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
            var reorderedVisibleItems by remember { mutableStateOf<List<ScenarioListItem>?>(null) }

            // Dialog states
            var showNewFolderDialog by remember { mutableStateOf(false) }
            var folderToRename by remember { mutableStateOf<String?>(null) }
            var folderToDelete by remember { mutableStateOf<String?>(null) }

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

            val visibleItems = remember(sourceItems, customFolders, collapsedFolders) {
                buildVisibleItems(sourceItems ?: emptyList(), customFolders, collapsedFolders)
            }

            val itemsToDisplay = reorderedVisibleItems ?: visibleItems
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val current = (reorderedVisibleItems ?: visibleItems).toMutableList()
                if (from.index in current.indices && to.index in current.indices) {
                    val moved = current.removeAt(from.index)
                    current.add(to.index, moved)
                    reorderedVisibleItems = current
                }
            }

            LaunchedEffect(sourceItems, customFolders, collapsedFolders) {
                if (!reorderableState.isAnyItemDragging) {
                    reorderedVisibleItems = null
                }
            }

            val onDragStarted: (androidx.compose.ui.geometry.Offset) -> Unit = remember(haptic) {
                {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }

            val onDragStopped: () -> Unit = remember(viewModel, sourceItems) {
                {
                    val currentReordered = reorderedVisibleItems
                    if (currentReordered != null) {
                        val updatedEvents = reconstructEventsFromVisibleItems(
                            visibleItems = currentReordered,
                            allSourceEvents = sourceItems ?: emptyList(),
                        )
                        viewModel.updateRawEvents(updatedEvents)
                        reorderedVisibleItems = null
                    }
                }
            }

            val allFolderNames = remember(sourceItems, customFolders) {
                val fromEvents = (sourceItems ?: emptyList()).mapNotNull { it.folder?.trim()?.ifEmpty { null } }
                (fromEvents + customFolders).distinct()
            }

            DisposableEffect(allFolderNames, collapsedFolders) {
                if (allFolderNames.isNotEmpty()) {
                    val anyExpanded = allFolderNames.any { it !in collapsedFolders }
                    dialogController.topBarBinding.extraAction = {
                        IconButton(
                            onClick = {
                                collapsedFolders = if (anyExpanded) {
                                    allFolderNames.toSet()
                                } else {
                                    emptySet()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (anyExpanded) UiR.drawable.ic_unfold_less else UiR.drawable.ic_unfold_more
                                ),
                                contentDescription = stringResource(
                                    if (anyExpanded) R.string.folder_collapse_all else R.string.folder_expand_all
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    dialogController.topBarBinding.extraAction = null
                }
                onDispose {
                    dialogController.topBarBinding.extraAction = null
                }
            }

            DisposableEffect(Unit) {
                dialogController.floatingActionButtons.setTertiary(
                    icon = UiR.drawable.ic_folder,
                    visible = true,
                    description = context.getString(R.string.folder_action_new),
                ) {
                    showNewFolderDialog = true
                }
                onDispose {
                    dialogController.floatingActionButtons.setTertiaryVisible(false)
                }
            }

            Box(Modifier.fillMaxSize()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    when {
                        sourceItems == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        sourceItems?.isEmpty() == true && customFolders.isEmpty() -> EmptyState(R.string.message_empty_screen_event_title, R.string.message_empty_screen_event_desc)
                        else -> LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
                            itemsIndexed(
                                items = itemsToDisplay,
                                key = { _, item -> item.key },
                                contentType = { _, item ->
                                    when (item) {
                                        is ScenarioListItem.FolderHeader -> "folder_header_item"
                                        is ScenarioListItem.FolderEndBoundary -> "folder_end_item"
                                        is ScenarioListItem.EventItem -> "image_event_item"
                                    }
                                },
                            ) { index, listItem ->
                                when (listItem) {
                                    is ScenarioListItem.FolderHeader -> {
                                        ReorderableItem(
                                            state = reorderableState,
                                            key = listItem.key,
                                            animateItemModifier = if (reorderableState.isAnyItemDragging) Modifier.animateItem() else Modifier,
                                        ) { isBeingDragged ->
                                            val reorderHandleModifier = if (!listItem.isExpanded) {
                                                Modifier
                                                    .draggableHandle(
                                                        onDragStarted = onDragStarted,
                                                        onDragStopped = onDragStopped,
                                                    )
                                                    .clearAndSetSemantics { }
                                            } else Modifier

                                            Column {
                                                FolderHeaderRow(
                                                    name = listItem.name,
                                                    eventCount = listItem.totalCount,
                                                    enabledCount = listItem.enabledCount,
                                                    isExpanded = listItem.isExpanded,
                                                    onToggleExpand = {
                                                        collapsedFolders = if (listItem.isExpanded) {
                                                            collapsedFolders + listItem.name
                                                        } else {
                                                            collapsedFolders - listItem.name
                                                        }
                                                    },
                                                    onRenameClick = { folderToRename = listItem.name },
                                                    onDeleteClick = { folderToDelete = listItem.name },
                                                    onAddEventClick = {
                                                        showEventConfigDialog(
                                                            viewModel.createNewEventInFolder(context, listItem.name)
                                                        )
                                                    },
                                                    reorderHandleModifier = reorderHandleModifier,
                                                    isBeingDragged = isBeingDragged,
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                    is ScenarioListItem.FolderEndBoundary -> {
                                        ReorderableItem(
                                            state = reorderableState,
                                            key = listItem.key,
                                            animateItemModifier = if (reorderableState.isAnyItemDragging) Modifier.animateItem() else Modifier,
                                        ) { _ ->
                                            FolderEndBoundaryRow(folderName = listItem.folderName)
                                        }
                                    }
                                    is ScenarioListItem.EventItem -> {
                                        ImageEventListItem(
                                            item = listItem.item,
                                            index = index,
                                            isLastIndex = index == itemsToDisplay.lastIndex,
                                            touchExplorationEnabled = touchExplorationEnabled,
                                            reorderableState = reorderableState,
                                            currentFolderContext = getEffectiveFolderAt(itemsToDisplay, index),
                                            onEventClick = remember(listItem.item.event) { { onEventItemClicked(listItem.item.event) } },
                                            onDragStarted = onDragStarted,
                                            onDragStopped = onDragStopped,
                                            onMoveEvent = { fromIdx, toIdx ->
                                                val current = itemsToDisplay.toMutableList()
                                                if (fromIdx in current.indices && toIdx in current.indices) {
                                                    val moved = current.removeAt(fromIdx)
                                                    current.add(toIdx, moved)
                                                    val updatedEvents = reconstructEventsFromVisibleItems(
                                                        visibleItems = current,
                                                        allSourceEvents = sourceItems ?: emptyList(),
                                                    )
                                                    viewModel.updateRawEvents(updatedEvents)
                                                }
                                            },
                                            context = context,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // New Folder Dialog
            if (showNewFolderDialog) {
                var folderNameInput by remember { mutableStateOf("") }
                InlineModalDialog(
                    onDismissRequest = { showNewFolderDialog = false },
                    title = { Text(stringResource(R.string.folder_action_new)) },
                    text = {
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text(stringResource(R.string.folder_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = folderNameInput.trim()
                                if (trimmed.isNotEmpty() && trimmed !in customFolders) {
                                    customFolders = customFolders + trimmed
                                }
                                showNewFolderDialog = false
                            },
                            enabled = folderNameInput.trim().isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.generic_create))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewFolderDialog = false }) {
                            Text(stringResource(R.string.generic_cancel))
                        }
                    },
                )
            }

            // Rename Folder Dialog
            if (folderToRename != null) {
                val oldName = folderToRename!!
                var newNameInput by remember(oldName) { mutableStateOf(oldName) }
                InlineModalDialog(
                    onDismissRequest = { folderToRename = null },
                    title = { Text(stringResource(R.string.folder_action_rename)) },
                    text = {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            label = { Text(stringResource(R.string.folder_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = newNameInput.trim()
                                if (trimmed.isNotEmpty() && trimmed != oldName) {
                                    viewModel.renameFolder(oldName, trimmed)
                                    customFolders = customFolders.map { if (it == oldName) trimmed else it }
                                }
                                folderToRename = null
                            },
                            enabled = newNameInput.trim().isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.generic_modify))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { folderToRename = null }) {
                            Text(stringResource(R.string.generic_cancel))
                        }
                    },
                )
            }

            // Delete Folder Dialog
            if (folderToDelete != null) {
                val targetFolder = folderToDelete!!
                InlineModalDialog(
                    onDismissRequest = { folderToDelete = null },
                    title = { Text(stringResource(R.string.folder_delete_dialog_title)) },
                    text = {
                        Text(stringResource(R.string.folder_delete_dialog_message, targetFolder))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteFolder(targetFolder, deleteEvents = true)
                                customFolders = customFolders - targetFolder
                                folderToDelete = null
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.folder_delete_all_events),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { folderToDelete = null }) {
                                Text(stringResource(R.string.generic_cancel))
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.deleteFolder(targetFolder, deleteEvents = false)
                                    customFolders = customFolders - targetFolder
                                    folderToDelete = null
                                },
                            ) {
                                Text(stringResource(R.string.folder_delete_keep_events))
                            }
                        }
                    },
                )
            }
        }
    }

    private fun buildVisibleItems(
        events: List<UiImageEvent>,
        customFolders: List<String>,
        collapsedFolders: Set<String>,
    ): List<ScenarioListItem> {
        val result = mutableListOf<ScenarioListItem>()
        val processedFolders = mutableSetOf<String>()

        for (event in events) {
            val f = event.folder?.trim()?.ifEmpty { null }
            if (f == null) {
                result.add(ScenarioListItem.EventItem(event, null))
            } else {
                if (processedFolders.add(f)) {
                    val folderEvents = events.filter { it.folder?.trim() == f }
                    val isExpanded = f !in collapsedFolders
                    result.add(
                        ScenarioListItem.FolderHeader(
                            name = f,
                            totalCount = folderEvents.size,
                            enabledCount = folderEvents.count { it.event.enabledOnStart },
                            isExpanded = isExpanded,
                        )
                    )
                    if (isExpanded) {
                        for (fe in folderEvents) {
                            result.add(ScenarioListItem.EventItem(fe, f))
                        }
                        result.add(ScenarioListItem.FolderEndBoundary(f))
                    }
                }
            }
        }

        // Add any empty custom folders
        for (cf in customFolders) {
            if (processedFolders.add(cf)) {
                val isExpanded = cf !in collapsedFolders
                result.add(
                    ScenarioListItem.FolderHeader(
                        name = cf,
                        totalCount = 0,
                        enabledCount = 0,
                        isExpanded = isExpanded,
                    )
                )
                if (isExpanded) {
                    result.add(ScenarioListItem.FolderEndBoundary(cf))
                }
            }
        }

        return result
    }

    private fun getEffectiveFolderAt(items: List<ScenarioListItem>, targetIndex: Int): String? {
        var activeFolder: String? = null
        for (i in 0 until targetIndex.coerceAtMost(items.size)) {
            when (val item = items[i]) {
                is ScenarioListItem.FolderHeader -> {
                    activeFolder = if (item.isExpanded) item.name else null
                }
                is ScenarioListItem.FolderEndBoundary -> {
                    activeFolder = null
                }
                is ScenarioListItem.EventItem -> { /* stays same */ }
            }
        }
        return activeFolder
    }

    private fun reconstructEventsFromVisibleItems(
        visibleItems: List<ScenarioListItem>,
        allSourceEvents: List<UiImageEvent>,
    ): List<ScreenEvent> {
        val result = mutableListOf<ScreenEvent>()
        var currentFolder: String? = null
        val seenEventIds = mutableSetOf<Long>()
        val allEventsByFolder = allSourceEvents.groupBy { it.folder?.trim()?.ifEmpty { null } }

        for (item in visibleItems) {
            when (item) {
                is ScenarioListItem.FolderHeader -> {
                    if (!item.isExpanded) {
                        val folderEvents = allEventsByFolder[item.name].orEmpty()
                        for (uiEvent in folderEvents) {
                            val idKey = uiEvent.event.id.databaseId.let { if (it != 0L) it else -requireNotNull(uiEvent.event.id.tempId) }
                            if (seenEventIds.add(idKey)) {
                                result.add(uiEvent.event.copy(folder = item.name))
                            }
                        }
                        currentFolder = null
                    } else {
                        currentFolder = item.name
                    }
                }
                is ScenarioListItem.FolderEndBoundary -> {
                    currentFolder = null
                }
                is ScenarioListItem.EventItem -> {
                    val idKey = item.item.event.id.databaseId.let { if (it != 0L) it else -requireNotNull(item.item.event.id.tempId) }
                    if (seenEventIds.add(idKey)) {
                        result.add(item.item.event.copy(folder = currentFolder))
                    }
                }
            }
        }

        for (uiEvent in allSourceEvents) {
            val idKey = uiEvent.event.id.databaseId.let { if (it != 0L) it else -requireNotNull(uiEvent.event.id.tempId) }
            if (seenEventIds.add(idKey)) {
                result.add(uiEvent.event)
            }
        }

        return result
    }

    @Composable
    private fun androidx.compose.foundation.lazy.LazyItemScope.ImageEventListItem(
        item: UiImageEvent,
        index: Int,
        isLastIndex: Boolean,
        touchExplorationEnabled: Boolean,
        reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
        currentFolderContext: String?,
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
                    dragFolderFeedback = if (isBeingDragged) currentFolderContext else null,
                    reorderHandleModifier = reorderHandleModifier,
                    accessibilityActions = accessibilityActions,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }


    @Composable private fun EmptyState(title: Int, description: Int) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(context.getString(title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(context.getString(description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    @Composable
    private fun InlineModalDialog(
        onDismissRequest: () -> Unit,
        title: @Composable () -> Unit,
        text: @Composable () -> Unit,
        confirmButton: @Composable () -> Unit,
        dismissButton: (@Composable () -> Unit)? = null,
    ) {
        BackHandler(onBack = onDismissRequest)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent click-through */ },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            title()
                        }
                        Spacer(Modifier.height(16.dp))
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            text()
                        }
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            dismissButton?.invoke()
                            if (dismissButton != null) {
                                Spacer(Modifier.width(8.dp))
                            }
                            confirmButton()
                        }
                    }
                }
            }
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

/**
 * Lazy layouts persist item state in a Bundle, so keys must be Bundle-saveable.
 * Database ids are positive and temporary ids are positive but have a database id of zero;
 * making temporary ids negative yields a stable, allocation-free key for both cases.
 */
private fun io.github.vibhor1102.macrion.core.base.identifier.Identifier.toLazyListKey(): Long =
    if (databaseId != 0L) databaseId else -requireNotNull(tempId)
