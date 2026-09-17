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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color

import android.graphics.PointF
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.hsvToColorInt
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.toHsv
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.toRgbaHexString

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_DEFAULT_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.getInitialComputeRateUnitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class ColorConditionViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val maxThreshold: StateFlow<Float> = settingsRepository.maxToleratedDifferenceFlow
        .map { it.toFloat() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 20f)

    private var currentHsv: FloatArray? = null
    private var lastColorInt: Int? = null

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Color>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userComputeRateUnit: MutableStateFlow<ComputeRateUnitDropdownItem?> =
        MutableStateFlow(editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.computeRate?.let { getInitialComputeRateUnitItem(it) })

    private var cachedComputeRate: Double = editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.computeRate?.takeIf { it > 0.0 } ?: FRAME_LIMIT_DEFAULT_VALUE

    val uiState: StateFlow<ColorConditionUiState?> = configuredCondition
        .let { conditionFlow ->
            kotlinx.coroutines.flow.combine(conditionFlow, userComputeRateUnit) { colorCondition, unit ->
                colorCondition.toUiState(unit)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.toUiState(userComputeRateUnit.value),
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

    fun setColor(@ColorInt colorInt: Int) {
        updateEditedCondition { it.copy(color = colorInt) }
    }

    fun setHue(hue: Float) {
        val hsv = currentHsv?.copyOf()
            ?: editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.color?.toHsv()
            ?: FloatArray(3)
        hsv[0] = hue.coerceIn(0f, 360f)
        currentHsv = hsv
        val newColor = hsvToColorInt(hsv[0], hsv[1], hsv[2])
        lastColorInt = newColor
        setColor(newColor)
    }

    fun setSaturation(saturation: Float) {
        val hsv = currentHsv?.copyOf()
            ?: editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.color?.toHsv()
            ?: FloatArray(3)
        hsv[1] = saturation.coerceIn(0f, 1f)
        currentHsv = hsv
        val newColor = hsvToColorInt(hsv[0], hsv[1], hsv[2])
        lastColorInt = newColor
        setColor(newColor)
    }

    fun setValue(value: Float) {
        val hsv = currentHsv?.copyOf()
            ?: editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.color?.toHsv()
            ?: FloatArray(3)
        hsv[2] = value.coerceIn(0f, 1f)
        currentHsv = hsv
        val newColor = hsvToColorInt(hsv[0], hsv[1], hsv[2])
        lastColorInt = newColor
        setColor(newColor)
    }

    fun setPosition(position: PointF) {
        updateEditedCondition {
            val x = position.x.toInt()
            val y = position.y.toInt()
            it.copy(detectionArea = Rect(x, y, x + 1, y + 1))
        }
    }

    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }


    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Color) -> ScreenCondition.Color?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    fun toggleLimiter() {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Color>()?.let { condition ->
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

    private fun ScreenCondition.Color.toUiState(userUnit: io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem?): ColorConditionUiState {
        val hsv = if (currentHsv != null && lastColorInt == color) {
            currentHsv!!
        } else {
            color.toHsv().also {
                currentHsv = it
                lastColorInt = color
            }
        }
        return ColorConditionUiState(
            id = id,
            canBeSaved = isComplete(),
            conditionName = name,
            conditionNameError = name.isEmpty(),
            conditionColor = color,
            conditionColorText = color.toRgbaHexString(),
            conditionPosition = PointF(detectionArea.left.toFloat(), detectionArea.top.toFloat()),
            hue = hsv[0],
            saturation = hsv[1],
            value = hsv[2],
            shouldBeDetectedChecked = shouldBeDetected,
            detectionThreshold = threshold,
            computeRateState = io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.toComputeRateLimitUiState(computeRate, userUnit, cachedComputeRate),
        )
    }
}
