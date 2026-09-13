/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import sh.calvin.reorderable.DragGestureDetector

/**
 * A drag gesture detector that initiates drag EITHER immediately when movement passes touch slop,
 * OR after holding still for [HOLD_TIMEOUT_MS] (350ms).
 */
internal object DualDragGestureDetector : DragGestureDetector {
    private const val HOLD_TIMEOUT_MS = 350L

    override suspend fun PointerInputScope.detect(
        onDragStart: (Offset) -> Unit,
        onDragEnd: () -> Unit,
        onDragCancel: () -> Unit,
        onDrag: (PointerInputChange, Offset) -> Unit,
    ) {
        awaitEachGesture {
            try {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragStarted = false
                var pointerWentUp = false
                var activePointerId = down.id

                withTimeoutOrNull(HOLD_TIMEOUT_MS) {
                    val change = awaitTouchSlopOrCancellation(down.id) { slopChange, over ->
                        slopChange.consume()
                        onDragStart(slopChange.position)
                        onDrag(slopChange, over)
                        dragStarted = true
                        activePointerId = slopChange.id
                    }
                    if (change == null) {
                        pointerWentUp = true
                    }
                }

                if (!dragStarted && !pointerWentUp) {
                    val currentDown = currentEvent.changes.firstOrNull { it.id == down.id && it.pressed }
                    if (currentDown != null) {
                        onDragStart(currentDown.position)
                        dragStarted = true
                        activePointerId = currentDown.id
                    }
                }

                if (dragStarted) {
                    val completed = drag(activePointerId) { change ->
                        onDrag(change, change.positionChange())
                        change.consume()
                    }
                    if (completed) {
                        currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
                        onDragEnd()
                    } else {
                        onDragCancel()
                    }
                }
            } catch (c: CancellationException) {
                onDragCancel()
                throw c
            }
        }
    }
}
