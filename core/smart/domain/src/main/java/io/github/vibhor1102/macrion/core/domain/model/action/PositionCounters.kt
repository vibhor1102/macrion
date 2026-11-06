package io.github.vibhor1102.macrion.core.domain.model.action

import android.graphics.Point

/** A null axis uses the fixed coordinate; a named axis reads a runtime counter. */
data class PositionCounters(val x: String? = null, val y: String? = null) {
    fun names(): List<String> = listOfNotNull(x, y)
    fun renamed(old: String, new: String) = copy(x = if (x == old) new else x, y = if (y == old) new else y)
}

fun isPositionConfigured(point: Point?, counters: PositionCounters?, allowNegative: Boolean = false): Boolean =
    (counters?.x?.isNotBlank() ?: (point != null && (allowNegative || point.x >= 0))) &&
        (counters?.y?.isNotBlank() ?: (point != null && (allowNegative || point.y >= 0)))
