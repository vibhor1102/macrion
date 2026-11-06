package io.github.vibhor1102.macrion.core.ui.compose.overlay

import android.graphics.PointF
import io.github.vibhor1102.macrion.core.base.gesture.MAX_TOUCH_DURATION_MS
import io.github.vibhor1102.macrion.core.base.gesture.MAX_TOUCH_STROKES
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.base.gesture.fitSwipePath
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture

/** One bounded recording. Completed contacts remain separate even if a pointer id is reused. */
internal class GestureRecordingSession(
    private val startTime: Long,
    private val width: Float,
    private val height: Float,
    private val swipeMinDistancePx: Float,
    private val fittingTolerancePx: Float,
) {
    private data class Stroke(
        val start: Long,
        var end: Long,
        val points: MutableList<SwipePoint>,
        var distance: Float = 0f,
        var current: SwipePoint = points.last(),
    )

    private val strokes = mutableListOf<Stroke>()
    private val active = mutableMapOf<Long, Stroke>()

    fun isExpired(uptime: Long): Boolean = uptime - startTime >= MAX_TOUCH_DURATION_MS
    fun remainingTime(uptime: Long): Long = (MAX_TOUCH_DURATION_MS - (uptime - startTime)).coerceAtLeast(1L)

    fun update(id: Long, x: Float, y: Float, uptime: Long, pressed: Boolean): Boolean {
        if (isExpired(uptime)) return false
        val position = SwipePoint(x.coerceIn(0f, width), y.coerceIn(0f, height))
        val stroke = active[id]
        if (stroke == null) {
            if (!pressed) return true
            if (strokes.size == MAX_TOUCH_STROKES) return false
            val newStroke = Stroke(uptime, uptime, mutableListOf(position))
            strokes.add(newStroke)
            active[id] = newStroke
        } else {
            val moved = stroke.current.distanceTo(position)
            stroke.distance += moved
            if (moved >= 1f || !pressed) stroke.points.add(position)
            stroke.current = position
            stroke.end = uptime
            if (!pressed) active.remove(id)
        }
        return true
    }

    fun finishAtDeadline() {
        active.values.forEach { it.end = startTime + MAX_TOUCH_DURATION_MS; if (it.points.last() != it.current) it.points.add(it.current) }
        active.clear()
    }

    fun gesture(finished: Boolean): RecordedGesture {
        val gestures = strokes.map { stroke ->
            val duration = (stroke.end - stroke.start).coerceAtLeast(1L)
            val offset = stroke.start - startTime
            val first = stroke.points.first().let { PointF(it.x, it.y) }
            if (stroke.distance <= swipeMinDistancePx) {
                RecordedGesture.Click(first, duration, offset)
            } else {
                RecordedGesture.Swipe(
                    first, stroke.current.let { PointF(it.x, it.y) }, duration, offset,
                    path = if (finished) fitSwipePath(stroke.points, fittingTolerancePx) else null,
                    previewTrace = if (finished) null else stroke.points.toList(),
                )
            }
        }
        return gestures.singleOrNull() ?: RecordedGesture.Split(
            gestures, gestures.maxOf { it.startOffsetMs + it.durationMs },
        )
    }
}
