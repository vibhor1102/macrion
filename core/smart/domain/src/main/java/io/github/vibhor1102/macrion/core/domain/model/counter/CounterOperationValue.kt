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
package io.github.vibhor1102.macrion.core.domain.model.counter

import io.github.vibhor1102.macrion.core.base.interfaces.Completable
import io.github.vibhor1102.macrion.core.database.entity.CounterOperationValueType

sealed class CounterOperationValue : Completable {

    abstract val value: Any

    data class Number(override val value: Double): CounterOperationValue() {
        override fun isComplete(): Boolean = value.isFinite() && value >= 0
    }

    data class Counter(override val value: String): CounterOperationValue() {
        override fun isComplete(): Boolean = value.isNotEmpty()
    }

    internal companion object {

        fun getCounterOperationValue(
            type: CounterOperationValueType?,
            numberValue: Double?,
            counterName: String?,
        ) = when (type ?: CounterOperationValueType.NUMBER) {
            CounterOperationValueType.COUNTER -> Counter(counterName ?: "")
            CounterOperationValueType.NUMBER -> Number(numberValue ?: 0.0)
        }
    }
}