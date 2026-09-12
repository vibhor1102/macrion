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
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation

import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.ui.utils.AutoHideAnimationController
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription

/**
 * [OverlayMenu] implementation for displaying the click area selection menu and its overlay view.
 *
 * This class will display the overlay menu for selecting the positions for an action. The overlay view
 * displayed between the menu and the activity shows those positions.
 *
 * @param itemBriefDescription the description of the action positions to edit.
 * @param onConfirm listener on the validation of the actions positions.
 * @param onDismiss listener on dismiss of the position selection.
 */
class PositionSelectorMenu(
    private val tutorialMonitoringTag: String = "PositionSelectorMenu",
    private val itemBriefDescription: ItemBriefDescription,
    private val onConfirm: (ItemBriefDescription) -> Unit,
    private val onDismiss: (() -> Unit)? = null,
) : OverlayMenu() {

    /** The view binding for the position selector. */
    private lateinit var selectorViews: PositionSelectorViews
    private lateinit var confirmButton: View

    /** Controls the instructions in and out animations. */
    private lateinit var instructionsAnimationController: AutoHideAnimationController

    private var confirmListener: (() -> Unit)? = null
    private var cancelListener: (() -> Unit)? = null

    override fun tutorialMonitoringTag(): String = tutorialMonitoringTag

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorViews = PositionSelectorViews(
            context = context,
            safeInsetTopPx = displayConfigManager.displayConfig.safeInsetTopPx,
        )

        instructionsAnimationController = AutoHideAnimationController().apply {
            attachToView(
                selectorViews.instructions,
                AutoHideAnimationController.ScreenSide.TOP,
            )
        }

        return createOverlayMenuLayout(
            context,
            listOf(
                OverlayMenuButton(R.id.btn_confirm, R.drawable.ic_confirm, R.string.content_desc_confirm),
                OverlayMenuButton(R.id.btn_cancel, R.drawable.ic_cancel, R.string.content_desc_go_back),
                OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on, R.string.content_desc_go_back),
                OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
            ),
        ).also { menu -> confirmButton = menu.findOverlayView(R.id.btn_confirm) }
    }

    override fun onCreateOverlayView(): View {
        return selectorViews.root
    }

    override fun onStart() {
        super.onStart()
        setActionDescription(itemBriefDescription)
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) instructionsAnimationController.showOrResetTimer()
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> confirmListener?.invoke()
            R.id.btn_cancel -> cancelListener?.invoke()
        }
    }

    private fun setActionDescription(description: ItemBriefDescription) {
        when (description) {
            is ClickDescription -> setClickDescription(description)
            is SwipeDescription -> setSwipeDescription(description)
        }

        instructionsAnimationController.showOrResetTimer()
    }

    private fun setClickDescription(description: ClickDescription) {
        selectorViews.setInstruction(R.string.toast_configure_single_click)
        selectorViews.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                setClickDescription(description.copy(position = position))
            }
        }

        setConfirmEnabledState(description.position != null) {
            onPositionSelectionCompleted(description)
        }
        setCancelListener {
            dismiss()
        }
    }

    private fun setSwipeDescription(description: SwipeDescription) {
        if (description.from == null) {
            toSelectSwipeFromState(description)
        } else {
            toSelectSwipeToState(description)
        }
    }

    private fun toSelectSwipeFromState(description: SwipeDescription) {
        selectorViews.setInstruction(R.string.toast_configure_swipe_from)
        selectorViews.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                toSelectSwipeFromState(description.copy(from = position))
            }
        }

        setConfirmEnabledState(description.from != null) {
            toSelectSwipeToState(description)
            instructionsAnimationController.showOrResetTimer()
        }
        setCancelListener {
            dismiss()
        }
    }

    private fun toSelectSwipeToState(description: SwipeDescription) {
        selectorViews.setInstruction(R.string.toast_configure_swipe_to)
        selectorViews.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                toSelectSwipeToState(description.copy(to = position))
            }
        }

        setConfirmEnabledState(description.to != null) {
            onPositionSelectionCompleted(description)
        }
        setCancelListener {
            toSelectSwipeFromState(description.copy(to = null))
            instructionsAnimationController.showOrResetTimer()
        }
    }

    private fun onPositionSelectionCompleted(description: ItemBriefDescription) {
        back()
        onConfirm(description)
    }

    private fun dismiss() {
        back()
        onDismiss?.invoke()
    }

    private fun setConfirmEnabledState(isEnabled: Boolean, action: (() -> Unit)? = null) {
        confirmListener = action
        setMenuItemViewEnabled(confirmButton, enabled = isEnabled, clickable = isEnabled)
    }

    private fun setCancelListener(action: (() -> Unit)) {
        cancelListener = action
    }
}
