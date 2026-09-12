/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selection

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialChoiceList

class ScreenConditionTypeSelectionDialog(
    private val choices: List<ScreenConditionTypeChoice>,
    private val onChoiceSelectedListener: (ScreenConditionTypeChoice) -> Unit,
    private val onCancelledListener: (() -> Unit)? = null,
) : OverlayDialog(R.style.AppTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SCREEN_CONDITION_TYPE_SELECTION.name

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme {
            TutorialChoiceList(
                title = R.string.dialog_title_screen_condition_type,
                choices = choices,
                onDismiss = ::cancel,
                onChoiceSelected = { back(); onChoiceSelectedListener(it) },
                monitoredType = { choice ->
                    when (choice) {
                        ScreenConditionTypeChoice.OnColorDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_COLOR
                        ScreenConditionTypeChoice.OnImageDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_IMAGE
                        ScreenConditionTypeChoice.OnNumberDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_NUMBER
                        ScreenConditionTypeChoice.OnTextDetected -> MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_TEXT
                    }
                },
            )
        } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit
    private fun cancel() { onCancelledListener?.invoke(); back() }
}
