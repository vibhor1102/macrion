/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.PointF
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture

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
    private val useRecordedSwipeDuration: Boolean = false,
) : OverlayMenu(recreateOverlayViewOnRotation = true) {

    /** The view binding for the position selector. */
    private lateinit var selectorViews: PositionSelectorViews
    private lateinit var confirmButton: View
    private lateinit var recordButton: View
    private lateinit var hideButton: View
    private var currentDescription: ItemBriefDescription = itemBriefDescription
    private var isRecordingSwipe = false
    private var isDraggingSwipe = false
    private var multiTouchWarningShown = false
    private var isFinishingSelection = false

    private var confirmListener: (() -> Unit)? = null
    private var cancelListener: (() -> Unit)? = null

    override fun tutorialMonitoringTag(): String = tutorialMonitoringTag

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        return createOverlayMenuLayout(
            context,
            buildList {
                add(OverlayMenuButton(R.id.btn_confirm, R.drawable.ic_confirm, R.string.content_desc_confirm))
                if (itemBriefDescription is SwipeDescription) {
                    add(OverlayMenuButton(R.id.btn_record_swipe, R.drawable.ic_gesture_record, R.string.swipe_position_record))
                }
                add(OverlayMenuButton(R.id.btn_cancel, R.drawable.ic_cancel, R.string.content_desc_go_back))
                add(OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on, R.string.content_desc_go_back))
                add(OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu))
            },
        ).also { menu ->
            confirmButton = menu.findOverlayView(R.id.btn_confirm)
            hideButton = menu.findOverlayView(R.id.btn_hide_overlay)
            if (itemBriefDescription is SwipeDescription) recordButton = menu.findOverlayView(R.id.btn_record_swipe)
        }
    }

    override fun onCreateOverlayView(): View {
        if (this::selectorViews.isInitialized) {
            selectorViews.dispose()
        }
        selectorViews = PositionSelectorViews(
            context = context,
            displayConfig = displayConfigManager.displayConfig,
        )
        setActionDescription(currentDescription)
        return selectorViews.root
    }

    override fun onDestroy() {
        if (this::selectorViews.isInitialized) {
            selectorViews.dispose()
        }
        super.onDestroy()
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible && this::selectorViews.isInitialized) selectorViews.showOrResetInstructionsTimer()
    }

    override fun onMenuItemClicked(viewId: Int) {
        if (isFinishingSelection) return
        when (viewId) {
            R.id.btn_confirm -> confirmListener?.invoke()
            R.id.btn_record_swipe -> startSwipeRecording()
            R.id.btn_cancel -> {
                val swipe = currentDescription as? SwipeDescription
                if (isRecordingSwipe && swipe?.from != null && swipe.to != null) {
                    stopSwipeRecording(swipe)
                } else {
                    cancelListener?.invoke()
                }
            }
        }
    }

    private fun setActionDescription(description: ItemBriefDescription) {
        when (description) {
            is ClickDescription -> setClickDescription(description)
            is SwipeDescription -> setSwipeDescription(description)
        }

        selectorViews.showOrResetInstructionsTimer()
    }

    private fun setClickDescription(description: ClickDescription) {
        currentDescription = description
        selectorViews.setInstruction(R.string.toast_configure_single_click)
        selectorViews.setDescription(description)
        selectorViews.onTouchListener = { position ->
            setClickDescription(description.copy(position = position))
        }

        setConfirmEnabledState(description.position != null) {
            onPositionSelectionCompleted(description)
        }
        setCancelListener {
            dismiss()
        }
    }

    private fun setSwipeDescription(description: SwipeDescription) {
        updateSwipeDescription(description)
        if (isRecordingSwipe) {
            selectorViews.showSwipeRecording(true)
            selectorViews.setDescription(null)
            selectorViews.showInstruction(R.string.swipe_position_record_instruction)
        } else if (description.from == null || description.to == null) {
            startSwipeRecording()
        }
    }

    private fun updateSwipeDescription(description: SwipeDescription) {
        currentDescription = description
        selectorViews.showSwipeEditor(true)
        if (!isRecordingSwipe) {
            selectorViews.setInstruction(R.string.swipe_position_drag_instruction)
            selectorViews.setDescription(description)
        }
        selectorViews.onSwipeHandleDragged = { handle, position ->
            val current = currentDescription as? SwipeDescription
            if (current != null) {
                updateSwipeDescription(when (handle) {
                    SwipeHandle.START -> current.copy(
                        from = position,
                        path = current.path?.moveNode(0, SwipePoint(position)),
                    )
                    SwipeHandle.END -> current.copy(
                        to = position,
                        path = current.path?.let { it.moveNode(it.nodes.lastIndex, SwipePoint(position)) },
                    )
                })
            }
        }
        selectorViews.onSwipeDragCancelled = { handle, original ->
            val current = currentDescription as? SwipeDescription
            if (current != null) {
                updateSwipeDescription(when (handle) {
                    SwipeHandle.START -> current.copy(
                        from = original,
                        path = current.path?.moveNode(0, SwipePoint(original)),
                    )
                    SwipeHandle.END -> current.copy(
                        to = original,
                        path = current.path?.let { it.moveNode(it.nodes.lastIndex, SwipePoint(original)) },
                    )
                })
            }
            selectorViews.showInstruction(R.string.swipe_position_multi_touch)
        }
        selectorViews.onSwipeMultiTouch = {
            selectorViews.showInstruction(R.string.swipe_position_multi_touch)
        }
        selectorViews.onSwipeDragStateChanged = { dragging ->
            isDraggingSwipe = dragging
            val current = currentDescription as? SwipeDescription
            setConfirmEnabledState(!isRecordingSwipe && !dragging && current?.from != null && current?.to != null) {
                onPositionSelectionCompleted(currentDescription)
            }
            setMenuItemViewEnabled(recordButton, !isRecordingSwipe && !dragging)
            setMenuItemViewEnabled(hideButton, !isRecordingSwipe && !dragging)
        }
        selectorViews.onGestureRecorded = ::onSwipeGestureRecorded
        setConfirmEnabledState(!isRecordingSwipe && !isDraggingSwipe && description.from != null && description.to != null) {
            onPositionSelectionCompleted(currentDescription)
        }
        setCancelListener { dismiss() }
    }

    private fun startSwipeRecording() {
        if (currentDescription !is SwipeDescription) return
        if (isRecordingSwipe) return
        isRecordingSwipe = true
        multiTouchWarningShown = false
        selectorViews.showSwipeRecording(true)
        selectorViews.setDescription(null)
        selectorViews.showInstruction(R.string.swipe_position_record_instruction)
        setConfirmEnabledState(false)
        setMenuItemViewEnabled(recordButton, false)
        setMenuItemViewEnabled(hideButton, false)
    }

    private fun stopSwipeRecording(description: SwipeDescription) {
        isRecordingSwipe = false
        multiTouchWarningShown = false
        selectorViews.showSwipeRecording(false)
        setMenuItemViewEnabled(recordButton, true)
        setMenuItemViewEnabled(hideButton, true)
        updateSwipeDescription(description)
        selectorViews.showOrResetInstructionsTimer()
    }

    private fun onSwipeGestureRecorded(gesture: RecordedGesture?, isFinished: Boolean) {
        val swipe = currentDescription as? SwipeDescription ?: return
        if (!isRecordingSwipe || isFinishingSelection) return
        when (gesture) {
            is RecordedGesture.Swipe -> {
                val preview = swipe.copy(
                    from = gesture.from.clampToDisplay(),
                    to = gesture.to.clampToDisplay(),
                    swipeDurationMs = if (useRecordedSwipeDuration) gesture.durationMs else swipe.swipeDurationMs,
                    path = gesture.path,
                )
                if (isFinished) onPositionSelectionCompleted(preview)
                else selectorViews.setDescription(preview)
            }
            is RecordedGesture.Click -> {
                if (isFinished) {
                    selectorViews.setDescription(null)
                    selectorViews.showInstruction(R.string.swipe_position_short_gesture)
                } else {
                    selectorViews.setDescription(swipe.copy(from = gesture.position.clampToDisplay(), to = null))
                }
            }
            is RecordedGesture.Split -> {
                selectorViews.setDescription(null)
                if (!multiTouchWarningShown) {
                    multiTouchWarningShown = true
                    selectorViews.showInstruction(R.string.swipe_position_multi_touch)
                }
                if (isFinished) multiTouchWarningShown = false
            }
            null -> if (isFinished) {
                selectorViews.setDescription(null)
                selectorViews.showInstruction(R.string.swipe_position_record_failed)
            }
        }
    }

    private fun PointF.clampToDisplay(): PointF {
        val size = displayConfigManager.displayConfig.sizePx
        return PointF(x.coerceIn(0f, size.x.toFloat()), y.coerceIn(0f, size.y.toFloat()))
    }

    private fun onPositionSelectionCompleted(description: ItemBriefDescription) {
        if (isFinishingSelection) return
        isFinishingSelection = true
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
