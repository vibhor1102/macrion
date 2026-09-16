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
package io.github.vibhor1102.macrion.core.processing.data.processor.state

import io.github.vibhor1102.macrion.core.processing.domain.model.ProcessedConditionResult

private const val ONE_SECOND_IN_NANOS = 1_000_000_000.0

interface IConditionLimitersState {

    fun isConditionEligible(conditionId: Long, computeRate: Double, currentTsNs: Long): Boolean
    fun recordConditionRun(conditionId: Long, currentTsNs: Long, result: ProcessedConditionResult)
    fun getCachedConditionResult(conditionId: Long): ProcessedConditionResult?
    fun invalidateConditionResults(conditionIds: Collection<Long>)
    fun clearConditionLimitersState()
}

internal class ConditionLimitersState : IConditionLimitersState {

    /** Map of condition database id to the monotonic timestamp in nanoseconds of its last run. */
    private val conditionLastRunNs: MutableMap<Long, Long> = mutableMapOf()

    /** Map of condition database id to its latest evaluation result. */
    private val conditionCachedResults: MutableMap<Long, ProcessedConditionResult> = mutableMapOf()

    override fun isConditionEligible(conditionId: Long, computeRate: Double, currentTsNs: Long): Boolean {
        if (computeRate <= 0.0) return true

        val minIntervalNs = (ONE_SECOND_IN_NANOS / computeRate).toLong()
        val lastRun = conditionLastRunNs[conditionId] ?: return true

        return (currentTsNs - lastRun) >= minIntervalNs
    }

    override fun recordConditionRun(conditionId: Long, currentTsNs: Long, result: ProcessedConditionResult) {
        conditionLastRunNs[conditionId] = currentTsNs
        conditionCachedResults[conditionId] = result
    }

    override fun getCachedConditionResult(conditionId: Long): ProcessedConditionResult? =
        conditionCachedResults[conditionId]

    override fun invalidateConditionResults(conditionIds: Collection<Long>) {
        for (id in conditionIds) {
            val cached = conditionCachedResults[id] ?: continue
            conditionCachedResults[id] = when (cached) {
                is ProcessedConditionResult.Screen -> cached.copy(
                    isFulfilled = false,
                    haveBeenDetected = false,
                    position = null,
                    size = null,
                )
                is ProcessedConditionResult.Trigger -> cached.copy(isFulfilled = false)
            }
        }
    }

    override fun clearConditionLimitersState() {
        conditionLastRunNs.clear()
        conditionCachedResults.clear()
    }
}
