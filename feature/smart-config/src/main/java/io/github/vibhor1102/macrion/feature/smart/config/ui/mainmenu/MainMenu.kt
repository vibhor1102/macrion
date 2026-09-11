/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import android.content.DialogInterface
import android.graphics.Region
import android.os.Build
import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.core.view.isVisible

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.navigation.TutorialNavigator
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters.newRestartMediaProjectionStarterOverlay
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetActivity
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet.required.RequiredAlphabetFragment
import io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu.debugging.LiveDebuggingUiState
import io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu.debugging.LiveDebuggingViewModel
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.ScenarioDialog
import io.github.vibhor1102.macrion.core.ui.R as CoreUiR

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource

/**
 * [OverlayMenu] implementation for displaying the main menu overlay.
 *
 * This is the menu displayed once the service is started via the [io.github.vibhor1102.macrion.scenarios.ScenarioActivity]
 * once the user has selected a scenario to be used. It allows the user to start the detection on the currently loaded
 * scenario, as well as editing the attached list of events.
 *
 * There is no overlay views attached to this overlay menu, meaning that the user will always be able to clicks on the
 * Activities displayed below it.
 */
class MainMenu(
    private val onStopClicked: () -> Unit,
    private val onOpenHomeClicked: () -> Unit,
    private val onSwitchScenarioClicked: () -> Unit,
    private val isSwitchButtonInitiallyVisible: Boolean,
    private val isHomeButtonInitiallyVisible: Boolean,
    private val shouldConfirmStop: suspend () -> Boolean,
) : OverlayMenu() {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.MAIN_MENU.name

    /** The view model for this menu. */
    private val viewModel: MainMenuModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { mainMenuViewModel() },
    )

    /** The view model for the live debugging. */
    private val debuggingViewModel: LiveDebuggingViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { liveDebuggingViewModel() },
    )

    private val tutorialNavigator: TutorialNavigator by lazy {
        context.getTutorialNavigator()
    }

    private var isHiddenForPaywall: Boolean = false
    private var hasReceivedSwitchVisibility = false

    private lateinit var viewBinding: MainMenuViews
    private var liveDebugUiState by mutableStateOf<LiveDebuggingUiState?>(null)
    private var isDetecting by mutableStateOf(false)
    /** The coroutine job for the observable used in debug mode. Null when not in debug mode. */
    private var debugObservableJob: Job? = null

    /**
     * Reused state for defining the tools-only touchable region on Android 13 and newer.
     * Older Android versions retain the original whole-window touch behavior.
     */
    private val menuItemsLocationInWindow = IntArray(2)
    private val toolsTouchableRegion = Region()
    private val updateTouchableRegion = Runnable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@Runnable

        viewBinding.menuItems.getLocationInWindow(menuItemsLocationInWindow)
        val left = menuItemsLocationInWindow[0]
        val top = menuItemsLocationInWindow[1]
        toolsTouchableRegion.set(
            left,
            top,
            left + viewBinding.menuItems.width,
            top + viewBinding.menuItems.height,
        )
        viewBinding.root.rootSurfaceControl?.setTouchableRegion(toolsTouchableRegion)
    }
    private val updateTouchableRegionOnLayout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        updateTouchableRegion.run()
    }

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = createMainOverlayMenu(
            context = context,
            debugContent = { MainLiveDebugPanel(liveDebugUiState) },
            playPauseContent = {
                AnimatedContent(isDetecting, label = "playPause") { detecting ->
                    Icon(
                        painterResource(if (detecting) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = colorResource(CoreUiR.color.overlayMenuButtons),
                    )
                }
            },
        )
        viewBinding.btnSwitchScenario.isVisible = isSwitchButtonInitiallyVisible
        viewBinding.btnOpenHome.isVisible = isHomeButtonInitiallyVisible
        return viewBinding.root
    }

    override fun onCreate() {
        super.onCreate()

        // On supported Android versions, keep one visual window while passing Debug-area touches through it.
        viewBinding.root.addOnLayoutChangeListener(updateTouchableRegionOnLayout)
        viewBinding.root.post(updateTouchableRegion)

        // Ensure the debug view state is correct
        viewBinding.layoutDebug.visibility = View.GONE
        setOverlayViewVisibility(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.paywallIsVisible.collect(::updateVisibilityForPaywall) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isStartButtonEnabled.collect(::updatePlayPauseButtonEnabledState) }
                launch { viewModel.isSwitchButtonVisible.collect(::updateSwitchButtonVisibility) }
                launch { viewModel.isMediaProjectionStarted.collect(::updateProjectionErrorBadge) }
                launch { viewModel.detectionState.collect(::updateDetectionState) }
                launch { viewModel.nativeLibError.collect(::showNativeLibErrorDialogIfNeeded) }
                launch { viewModel.screenCaptureError.collect(::showScreenCaptureErrorDialogIfNeeded) }
                launch { debuggingViewModel.isDebugging.collect(::updateDebugOverlayViewVisibility) }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.monitorViews(
            playMenuButton = viewBinding.btnPlay,
            configMenuButton = viewBinding.btnClickList,
        )

        // Start loading advertisement if needed
        viewModel.loadAdIfNeeded(context)
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopViewMonitoring()
        viewBinding.btnPlay.tag = null
    }

    override fun onDestroy() {
        viewBinding.root.removeCallbacks(updateTouchableRegion)
        viewBinding.root.removeOnLayoutChangeListener(updateTouchableRegionOnLayout)
        super.onDestroy()
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopDetection()) {
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
            R.id.btn_play -> onPlayPauseClicked()
            R.id.btn_click_list -> onConfigureClicked()
            R.id.btn_switch_scenario -> onSwitchScenarioClicked()
            R.id.btn_open_home -> onOpenHomeClicked()
            R.id.btn_stop -> onStopButtonClicked()
        }
    }

    private fun onStopButtonClicked() {
        lifecycleScope.launch {
            if (!shouldConfirmStop()) {
                onStopClicked()
                return@launch
            }

            MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
                .setTitle(R.string.dialog_stop_confirmation_title)
                .setMessage(R.string.dialog_stop_confirmation_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.dialog_stop_confirmation_stop) { _, _ -> onStopClicked() }
                .create()
                .showAsOverlay()
        }
    }

    override fun getWindowMaximumSize(backgroundView: ViewGroup): Size {
        val buttons = backgroundView.findViewById<ViewGroup>(
            io.github.vibhor1102.macrion.core.common.overlays.R.id.menu_items,
        )
        val buttonWidth = (0 until buttons.childCount).maxOfOrNull { index ->
            buttons.getChildAt(index).layoutParams.width
        } ?: 0
        val buttonsHeight = (0 until buttons.childCount).sumOf { index ->
            buttons.getChildAt(index).layoutParams.height
        }
        return Size(
            buttonWidth + buttons.paddingLeft + buttons.paddingRight +
                context.resources.getDimensionPixelSize(R.dimen.overlay_debug_panel_width),
            buttonsHeight + buttons.paddingTop + buttons.paddingBottom,
        )
    }

    fun onMediaProjectionLost() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return

        overlayManager.navigateUpToRoot(context)
        viewModel.cancelScenarioChanges()
    }

    private fun onConfigureClicked() {
        if (viewModel.shouldRestartMediaProjection()) {
            showRestartMediaProjectionScreen()
            return
        }

        viewModel.startScenarioEdition {
            showScenarioConfigDialog()
        }
    }

    private fun onPlayPauseClicked() {
        if (viewModel.shouldDownloadModels()) {
            context.startActivity(AlphabetActivity.getStartIntent(context, RequiredAlphabetFragment.FRAGMENT_TAG))
            return
        }

        if (viewModel.shouldRestartMediaProjection()) {
            showRestartMediaProjectionScreen()
            return
        }

        if (viewModel.shouldShowStopVolumeDownTutorialDialog()) {
            showStopVolumeDownTutorialDialog()
            return
        }

        viewModel.toggleDetection(context)
    }

    /** Refresh the play menu item according to the scenario state. */
    private fun updatePlayPauseButtonEnabledState(canStartDetection: Boolean) =
        setMenuItemViewEnabled(viewBinding.btnPlay, canStartDetection)

    private fun updateSwitchButtonVisibility(isVisible: Boolean) {
        if (!hasReceivedSwitchVisibility) {
            hasReceivedSwitchVisibility = true
            if (isVisible != isSwitchButtonInitiallyVisible) return
        }
        if (viewBinding.btnSwitchScenario.isVisible == isVisible) return

        if (viewBinding.btnPlay.tag == null) {
            viewBinding.btnSwitchScenario.visibility = if (isVisible) View.VISIBLE else View.GONE
            return
        }

        animateLayoutChanges {
            setMenuItemVisibility(viewBinding.btnSwitchScenario, isVisible)
        }
    }

    /** Refresh the menu layout according to the detection state. */
    private fun updateDetectionState(newState: UiState) {
        val currentState = viewBinding.btnPlay.tag
        if (currentState == newState) return

        viewBinding.btnPlay.tag = newState
        isDetecting = newState is UiState.Detecting
        when (newState) {
            UiState.Idle -> {
                if (currentState == null) {
                    viewBinding.btnStop.isVisible = true
                    viewBinding.btnClickList.isVisible = true
                    viewBinding.btnSwitchScenario.isVisible = isSwitchButtonInitiallyVisible
                    viewBinding.btnOpenHome.isVisible = isHomeButtonInitiallyVisible
                } else {
                    animateLayoutChanges {
                        setMenuItemVisibility(viewBinding.btnStop, true)
                        setMenuItemVisibility(viewBinding.btnClickList, true)
                        setMenuItemVisibility(viewBinding.btnSwitchScenario, viewModel.isSwitchButtonVisible.value)
                        setMenuItemVisibility(viewBinding.btnOpenHome, isHomeButtonInitiallyVisible)
                    }
                }
            }

            UiState.Detecting -> {
                if (currentState == null) {
                    viewBinding.btnStop.isVisible = false
                    viewBinding.btnClickList.isVisible = false
                    viewBinding.btnSwitchScenario.isVisible = false
                    viewBinding.btnOpenHome.isVisible = false
                } else {
                    animateLayoutChanges {
                        setMenuItemVisibility(viewBinding.btnStop, false)
                        setMenuItemVisibility(viewBinding.btnClickList, false)
                        setMenuItemVisibility(viewBinding.btnSwitchScenario, false)
                        setMenuItemVisibility(viewBinding.btnOpenHome, false)
                    }
                }
            }
        }
    }

    private fun updateVisibilityForPaywall(isHidden: Boolean) {
        if (isHidden) {
            isHiddenForPaywall = true
            hide()
        } else if (isHiddenForPaywall) {
            isHiddenForPaywall = false
            show()
        }
    }

    private fun updateProjectionErrorBadge(isProjectionStarted: Boolean) {
        viewBinding.errorBadge.visibility = if (isProjectionStarted) View.GONE else View.VISIBLE
    }

    /**
     * Change the debug state of this UI.
     * @param isVisible true when the debug view should be shown, false to hide it.
     */
    private fun updateDebugOverlayViewVisibility(isVisible: Boolean) {
        if (isVisible && debugObservableJob == null) {
            viewBinding.layoutDebug.visibility = View.VISIBLE
            debugObservableJob = observeDebugValues()

        } else if (!isVisible && debugObservableJob != null) {
            debugObservableJob?.cancel()
            debugObservableJob = null

            updateLiveDebugUiState(null)
            viewBinding.layoutDebug.visibility = View.GONE
        }
    }

    /**
     * Observe the values for the debug and update the debug views.
     * @return the coroutine job for the observable. Can be cancelled to stop the observation.
     */
    private fun observeDebugValues() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                debuggingViewModel.debugLastPositive.collect(::updateLiveDebugUiState)
            }
        }
    }

    private fun updateLiveDebugUiState(uiState: LiveDebuggingUiState?) {
        liveDebugUiState = uiState
    }

    private fun showScenarioConfigDialog() =
        overlayManager.navigateTo(
            context = context,
            newOverlay = ScenarioDialog(
                onConfigDiscarded = viewModel::cancelScenarioChanges,
                onConfigSaved = { viewModel.saveScenarioChanges { success -> if (!success) showScenarioSaveErrorDialog() } },
            ),
            hideCurrent = true,
        )

    private fun showScenarioSaveErrorDialog() {
        MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.error_dialog_message_scenario_saving)
            .setPositiveButton(R.string.generic_modify) { _: DialogInterface, _: Int ->
                showScenarioConfigDialog()
            }
            .setNegativeButton(android.R.string.cancel) { _: DialogInterface, _: Int ->
                viewModel.cancelScenarioChanges()
            }
            .create()
            .showAsOverlay()
    }

    private fun showStopVolumeDownTutorialDialog() {
        tutorialNavigator.showTipDialog(context, Tip.STOP_WITH_VOLUME_DOWN) {
            viewModel.toggleDetection(context)
        }
    }

    private fun showNativeLibErrorDialogIfNeeded(haveError: Boolean) {
        if (!haveError) return

        MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.error_dialog_message_error_native_lib)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onStopClicked()
            }
            .create()
            .showAsOverlay()
    }

    private fun showScreenCaptureErrorDialogIfNeeded(haveError: Boolean) {
        if (!haveError) return

        MaterialAlertDialogBuilder(context.getDynamicColorsContext(R.style.AppTheme))
            .setTitle(R.string.dialog_overlay_title_warning)
            .setMessage(R.string.error_dialog_message_screen_capture_unsupported)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onStopClicked()
            }
            .create()
            .showAsOverlay()
    }

    private fun showRestartMediaProjectionScreen() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = newRestartMediaProjectionStarterOverlay(context),
            hideCurrent = true,
        )
    }

}
