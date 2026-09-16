/*
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.core.processing.tests.processor.state

import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.processing.data.processor.state.ConditionLimitersState
import io.github.vibhor1102.macrion.core.processing.domain.model.ProcessedConditionResult

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConditionLimitersStateTests {

    private lateinit var state: ConditionLimitersState

    @Before
    fun setUp() {
        state = ConditionLimitersState()
    }

    private fun newColorCondition(id: Long, computeRate: Double = 0.0) = ScreenCondition.Color(
        id = Identifier(databaseId = id),
        eventId = Identifier(databaseId = 1L),
        name = "ColorCondition$id",
        priority = 0,
        shouldBeDetected = true,
        color = 0xFFFF0000.toInt(),
        detectionArea = Rect(0, 0, 10, 10),
        threshold = 10,
        computeRate = computeRate,
    )

    private fun newScreenResult(condition: ScreenCondition, isFulfilled: Boolean = true) =
        ProcessedConditionResult.Screen(
            isFulfilled = isFulfilled,
            haveBeenDetected = isFulfilled,
            condition = condition,
            confidenceRate = 100.0,
            position = if (isFulfilled) Point(5, 5) else null,
            size = if (isFulfilled) Point(10, 10) else null,
        )

    // ---- isConditionEligible ----

    @Test
    fun isConditionEligible_zeroComputeRate_alwaysTrue() {
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = 0.0, currentTsNs = 10_000_000_000L))
    }

    @Test
    fun isConditionEligible_negativeComputeRate_alwaysTrue() {
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = -1.0, currentTsNs = 10_000_000_000L))
    }

    @Test
    fun isConditionEligible_firstRun_returnsTrue() {
        // 2 per second = 500ms (500_000_000 ns) interval
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = 2.0, currentTsNs = 1_000_000_000L))
    }

    @Test
    fun isConditionEligible_withinInterval_returnsFalse() {
        val condition = newColorCondition(id = 1L, computeRate = 2.0)
        val result = newScreenResult(condition)
        val t0 = 1_000_000_000L // 1.0s

        state.recordConditionRun(conditionId = 1L, currentTsNs = t0, result = result)

        // 2.0 rate -> 500ms interval (500_000_000 ns). At t0 + 200ms, not eligible
        assertFalse(state.isConditionEligible(conditionId = 1L, computeRate = 2.0, currentTsNs = t0 + 200_000_000L))
    }

    @Test
    fun isConditionEligible_afterInterval_returnsTrue() {
        val condition = newColorCondition(id = 1L, computeRate = 2.0)
        val result = newScreenResult(condition)
        val t0 = 1_000_000_000L

        state.recordConditionRun(conditionId = 1L, currentTsNs = t0, result = result)

        // At t0 + 500ms, exactly eligible
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = 2.0, currentTsNs = t0 + 500_000_000L))
        // At t0 + 600ms, eligible
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = 2.0, currentTsNs = t0 + 600_000_000L))
    }

    @Test
    fun isConditionEligible_separateConditions_evaluatedIndependently() {
        val c1 = newColorCondition(id = 1L, computeRate = 2.0)
        val c2 = newColorCondition(id = 2L, computeRate = 1.0)
        val t0 = 1_000_000_000L

        state.recordConditionRun(conditionId = 1L, currentTsNs = t0, result = newScreenResult(c1))

        // c1 is throttled at t0 + 200ms, but c2 has never run so it is eligible
        assertFalse(state.isConditionEligible(conditionId = 1L, computeRate = 2.0, currentTsNs = t0 + 200_000_000L))
        assertTrue(state.isConditionEligible(conditionId = 2L, computeRate = 1.0, currentTsNs = t0 + 200_000_000L))
    }

    // ---- recordConditionRun & getCachedConditionResult ----

    @Test
    fun getCachedConditionResult_noRecord_returnsNull() {
        assertNull(state.getCachedConditionResult(conditionId = 1L))
    }

    @Test
    fun recordConditionRun_storesResult() {
        val condition = newColorCondition(id = 1L)
        val result = newScreenResult(condition, isFulfilled = true)

        state.recordConditionRun(conditionId = 1L, currentTsNs = 1_000_000L, result = result)

        val cached = state.getCachedConditionResult(conditionId = 1L)
        assertNotNull(cached)
        assertTrue(cached!!.isFulfilled)
    }

    // ---- invalidateConditionResults ----

    @Test
    fun invalidateConditionResults_resetsFulfilledAndDetectionState() {
        val c1 = newColorCondition(id = 1L)
        val c2 = newColorCondition(id = 2L)
        val r1 = newScreenResult(c1, isFulfilled = true)
        val r2 = newScreenResult(c2, isFulfilled = true)

        state.recordConditionRun(conditionId = 1L, currentTsNs = 1_000_000L, result = r1)
        state.recordConditionRun(conditionId = 2L, currentTsNs = 1_000_000L, result = r2)

        state.invalidateConditionResults(listOf(1L))

        val cached1 = state.getCachedConditionResult(conditionId = 1L) as? ProcessedConditionResult.Screen
        val cached2 = state.getCachedConditionResult(conditionId = 2L) as? ProcessedConditionResult.Screen

        assertNotNull(cached1)
        assertFalse(cached1!!.isFulfilled)
        assertFalse(cached1.haveBeenDetected)
        assertNull(cached1.position)
        assertNull(cached1.size)

        // c2 was not invalidated, should still be fulfilled
        assertNotNull(cached2)
        assertTrue(cached2!!.isFulfilled)
        assertTrue(cached2.haveBeenDetected)
        assertNotNull(cached2.position)
    }

    // ---- clearConditionLimitersState ----

    @Test
    fun clearConditionLimitersState_clearsTimestampsAndResults() {
        val condition = newColorCondition(id = 1L, computeRate = 1.0)
        val t0 = 1_000_000_000L

        state.recordConditionRun(conditionId = 1L, currentTsNs = t0, result = newScreenResult(condition))
        assertFalse(state.isConditionEligible(conditionId = 1L, computeRate = 1.0, currentTsNs = t0 + 100_000_000L))
        assertNotNull(state.getCachedConditionResult(conditionId = 1L))

        state.clearConditionLimitersState()

        // After clear, cache is null and condition is eligible as if first run
        assertNull(state.getCachedConditionResult(conditionId = 1L))
        assertTrue(state.isConditionEligible(conditionId = 1L, computeRate = 1.0, currentTsNs = t0 + 100_000_000L))
    }
}
