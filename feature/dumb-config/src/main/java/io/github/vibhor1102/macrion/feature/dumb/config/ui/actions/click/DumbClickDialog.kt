/* Copyright (C) 2023 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.click

import android.graphics.Point
import android.graphics.PointF
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.PositionSelectorMenu
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.compose.MacrionGestureEditor
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

class DumbClickDialog(
    private val dumbClick: DumbAction.DumbClick,
    private val onConfirmClicked: (DumbAction.DumbClick) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbClick) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    private val viewModel: DumbClickViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbClickViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbClick(dumbClick)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbClickDialog.Content() } }
        }
    }

@Composable private fun Content() {
        val initialName by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val initialDuration by viewModel.pressDuration.collectAsStateWithLifecycle(initialValue = null)
        val initialCount by viewModel.repeatCount.collectAsStateWithLifecycle(initialValue = null)
        val initialDelay by viewModel.repeatDelay.collectAsStateWithLifecycle(initialValue = null)
        var name by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("") }
        var count by remember { mutableStateOf("") }
        var delay by remember { mutableStateOf("") }
        LaunchedEffect(initialName) { initialName?.let { name = it } }
        LaunchedEffect(initialDuration) { initialDuration?.let { duration = it } }
        LaunchedEffect(initialCount) { initialCount?.let { count = it } }
        LaunchedEffect(initialDelay) { initialDelay?.let { delay = it } }
        MacrionGestureEditor(
            title = context.getString(R.string.item_title_dumb_click), name = name, duration = duration,
            repeatCount = count, repeatDelay = delay,
            positionTitle = context.getString(R.string.field_click_position_title),
            positionDescription = viewModel.clickPositionText.collectAsStateWithLifecycle("").value,
            nameLabel = context.getString(R.string.input_field_label_name),
            durationLabel = context.getString(R.string.input_field_label_click_press_duration),
            repeatCountLabel = context.getString(R.string.input_field_label_repeat_count),
            repeatDelayLabel = context.getString(R.string.input_field_label_repeat_delay),
            nameError = viewModel.nameError.collectAsStateWithLifecycle(false).value,
            durationError = viewModel.pressDurationError.collectAsStateWithLifecycle(false).value,
            repeatCountError = viewModel.repeatCountError.collectAsStateWithLifecycle(false).value,
            repeatDelayError = viewModel.repeatDelayError.collectAsStateWithLifecycle(false).value,
            infiniteRepeat = viewModel.repeatInfiniteState.collectAsStateWithLifecycle(false).value,
            saveEnabled = viewModel.isValidDumbClick.collectAsStateWithLifecycle(false).value,
            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
            infiniteRepeatIcon = R.drawable.ic_infinite,
            onNameChanged = { name = it; viewModel.setName(it) },
            onDurationChanged = { duration = it; viewModel.setPressDurationMs(it.toLongOrNull() ?: 0) },
            onRepeatCountChanged = { count = it; viewModel.setRepeatCount(it.toIntOrNull() ?: 0) },
            onRepeatDelayChanged = { delay = it; viewModel.setRepeatDelay(it.toLongOrNull() ?: 0) },
            onInfiniteRepeatChanged = viewModel::toggleInfiniteRepeat,
            onPositionClicked = ::onPositionCardClicked,
            onDismiss = { onDismissClicked(); back() },
            onDelete = { viewModel.getEditedDumbClick()?.let(onDeleteClicked); back() },
            onSave = { viewModel.getEditedDumbClick()?.let { viewModel.saveLastConfig(context); onConfirmClicked(it); back() } },
        )
    }

    private fun onPositionCardClicked() {
        viewModel.getEditedDumbClick()?.let { click ->
            overlayManager.navigateTo(context, PositionSelectorMenu(
                itemBriefDescription = ClickDescription(
                    pressDurationMs = click.pressDurationMs,
                    position = click.position.toEditionPosition(),
                ),
                onConfirm = { viewModel.setPosition((it as? ClickDescription)?.position?.toPoint()) },
            ), hideCurrent = true)
        }
    }

    private fun Point.toEditionPosition(): PointF? = if (x == 0 && y == 0) null else toPointF()
}
