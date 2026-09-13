/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.swipe

import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.PositionSelectorMenu
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionPositionGestureEditor
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class SwipeDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SWIPE.name

    private val viewModel: SwipeViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { swipeViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@SwipeDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingAction.collect(::onActionEditingStateChanged)
            }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        MacrionPositionGestureEditor(
            title = context.getString(R.string.dialog_title_swipe),
            name = ui.name.orEmpty(),
            duration = ui.swipeDuration.orEmpty(),
            positionTitle = context.getString(R.string.field_swipe_positions_title),
            positionDescription = ui.positionsDescription,
            nameLabel = context.getString(R.string.generic_name),
            durationLabel = context.getString(R.string.input_field_label_swipe_duration),
            nameError = ui.nameError,
            durationError = ui.swipeDurationError,
            positionError = ui.positionsError,
            saveEnabled = ui.canBeSaved,
            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
            onNameChanged = viewModel::setName,
            onDurationChanged = { viewModel.setSwipeDuration(it.toLongOrNull()) },
            onPositionClicked = ::showPositionSelector,
            onDismiss = ::back,
            onDelete = ::onDeleteButtonClicked,
            onSave = ::onSaveButtonClicked,
        )
    }

    override fun back() {
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

    private fun onSaveButtonClicked() {
        viewModel.saveLastConfig()
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun showPositionSelector() {
        viewModel.getEditedSwipe()?.let { swipe ->
            overlayManager.navigateTo(
                context = context,
                newOverlay = PositionSelectorMenu(
                    tutorialMonitoringTag = MonitoredOverlayType.SWIPE_POSITION.name,
                    itemBriefDescription = SwipeDescription(
                        from = swipe.from?.toPointF(),
                        to = swipe.to?.toPointF(),
                        swipeDurationMs = swipe.swipeDuration ?: 250L,
                    ),
                    onConfirm = { description ->
                        (description as SwipeDescription).let {
                            viewModel.setPositions(it.from!!.toPoint(), it.to!!.toPoint())
                        }
                    },
                ),
                hideCurrent = true,
            )
        }
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing SwipeDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "SwipeDialog"
