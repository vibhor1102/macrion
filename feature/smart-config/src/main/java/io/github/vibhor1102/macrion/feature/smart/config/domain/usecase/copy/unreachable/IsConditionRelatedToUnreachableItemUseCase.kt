/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.unreachable

import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository

import javax.inject.Inject

class IsConditionRelatedToUnreachableItemUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(condition: Condition): Boolean =
        when (condition) {
            is ScreenCondition.Number -> condition.isRelatedToUnreachableItem()
            is TriggerCondition.OnCounterCountReached -> condition.isRelatedToUnreachableItem()

            // Nothing is referenced in those actions
            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnTimerReached -> false
        }

    private fun ScreenCondition.Number.isRelatedToUnreachableItem(): Boolean =
        when (val ctnValue = counterValue) {
            is CounterOperationValue.Counter -> editionRepository.editionState.getCounter(ctnValue.value) == null
            is CounterOperationValue.ConditionPosition -> true
            is CounterOperationValue.Number -> false
        }

    private fun TriggerCondition.OnCounterCountReached.isRelatedToUnreachableItem(): Boolean {
        if (editionRepository.editionState.getCounter(counterName) == null) return true

        return when (val ctnValue = counterValue) {
            is CounterOperationValue.Counter -> editionRepository.editionState.getCounter(ctnValue.value) == null
            is CounterOperationValue.ConditionPosition -> true
            is CounterOperationValue.Number -> false
        }
    }
}