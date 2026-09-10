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
package io.github.vibhor1102.macrion.feature.dumb.config.ui.scenario.actionlist

import android.content.Context
import android.view.ViewGroup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionCreator
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionListItem
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionUiFlowListener
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.onCopyDumbActionSelected
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.startDumbActionCreationUiFlow
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.startDumbActionEditionUiFlow

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.launch

class DumbActionListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val viewModel: DumbActionListViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbActionListViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var dumbActionCreator: DumbActionCreator

    override fun floatingActionButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbActionListContent.Content() } }
        }
    }

    override fun onViewCreated() {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = { position -> viewModel.createNewDumbClick(context, position) },
            createNewDumbSwipe = { from, to -> viewModel.createNewDumbSwipe(context, from, to) },
            createNewDumbPause = { viewModel.createNewDumbPause(context) },
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::addNewDumbAction,
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyAction.collect(::updateCopyButtonState) }
            }
        }
    }

    override fun onPrimaryFloatingActionButtonClicked() {
        debounceUserInteraction {
            dialogController.overlayManager.startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    override fun onSecondaryFloatingActionButtonClicked() {
        debounceUserInteraction {
            dialogController.overlayManager.onCopyDumbActionSelected(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    private fun onDumbActionClicked(dumbActionDetails: DumbActionDetails) {
        debounceUserInteraction {
            dialogController.overlayManager.startDumbActionEditionUiFlow(
                context = context,
                dumbAction = dumbActionDetails.action,
                listener = updateActionUiFlowListener,
            )
        }
    }

    private fun updateCopyButtonState(canCopy: Boolean) {
        dialogController.floatingActionButtons.setSecondaryVisible(canCopy)
    }

    @Composable private fun Content() {
        val sourceItems = viewModel.dumbActionsDetails.collectAsStateWithLifecycle(emptyList()).value
        var displayedItems by remember { mutableStateOf(emptyList<DumbActionDetails>()) }
        var isReordering by remember { mutableStateOf(false) }
        LaunchedEffect(sourceItems) { if (!isReordering) displayedItems = sourceItems }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            displayedItems = displayedItems.toMutableList().apply { add(to.index, removeAt(from.index)) }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            if (displayedItems.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(context.getString(R.string.message_empty_dumb_action_list), style = MaterialTheme.typography.headlineSmall)
                    Text(context.getString(R.string.message_empty_secondary_dumb_action_list), style = MaterialTheme.typography.bodyMedium)
                }
            } else LazyColumn(Modifier.fillMaxSize(), state = listState) {
                items(displayedItems, key = { it.action.id.databaseId.takeIf { id -> id != 0L } ?: -requireNotNull(it.action.id.tempId) }) { item ->
                    val key = item.action.id.databaseId.takeIf { it != 0L } ?: -requireNotNull(item.action.id.tempId)
                    ReorderableItem(reorderState, key) { dragging ->
                        DumbActionListItem(
                            details = item,
                            showHandle = true,
                            reorderHandleModifier = Modifier.longPressDraggableHandle(
                                onDragStarted = { isReordering = true },
                                onDragStopped = { viewModel.updateDumbActionOrder(displayedItems); isReordering = false },
                            ).clearAndSetSemantics { },
                            isBeingDragged = dragging,
                            onClick = { onDumbActionClicked(item) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
