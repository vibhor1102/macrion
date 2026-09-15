package io.github.vibhor1102.macrion.feature.smart.debugging.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.Rect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AbstractComposeView

import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState

/** Compose renderer for the condition-detection outlines shown in testing overlays. */
internal class DebugOverlayView(context: Context) : AbstractComposeView(context) {

    private var displayedResults by mutableStateOf(emptyList<DisplayedResult>())

    fun setResults(newResults: List<ScreenConditionResultUiState>) {
        displayedResults = newResults.mapNotNull { result ->
            if (!result.positive && (result.coordinates.width() == 0 || result.coordinates.height() == 0)) {
                return@mapNotNull null
            }
            DisplayedResult(
                positive = result.positive,
                bounds = Rect(
                    result.coordinates.left - CONDITION_BORDERS_MARGIN_PX,
                    result.coordinates.top - CONDITION_BORDERS_MARGIN_PX,
                    result.coordinates.right + CONDITION_BORDERS_MARGIN_PX,
                    result.coordinates.bottom + CONDITION_BORDERS_MARGIN_PX,
                ),
            )
        }
    }

    fun clear() {
        displayedResults = emptyList()
    }

    @Composable
    override fun Content() {
        val results = displayedResults
        Canvas(Modifier.fillMaxSize()) {
            results.forEach { result ->
                val bounds = result.bounds
                drawRect(
                    color = if (result.positive) ComposeColor(Color.GREEN) else ComposeColor(Color.RED),
                    topLeft = Offset(bounds.left.toFloat(), bounds.top.toFloat()),
                    size = Size(bounds.width().toFloat(), bounds.height().toFloat()),
                    style = Stroke(width = OUTLINE_WIDTH_PX),
                )
            }
        }
    }

    private data class DisplayedResult(val positive: Boolean, val bounds: Rect)

    private companion object {
        const val CONDITION_BORDERS_MARGIN_PX = 20
        const val OUTLINE_WIDTH_PX = 10f
    }
}
