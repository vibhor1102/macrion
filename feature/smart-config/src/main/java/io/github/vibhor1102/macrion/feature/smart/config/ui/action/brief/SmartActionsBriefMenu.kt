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
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButtonView

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor

import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ItemsReorderDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder.ReorderItemDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

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
    private var canCompareActionPreviews = false
    private var isRecording = false
    private var isReplaying = false

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
                launch { viewModel.actionBriefList.collect(::updateActions) }
                launch { viewModel.canCompareActionPreviews.collect(::updatePreviewToggleVisibility) }
                launch { viewModel.actionVisualization.collect(::updateActionVisualisation) }
                launch { viewModel.showAllActionPreviews.collect(::updatePreviewMode) }
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
        canCompareActionPreviews = viewModel.canCompareActionPreviews.value
        menuView.findOverlayView<OverlayMenuButtonView>(R.id.btn_show_all_action_previews).visibility =
            if (canCompareActionPreviews) View.VISIBLE else View.GONE
        updatePreviewMode(viewModel.showAllActionPreviews.value)
        return menuView
    }

    @androidx.compose.runtime.Composable
    override fun ItemBriefContent(item: ItemBrief, orientation: Int, onClick: () -> Unit) {
        val uiAction = item.data as UiAction
        SmartActionBriefItem(
            details = uiAction,
            orientation = orientation,
            onClick = onClick,
        )
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
            R.id.btn_show_all_action_previews -> viewModel.toggleShowAllActionPreviews()
        }
    }

    override fun shouldDebounceMenuItemClick(viewId: Int): Boolean =
        viewId != R.id.btn_show_all_action_previews

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        super.onScreenOverlayVisibilityChanged(isVisible)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), isVisible)
    }

    override fun onReorderClicked() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ItemsReorderDialog(
                theme = R.style.ScenarioConfigTheme,
                titleRes = R.string.menu_item_title_actions,
                itemsFlow = viewModel.actionBriefList,
                itemDescriptor = { brief ->
                    val action = brief.data as UiAction
                    ReorderItemDescriptor(
                        title = action.name,
                        subtitle = action.description,
                        trailingContent = {
                            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(action.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                if (action.haveError) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape),
                                    )
                                }
                            }
                        },
                    )
                },
                onSaveOrder = { viewModel.updateActionOrder(it) },
            ),
            hideCurrent = true,
        )
    }

    override fun onDeleteItemClicked(index: Int) {
        viewModel.deleteAction(index)
    }

    override fun onPlayItemClicked(index: Int) {
        viewModel.playAction(context, index)
    }

    override fun onItemPositionCardClicked(index: Int, itemCount: Int) {
        if (itemCount < 2) return
        overlayManager.navigateTo(
            context = context,
            newOverlay = MoveToDialog(
                theme = R.style.ScenarioConfigTheme,
                defaultValue = index + 1,
                itemCount = itemCount,
                titleRes = io.github.vibhor1102.macrion.core.common.overlays.R.string.dialog_jump_to_title,
                onValueSelected = { value ->
                    val targetIndex = (value - 1).coerceIn(0, itemCount - 1)
                    briefViewBinding.scrollToItem(targetIndex)
                },
            ),
        )
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
            if (!isFinished) return@startGestureCapture
            if (gesture == null) {
                viewModel.cancelGestureCaptureState()
                return@startGestureCapture
            }
            viewModel.endGestureCaptureState(context, gesture)
        }
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.setFocusedActionIndex(index)
    }

    private fun updateRecordingState(isRecording: Boolean) {
        this.isRecording = isRecording
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
        updatePreviewToggleAvailability()
    }

    private fun updateReplayingState(isReplaying: Boolean) {
        this.isReplaying = isReplaying
        setOverlayViewVisibility(!isReplaying && isUserOverlayVisible)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_back), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_add_other), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_hide_overlay), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_move), !isReplaying)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_record), !isReplaying)
        updatePreviewToggleAvailability()
    }

    private fun updateActions(items: List<ItemBrief>) {
        updateItemList(items)
    }

    private fun updatePreviewToggleVisibility(canCompare: Boolean) {
        canCompareActionPreviews = canCompare
        updatePreviewToggleAvailability()
    }

    private fun updatePreviewToggleAvailability() {
        if (this::menuView.isInitialized) {
            val button = menuView.findOverlayView<OverlayMenuButtonView>(R.id.btn_show_all_action_previews)
            setMenuItemVisibility(button, canCompareActionPreviews)
            setMenuItemViewEnabled(
                button,
                canCompareActionPreviews && !isRecording && !isReplaying,
            )
        }
    }

    private fun updatePreviewMode(showAll: Boolean) {
        if (!this::menuView.isInitialized) return
        val button = menuView.findOverlayView<OverlayMenuButtonView>(R.id.btn_show_all_action_previews)
        button.isSelected = showAll
        button.setImageResource(if (showAll) R.drawable.ic_action_preview_all else R.drawable.ic_action_preview_current)
        ViewCompat.setStateDescription(button, context.getString(
            if (showAll) R.string.action_previews_all_on else R.string.action_previews_all_off,
        ))
        button.tooltipText = context.getString(R.string.content_desc_show_all_action_previews)
    }

    private fun updateActionVisualisation(visualization: ItemBriefDescription?) {
        briefViewBinding.viewBrief.setDescription(visualization, true)
    }

    private fun updateTutorialModeState(isTutorialEnabled: Boolean) {
        setBriefPanelAutoHide(!isTutorialEnabled)
    }


    private fun showNewActionDialog() {
        showActionTypeSelectionDialog(viewModel)
    }

    private fun showActionConfigDialog(action: Action) {
        showActionConfigDialog(viewModel, action)
    }
}
