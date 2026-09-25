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

import androidx.compose.ui.input.pointer.PointerId

private const val SWIPE_MIN_DISTANCE_PX = 40f
private const val MAX_RECORDING_POINTERS = 10

private data class PointerTrack(
    val id: PointerId,
    val origin: PointF,
    val downTime: Long,
    var current: PointF,
    var lastUptime: Long,
    var isUp: Boolean = false,
)

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
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRecording, swipeMinDistancePx) {
                if (!isRecording) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val sessionStartTime = down.uptimeMillis
                    val tracks = mutableMapOf<PointerId, PointerTrack>()
                    val firstOrigin = PointF(down.position.x, down.position.y)
                    tracks[down.id] = PointerTrack(
                        id = down.id,
                        origin = firstOrigin,
                        downTime = down.uptimeMillis,
                        current = firstOrigin,
                        lastUptime = down.uptimeMillis,
                    )
                    down.consume()
                    onGestureCaptured(RecordedGesture.Click(firstOrigin, 1L), false)

                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            val track = tracks[change.id]
                            if (track != null && !track.isUp) {
                                track.current = PointF(change.position.x, change.position.y)
                                track.lastUptime = change.uptimeMillis
                                if (!change.pressed) {
                                    track.isUp = true
                                }
                            } else if (track == null && change.pressed && tracks.size < MAX_RECORDING_POINTERS) {
                                val origin = PointF(change.position.x, change.position.y)
                                tracks[change.id] = PointerTrack(
                                    id = change.id,
                                    origin = origin,
                                    downTime = change.uptimeMillis,
                                    current = origin,
                                    lastUptime = change.uptimeMillis,
                                )
                            }
                            change.consume()
                        }

                        // Never save a partial gesture when the platform stroke limit is exceeded.
                        if (event.changes.any { it.pressed && it.id !in tracks }) {
                            onGestureCaptured(null, true)
                            break
                        }
                        val allPointersUp = !event.changes.any { it.pressed }

                        val subGestures = tracks.values.map { t ->
                            val dist = hypot(t.origin.x - t.current.x, t.origin.y - t.current.y)
                            val dur = (t.lastUptime - t.downTime).coerceAtLeast(1L)
                            if (dist <= swipeMinDistancePx) {
                                RecordedGesture.Click(t.origin, dur, t.downTime - sessionStartTime)
                            } else {
                                RecordedGesture.Swipe(t.origin, t.current, dur, t.downTime - sessionStartTime)
                            }
                        }

                        val maxUptime = tracks.values.maxOfOrNull { it.lastUptime } ?: sessionStartTime
                        val totalDuration = (maxUptime - sessionStartTime).coerceAtLeast(1L)

                        val finalGesture: RecordedGesture = if (subGestures.size == 1) {
                            subGestures[0]
                        } else {
                            RecordedGesture.Split(subGestures = subGestures, durationMs = totalDuration)
                        }

                        if (allPointersUp) {
                            onGestureCaptured(finalGesture, true)
                            break
                        } else {
                            onGestureCaptured(finalGesture, false)
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
