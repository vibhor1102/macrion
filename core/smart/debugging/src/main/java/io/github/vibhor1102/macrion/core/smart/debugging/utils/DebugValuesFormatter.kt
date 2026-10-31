/*
 * Copyright (C) 2025 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.smart.debugging.utils

import java.text.NumberFormat
import java.math.RoundingMode
import io.github.vibhor1102.macrion.core.smart.debugging.domain.model.live.DebugLiveEventOccurrence

/** Format this value as a displayable confidence rate. */
fun Double.formatDebugConfidenceRate(): String =
    NumberFormat.getNumberInstance().apply {
        isGroupingUsed = false
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }.format((this * 100).coerceIn(0.0, 100.0)) + " %"

/**
 * Format this image event occurrence conditions results as a displayable text.
 * If only one condition have been fulfilled, use its name. If more are fulfilled, use 'fulfilled/conditionsCount'.
 */
fun DebugLiveEventOccurrence.formatConditionResultsDisplayText(): String =
    if (event.conditions.size == 1) {
        conditionsResults.first().condition.name
    } else {
        val fulfilled = conditionsResults.fold(0) { acc, result ->
            acc + (if (result.isFulfilled) 1 else 0)
        }
        "$fulfilled/${event.conditions.size}"
    }