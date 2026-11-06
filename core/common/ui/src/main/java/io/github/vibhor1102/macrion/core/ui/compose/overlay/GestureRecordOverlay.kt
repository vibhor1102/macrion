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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture

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
    swipeMinDistancePx: Float = SWIPE_MIN_DISTANCE_PX,
    borderColor: Color = colorResource(R.color.overlayGestureRecorder),
    borderThicknessPx: Float = dimensionResource(R.dimen.overlay_gesture_recorder_thickness).value,
) {
    val fittingTolerancePx = with(LocalDensity.current) { 4.dp.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRecording, swipeMinDistancePx) {
                if (!isRecording) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val session = GestureRecordingSession(
                        startTime = down.uptimeMillis,
                        width = displayConfig.sizePx.x.toFloat(),
                        height = displayConfig.sizePx.y.toFloat(),
                        swipeMinDistancePx = swipeMinDistancePx,
                        fittingTolerancePx = fittingTolerancePx,
                    )
                    session.update(down.id.value, down.position.x, down.position.y, down.uptimeMillis, true)
                    down.consume()
                    onGestureCaptured(session.gesture(finished = false), false)

                    while (true) {
                        // AwaitPointerEventScope's timeout also fires while a finger is held motionless.
                        val event = withTimeoutOrNull(session.remainingTime(android.os.SystemClock.uptimeMillis())) {
                            awaitPointerEvent()
                        }
                        if (event == null || event.changes.any { session.isExpired(it.uptimeMillis) }) {
                            session.finishAtDeadline()
                            onGestureCaptured(session.gesture(finished = true), true)
                            break
                        }
                        val accepted = event.changes.map { change ->
                            val result = session.update(
                                change.id.value, change.position.x, change.position.y,
                                change.uptimeMillis, change.pressed,
                            )
                            change.consume()
                            result
                        }.all { it }
                        // Never save a partial gesture when the platform stroke limit is exceeded.
                        if (!accepted) {
                            onGestureCaptured(null, true)
                            break
                        }
                        val finished = event.changes.none { it.pressed }
                        onGestureCaptured(session.gesture(finished), finished)
                        if (finished) break
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
