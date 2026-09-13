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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.MoveToDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.createActionsOverlayToolbar
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction

import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor

class SmartActionsBriefMenu(initialItemIndex: Int) : ItemBriefMenu(
    theme = R.style.ScenarioConfigTheme,
    noItemText = R.string.brief_empty_actions,
    initialItemIndex = initialItemIndex,
) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU.name

    /** The view model for this dialog. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    private lateinit var menuView: ViewGroup

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false


    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isGestureCaptureStarted.collect(::updateRecordingState) }
                launch { viewModel.actionBriefList.collect(::updateItemList) }
                launch { viewModel.actionVisualization.collect(::updateActionVisualisation) }
                launch { viewModel.isTutorialModeEnabled.collect(::updateTutorialModeState) }
                launch { viewModel.isTestingAction.collect(::updateReplayingState) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        menuView = createActionsOverlayToolbar(
            context = context,
            buttonModifier = { button, performClick ->
                when (button.id) {
                    R.id.btn_add_other -> Modifier.tutorialAnchor(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                        onClick = performClick,
                    )
                    R.id.btn_back -> Modifier.tutorialAnchor(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                        onClick = performClick,
                    )
                    else -> Modifier
                }
            },
        )
        return menuView
    }

    @androidx.compose.runtime.Composable
    override fun ItemBriefContent(item: ItemBrief, orientation: Int, onClick: () -> Unit) {
        SmartActionBriefItem(item.data as UiAction, orientation, onClick)
    }

    @Composable
    override fun firstBriefItemModifier(): Modifier =
        Modifier.tutorialAnchor(MonitoredViewType.ACTIONS_BRIEF_FIRST_ITEM)

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showActionConfigDialog((item.data as UiAction).action)
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopAction()) {
                    keyDownHandled = true
                    return true
                }
            }

            KeyEvent.ACTION_UP -> {
                if (keyDownHandled) {
                    keyDownHandled = false
                    return true
                }
            }
        }

        return false
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_back -> onBackClicked()
            R.id.btn_record -> onRecordClicked()
            R.id.btn_add_other -> showNewActionDialog()
        }
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), isVisible)
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapActions(from, to)
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        viewModel.playAction(context, index)
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    private fun onBackClicked() {
        if (isGestureCaptureStarted()) {
            viewModel.cancelGestureCaptureState()
            stopGestureCapture()
            return
        }

        back()
    }

    private fun onRecordClicked() {
        if (isGestureCaptureStarted()) return

        viewModel.startGestureCaptureState()
        startGestureCapture { gesture, isFinished ->
            if (gesture == null || !isFinished) return@startGestureCapture
            viewModel.endGestureCaptureState(context, gesture)
        }
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedActionIndex(index)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        if (isRecording) {
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_back), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_add_other), false)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_hide_overlay), false)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_move), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), false)
        } else {
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_back), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_add_other), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_hide_overlay), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_move), true)
            setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), true)
        }
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        setOverlayViewVisibility(!isReplaying && isUserOverlayVisible)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_back), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_add_other), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_hide_overlay), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_move), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), !isReplaying)
    }

    private fun updateActionVisualisation(visualization: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun updateTutorialModeState(isTutorialEnabled: Boolean) {
        setBriefPanelAutoHide(!isTutorialEnabled)
    }

    private fun showMoveToDialog(index: Int, itemCount: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.ScenarioConfigTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                onValueSelected = { value ->
                    if (value - 1 == index) return@MoveToDialog
                    viewModel.moveAction(index, value - 1)
                }
            ),
        )
    }

    private fun showNewActionDialog() {
        showActionTypeSelectionDialog(viewModel)
    }

    private fun showActionConfigDialog(action: Action) {
        showActionConfigDialog(viewModel, action)
    }
}
