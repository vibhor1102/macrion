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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.number

import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiNumberFormatDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection
import io.github.vibhor1102.macrion.core.base.identifier.Identifier

data class NumberConditionUiState(
    val id: Identifier,
    val canBeSaved: Boolean,
    val name: String,
    val nameError: Boolean,
    val detectionAreaDescription: String,
    val detectionAreaError: Boolean,
    val detectionThreshold: Int,
    val selectorOperatorDropdownItem: UiCounterOperatorDropdownItem,
    val operandValue: UiStaticOrCounterSelection,
    val conditionEffectDesc: String,
    val numberFormatDropdownItem: UiNumberFormatDropdownItem,
    val computeRateState: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateLimitUiState,
)
