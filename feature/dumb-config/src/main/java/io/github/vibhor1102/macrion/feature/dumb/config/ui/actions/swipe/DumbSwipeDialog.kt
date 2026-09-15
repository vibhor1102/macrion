/* Copyright (C) 2023 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.swipe

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
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

class DumbSwipeDialog(
    private val dumbSwipe: DumbAction.DumbSwipe,
    private val onConfirmClicked: (DumbAction.DumbSwipe) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbSwipe) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    private val viewModel: DumbSwipeViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbSwipeViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbSwipe(dumbSwipe)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbSwipeDialog.Content() } }
        }
    }

@Composable private fun Content() {
        val initialName by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val initialDuration by viewModel.swipeDuration.collectAsStateWithLifecycle(initialValue = null)
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
            title = context.getString(R.string.item_title_dumb_swipe), name = name, duration = duration,
            repeatCount = count, repeatDelay = delay,
            positionTitle = context.getString(R.string.field_swipe_positions_title),
            positionDescription = viewModel.swipePositionText.collectAsStateWithLifecycle("").value,
            nameLabel = context.getString(R.string.input_field_label_name),
            durationLabel = context.getString(R.string.input_field_label_swipe_duration),
            repeatCountLabel = context.getString(R.string.input_field_label_repeat_count),
            repeatDelayLabel = context.getString(R.string.input_field_label_repeat_delay),
            nameError = viewModel.nameError.collectAsStateWithLifecycle(false).value,
            durationError = viewModel.swipeDurationError.collectAsStateWithLifecycle(false).value,
            repeatCountError = viewModel.repeatCountError.collectAsStateWithLifecycle(false).value,
            repeatDelayError = viewModel.repeatDelayError.collectAsStateWithLifecycle(false).value,
            infiniteRepeat = viewModel.repeatInfiniteState.collectAsStateWithLifecycle(false).value,
            saveEnabled = viewModel.isValidDumbSwipe.collectAsStateWithLifecycle(false).value,
            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
            infiniteRepeatIcon = R.drawable.ic_infinite,
            onNameChanged = { name = it; viewModel.setName(it) },
            onDurationChanged = { duration = it; viewModel.setPressDurationMs(it.toLongOrNull() ?: 0) },
            onRepeatCountChanged = { count = it; viewModel.setRepeatCount(it.toIntOrNull() ?: 0) },
            onRepeatDelayChanged = { delay = it; viewModel.setRepeatDelay(it.toLongOrNull() ?: 0) },
            onInfiniteRepeatChanged = viewModel::toggleInfiniteRepeat,
            onPositionClicked = ::onPositionCardClicked,
            onDismiss = { onDismissClicked(); back() },
            onDelete = { viewModel.getEditedDumbSwipe()?.let(onDeleteClicked); back() },
            onSave = { viewModel.getEditedDumbSwipe()?.let { viewModel.saveLastConfig(context); onConfirmClicked(it); back() } },
        )
    }

    private fun onPositionCardClicked() {
        viewModel.getEditedDumbSwipe()?.let { swipe ->
            overlayManager.navigateTo(context, PositionSelectorMenu(
                itemBriefDescription = SwipeDescription(
                    swipeDurationMs = swipe.swipeDurationMs,
                    from = swipe.fromPosition.toEditionPosition(),
                    to = swipe.toPosition.toEditionPosition(),
                ),
                onConfirm = { description -> (description as? SwipeDescription)?.let {
                    viewModel.setPositions(it.from?.toPoint(), it.to?.toPoint())
                } },
            ), hideCurrent = true)
        }
    }

    private fun Point.toEditionPosition(): PointF? = if (x == 0 && y == 0) null else toPointF()
}
