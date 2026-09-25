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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.base.gesture.fitSwipePath

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
    val points: MutableList<SwipePoint> = mutableListOf(SwipePoint(origin)),
    var traveledDistance: Float = 0f,
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
    val fittingTolerancePx = with(LocalDensity.current) { 4.dp.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRecording, swipeMinDistancePx) {
                if (!isRecording) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val sessionStartTime = down.uptimeMillis
                    val tracks = mutableMapOf<PointerId, PointerTrack>()
                    val width = displayConfig.sizePx.x.toFloat()
                    val height = displayConfig.sizePx.y.toFloat()
                    fun clamped(x: Float, y: Float) = PointF(x.coerceIn(0f, width), y.coerceIn(0f, height))
                    val firstOrigin = clamped(down.position.x, down.position.y)
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
                                val next = clamped(change.position.x, change.position.y)
                                val moved = hypot(track.current.x - next.x, track.current.y - next.y)
                                track.traveledDistance += moved
                                if (moved >= 1f || !change.pressed) track.points.add(SwipePoint(next))
                                track.current = next
                                track.lastUptime = change.uptimeMillis
                                if (!change.pressed) {
                                    track.isUp = true
                                }
                            } else if (track == null && change.pressed && tracks.size < MAX_RECORDING_POINTERS) {
                                val origin = clamped(change.position.x, change.position.y)
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
                            val dur = (t.lastUptime - t.downTime).coerceAtLeast(1L)
                            if (t.traveledDistance <= swipeMinDistancePx) {
                                RecordedGesture.Click(t.origin, dur, t.downTime - sessionStartTime)
                            } else {
                                RecordedGesture.Swipe(
                                    t.origin, t.current, dur, t.downTime - sessionStartTime,
                                    path = if (allPointersUp) fitSwipePath(t.points, fittingTolerancePx) else null,
                                )
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
