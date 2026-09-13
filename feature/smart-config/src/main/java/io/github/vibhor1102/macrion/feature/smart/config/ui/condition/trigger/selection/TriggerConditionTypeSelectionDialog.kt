/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.selection

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialChoiceList

class TriggerConditionTypeSelectionDialog(
    private val choices: List<TriggerConditionTypeChoice>,
    private val onChoiceSelectedListener: (TriggerConditionTypeChoice) -> Unit,
    private val onCancelledListener: (() -> Unit)? = null,
) : OverlayDialog(R.style.AppTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TRIGGER_CONDITION_TYPE_SELECTION.name

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme {
            TutorialChoiceList(
                title = R.string.dialog_title_trigger_condition_type,
                choices = choices,
                onDismiss = ::cancel,
                onChoiceSelected = { back(); onChoiceSelectedListener(it) },
                monitoredType = { choice ->
                    when (choice) {
                        TriggerConditionTypeChoice.OnBroadcastReceived -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_BROADCAST_RECEIVED
                        TriggerConditionTypeChoice.OnCounterReached -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_COUNTER_REACHED
                        TriggerConditionTypeChoice.OnTimerReached -> MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_TIMER_REACHED
                    }
                },
            )
        } }
    }
private fun cancel() { onCancelledListener?.invoke(); back() }
}
