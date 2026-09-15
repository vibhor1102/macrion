/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.areaselector

import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.createValidationOverlayToolbar
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selector.SelectorController
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selector.SelectorOverlay

import kotlinx.coroutines.launch
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType

class ConditionAreaSelectorMenu(
    private val onHelpClicked: (() -> Unit)? = null,
    private val onAreaSelected: (Rect) -> Unit
) : OverlayMenu(theme = R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CONDITION_AREA_SELECTOR_MENU.name

    /** The view model for this dialog. */
    private val viewModel: ConditionAreaSelectorViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionAreaSelectorViewModel() },
    )

    /** The Compose selector preserves the physical-screen coordinates used by conditions. */
    private val selectorController = SelectorController(hasCapture = false)

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        val menuView = createValidationOverlayToolbar(context)

        if (onHelpClicked == null) {
            menuView.findOverlayView<View>(R.id.btn_help).visibility = View.GONE
        }

        return menuView
    }

    override fun onCreateOverlayView(): View = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { SelectorOverlay(selectorController) } }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.initialArea.collect { selectorState ->
                    selectorController.setAreaSelection(selectorState.initialArea, selectorState.minimalArea)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
            R.id.btn_help -> onHelpClicked?.invoke()
        }
    }

    /** Called when the user press the confirmation button. */
    private fun onConfirm() {
        onAreaSelected(selectorController.getAreaSelection())
        back()
    }

    /** Called when the user press the cancel button. */
    private fun onCancel() {
        back()
    }
}
