/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.split

import android.app.Dialog
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.common.actions.GESTURE_DURATION_MAX_VALUE
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.PositionSelectorMenu
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionPicker
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchEditorFrame
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspace
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspaceItem
import io.github.vibhor1102.macrion.core.ui.compose.PositionGestureFields
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.ClickFields
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartActionHeaderMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartCombinationOptions
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.toPickerOptions
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.offset.ClickOffsetDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import kotlinx.coroutines.launch

class SplitActionDialog(
    private val listener: OnActionConfigCompleteListener,
    private val onConfigureSubAction: ((SplitAction, Int) -> Unit)? = null,
    private val combinationOptions: SmartCombinationOptions? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    private var showingExistingPicker by mutableStateOf(false)

    private val viewModel: SplitActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { splitActionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@SplitActionDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isEditingAction.collect { isEditing ->
                    if (!isEditing) {
                        Log.i("SplitActionDialog", "Action edition stopped, closing dialog")
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var showingName by rememberSaveable { mutableStateOf(false) }
        val workspaceState = rememberSaveableStateHolder()
        val existing = combinationOptions?.existingActions.orEmpty().filter {
            ui.subActions.size + ((it as? SplitAction)?.subActions?.size ?: 1) <= 10
        }
        if (showingExistingPicker) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                ExistingActionPicker(existing.toPickerOptions(context), onBack = { showingExistingPicker = false }) { other ->
                    val source = viewModel.getEditedSplit() ?: return@ExistingActionPicker
                    combinationOptions?.onExisting?.invoke(source, other)
                }
            }
            return
        }
        MultiTouchEditorFrame(expanded = !showingName) {
            TopBar(
                title = if (showingName) stringResource(R.string.split_action_rename)
                    else ui.name.ifBlank { stringResource(R.string.dialog_title_split_action) },
                saveEnabled = ui.canBeSaved,
                onDismiss = { if (showingName) showingName = false else back() },
                onDelete = ::onDeleteClicked,
                onSave = ::onSaveClicked,
                headerActions = {
                    if (!showingName) SmartActionHeaderMenu(
                        showNewOptions = false,
                        canAdd = false,
                        canCombineExisting = existing.isNotEmpty(),
                        canUnsplit = ui.canUnsplit && ui.canBeSaved,
                        onRename = { showingName = true },
                        onExisting = { showingExistingPicker = true },
                        onUnsplit = ::onUnsplitClicked,
                    )
                },
            )
            if (showingName) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    MacrionTextField(
                        value = ui.name,
                        onValueChange = viewModel::setName,
                        label = stringResource(R.string.generic_name),
                        isError = ui.nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                }
            } else {
                if (ui.nameError) TextButton(onClick = { showingName = true }) {
                    Icon(painterResource(UiR.drawable.ic_warning), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.split_action_name_required))
                }
                workspaceState.SaveableStateProvider("touch-workspace") {
                    MultiTouchWorkspace(
                        items = ui.subActions.map { item ->
                            val type = when (item.action) {
                                is Click -> stringResource(R.string.item_click_title)
                                is Swipe -> stringResource(R.string.item_swipe_title)
                                else -> item.name
                            }
                            val name = when (val action = item.action) {
                                is Click -> action.name.orEmpty()
                                is Swipe -> action.name.orEmpty()
                                else -> item.name
                            }
                            MultiTouchWorkspaceItem(item.action.id.toString(), name, type,
                                item.isComplete)
                        },
                        canDeleteChild = ui.canDeleteSubAction,
                        addClickLabel = stringResource(R.string.split_action_add_click),
                        addSwipeLabel = stringResource(R.string.split_action_add_swipe),
                        onAddClick = viewModel::addClick,
                        onAddSwipe = viewModel::addSwipe,
                        onDeleteChild = viewModel::removeSubActionByKey,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp),
                    ) { index ->
                        SmartChildEditor(ui, ui.subActions[index])
                    }
                }
            }
        }
    }

    @Composable
    private fun SmartChildEditor(ui: SplitActionUiState, item: SubActionItemUiState) {
        val key = item.action.id.toString()
        val maxNameLength = context.resources.getInteger(R.integer.name_max_length)
        val action = item.action
        val initialBefore = when (action) {
            is Click -> action.waitBeforeMs
            is Swipe -> action.waitBeforeMs
            else -> null
        }
        val initialAfter = when (action) {
            is Click -> action.waitAfterMs
            is Swipe -> action.waitAfterMs
            else -> null
        }
        var before by rememberSaveable(key) { mutableStateOf(initialBefore?.toString().orEmpty()) }
        var after by rememberSaveable(key) { mutableStateOf(initialAfter?.toString().orEmpty()) }
        val onBefore: (String) -> Unit = { input ->
            before = input
            val value = input.toLongOrNull() ?: if (input.isBlank()) null else -1L
            viewModel.updateSubAction(key) { child -> when (child) {
                is Click -> child.copy(waitBeforeMs = value)
                is Swipe -> child.copy(waitBeforeMs = value)
                else -> child
            } }
        }
        val onAfter: (String) -> Unit = { input ->
            after = input
            val value = input.toLongOrNull() ?: if (input.isBlank()) null else -1L
            viewModel.updateSubAction(key) { child -> when (child) {
                is Click -> child.copy(waitAfterMs = value)
                is Swipe -> child.copy(waitAfterMs = value)
                else -> child
            } }
        }
        when (action) {
            is Swipe -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PositionGestureFields(
                    name = action.name.orEmpty(), duration = action.swipeDuration?.toString().orEmpty(),
                    positionTitle = stringResource(R.string.field_swipe_positions_title),
                    positionDescription = if (action.from != null && action.to != null)
                        stringResource(R.string.field_swipe_positions_desc, action.from!!.x, action.from!!.y,
                            action.to!!.x, action.to!!.y)
                    else stringResource(R.string.generic_select_the_position),
                    nameLabel = stringResource(R.string.generic_name),
                    durationLabel = stringResource(R.string.input_field_label_swipe_duration),
                    nameError = action.name.isNullOrBlank(),
                    durationError = (action.swipeDuration ?: 0L) <= 0L,
                    positionError = action.from == null || action.to == null,
                    maxNameLength = maxNameLength, waitBefore = before, waitAfter = after,
                    onNameChanged = { name -> viewModel.updateSubAction(key) { (it as Swipe).copy(name = name) } },
                    onDurationChanged = { input ->
                        if (input.isBlank() || input.toLongOrNull()?.let { it <= GESTURE_DURATION_MAX_VALUE } == true)
                            viewModel.updateSubAction(key) { (it as Swipe).copy(swipeDuration = input.toLongOrNull()) }
                    },
                    onPositionClicked = { showSwipePositionSelector(item.index) },
                    onWaitBeforeChanged = onBefore, onWaitAfterChanged = onAfter,
                )
            }
            is Click -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClickFields(
                    name = action.name.orEmpty(), duration = action.pressDuration?.toString().orEmpty(),
                    nameError = action.name.isNullOrBlank(),
                    durationError = (action.pressDuration ?: 0L) <= 0L,
                    positionState = item.clickPositionState, maxNameLength = maxNameLength,
                    waitBefore = before, waitAfter = after,
                    onNameChanged = { name -> viewModel.updateSubAction(key) { (it as Click).copy(name = name) } },
                    onDurationChanged = { input ->
                        if (input.isBlank() || input.toLongOrNull()?.let { it <= GESTURE_DURATION_MAX_VALUE } == true)
                            viewModel.updateSubAction(key) { (it as Click).copy(pressDuration = input.toLongOrNull()) }
                    },
                    onTypeSelected = { type -> viewModel.updateSubAction(key) { (it as Click).copy(positionType = type) } },
                    onPositionSelected = { showClickPositionSelector(item.index) },
                    onConditionSelected = { showConditionSelector(ui, item.index) },
                    onOffsetSelected = { showClickOffsetEditor(item.index) },
                    onWaitBeforeChanged = onBefore, onWaitAfterChanged = onAfter,
                )
            }
            else -> TextButton(onClick = {
                viewModel.getEditedSplit()?.let { onConfigureSubAction?.invoke(it, item.index) }
            }) { Text(item.name) }
        }
    }

    @Composable
    private fun TopBar(
        title: String,
        saveEnabled: Boolean,
        onDismiss: () -> Unit,
        onDelete: () -> Unit,
        onSave: () -> Unit,
        headerActions: @Composable () -> Unit,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(UiR.drawable.ic_cancel), stringResource(UiR.string.action_editor_close_multi_touch))
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        val click = viewModel.getEditedSplit()?.subActions?.getOrNull(index) as? Click ?: return
        overlayManager.navigateTo(context, PositionSelectorMenu(
            tutorialMonitoringTag = MonitoredOverlayType.CLICK_POSITION.name,
            itemBriefDescription = ClickDescription(click.pressDuration ?: 1L, click.position?.toPointF()),
            onConfirm = { description -> (description as? ClickDescription)?.position?.let { point ->
                viewModel.updateSubAction(index) { (it as Click).copy(position = point.toPoint()) }
            } },
        ), hideCurrent = true)
    }

    private fun showSwipePositionSelector(index: Int) {
        val swipe = viewModel.getEditedSplit()?.subActions?.getOrNull(index) as? Swipe ?: return
        overlayManager.navigateTo(context, PositionSelectorMenu(
            tutorialMonitoringTag = MonitoredOverlayType.SWIPE_POSITION.name,
            itemBriefDescription = SwipeDescription(swipe.swipeDuration ?: 1L, swipe.from?.toPointF(), swipe.to?.toPointF()),
            onConfirm = { description -> (description as? SwipeDescription)?.let { selected ->
                val from = selected.from?.toPoint() ?: return@let
                val to = selected.to?.toPoint() ?: return@let
                viewModel.updateSubAction(index) { (it as Swipe).copy(from = from, to = to) }
            } },
        ), hideCurrent = true)
    }

    private fun showConditionSelector(ui: SplitActionUiState, index: Int) {
        overlayManager.navigateTo(context, ScreenConditionSelectionDialog(ui.availableConditions) { condition ->
            viewModel.updateSubAction(index) { (it as Click).copy(clickOnConditionId = condition.id) }
        }, false)
    }

    private fun showClickOffsetEditor(index: Int) {
        val click = viewModel.getEditedSplit()?.subActions?.getOrNull(index) as? Click ?: return
        overlayManager.navigateTo(context, ClickOffsetDialog(click) { offset ->
            viewModel.updateSubAction(index) { (it as Click).copy(clickOffset = offset) }
        }, false)
    }

    override fun back() {
        if (showingExistingPicker) { showingExistingPicker = false; return }
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }
        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveClicked() {
        viewModel.saveLastChildDurations()
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun onUnsplitClicked() {
        viewModel.unsplit()
        listener.onDismissClicked()
        super.back()
    }
}
