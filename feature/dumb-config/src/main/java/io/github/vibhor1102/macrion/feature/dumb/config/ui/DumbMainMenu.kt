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
package io.github.vibhor1102.macrion.feature.dumb.config.ui

import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.navigation.TutorialNavigator
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager.Companion.showAsOverlay
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.dumb.config.ui.brief.DumbScenarioBriefMenu
import io.github.vibhor1102.macrion.feature.dumb.config.ui.scenario.DumbScenarioDialog

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

class DumbMainMenu(
    private val dumbScenarioId: Identifier,
    private val shouldConfirmStop: suspend () -> Boolean,
    private val onStopClicked: () -> Unit,
) : OverlayMenu(theme = R.style.AppTheme) {

    /** The view model for this menu. */
    private val viewModel: DumbMainMenuModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbMainMenuModel() },
    )

    private val tutorialNavigator: TutorialNavigator by lazy {
        context.getTutorialNavigator()
    }

    private lateinit var menuView: ViewGroup
    private val playButton get() = menuView.findViewById<View>(R.id.btn_play)
    private val stopButton get() = menuView.findViewById<View>(R.id.btn_stop)
    private val showActionsButton get() = menuView.findViewById<View>(R.id.btn_show_actions)
    private val actionListButton get() = menuView.findViewById<View>(R.id.btn_action_list)
    private var isPlaying by mutableStateOf(false)

    /**
     * Tells if this service has handled onKeyEvent with ACTION_DOWN for a key in order to return
     * the correct value when ACTION_UP is received.
     */
    private var keyDownHandled: Boolean = false

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isPlaying.collect(::updateMenuPlayingState) }
                launch { viewModel.canPlay.collect(::updatePlayPauseButtonEnabledState) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        val buttons = listOf(
            OverlayMenuButton(R.id.btn_play, R.drawable.ic_play_arrow, R.string.content_desc_play_pause_scenario),
            OverlayMenuButton(R.id.btn_stop, R.drawable.ic_stop, R.string.content_desc_stop_clicker),
            OverlayMenuButton(R.id.btn_show_actions, R.drawable.ic_show_path, R.string.content_desc_show_actions),
            OverlayMenuButton(R.id.btn_action_list, R.drawable.ic_settings_filled, R.string.content_desc_open_action_list),
            OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
        )
        menuView = createOverlayMenuLayout(context, buttons, buttonViewFactory = { button ->
            OverlayButtonFrameLayout(context).apply {
                addView(TouchTransparentComposeView(context) {
                    MacrionTheme {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (button.id == R.id.btn_play) {
                                AnimatedContent(targetState = isPlaying, label = "playPause") { playing ->
                                    Icon(
                                        painter = painterResource(
                                            if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = colorResource(
                                            io.github.vibhor1102.macrion.core.ui.R.color.overlayMenuButtons,
                                        ),
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(button.icon),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    tint = colorResource(
                                        io.github.vibhor1102.macrion.core.ui.R.color.overlayMenuButtons,
                                    ),
                                )
                            }
                        }
                    }
                }.apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                })
            }
        })

        return menuView
    }

    /**
     * Compose renders the button, while its containing Android view remains the interaction
     * surface expected by [OverlayMenu]. The renderer must decline touch events so the parent can
     * retain the original click debouncing, resize guard, enabled state, and move handling.
     */
    private class TouchTransparentComposeView(
        context: android.content.Context,
        private val content: @Composable () -> Unit,
    ) : AbstractComposeView(context) {
        @Composable
        override fun Content() = content()

        override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
    }

    /**
     * Behaves like the leaf ImageButton used before migration: this direct menu item owns the
     * complete pointer sequence, and its renderer never becomes a touch target.
     */
    private class OverlayButtonFrameLayout(context: android.content.Context) : FrameLayout(context) {
        override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
    }

    override fun getWindowMaximumSize(backgroundView: ViewGroup): Size {
        val buttonContainer = backgroundView.findViewById<ViewGroup>(
            io.github.vibhor1102.macrion.core.common.overlays.R.id.menu_items,
        )
        val contentWidth = (0 until buttonContainer.childCount).maxOfOrNull { index ->
            buttonContainer.getChildAt(index).layoutParams.width
        } ?: 0
        val contentHeight = (0 until buttonContainer.childCount).sumOf { index ->
            buttonContainer.getChildAt(index).layoutParams.height
        }
        return Size(
            contentWidth + buttonContainer.paddingLeft + buttonContainer.paddingRight,
            contentHeight + buttonContainer.paddingTop + buttonContainer.paddingBottom,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopEdition()
    }

    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false

        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> {
                if (viewModel.stopScenarioPlay()) {
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

    /** Refresh the play menu item according to the scenario state. */
    private fun updatePlayPauseButtonEnabledState(canStartDetection: Boolean) {
        setMenuItemViewEnabled(playButton, canStartDetection)
    }

    private fun updateMenuPlayingState(isPlaying: Boolean) {
        val currentState = playButton.tag
        if (currentState == isPlaying) return

        playButton.tag = isPlaying
        this.isPlaying = isPlaying
        if (isPlaying) {
            if (currentState == null) {
                setMenuItemVisibility(stopButton, false)
                setMenuItemVisibility(showActionsButton, false)
                setMenuItemVisibility(actionListButton, false)
            } else {
                animateLayoutChanges {
                    setMenuItemVisibility(stopButton, false)
                    setMenuItemVisibility(showActionsButton, false)
                    setMenuItemVisibility(actionListButton, false)
                }
            }
        } else {
            if (currentState != null) {
                animateLayoutChanges {
                    setMenuItemVisibility(stopButton, true)
                    setMenuItemVisibility(showActionsButton, true)
                    setMenuItemVisibility(actionListButton, true)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_play -> onPlayPauseClicked()
            R.id.btn_stop -> onStopButtonClicked()
            R.id.btn_show_actions -> onShowBriefClicked()
            R.id.btn_action_list -> onDumbScenarioConfigClicked()
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

    private fun onPlayPauseClicked() {
        if (viewModel.shouldShowStopVolumeDownTutorialDialog()) {
            showStopVolumeDownTutorialDialog()
            return
        }

        viewModel.toggleScenarioPlay()
    }

    private fun onShowBriefClicked() {
        viewModel.startEdition(dumbScenarioId) {
            overlayManager.navigateTo(
                context = context,
                newOverlay = DumbScenarioBriefMenu(
                    onConfigSaved = viewModel::saveEditions
                ),
                hideCurrent = true,
            )
        }
    }

    private fun onDumbScenarioConfigClicked() {
        viewModel.startEdition(dumbScenarioId) {
            overlayManager.navigateTo(
                context = context,
                newOverlay = DumbScenarioDialog(
                    onConfigSaved = viewModel::saveEditions,
                    onConfigDiscarded = viewModel::stopEdition,
                ),
                hideCurrent = true,
            )
        }
    }

    private fun showStopVolumeDownTutorialDialog() {
        tutorialNavigator.showTipDialog(context, Tip.STOP_WITH_VOLUME_DOWN) {
            viewModel.toggleScenarioPlay()
        }
    }
}
