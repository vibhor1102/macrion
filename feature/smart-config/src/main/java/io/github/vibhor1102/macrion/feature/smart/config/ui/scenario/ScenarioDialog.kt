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
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario

import android.util.Log
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.DialogNavigationButton
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.DialogNavigationItem
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ScenarioConfigContent
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents.ImageEventListContent
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.more.MoreContent
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.triggerevents.TriggerEventListContent

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor

class ScenarioDialog(
    private val onConfigSaved: () -> Unit,
    private val onConfigDiscarded: () -> Unit,
) : NavBarDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SCENARIO.name

    /** The view model for this dialog. */
    private val viewModel: ScenarioDialogViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioDialogViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        return super.onCreateView().also {
            topBarBinding.setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
            topBarBinding.setTitle(R.string.dialog_title_scenario_config)
            topBarBinding.setButtonModifier(DialogNavigationButton.SAVE) {
                Modifier.tutorialAnchor(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    onClick = { topBarBinding.performButtonClick(DialogNavigationButton.SAVE) },
                )
            }
            floatingActionButtons.primaryModifier = {
                Modifier.tutorialAnchor(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    onClick = { floatingActionButtons.performPrimaryClick() },
                )
            }
        }
    }

    @Composable
    override fun navigationItemModifier(item: DialogNavigationItem): Modifier {
        return if (item.id == R.id.page_trigger_events) {
            Modifier.tutorialAnchor(
                MonitoredViewType.SCENARIO_DIALOG_TRIGGER_EVENT_TAB,
                onClick = { selectNavigationItem(item.id) },
            )
        } else {
            Modifier
        }
    }

    override fun navigationItems(): List<DialogNavigationItem> = listOf(
        DialogNavigationItem(R.id.page_image_events, R.drawable.ic_screen_event, R.string.menu_item_title_image_events),
        DialogNavigationItem(R.id.page_trigger_events, R.drawable.ic_trigger_event, R.string.menu_item_title_trigger_events),
        DialogNavigationItem(R.id.page_config, R.drawable.ic_settings, R.string.generic_config),
        DialogNavigationItem(R.id.page_more, R.drawable.ic_more, R.string.menu_item_title_more),
    )

    override fun onCreateContent(navItemId: Int): NavBarDialogContent = when (navItemId) {
        R.id.page_image_events -> ImageEventListContent(context.applicationContext)
        R.id.page_trigger_events -> TriggerEventListContent(context.applicationContext)
        R.id.page_config -> ScenarioConfigContent(context.applicationContext)
        R.id.page_more -> MoreContent(context.applicationContext)
        else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingScenario.collect(::onScenarioEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.navItemsValidity.collect(::updateContentsValidity) }
                launch { viewModel.scenarioCanBeSaved.collect(::updateSaveButtonState) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        when (buttonType) {
            DialogNavigationButton.SAVE -> {
                onConfigSaved()
                super.back()
            }

            DialogNavigationButton.DISMISS -> {
                back()
                return
            }

            DialogNavigationButton.DELETE -> Unit
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                onConfigDiscarded()
                super.back()
            }
            return
        }

        onConfigDiscarded()
        super.back()
    }

    private fun updateContentsValidity(itemsValidity: Map<Int, Boolean>) {
        itemsValidity.forEach { (itemId, isValid) ->
            setMissingInputBadge(itemId, !isValid)
        }
    }

    private fun updateSaveButtonState(isEnabled: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }

    private fun onScenarioEditingStateChanged(isEditingScenario: Boolean) {
        if (!isEditingScenario) {
            Log.e(TAG, "Closing ScenarioDialog because there is no scenario edited")
            finish()
        }
    }
}

private const val TAG = "ScenarioDialog"
