/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.overlay.ItemBriefCanvas
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription

internal class PositionSelectorViews(
    context: Context,
    private val displayConfig: DisplayConfig,
) {
    val root: ComposeView = ComposeView(context)

    var onTouchListener: ((position: PointF) -> Unit)? = null

    private var currentDescription by mutableStateOf<ItemBriefDescription?>(null)
    private var instructionText by mutableIntStateOf(R.string.toast_configure_single_click)
    private var isInstructionsVisible by mutableStateOf(true)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideInstructionsRunnable = Runnable { isInstructionsVisible = false }

    companion object {
        private const val AUTO_HIDE_DELAY_MS = 3_000L
    }

    init {
        val safeInsetTopDp = (displayConfig.safeInsetTopPx / context.resources.displayMetrics.density).dp

        root.setContent {
            MacrionTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onTouchListener?.invoke(PointF(offset.x, offset.y))
                            }
                        },
                ) {
                    ItemBriefCanvas(
                        description = currentDescription,
                        displayConfig = displayConfig,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AnimatedVisibility(
                        visible = isInstructionsVisible,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        InstructionsBanner(
                            safeInsetTopDp = safeInsetTopDp,
                            instructionText = instructionText,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun InstructionsBanner(safeInsetTopDp: androidx.compose.ui.unit.Dp, instructionText: Int) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        0.7f to Color.Black.copy(alpha = 0.53f),
                        1f to Color.Transparent,
                    ),
                )
                .padding(
                    start = 32.dp,
                    top = safeInsetTopDp + 4.dp,
                    end = 32.dp,
                    bottom = 32.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(instructionText),
                color = colorResource(R.color.md_theme_light_onPrimary),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    fun setDescription(description: ItemBriefDescription?) {
        currentDescription = description
    }

    fun setInstruction(@StringRes text: Int) {
        instructionText = text
    }

    fun showOrResetInstructionsTimer() {
        isInstructionsVisible = true
        mainHandler.removeCallbacks(hideInstructionsRunnable)
        mainHandler.postDelayed(hideInstructionsRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun hideInstructions() {
        mainHandler.removeCallbacks(hideInstructionsRunnable)
        isInstructionsVisible = false
    }

    fun dispose() {
        mainHandler.removeCallbacksAndMessages(null)
    }
}
