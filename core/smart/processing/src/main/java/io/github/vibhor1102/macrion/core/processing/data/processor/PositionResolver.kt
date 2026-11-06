package io.github.vibhor1102.macrion.core.processing.data.processor

import android.graphics.Point
import io.github.vibhor1102.macrion.core.domain.model.action.PositionCounters

/** Missing or non-finite counter values must never turn into a touch at the screen origin. */
internal fun resolvePosition(
    fixed: Point?,
    counters: PositionCounters?,
    readCounter: (String) -> Double?,
    allowNegative: Boolean = false,
): Point? {
    fun axis(literal: Int?, name: String?): Int? {
        val value = if (name != null) readCounter(name) ?: return null else literal?.toDouble() ?: return null
        val minimum = if (allowNegative) Int.MIN_VALUE.toDouble() else 0.0
        if (!value.isFinite() || value < minimum || value > Int.MAX_VALUE.toDouble()) return null
        return value.toInt()
    }
    return Point(axis(fixed?.x, counters?.x) ?: return null, axis(fixed?.y, counters?.y) ?: return null)
}
