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

import android.content.Context
import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiNumberFormatDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.toDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.toNumberFormatType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.UiCounterOperatorDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toAreaDisplayText
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toEffectDescription
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.UiOperandType
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.UiStaticOrCounterSelection
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.toComparisonOperation
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.toCounterOperatorDropdownItem

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_DEFAULT_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MAX_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MIN_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.getInitialComputeRateUnitItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.toComputeRateLimitUiState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class NumberConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val maxThreshold: StateFlow<Float> = settingsRepository.maxToleratedDifferenceFlow
        .map { it.toFloat() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 20f)

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Number>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userComputeRateUnit: MutableStateFlow<ComputeRateUnitDropdownItem?> =
        MutableStateFlow(editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.computeRate?.let { getInitialComputeRateUnitItem(it) })

    private var cachedComputeRate: Double = editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.computeRate?.takeIf { it > 0.0 } ?: FRAME_LIMIT_DEFAULT_VALUE

    val uiState: StateFlow<NumberConditionUiState?> = configuredCondition
        .let { conditionFlow ->
            kotlinx.coroutines.flow.combine(conditionFlow, userComputeRateUnit) { numberCondition, unit ->
                numberCondition.toUiState(context, unit)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.toUiState(context, userComputeRateUnit.value),
        )

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    @OptIn(FlowPreview::class)
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)


    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()

    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    fun setComparisonOperator(item: UiCounterOperatorDropdownItem) {
        updateEditedCondition { old -> old.copy(comparisonOperation = item.toComparisonOperation()) }
    }

    fun setOperandType(type: UiOperandType) {
        // Do nothing if this is the same operand
        val currentOperand = uiState.value?.operandValue
        if (currentOperand is UiStaticOrCounterSelection.CounterValue && type == UiOperandType.COUNTER) return
        if (currentOperand is UiStaticOrCounterSelection.StaticValue && type == UiOperandType.STATIC) return

        // Change operand and use default value
        setOperationValue(
            when (type) {
                UiOperandType.STATIC -> CounterOperationValue.Number(0.0)
                UiOperandType.COUNTER -> CounterOperationValue.Counter("")
            }
        )
    }

    fun setOperationValue(value: CounterOperationValue) {
        updateEditedCondition { old ->
            old.copy(counterValue = value)
        }
    }

    fun setDetectionArea(area: Rect) {
        updateEditedCondition {
            it.copy(detectionArea = area)
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun setNumberFormat(item: UiNumberFormatDropdownItem) {
        updateEditedCondition { it.copy(numberFormatType = item.toNumberFormatType()) }
    }


    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Number) -> ScreenCondition.Number?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    fun toggleLimiter() {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Number>()?.let { condition ->
            val newRate = if (condition.computeRate != 0.0) 0.0 else cachedComputeRate
            updateEditedCondition { it.copy(computeRate = newRate) }
        }
    }

    fun setComputeRateUnit(unit: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem) {
        userComputeRateUnit.value = unit
    }

    fun setComputeRate(value: Double) {
        if (value <= io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MIN_VALUE) return
        val unit = userComputeRateUnit.value ?: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem.Second
        val newValue = if (unit is io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem.Minute) value / 60 else value
        if (newValue > io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MAX_VALUE) return
        cachedComputeRate = newValue
        updateEditedCondition { it.copy(computeRate = newValue) }
    }

    private fun ScreenCondition.Number.toUiState(
        context: Context,
        userUnit: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem?,
    ): NumberConditionUiState =
        NumberConditionUiState(
            id = id,
            canBeSaved = isComplete(),
            name = name,
            nameError = name.isEmpty(),
            detectionAreaDescription = detectionArea.toAreaDisplayText(context),
            detectionAreaError = detectionArea.isEmpty,
            detectionThreshold = threshold,
            selectorOperatorDropdownItem = comparisonOperation.toCounterOperatorDropdownItem(),
            operandValue = counterValue.toUiStaticOrCounterSelection(),
            conditionEffectDesc = counterValue.toEffectDescription(context, comparisonOperation),
            numberFormatDropdownItem = numberFormatType.toDropdownItem(),
            computeRateState = io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.toComputeRateLimitUiState(computeRate, userUnit, cachedComputeRate),
        )

    private fun CounterOperationValue.toUiStaticOrCounterSelection(): UiStaticOrCounterSelection =
        when (this) {
            is CounterOperationValue.Counter ->
                UiStaticOrCounterSelection.CounterValue(editionRepository.editionState.getCounter(value))

            is CounterOperationValue.Number ->
                UiStaticOrCounterSelection.StaticValue(value)
        }
}
