/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.split

import android.app.Dialog
import android.graphics.Point
import android.graphics.PointF
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.actions.GESTURE_DURATION_MAX_VALUE
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.PositionSelectorMenu
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.ActionDelaysCard
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionPicker
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchEditorFrame
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspace
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspaceItem
import io.github.vibhor1102.macrion.core.ui.compose.NumericField
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionHeaderMenu
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbCombinationOptions
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.toPickerOptions

class DumbSplitActionDialog(
    private val dumbSplitAction: DumbAction.DumbSplitAction,
    private val onConfirmClicked: (DumbAction.DumbSplitAction) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbSplitAction) -> Unit,
    private val onDismissClicked: () -> Unit,
    private val onUnsplitClicked: ((DumbAction.DumbSplitAction) -> Unit)? = null,
    private val onConfigureSubAction: ((parent: DumbAction.DumbSplitAction, subIndex: Int) -> Unit)? = null,
    private val combinationOptions: DumbCombinationOptions? = null,
    private val closeSourceEditorOnSave: Boolean = false,
) : OverlayDialog(R.style.AppTheme) {
    private var showDiscardConfirmation by mutableStateOf(false)
    private var showingExistingPicker by mutableStateOf(false)

    private val viewModel: DumbSplitActionViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbSplitActionViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbSplit(dumbSplitAction)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbSplitActionDialog.Content() } }
        }
    }

    override fun onDialogCreated(dialog: Dialog) {
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var showingGroupSettings by rememberSaveable { mutableStateOf(false) }
        val workspaceState = rememberSaveableStateHolder()
        val existing = combinationOptions?.existingActions.orEmpty().filter { details ->
            ui.subActions.size + ((details.action as? DumbAction.DumbSplitAction)?.subActions?.size ?: 1) <= 10
        }
        if (showingExistingPicker) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                ExistingActionPicker(existing.toPickerOptions(), onBack = { showingExistingPicker = false }) { other ->
                    val source = viewModel.getEditedDumbSplit() ?: return@ExistingActionPicker
                    combinationOptions?.onExisting?.invoke(source, other)
                }
            }
            return
        }
        if (showDiscardConfirmation) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirmation = false },
                title = { Text(stringResource(R.string.split_discard_title)) },
                text = { Text(stringResource(R.string.split_discard_message)) },
                confirmButton = { TextButton(onClick = { showDiscardConfirmation = false; dismissDraft() }) {
                    Text(stringResource(R.string.split_discard_confirm))
                } },
                dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.split_keep_editing))
                } },
            )
        }
        MultiTouchEditorFrame(expanded = !showingGroupSettings) {
            TopBar(
                title = if (showingGroupSettings) stringResource(R.string.split_action_group_settings)
                    else ui.name.ifBlank { stringResource(R.string.dialog_title_split_action) },
                saveEnabled = ui.canBeSaved,
                onDismiss = { if (showingGroupSettings) showingGroupSettings = false else back() },
                onDelete = ::onDeleteDialog,
                onSave = ::onSaveDialog,
                headerActions = {
                    if (!showingGroupSettings) DumbActionHeaderMenu(
                        showNewOptions = false,
                        canAdd = false,
                        canCombineExisting = existing.isNotEmpty(),
                        canUnsplit = ui.canUnsplit && ui.canBeSaved,
                        onGroupSettings = { showingGroupSettings = true },
                        onExisting = { showingExistingPicker = true },
                        onUnsplit = ::onUnsplitDialog,
                    )
                },
            )
            if (showingGroupSettings) GroupSettings(ui)
            else {
                val groupError = ui.nameError || ui.repeatCountError || ui.repeatDelayError ||
                    listOf(ui.waitBefore, ui.waitAfter).any { value ->
                        value.isNotBlank() && value.toLongOrNull()?.let { it in 0..59_999L } != true
                    }
                if (groupError) TextButton(onClick = { showingGroupSettings = true }) {
                    Icon(painterResource(UiR.drawable.ic_warning), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.split_action_check_group_settings))
                }
                workspaceState.SaveableStateProvider("touch-workspace") {
                    MultiTouchWorkspace(
                        items = ui.subActions.map { item ->
                            val type = when (item.action) {
                                is DumbAction.DumbClick -> stringResource(R.string.item_title_dumb_click)
                                is DumbAction.DumbSwipe -> stringResource(R.string.item_title_dumb_swipe)
                                else -> item.name
                            }
                            val name = when (val action = item.action) {
                                is DumbAction.DumbClick -> action.name
                                is DumbAction.DumbSwipe -> action.name
                                else -> item.name
                            }
                            MultiTouchWorkspaceItem(item.action.id.toString(), name, type,
                                item.icon, item.isComplete)
                        },
                        canDeleteChild = ui.canDeleteSubAction,
                        addClickLabel = stringResource(R.string.split_action_add_click),
                        addSwipeLabel = stringResource(R.string.split_action_add_swipe),
                        onAddClick = viewModel::addClick,
                        onAddSwipe = viewModel::addSwipe,
                        onDeleteChild = viewModel::removeSubActionByKey,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp),
                    ) { index, renaming ->
                        ChildEditor(ui.subActions[index], renaming)
                    }
                }
            }
        }
    }

    @Composable
    private fun GroupSettings(ui: DumbSplitActionUiState) {
        var count by rememberSaveable { mutableStateOf(ui.repeatCount) }
        var delay by rememberSaveable { mutableStateOf(ui.repeatDelay) }
        var before by rememberSaveable { mutableStateOf(ui.waitBefore) }
        var after by rememberSaveable { mutableStateOf(ui.waitAfter) }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MacrionTextField(ui.name, viewModel::setName,
                stringResource(R.string.input_field_label_name), isError = ui.nameError,
                maxLength = context.resources.getInteger(R.integer.name_max_length))
            Row(verticalAlignment = Alignment.Top) {
                NumericField(count, stringResource(R.string.input_field_label_repeat_count),
                    ui.repeatCountError, { count = it; viewModel.setRepeatCount(it.toIntOrNull() ?: 0) },
                    Modifier.weight(1f), enabled = !ui.isRepeatInfinite)
                Spacer(Modifier.width(12.dp))
                OutlinedIconToggleButton(ui.isRepeatInfinite, { viewModel.toggleInfiniteRepeat() },
                    Modifier.padding(top = 8.dp).size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_infinite),
                        stringResource(R.string.item_desc_dumb_repeat_infinite), Modifier.size(24.dp))
                }
            }
            NumericField(delay, stringResource(R.string.input_field_label_repeat_delay),
                ui.repeatDelayError, { delay = it; viewModel.setRepeatDelay(it.toLongOrNull() ?: 0L) })
            ActionDelaysCard(before, after,
                onWaitBeforeChanged = { input ->
                    before = input
                    viewModel.setWaitBeforeMs(input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                },
                onWaitAfterChanged = { input ->
                    after = input
                    viewModel.setWaitAfterMs(input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                })
        }
    }

    @Composable
    private fun ChildEditor(item: DumbSubActionItemUiState, renaming: Boolean) {
        val maxNameLength = context.resources.getInteger(R.integer.name_max_length)
        val key = item.action.id.toString()
        val nameFocus = remember(key) { FocusRequester() }
        var keepNameField by rememberSaveable(key) { mutableStateOf(when (val action = item.action) {
            is DumbAction.DumbClick -> action.name.isBlank()
            is DumbAction.DumbSwipe -> action.name.isBlank()
            else -> false
        }) }
        LaunchedEffect(renaming) { if (renaming) nameFocus.requestFocus() }
        when (val action = item.action) {
            is DumbAction.DumbClick -> {
                var duration by rememberSaveable(action.id.toString()) { mutableStateOf(action.pressDurationMs.toString()) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (renaming || keepNameField || action.name.isBlank()) {
                        MacrionTextField(action.name,
                            { name ->
                                if (name.isBlank()) keepNameField = true
                                viewModel.updateSubAction(key) { (it as DumbAction.DumbClick).copy(name = name) }
                            },
                            stringResource(R.string.input_field_label_name),
                            modifier = Modifier.focusRequester(nameFocus).onFocusChanged {
                                if (!it.isFocused && action.name.isNotBlank()) keepNameField = false
                            },
                            isError = action.name.isBlank(), maxLength = maxNameLength)
                    }
                    PositionCard(stringResource(UiR.string.field_click_position_title),
                        if (action.position.x >= 0 && action.position.y >= 0)
                            context.getString(R.string.split_action_click_coordinates, action.position.x, action.position.y)
                        else stringResource(R.string.split_action_position_not_set),
                        action.position.x < 0 || action.position.y < 0) { showClickPositionSelector(item.index) }
                    NumericField(duration, stringResource(R.string.input_field_label_click_press_duration),
                        action.pressDurationMs <= 0, { input ->
                            if (input.toLongOrNull() == null && input.isNotEmpty()) return@NumericField
                            if ((input.toLongOrNull() ?: 0L) > GESTURE_DURATION_MAX_VALUE) return@NumericField
                            duration = input
                            viewModel.updateSubAction(key) { (it as DumbAction.DumbClick).copy(pressDurationMs = input.toLongOrNull() ?: 0L) }
                        })
                    ChildDelays(item.index, key, action.waitBeforeMs, action.waitAfterMs)
                }
            }
            is DumbAction.DumbSwipe -> {
                var duration by rememberSaveable(action.id.toString()) { mutableStateOf(action.swipeDurationMs.toString()) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (renaming || keepNameField || action.name.isBlank()) {
                        MacrionTextField(action.name,
                            { name ->
                                if (name.isBlank()) keepNameField = true
                                viewModel.updateSubAction(key) { (it as DumbAction.DumbSwipe).copy(name = name) }
                            },
                            stringResource(R.string.input_field_label_name),
                            modifier = Modifier.focusRequester(nameFocus).onFocusChanged {
                                if (!it.isFocused && action.name.isNotBlank()) keepNameField = false
                            },
                            isError = action.name.isBlank(), maxLength = maxNameLength)
                    }
                    PositionCard(stringResource(UiR.string.field_swipe_positions_title),
                        if (action.fromPosition.x >= 0 && action.fromPosition.y >= 0 &&
                            action.toPosition.x >= 0 && action.toPosition.y >= 0)
                            context.getString(R.string.split_action_swipe_coordinates,
                                action.fromPosition.x, action.fromPosition.y, action.toPosition.x, action.toPosition.y)
                        else stringResource(R.string.split_action_position_not_set),
                        action.fromPosition.x < 0 || action.fromPosition.y < 0 ||
                            action.toPosition.x < 0 || action.toPosition.y < 0) { showSwipePositionSelector(item.index) }
                    NumericField(duration, stringResource(R.string.input_field_label_swipe_duration),
                        action.swipeDurationMs <= 0, { input ->
                            if (input.toLongOrNull() == null && input.isNotEmpty()) return@NumericField
                            if ((input.toLongOrNull() ?: 0L) > GESTURE_DURATION_MAX_VALUE) return@NumericField
                            duration = input
                            viewModel.updateSubAction(key) { (it as DumbAction.DumbSwipe).copy(swipeDurationMs = input.toLongOrNull() ?: 0L) }
                        })
                    ChildDelays(item.index, key, action.waitBeforeMs, action.waitAfterMs)
                }
            }
            else -> TextButton(onClick = {
                viewModel.getEditedDumbSplit()?.let { onConfigureSubAction?.invoke(it, item.index) }
            }) { Text(item.name) }
        }
    }

    @Composable
    private fun ChildDelays(index: Int, key: String, waitBefore: Long?, waitAfter: Long?) {
        var before by rememberSaveable(index) { mutableStateOf(waitBefore?.toString().orEmpty()) }
        var after by rememberSaveable(index) { mutableStateOf(waitAfter?.toString().orEmpty()) }
        ActionDelaysCard(before, after,
            onWaitBeforeChanged = { input ->
                before = input
                viewModel.updateSubAction(key) { child -> when (child) {
                    is DumbAction.DumbClick -> child.copy(waitBeforeMs = input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                    is DumbAction.DumbSwipe -> child.copy(waitBeforeMs = input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                    else -> child
                } }
            },
            onWaitAfterChanged = { input ->
                after = input
                viewModel.updateSubAction(key) { child -> when (child) {
                    is DumbAction.DumbClick -> child.copy(waitAfterMs = input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                    is DumbAction.DumbSwipe -> child.copy(waitAfterMs = input.takeIf(String::isNotBlank)?.toLongOrNull()
                        ?: if (input.isBlank()) null else -1L)
                    else -> child
                } }
            })
    }

    @Composable
    private fun PositionCard(title: String, description: String, isError: Boolean, onClick: () -> Unit) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
            border = if (isError) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun TopBar(title: String, saveEnabled: Boolean, onDismiss: () -> Unit,
        onDelete: () -> Unit, onSave: () -> Unit, headerActions: @Composable () -> Unit) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(UiR.drawable.ic_cancel), stringResource(UiR.string.action_editor_close_multi_touch))
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            headerActions()
            FilledTonalIconButton(onClick = onDelete) {
                Icon(painterResource(UiR.drawable.ic_delete), stringResource(UiR.string.action_editor_delete_multi_touch))
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSave, enabled = saveEnabled) {
                Icon(painterResource(UiR.drawable.ic_save_filled), stringResource(UiR.string.action_editor_save_multi_touch))
            }
        }
    }

    private fun showClickPositionSelector(index: Int) {
        val click = viewModel.getEditedDumbSplit()?.subActions?.getOrNull(index) as? DumbAction.DumbClick ?: return
        overlayManager.navigateTo(context, PositionSelectorMenu(
            itemBriefDescription = ClickDescription(click.pressDurationMs, click.position.toEditionPosition()),
            onConfirm = { description -> (description as? ClickDescription)?.position?.let { position ->
                viewModel.updateSubAction(index) { (it as DumbAction.DumbClick).copy(position = position.toPoint()) }
            } },
        ), hideCurrent = true)
    }

    private fun showSwipePositionSelector(index: Int) {
        val swipe = viewModel.getEditedDumbSplit()?.subActions?.getOrNull(index) as? DumbAction.DumbSwipe ?: return
        overlayManager.navigateTo(context, PositionSelectorMenu(
            itemBriefDescription = SwipeDescription(swipe.swipeDurationMs,
                swipe.fromPosition.toEditionPosition(), swipe.toPosition.toEditionPosition()),
            onConfirm = { description -> (description as? SwipeDescription)?.let { selected ->
                val from = selected.from?.toPoint() ?: return@let
                val to = selected.to?.toPoint() ?: return@let
                viewModel.updateSubAction(index) { (it as DumbAction.DumbSwipe).copy(fromPosition = from, toPosition = to) }
            } },
        ), hideCurrent = true)
    }

    private fun Point.toEditionPosition(): PointF? = if (x < 0 || y < 0) null else toPointF()

    override fun back() {
        if (showingExistingPicker) { showingExistingPicker = false; return }
        if (viewModel.hasUnsavedModifications()) showDiscardConfirmation = true
        else dismissDraft()
    }

    private fun dismissDraft() {
        onDismissClicked()
        super.back()
    }

    private fun onSaveDialog() {
        viewModel.getEditedDumbSplit()?.let {
            viewModel.saveLastChildDurations()
            onConfirmClicked(it)
            super.back()
            if (closeSourceEditorOnSave) overlayManager.navigateUp(context)
        }
    }

    private fun onDeleteDialog() {
        viewModel.getEditedDumbSplit()?.let {
            onDeleteClicked(it)
            super.back()
        }
    }

    private fun onUnsplitDialog() {
        viewModel.getEditedDumbSplit()?.let {
            onUnsplitClicked?.invoke(it)
            super.back()
        }
    }
}
