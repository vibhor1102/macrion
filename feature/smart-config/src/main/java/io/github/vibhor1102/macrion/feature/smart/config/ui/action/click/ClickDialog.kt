/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click

import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.actions.GESTURE_DURATION_MAX_VALUE
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.PositionSelectorMenu
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartActionHeaderMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.toPickerOptions
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.offset.ClickOffsetDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selection.ScreenConditionSelectionDialog
import kotlinx.coroutines.launch

class ClickDialog(
    private val listener: OnActionConfigCompleteListener,
    private val canDelete: Boolean = true,
    private val combinationOptions: io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief.SmartCombinationOptions? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    private var showingExistingPicker by mutableStateOf(false)
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CLICK.name
    private val viewModel: ClickViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ClickDialog.Content() } }
    }
    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.isEditingAction.collect { if (!it) { Log.e(TAG, "Closing ClickDialog because there is no action edited"); finish() } }
        } }
    }

    @Composable private fun Content() {
        val ui by viewModel.uiState.collectAsStateWithLifecycle()
        val state = ui ?: return
        if (showingExistingPicker) {
            io.github.vibhor1102.macrion.core.ui.compose.ExistingActionPicker(
                options = combinationOptions?.existingActions.orEmpty().toPickerOptions(context),
                onBack = { showingExistingPicker = false },
                onSelected = { other ->
                    val source = viewModel.getEditedClick() ?: return@ExistingActionPicker
                    showingExistingPicker = false
                    combinationOptions?.onExisting?.invoke(source, other)
                },
            )
            return
        }
            var name by rememberSaveable { mutableStateOf(state.name.orEmpty()) }
            var duration by rememberSaveable { mutableStateOf(state.pressDuration.orEmpty()) }
            LaunchedEffect(state.name) { if (state.name != name) name = state.name.orEmpty() }
            LaunchedEffect(state.pressDuration) { if (state.pressDuration != duration) duration = state.pressDuration.orEmpty() }
            Surface(
            shape = OverlayDialogShape,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column {
                    TopBar(state.canBeSaved)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClickFields(
                            name = name, duration = duration, nameError = state.nameError,
                            durationError = state.pressDurationError, positionState = state.positionState,
                            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
                            waitBefore = state.waitBeforeMs.orEmpty(), waitAfter = state.waitAfterMs.orEmpty(),
                            onNameChanged = { name = it; viewModel.setName(it) },
                            onDurationChanged = { input ->
                                if (input.isEmpty() || (input.toLongOrNull() ?: Long.MAX_VALUE) <= GESTURE_DURATION_MAX_VALUE) {
                                    duration = input; viewModel.setPressDuration(input.toLongOrNull())
                                }
                            },
                            onTypeSelected = viewModel::setClickOnCondition,
                            onPositionSelected = ::showPositionSelector,
                            onConditionSelected = ::showConditionSelector,
                            onOffsetSelected = ::showClickOffsetDialog,
                            onWaitBeforeChanged = { viewModel.setWaitBeforeMs(it.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (it.isBlank()) null else -1L) },
                            onWaitAfterChanged = { viewModel.setWaitAfterMs(it.takeIf(String::isNotBlank)?.toLongOrNull() ?: if (it.isBlank()) null else -1L) },
                            typeModifier = { type -> if (type == Click.PositionType.ON_DETECTED_CONDITION)
                                Modifier.tutorialAnchor(MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION,
                                    onClick = { viewModel.setClickOnCondition(type) }) else Modifier },
                            selectorModifier = Modifier.tutorialAnchor(
                                MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                                onClick = if (state.positionState?.positionType == Click.PositionType.USER_SELECTED)
                                    ::showPositionSelector else ::showConditionSelector,
                                enabled = state.positionState?.isSelectorEnabled == true),
                        )
                    }
                }
            }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_click), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            combinationOptions?.let { options ->
                SmartActionHeaderMenu(
                    showNewOptions = true,
                    canAdd = true,
                    canCombineExisting = options.existingActions.isNotEmpty(),
                    onNewClick = { viewModel.getEditedClick()?.let(options.onNewClick) },
                    onNewSwipe = { viewModel.getEditedClick()?.let(options.onNewSwipe) },
                    onExisting = { showingExistingPicker = true },
                )
            }
            FilledTonalIconButton(onClick = ::delete, enabled = canDelete) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = ::save,
                enabled = saveEnabled,
                modifier = Modifier.tutorialAnchor(
                    MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    onClick = ::save,
                    enabled = saveEnabled,
                ),
            ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    override fun back() {
        if (showingExistingPicker) { showingExistingPicker = false; return }
        if (viewModel.hasUnsavedModifications()) { context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }; return }
        listener.onDismissClicked(); super.back()
    }
    private fun save() { viewModel.saveLastConfig(); listener.onConfirmClicked(); super.back() }
    private fun delete() { listener.onDeleteClicked(); super.back() }
    private fun showPositionSelector() { viewModel.getEditedClick()?.let { click -> overlayManager.navigateTo(context,
        PositionSelectorMenu(MonitoredOverlayType.CLICK_POSITION.name,
            ClickDescription(pressDurationMs = click.pressDuration ?: 1L, position = click.position?.toPointF()), { description ->
                (description as ClickDescription).position?.let { viewModel.setPosition(it.toPoint()) }
            }), true) } }
    private fun showConditionSelector() = overlayManager.navigateTo(context, ScreenConditionSelectionDialog(
        viewModel.uiState.value?.availableConditions ?: emptyList(), viewModel::setConditionToBeClicked), false)
    private fun showClickOffsetDialog() = overlayManager.navigateTo(context, ClickOffsetDialog(), false)
}

private const val TAG = "ClickDialog"
