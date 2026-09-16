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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text

import android.content.Context
import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.getDisplayNameResId
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toAreaDisplayText

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

class TextConditionViewModel @Inject constructor(
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
        .filterIsInstance<ScreenCondition.Text>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userComputeRateUnit: MutableStateFlow<ComputeRateUnitDropdownItem?> =
        MutableStateFlow(editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.computeRate?.let { getInitialComputeRateUnitItem(it) })

    private var cachedComputeRate: Double = editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.computeRate?.takeIf { it > 0.0 } ?: FRAME_LIMIT_DEFAULT_VALUE

    val uiState: StateFlow<TextConditionUiState?> = configuredCondition
        .let { conditionFlow ->
            kotlinx.coroutines.flow.combine(conditionFlow, userComputeRateUnit) { textCondition, unit ->
                textCondition.toUiState(context, unit)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.toUiState(context, userComputeRateUnit.value),
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

    fun setTextToDetect(text: String) {
        updateEditedCondition { it.copy(text = text) }
    }

    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
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


    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Text) -> ScreenCondition.Text?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    fun toggleLimiter() {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Text>()?.let { condition ->
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

    private fun ScreenCondition.Text.toUiState(
        context: Context,
        userUnit: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem?,
    ): TextConditionUiState =
        TextConditionUiState(
            id = id,
            canBeSaved = isComplete(),
            name = name,
            nameError = name.isEmpty(),
            textToSearch = text,
            shouldBeDetectedChecked = shouldBeDetected,
            detectionAreaDescription = detectionArea.toAreaDisplayText(context),
            detectionAreaError = detectionArea.isEmpty,
            detectionThreshold = threshold,
            alphabetDesc = context.getString(alphabet.getDisplayNameResId()),
            computeRateState = io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.toComputeRateLimitUiState(computeRate, userUnit, cachedComputeRate),
        )
}
