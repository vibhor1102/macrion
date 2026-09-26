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
import android.content.res.Configuration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
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

    override fun onStart() {
        dialogController.floatingActionButtons.setSecondaryVisible(false)
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
            val draft by viewModel.listState.collectAsStateWithLifecycle(null)
            val sourceItems = draft?.events
            val folders = draft?.folders.orEmpty()
            var dragActive by remember { mutableStateOf(false) }
            var pendingEvents by remember { mutableStateOf<List<ScreenEvent>?>(null) }
            var pendingFolders by remember {
                mutableStateOf<List<io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder>?>(null)
            }
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

            val visibleItems = remember(sourceItems, folders, collapsedFolders) {
                ScenarioFolderReorderHelper.buildVisibleItems(sourceItems ?: emptyList(), folders, collapsedFolders)
            }

            val itemsToDisplay = reorderedVisibleItems ?: visibleItems
            val lazyListState = rememberLazyListState()

            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val current = reorderedVisibleItems ?: visibleItems
                reorderedVisibleItems = ScenarioFolderReorderHelper.moveItem(
                    current, current.indexOfFirst { it.key == from.key }, current.indexOfFirst { it.key == to.key },
                )
            }

            LaunchedEffect(sourceItems, folders, collapsedFolders, dragActive, pendingEvents, pendingFolders) {
                val expectedEvents = pendingEvents
                if (!dragActive && (expectedEvents == null ||
                    (sourceItems?.map { it.event } == expectedEvents && folders == pendingFolders))) {
                    reorderedVisibleItems = null
                    pendingEvents = null
                    pendingFolders = null
                }
            }

            val onDragStarted: (androidx.compose.ui.geometry.Offset) -> Unit = {
                dragActive = true
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }

            fun commitLayout(items: List<ScenarioListItem>) {
                val source = sourceItems.orEmpty()
                reorderedVisibleItems = items
                pendingEvents = ScenarioFolderReorderHelper.reconstructEvents(items, source)
                    .mapIndexed { index, event -> event.copy(priority = index) }
                pendingFolders = ScenarioFolderReorderHelper.reconstructFolders(items, source)
                viewModel.updateLayout(items, source)
            }

            val onDragStopped: () -> Unit = {
                reorderedVisibleItems?.let { commitLayout(it) }
                dragActive = false
            }

            val allFolderNames = remember(sourceItems, folders) {
                ((sourceItems ?: emptyList()).mapNotNull { it.folder } + folders.map { it.name }).distinct()
            }
            val allFoldersCollapsed = remember(allFolderNames, collapsedFolders) {
                allFolderNames.isNotEmpty() && allFolderNames.none { it !in collapsedFolders }
            }

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            val onDisabledFolderDragClick: () -> Unit = remember(allFolderNames) {
                {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.folder_reorder_disabled_hint),
                            actionLabel = context.getString(R.string.folder_collapse_all),
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            collapsedFolders = allFolderNames.toSet()
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                dialogController.topBarBinding.extraAction = {
                    IconButton(onClick = { showNewFolderDialog = true }) {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_create_new_folder),
                            contentDescription = stringResource(R.string.folder_action_new),
                        )
                    }
                }
                onDispose {
                    dialogController.topBarBinding.extraAction = null
                }
            }

            Box(Modifier.fillMaxSize()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    when {
                        sourceItems == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        sourceItems.isEmpty() && folders.isEmpty() -> EmptyState(R.string.message_empty_screen_event_title, R.string.message_empty_screen_event_desc)
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                        ) {
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
                                        val canDragFolder = !listItem.isExpanded && allFoldersCollapsed
                                        ReorderableItem(
                                            state = reorderableState,
                                            key = listItem.key,
                                            enabled = canDragFolder,
                                        ) { isBeingDragged ->
                                            val folderInteractionSource = remember { MutableInteractionSource() }
                                            val reorderHandleModifier = if (canDragFolder) {
                                                Modifier
                                                    .draggableHandle(
                                                        onDragStarted = onDragStarted,
                                                        onDragStopped = onDragStopped,
                                                        interactionSource = folderInteractionSource,
                                                    )
                                                    .clearAndSetSemantics { }
                                            } else Modifier

                                            Column {
                                                FolderHeaderRow(
                                                    name = listItem.name,
                                                    isExpanded = listItem.isExpanded,
                                                    showReorderHandle = canDragFolder,
                                                    onDisabledHandleClick = if (!canDragFolder) onDisabledFolderDragClick else null,
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
                                                    handleInteractionSource = folderInteractionSource,
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
                                            currentFolderContext = ScenarioFolderReorderHelper.getEffectiveFolderAt(itemsToDisplay, index),
                                            isInFolder = listItem.folderName != null,
                                            onEventClick = remember(listItem.item.event) { { onEventItemClicked(listItem.item.event) } },
                                            onDragStarted = onDragStarted,
                                            onDragStopped = onDragStopped,
                                            onMoveEvent = { fromIdx, toIdx ->
                                                val current = itemsToDisplay
                                                val updated = ScenarioFolderReorderHelper.moveItem(current, fromIdx, toIdx)
                                                commitLayout(updated)
                                            },
                                            context = context,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 88.dp, bottom = 12.dp),
                )
            }

            // New Folder Dialog
            if (showNewFolderDialog) {
                var folderNameInput by remember { mutableStateOf("") }
                val trimmed = folderNameInput.trim()
                val canCreate = trimmed.isNotEmpty() && trimmed !in allFolderNames
                InlineModalDialog(
                    onDismissRequest = { showNewFolderDialog = false },
                    title = { Text(stringResource(R.string.folder_action_new)) },
                    text = {
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text(stringResource(R.string.folder_name_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (canCreate) {
                                        viewModel.createFolder(trimmed)
                                        showNewFolderDialog = false
                                    }
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.createFolder(trimmed)
                                showNewFolderDialog = false
                            },
                            enabled = canCreate,
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
                val trimmed = newNameInput.trim()
                val canModify = trimmed.isNotEmpty() && (trimmed == oldName || trimmed !in allFolderNames)
                InlineModalDialog(
                    onDismissRequest = { folderToRename = null },
                    title = { Text(stringResource(R.string.folder_action_rename)) },
                    text = {
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { newNameInput = it },
                            label = { Text(stringResource(R.string.folder_name_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (canModify) {
                                        if (trimmed != oldName) {
                                            viewModel.renameFolder(oldName, trimmed)
                                            if (oldName in collapsedFolders) {
                                                collapsedFolders = collapsedFolders - oldName + trimmed
                                            }
                                        }
                                        folderToRename = null
                                    }
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (trimmed != oldName) {
                                    viewModel.renameFolder(oldName, trimmed)
                                    if (oldName in collapsedFolders) {
                                        collapsedFolders = collapsedFolders - oldName + trimmed
                                    }
                                }
                                folderToRename = null
                            },
                            enabled = canModify,
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
                                collapsedFolders = collapsedFolders - targetFolder
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
                                    collapsedFolders = collapsedFolders - targetFolder
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

    @Composable
    private fun androidx.compose.foundation.lazy.LazyItemScope.ImageEventListItem(
        item: UiImageEvent,
        index: Int,
        isLastIndex: Boolean,
        touchExplorationEnabled: Boolean,
        reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
        currentFolderContext: String?,
        isInFolder: Boolean = false,
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
        ) { isBeingDragged ->
            val handleInteractionSource = remember { MutableInteractionSource() }
            val reorderHandleModifier = Modifier
                .draggableHandle(
                    onDragStarted = onDragStarted,
                    onDragStopped = onDragStopped,
                    interactionSource = handleInteractionSource,
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
                    handleInteractionSource = handleInteractionSource,
                    accessibilityActions = accessibilityActions,
                    isInFolder = isInFolder,
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
        DisposableEffect(Unit) {
            onDispose {
                dialogController.activeModal = null
            }
        }

        SideEffect {
            dialogController.activeModal = {
                InlineModalDialogContent(
                    onDismissRequest = onDismissRequest,
                    title = title,
                    text = text,
                    confirmButton = confirmButton,
                    dismissButton = dismissButton,
                )
            }
        }
    }

    @Composable
    private fun InlineModalDialogContent(
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
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val verticalPadding = if (isLandscape) 16.dp else 24.dp
            val titleSpacer = if (isLandscape) 12.dp else 16.dp
            val buttonSpacer = if (isLandscape) 16.dp else 24.dp

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* prevent click-through */ },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = verticalPadding),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    ) {
                        ProvideTextStyle(
                            if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        ) {
                            title()
                        }
                        Spacer(Modifier.height(titleSpacer))
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            text()
                        }
                        Spacer(Modifier.height(buttonSpacer))
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
