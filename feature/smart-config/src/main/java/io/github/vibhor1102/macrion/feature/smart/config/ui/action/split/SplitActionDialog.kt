/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.split

import android.app.Dialog
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalConfiguration
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
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.ActionDelaysCard
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionPicker
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspace
import io.github.vibhor1102.macrion.core.ui.compose.MultiTouchWorkspaceItem
import io.github.vibhor1102.macrion.core.ui.compose.NumericField
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
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
            ExistingActionPicker(existing.toPickerOptions(context), onBack = { showingExistingPicker = false }) { other ->
                val source = viewModel.getEditedSplit() ?: return@ExistingActionPicker
                combinationOptions?.onExisting?.invoke(source, other)
            }
            return
        }
        val maxHeight = minOf(700.dp, (LocalConfiguration.current.screenHeightDp - 16).dp)

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MacrionDialogSurface {
                Column(Modifier.fillMaxWidth()) {
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
                                    MultiTouchWorkspaceItem(item.action.id.toString(), type, item.icon, item.isComplete)
                                },
                                canDeleteChild = ui.canDeleteSubAction,
                                addClickLabel = stringResource(R.string.split_action_add_click),
                                addSwipeLabel = stringResource(R.string.split_action_add_swipe),
                                onAddClick = viewModel::addClick,
                                onAddSwipe = viewModel::addSwipe,
                                onDeleteChild = viewModel::removeSubActionByKey,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            ) { index ->
                                SmartChildEditor(ui, ui.subActions[index])
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SmartChildEditor(ui: SplitActionUiState, item: SubActionItemUiState) {
        val nameLabel = stringResource(R.string.generic_name)
        val maxNameLength = context.resources.getInteger(R.integer.name_max_length)
        val key = item.action.id.toString()
        when (val action = item.action) {
            is Swipe -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val from = action.from
                val to = action.to
                MacrionTextField(action.name.orEmpty(),
                    { name -> viewModel.updateSubAction(key) { (it as Swipe).copy(name = name) } },
                    nameLabel, isError = action.name.isNullOrBlank(), maxLength = maxNameLength)
                NumericField(action.swipeDuration?.toString().orEmpty(),
                    stringResource(R.string.input_field_label_swipe_duration),
                    action.swipeDuration?.let { it <= 0 } ?: true,
                    { input -> if (input.isBlank() || input.toLongOrNull()?.let { it <= GESTURE_DURATION_MAX_VALUE } == true)
                        viewModel.updateSubAction(key) { (it as Swipe).copy(swipeDuration = input.toLongOrNull()) } })
                PositionCard(
                    title = stringResource(R.string.field_swipe_positions_title),
                    description = if (from != null && to != null)
                        stringResource(R.string.field_swipe_positions_desc, from.x, from.y, to.x, to.y)
                    else stringResource(R.string.generic_select_the_position),
                    isError = from == null || to == null,
                    onClick = { showSwipePositionSelector(item.index) },
                )
                ChildDelays(item.index, key, action.waitBeforeMs, action.waitAfterMs)
            }
            is Click -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacrionTextField(action.name.orEmpty(),
                    { name -> viewModel.updateSubAction(key) { (it as Click).copy(name = name) } },
                    nameLabel, isError = action.name.isNullOrBlank(), maxLength = maxNameLength)
                NumericField(action.pressDuration?.toString().orEmpty(),
                    stringResource(R.string.input_field_label_click_press_duration),
                    action.pressDuration?.let { it <= 0 } ?: true,
                    { input -> if (input.isBlank() || input.toLongOrNull()?.let { it <= GESTURE_DURATION_MAX_VALUE } == true)
                        viewModel.updateSubAction(key) { (it as Click).copy(pressDuration = input.toLongOrNull()) } })
                ClickTarget(ui, item.index, key, action)
                ChildDelays(item.index, key, action.waitBeforeMs, action.waitAfterMs)
            }
            else -> TextButton(onClick = {
                viewModel.getEditedSplit()?.let { onConfigureSubAction?.invoke(it, item.index) }
            }) { Text(item.name) }
        }
    }

    @Composable
    private fun ClickTarget(ui: SplitActionUiState, index: Int, key: String, click: Click) {
        val screenEvent = ui.event as? ScreenEvent
        if (screenEvent != null) {
            Text(stringResource(R.string.field_click_type_title), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Click.PositionType.USER_SELECTED to stringResource(R.string.split_action_fixed_position),
                    Click.PositionType.ON_DETECTED_CONDITION to stringResource(R.string.split_action_detected_condition),
                ).forEach { (type, label) ->
                    Row(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable {
                            viewModel.updateSubAction(key) { (it as Click).copy(positionType = type) }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = click.positionType == type, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (screenEvent == null || click.positionType == Click.PositionType.USER_SELECTED) {
            PositionCard(
                title = stringResource(R.string.field_click_position_title),
                description = click.position?.let {
                    stringResource(R.string.field_click_position_desc, it.x, it.y)
                } ?: stringResource(R.string.generic_select_the_position),
                isError = click.position == null,
                onClick = { showClickPositionSelector(index) },
            )
        } else {
            val isAnd = screenEvent.conditionOperator == AND
            val condition = ui.availableConditions.find { it.condition.id == click.clickOnConditionId }
            PositionCard(
                title = if (isAnd) stringResource(R.string.field_condition_selection_title_and_operator)
                    else stringResource(R.string.field_condition_selection_title_or_operator),
                description = if (isAnd) condition?.name
                    ?: stringResource(R.string.field_condition_selection_desc_and_operator_not_found)
                    else null,
                isError = isAnd && condition == null,
                enabled = isAnd && ui.availableConditions.isNotEmpty(),
                onClick = { showConditionSelector(ui, index) },
            )
            PositionCard(
                title = stringResource(R.string.field_click_offset_title),
                description = click.clickOffset?.let {
                    stringResource(R.string.field_click_offset_desc, it.x, it.y)
                } ?: stringResource(R.string.field_click_offset_desc_none),
                isError = false,
                onClick = { showClickOffsetEditor(index) },
            )
        }
    }

    @Composable
    private fun ChildDelays(index: Int, key: String, waitBefore: Long?, waitAfter: Long?) {
        var before by rememberSaveable(index) { mutableStateOf(waitBefore?.toString().orEmpty()) }
        var after by rememberSaveable(index) { mutableStateOf(waitAfter?.toString().orEmpty()) }
        ActionDelaysCard(
            waitBefore = before,
            waitAfter = after,
            onWaitBeforeChanged = { input ->
                before = input
                viewModel.updateSubAction(key) { child -> when (child) {
                    is Click -> child.copy(waitBeforeMs = input.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (input.isBlank()) null else -1L)
                    is Swipe -> child.copy(waitBeforeMs = input.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (input.isBlank()) null else -1L)
                    else -> child
                } }
            },
            onWaitAfterChanged = { input ->
                after = input
                viewModel.updateSubAction(key) { child -> when (child) {
                    is Click -> child.copy(waitAfterMs = input.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (input.isBlank()) null else -1L)
                    is Swipe -> child.copy(waitAfterMs = input.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (input.isBlank()) null else -1L)
                    else -> child
                } }
            },
        )
    }

    @Composable
    private fun PositionCard(
        title: String,
        description: String?,
        isError: Boolean,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            border = if (isError) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                description?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
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
