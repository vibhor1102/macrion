/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.compose.overlay

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource

import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture

import kotlin.math.hypot

private const val SWIPE_MIN_DISTANCE_PX = 40f

/**
 * Pure Compose overlay that intercepts touch gestures for recording and draws the display border.
 */
@Composable
fun GestureRecordOverlay(
    displayConfig: DisplayConfig,
    isRecording: Boolean,
    onGestureCaptured: (gesture: RecordedGesture?, isFinished: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = colorResource(R.color.overlayGestureRecorder),
    borderThicknessPx: Float = dimensionResource(R.dimen.overlay_gesture_recorder_thickness).value,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRecording) {
                if (!isRecording) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = down.uptimeMillis
                    val origin = PointF(down.position.x, down.position.y)
                    onGestureCaptured(RecordedGesture.Click(origin, 1L), false)

                    var isFinished = false
                    while (!isFinished) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            onGestureCaptured(null, false)
                            break
                        }
                        val change = event.changes.firstOrNull() ?: break
                        val durationMs = (change.uptimeMillis - downTime).coerceAtLeast(1L)
                        val currentPos = PointF(change.position.x, change.position.y)
                        val distance = hypot(origin.x - currentPos.x, origin.y - currentPos.y)

                        if (change.pressed) {
                            val gesture = if (distance <= SWIPE_MIN_DISTANCE_PX) {
                                RecordedGesture.Click(origin, durationMs)
                            } else {
                                RecordedGesture.Swipe(origin, currentPos, durationMs)
                            }
                            onGestureCaptured(gesture, false)
                        } else {
                            isFinished = true
                            val gesture = if (distance <= SWIPE_MIN_DISTANCE_PX) {
                                RecordedGesture.Click(origin, durationMs)
                            } else {
                                RecordedGesture.Swipe(origin, currentPos, durationMs)
                            }
                            onGestureCaptured(gesture, true)
                        }
                    }
                }
            },
    ) {
        if (isRecording) {
            drawDisplayBorder(
                displayConfig = displayConfig,
                color = borderColor,
                thicknessPx = borderThicknessPx,
            )
        }
    }
}
