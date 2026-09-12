/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.conditiontry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.color.MaterialColors
import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.createDebugOverlayMenu
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.view.DebugOverlayView
import kotlinx.coroutines.launch

class TryImageConditionOverlayMenu(
    private val scenario: Scenario,
    private val imageCondition: ScreenCondition,
    private val onNewThresholdSelected: (Int) -> Unit,
) : OverlayMenu() {
    private val viewModel: TryImageConditionViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { tryImageConditionViewModel() },
    )
    private var result by mutableStateOf<ScreenConditionResultUiState?>(null)
    private var thresholdText by mutableStateOf("")

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        createDebugOverlayMenu(context, contentWidthDp = 287, contentHeightDp = 152) { ResultPanel() }
    override fun onCreateOverlayView(): View = DebugOverlayView(context)

    override fun getMenuWindowWidth(): Int =
        (343 * context.resources.displayMetrics.density).toInt()

    override fun onStart() {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.displayResults.collect { state ->
                result = state
                (screenOverlayView as? DebugOverlayView)?.setResults(state?.let(::listOf) ?: emptyList())
            } }
            launch { viewModel.thresholdText.collect { thresholdText = it } }
        } }
        viewModel.startTry(context, scenario, imageCondition)
    }
    override fun onStop() { viewModel.stopTry(); onNewThresholdSelected(viewModel.getSelectedThreshold()) }
    override fun onMenuItemClicked(viewId: Int) {
        if (viewId == R.id.btn_back) { viewModel.stopTry(); back() }
    }
    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false
        if (keyEvent.action == KeyEvent.ACTION_DOWN) { viewModel.stopTry(); back() }
        return true
    }

    @Composable private fun ResultPanel() {
        val textColor = colorResource(R.color.textTitle)
        val controlColor = colorResource(R.color.overlayMenuButtons)
        val dividerColor = Color(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutlineVariant,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        Column(Modifier.width(287.dp).height(152.dp).padding(start = 8.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                ResultValue(context.getString(R.string.overlay_title_results), result?.resultText.orEmpty(), Modifier.weight(1f))
                    Box(Modifier.width(1.dp))
                ResultValue(context.getString(R.string.overlay_title_threshold), thresholdText, Modifier.weight(1f))
                }
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .width(1.dp)
                        .background(dividerColor),
                )
            }
            Box(Modifier.fillMaxWidth().height(52.dp).padding(top = 4.dp)) {
                Slider(
                    value = viewModel.getSelectedThreshold().toFloat(),
                    onValueChange = { viewModel.setThreshold(it.toInt()) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    valueRange = MIN_THRESHOLD..MAX_THRESHOLD,
                    steps = (MAX_THRESHOLD - MIN_THRESHOLD - 1).toInt(),
                    colors = SliderDefaults.colors(
                        thumbColor = controlColor,
                        activeTrackColor = controlColor,
                        inactiveTrackColor = controlColor.copy(alpha = 0.24f),
                    ),
                )
            }
        }
    }

    @Composable private fun ResultValue(title: String, value: String, modifier: Modifier) {
        val color = colorResource(R.color.textTitle)
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                ),
            )
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    value,
                    color = color,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 26.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                    ),
                )
            }
        }
    }
}
