/*
 * Copyright (C) 2025 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.DialogNavigationItem
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.DialogNavigationButton
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.overview.DebugReportOverviewContent
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineContent
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.ConditionPerformanceContent

import android.app.Dialog
import kotlinx.coroutines.launch


/** Displays the content of the current debug report. */
class DebugReportDialog : NavBarDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DebugReportViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { debugReportViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        return super.onCreateView().also {
            topBarBinding.apply {
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setTitle(R.string.dialog_overlay_title_debug_report)
            }
        }
    }

    override fun navigationItems(): List<DialogNavigationItem> = listOf(
        DialogNavigationItem(R.id.page_overview, R.drawable.ic_debug_overview, R.string.menu_item_debug_report_overview),
        DialogNavigationItem(R.id.page_conditions, R.drawable.ic_debug_conditions, R.string.menu_item_debug_report_conditions),
        DialogNavigationItem(R.id.page_timeline, R.drawable.ic_debug_timeline, R.string.menu_item_debug_report_timeline),
    )

    override fun onCreateContent(navItemId: Int): NavBarDialogContent = when (navItemId) {
        R.id.page_overview -> DebugReportOverviewContent(context.applicationContext)
        R.id.page_conditions -> ConditionPerformanceContent(context.applicationContext)
        R.id.page_timeline -> DebugReportTimelineContent(context.applicationContext)
        else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: Dialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isDebugReportAvailable.collect(::updateReportAvailability) }
            }
        }
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        when (buttonType) {
            DialogNavigationButton.DISMISS -> {
                back()
                return
            }

            DialogNavigationButton.SAVE -> Unit
            DialogNavigationButton.DELETE -> Unit
        }
    }

    private fun updateReportAvailability(isAvailable: Boolean) {
        if (!isAvailable) back()
    }
}
