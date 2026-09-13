/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.activities

import android.content.ComponentName
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class ActivitySelectionDialog(
    private val onApplicationSelected: (ComponentName) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.ACTIVITY_SELECTION.name

    private val viewModel: ActivitySelectionModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { activitySelectionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ActivitySelectionDialog.Content() } }
    }

@Composable
    private fun Content() {
        val applications by viewModel.activities.collectAsStateWithLifecycle(initialValue = null)
        ApplicationSelectionContent(
            title = context.getString(R.string.dialog_title_application_selection),
            applications = applications,
            onDismiss = ::back,
            onSelected = { application ->
                debounceUserInteraction {
                    onApplicationSelected(application.componentName)
                    back()
                }
            },
        )
    }
}
