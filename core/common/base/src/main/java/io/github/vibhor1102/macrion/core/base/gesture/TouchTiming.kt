package io.github.vibhor1102.macrion.core.base.gesture

/** Android dispatches at most ten strokes within a one-minute gesture. */
const val MAX_TOUCH_STROKES = 10
const val MAX_TOUCH_DURATION_MS = 59_999L

fun isTouchTimingValid(duration: Long?, before: Long?, after: Long?): Boolean =
    duration != null && duration in 1..MAX_TOUCH_DURATION_MS &&
        (before ?: 0L) in 0..MAX_TOUCH_DURATION_MS && (after ?: 0L) in 0..MAX_TOUCH_DURATION_MS

fun isCombinedTouchTimingValid(duration: Long?, before: Long?, after: Long?): Boolean =
    isTouchTimingValid(duration, before, after) && (before ?: 0L) <= MAX_TOUCH_DURATION_MS - duration!!

/** Each wait-after starts when its own finger lifts, including while other fingers remain down. */
fun combinedTouchTailDelay(endTimes: List<Long>, waitsAfter: List<Long>): Long {
    val gestureEnd = endTimes.maxOrNull() ?: return 0L
    return endTimes.indices.maxOfOrNull { index ->
        ((waitsAfter.getOrNull(index) ?: 0L) - (gestureEnd - endTimes[index])).coerceAtLeast(0L)
    } ?: 0L
}
