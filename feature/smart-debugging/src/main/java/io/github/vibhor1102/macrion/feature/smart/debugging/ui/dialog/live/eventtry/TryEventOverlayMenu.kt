/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.eventtry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.base.isStopScenarioKey
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.di.DebuggingViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.createDebugOverlayMenu
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.uistate.EventResultUiState
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.view.DebugOverlayView
import kotlinx.coroutines.launch

class TryEventOverlayMenu(
    private val scenario: Scenario,
    private val triedElement: ScreenEvent,
) : OverlayMenu() {
    private val viewModel: TryElementViewModel by viewModels(
        entryPoint = DebuggingViewModelsEntryPoint::class.java,
        creator = { tryElementViewModel() },
    )
    private var result by mutableStateOf<EventResultUiState?>(null)

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        createDebugOverlayMenu(context, contentWidthDp = 200, contentHeightDp = 152) { ResultPanel(result) }
    override fun onCreateOverlayView(): View = DebugOverlayView(context)

    override fun onStart() {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.displayResults.collect { state ->
                result = state
                (screenOverlayView as? DebugOverlayView)?.setResults(state?.detectionResults ?: emptyList())
            }
        } }
        viewModel.startTry(context, scenario, triedElement)
    }
    override fun onStop() = viewModel.stopTry()
    override fun onMenuItemClicked(viewId: Int) {
        if (viewId == R.id.btn_back) { viewModel.stopTry(); back() }
    }
    override fun onKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!keyEvent.isStopScenarioKey()) return false
        if (keyEvent.action == KeyEvent.ACTION_DOWN) { viewModel.stopTry(); back() }
        return true
    }

    @Composable private fun ResultPanel(state: EventResultUiState?) {
        val textColor = colorResource(R.color.overlayViewPrimary)
        val iconColor = colorResource(R.color.overlayMenuButtons)
        val dividerColor = textColor.copy(alpha = 19f / 255f)
        val textStyle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = true),
        )
        Box(Modifier.width(200.dp).height(152.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(2.dp)
                    .height(140.dp)
                    .background(dividerColor),
            )
            Column(Modifier.fillMaxWidth().height(140.dp).align(Alignment.Center)) {
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state?.let { Icon(painterResource(it.eventIcon), null, Modifier.size(24.dp), tint = iconColor) }
                    Text(
                        state?.eventName.orEmpty(),
                        Modifier.weight(1f).padding(start = 8.dp).basicMarquee(),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        style = textStyle,
                    )
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state?.eventConditionOperator.orEmpty(),
                        Modifier.weight(1f).padding(start = 12.dp),
                        color = textColor,
                        style = textStyle,
                    )
                    Spacer(Modifier.fillMaxHeight().width(2.dp).background(dividerColor))
                    Row(Modifier.weight(1f).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (state != null) Icon(
                            painterResource(R.drawable.ic_duration),
                            null,
                            Modifier.size(20.dp),
                            tint = iconColor,
                        )
                        Text(
                            state?.eventDuration.orEmpty(),
                            Modifier.padding(start = 4.dp),
                            color = textColor,
                            maxLines = 1,
                            style = textStyle,
                        )
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state?.actions?.forEach { action ->
                        Icon(
                            painterResource(action.icon),
                            null,
                            Modifier.width(32.dp).height(24.dp).padding(horizontal = 4.dp),
                            tint = textColor,
                        )
                    }
                }
            }
        }
    }
}
