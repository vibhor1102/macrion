/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.selection

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialChoiceList

class ActionTypeSelectionDialog(
    private val choices: List<ActionTypeChoice>,
    private val onChoiceSelectedListener: (ActionTypeChoice) -> Unit,
    private val onCancelledListener: (() -> Unit)? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.ACTION_TYPE_SELECTION.name

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme {
            TutorialChoiceList(
                title = R.string.dialog_title_action_type,
                choices = choices,
                onDismiss = ::cancel,
                onChoiceSelected = { back(); onChoiceSelectedListener(it) },
                monitoredType = { choice ->
                    when (choice) {
                        ActionTypeChoice.Click -> MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION
                        ActionTypeChoice.ToggleEvent -> MonitoredViewType.ACTION_TYPE_DIALOG_TOGGLE_EVENT_ACTION
                        ActionTypeChoice.ChangeCounter -> MonitoredViewType.ACTION_TYPE_DIALOG_COUNTER_ACTION
                        else -> null
                    }
                },
            )
        } }
    }
private fun cancel() { onCancelledListener?.invoke(); back() }
}
