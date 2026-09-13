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
package io.github.vibhor1102.macrion.feature.dumb.config.ui.brief

import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBriefMenu
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.MoveToDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.dumb.config.ui.createDumbBriefOverlayToolbar
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionCreator
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionUiFlowListener
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.startDumbActionCreationUiFlow
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.startDumbActionEditionUiFlow
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails

import kotlinx.coroutines.launch

class DumbScenarioBriefMenu(
    private val onConfigSaved: () -> Unit,
) : ItemBriefMenu(
    theme = R.style.AppTheme,
    noItemText = R.string.message_dumb_brief_empty_action_list,
) {

    /** The view model for this menu. */
    private val viewModel: DumbScenarioBriefViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioBriefViewModel() },
    )

    private lateinit var menuView: ViewGroup
    private val backButton get() = menuView.findOverlayView<View>(R.id.btn_back)
    private val recordButton get() = menuView.findOverlayView<View>(R.id.btn_record)
    private val addButton get() = menuView.findOverlayView<View>(R.id.btn_add)
    private val hideButton get() = menuView.findOverlayView<View>(R.id.btn_hide_overlay)
    private val moveButtonView get() = menuView.findOverlayView<View>(R.id.btn_move)

    private lateinit var dumbActionCreator: DumbActionCreator
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener

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
                launch { viewModel.dumbActionsBriefList.collect(::updateItemList) }
                launch { viewModel.dumbActionVisualization.collect(::updateDumbActionVisualisation) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = { position -> viewModel.createNewDumbClick(context, position) },
            createNewDumbSwipe = { from, to -> viewModel.createNewDumbSwipe(context, from, to) },
            createNewDumbPause = { viewModel.createNewDumbPause(context) },
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = { action -> viewModel.addNewDumbAction(action, getFocusedItemIndex() + 1) },
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )

        menuView = createDumbBriefOverlayToolbar(context)
        return menuView
    }

    @androidx.compose.runtime.Composable
    override fun ItemBriefContent(item: ItemBrief, orientation: Int, onClick: () -> Unit) {
        DumbActionBriefItem(item.data as DumbActionDetails, orientation, onClick)
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(recordButton, isVisible)
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedDumbActionIndex(index)
    }

    override fun onMoveItemClicked(from: Int, to: Int) {
        viewModel.swapDumbActions(from, to)
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteDumbAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        updateReplayingState(true)
        viewModel.playAction(index) {
            updateReplayingState(false)
        }
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        showMoveToDialog(index, itemCount)
    }

    override fun onItemBriefClicked(index: Int, item: ItemBrief) {
        showDumbActionEditionUiFlow((item.data as DumbActionDetails).action)
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
            R.id.btn_add -> onCreateDumbActionClicked()
        }
    }

    private fun onBackClicked() {
        if (isGestureCaptureStarted()) {
            viewModel.cancelGestureCaptureState()
            stopGestureCapture()
            return
        }

        if (viewModel.stopAction()) return

        onConfigSaved()
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

    private fun onCreateDumbActionClicked() {
        hidePanel()
        showDumbActionCreationUiFlow()
    }

    private fun updateDumbActionVisualisation(details: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(details, true)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        if (isRecording) {
            setMenuItemViewEnabled(backButton, true)
            setMenuItemViewEnabled(addButton, false)
            setMenuItemViewEnabled(hideButton, false)
            setMenuItemViewEnabled(moveButtonView, true)
            setMenuItemViewEnabled(recordButton, false)
        } else {
            setMenuItemViewEnabled(backButton, true)
            setMenuItemViewEnabled(addButton, true)
            setMenuItemViewEnabled(hideButton, true)
            setMenuItemViewEnabled(moveButtonView, true)
            setMenuItemViewEnabled(recordButton, true)
        }
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        setOverlayViewVisibility(!isReplaying && isUserOverlayVisible)
        setMenuItemViewEnabled(backButton, true)
        setMenuItemViewEnabled(addButton, !isReplaying)
        setMenuItemViewEnabled(hideButton, !isReplaying)
        setMenuItemViewEnabled(moveButtonView, !isReplaying)
        setMenuItemViewEnabled(recordButton, !isReplaying)
    }

    private fun showDumbActionCreationUiFlow(): Unit =
        overlayManager.startDumbActionCreationUiFlow(
            context = context,
            creator = dumbActionCreator,
            listener = createCopyActionUiFlowListener,
        )

    private fun showDumbActionEditionUiFlow(action: DumbAction): Unit =
        overlayManager.startDumbActionEditionUiFlow(
            context = context,
            dumbAction = action,
            listener = updateActionUiFlowListener,
        )

    private fun showMoveToDialog(index: Int, itemCount: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.AppTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                onValueSelected = { value ->
                    if (value - 1 == index) return@MoveToDialog
                    viewModel.moveDumbAction(index, value - 1)
                }
            ),
        )
    }
}
