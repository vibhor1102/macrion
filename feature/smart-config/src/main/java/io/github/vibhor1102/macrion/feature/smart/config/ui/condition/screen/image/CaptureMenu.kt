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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast

import androidx.annotation.IntDef
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator

import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.createValidationOverlayToolbar
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selector.SelectorController
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selector.SelectorOverlay

/**
 * [OverlayMenu] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new event condition.
 *
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the event condition.
 */
class CaptureMenu(
    private val onConditionSelected: (ScreenCondition.Image) -> Unit
) : OverlayMenu(theme = R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CAPTURE_MENU.name

    private companion object {

        /** Tag for logs */
        private const val TAG = "ConditionSelectorMenu"

        /** Describe the state of the capture. */
        @IntDef(SELECTION, CAPTURE, ADJUST, SAVE)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class ConditionCaptureState
        /** User is selecting the screenshot to take. */
        private const val SELECTION = 1
        /** User have clicked on the capture button and we are waiting for the screenshot result. */
        private const val CAPTURE = 2
        /** User is selecting a part of the capture for the event condition. */
        private const val ADJUST = 3
        /** User is selecting a part of the capture for the event condition. */
        private const val SAVE = 4
    }

    /** The view model for this menu. */
    private val viewModel: CaptureViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { captureViewModel() },
    )

    private lateinit var menuView: ViewGroup
    private val confirmButton get() = menuView.findViewById<ImageButton>(R.id.btn_confirm)
    private val cancelButton get() = menuView.findViewById<ImageButton>(R.id.btn_cancel)
    private val helpButton get() = menuView.findViewById<ImageButton>(R.id.btn_help)
    private val hideButton get() = menuView.findViewById<ImageButton>(R.id.btn_hide_overlay)
    /** Compose selector state retained across the menu and overlay view lifecycles. */
    private val selectorController = SelectorController(hasCapture = true, ::onSelectorValidityChanged)

    /** The current state of the overlay. */
    @ConditionCaptureState
    private var state: Int = 0
        set(value) {
            field = value
            when (value) {
                SELECTION -> {
                    confirmButton.setImageResource(R.drawable.ic_capture)
                    setMenuVisibility(View.VISIBLE)
                    setMenuItemsVisibility(
                        mapOf(
                            helpButton to false,
                            hideButton to false,
                        )
                    )
                    setOverlayViewVisibility(false)
                    selectorController.setShown(false)
                    // Resetting an old valid selection reports invalid; the capture action itself
                    // must remain available in this initial state.
                    setMenuItemViewEnabled(confirmButton, true)
                }
                CAPTURE -> {
                    setMenuVisibility(View.GONE)
                    setOverlayViewVisibility(true)
                    selectorController.setShown(false)
                }
                ADJUST -> {
                    confirmButton.setImageResource(R.drawable.ic_confirm)
                    setMenuVisibility(View.VISIBLE)
                    setMenuItemsVisibility(
                        mapOf(
                            helpButton to true,
                            hideButton to true,
                        )
                    )
                    selectorController.setShown(true)
                }
                SAVE -> {
                    setMenuItemViewEnabled(confirmButton, false)
                    setMenuItemViewEnabled(cancelButton, false)
                    setMenuItemViewEnabled(helpButton, false)
                    setMenuItemViewEnabled(hideButton, false)
                    selectorController.setShown(true)
                }
            }
        }

    override fun animateOverlayView(): Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        menuView = createValidationOverlayToolbar(context)
        return menuView
    }

    override fun onCreateOverlayView(): View = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { SelectorOverlay(selectorController) } }
    }

    override fun onStart() {
        super.onStart()
        state = SELECTION
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
            R.id.btn_help -> onHelp()
        }
    }

    /**
     * Called when the validity of the selector have changed.
     * Update the buttons to avoid a capture if the selector can't provide the bitmap.
     *
     * @param isValid validity of the selector.
     */
    private fun onSelectorValidityChanged(isValid: Boolean) {
        setMenuItemViewEnabled(confirmButton, isValid, isValid)
    }

    /**
     * Called when the user press the confirmation button.
     * Depending on the current [state], this will have different effect.
     */
    private fun onConfirm() {
        when (state) {
            SELECTION -> onTakeScreenshotClicked()

            ADJUST -> {
                state = SAVE
                try {
                    val selection = selectorController.getCaptureSelection()
                    viewModel.createImageCondition(context, selection.first, selection.second) { imageCondition ->
                        back()
                        onConditionSelected(imageCondition)
                    }
                } catch (ex: IllegalStateException) {
                    Log.e(TAG, "Condition selection failed", ex)
                    state = ADJUST
                }
            }
        }
    }

    /**
     * Called when the user press the cancel button.
     * Depending on the current state, dismiss this overlay or return to the previous step.
     */
    private fun onCancel() {
        when (state) {
            SELECTION -> back()
            ADJUST -> state = SELECTION
        }
    }

    private fun onHelp() {
        context.getTutorialNavigator().showTipDialog(
            context = context,
            tip = Tip.IMAGE_CAPTURE,
        )
    }

    private fun onTakeScreenshotClicked() {
        state = CAPTURE
        viewModel.takeScreenshot { screenshot ->
            if (screenshot == null) {
                Toast.makeText(context, R.string.toast_capture_failed, Toast.LENGTH_LONG).show()
                state = SELECTION
                return@takeScreenshot
            }

            selectorController.showCapture(screenshot)
            state = ADJUST
        }
    }
}
