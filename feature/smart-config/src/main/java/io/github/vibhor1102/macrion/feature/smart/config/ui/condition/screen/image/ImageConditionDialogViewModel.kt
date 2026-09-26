/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.domain.ext.getConditionBitmap
import io.github.vibhor1102.macrion.core.domain.model.DetectionType
import io.github.vibhor1102.macrion.core.domain.model.IN_AREA
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateLimitUiState
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.ComputeRateUnitDropdownItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_DEFAULT_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MAX_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.FRAME_LIMIT_MIN_VALUE
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.getInitialComputeRateUnitItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.config.toComputeRateLimitUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import kotlin.math.max

@OptIn(FlowPreview::class)
class ImageConditionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val maxThreshold: StateFlow<Float> = settingsRepository.maxToleratedDifferenceFlow
        .map { it.toFloat() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 20f)

    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedScreenConditionState
        .mapNotNull { it.value }
        .filterIsInstance<ScreenCondition.Image>()

    private val editedConditionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedScreenConditionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val userComputeRateUnit: MutableStateFlow<ComputeRateUnitDropdownItem?> =
        MutableStateFlow(editionRepository.editionState.getEditedCondition<ScreenCondition.Image>()?.computeRate?.let { getInitialComputeRateUnitItem(it) })

    private var cachedComputeRate: Double = editionRepository.editionState.getEditedCondition<ScreenCondition.Image>()?.computeRate?.takeIf { it > 0.0 } ?: FRAME_LIMIT_DEFAULT_VALUE

    val computeRateState: StateFlow<ComputeRateLimitUiState> =
        kotlinx.coroutines.flow.combine(configuredCondition, userComputeRateUnit) { condition, unit ->
            toComputeRateLimitUiState(condition.computeRate, unit, cachedComputeRate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), toComputeRateLimitUiState(0.0, null, cachedComputeRate))

    /** Tells if the user is currently editing a condition. If that's not the case, dialog should be closed. */
    val isEditingCondition: Flow<Boolean> = editionRepository.isEditingCondition
        .distinctUntilChanged()
        .debounce(1000)

    private val initialCondition = editionRepository.editionState.getEditedCondition<ScreenCondition.Image>()

    val conditionBitmap: Flow<Bitmap?> = configuredCondition.map { condition ->
        bitmapRepository.getConditionBitmap(condition)
    }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<ImageConditionUiState?> = kotlinx.coroutines.flow.combine(
        editionRepository.editionState.editedScreenConditionState,
        userComputeRateUnit,
        conditionBitmap,
    ) { editedState, unit, bitmap ->
        val condition = editedState.value as? ScreenCondition.Image ?: return@combine null
        ImageConditionUiState(
            id = condition.id,
            name = condition.name,
            nameError = condition.name.isEmpty(),
            bitmap = bitmap,
            shouldBeDetected = condition.shouldBeDetected,
            detectionType = context.getDetectionTypeState(condition.detectionType, condition.detectionArea ?: condition.area),
            threshold = condition.threshold,
            computeRateState = toComputeRateLimitUiState(condition.computeRate, unit, cachedComputeRate),
            canBeSaved = editedState.canBeSaved,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialCondition?.let { condition ->
            ImageConditionUiState(
                id = condition.id,
                name = condition.name,
                nameError = condition.name.isEmpty(),
                bitmap = null,
                shouldBeDetected = condition.shouldBeDetected,
                detectionType = context.getDetectionTypeState(condition.detectionType, condition.detectionArea ?: condition.area),
                threshold = condition.threshold,
                computeRateState = toComputeRateLimitUiState(condition.computeRate, userComputeRateUnit.value, cachedComputeRate),
                canBeSaved = true,
            )
        },
    )

    fun hasUnsavedModifications(): Boolean =
        editedConditionHasChanged.value

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        updateEditedCondition { it.copy(name = name) }
    }

    /** Set the shouldBeDetected value of the condition. */
    fun toggleShouldBeDetected() {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(shouldBeDetected = !oldCondition.shouldBeDetected)
        }
    }

    /** Set the detection type. */
    fun setDetectionType(newType: Int) {
        updateEditedCondition { oldCondition ->
            val detectionArea =
                if (oldCondition.detectionArea == null && newType == IN_AREA) oldCondition.area
                else oldCondition.detectionArea

            oldCondition.copy(detectionType = newType, detectionArea = detectionArea)
        }
    }

    /** Set the area to detect in. */
    fun setDetectionArea(area: Rect) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(detectionArea = sanitizeAreaForCondition(area, oldCondition.area))
        }
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        updateEditedCondition { oldCondition ->
            oldCondition.copy(threshold = value)
        }
    }

    fun isConditionRelatedToClick(): Boolean =
        editionRepository.editionState.isEditedConditionReferencedByClick()

    fun toggleLimiter() {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Image>()?.let { condition ->
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



    private fun sanitizeAreaForCondition(area: Rect, conditionArea: Rect): Rect {
        val left = max(area.left, 0)
        val top = max(area.top, 0)
        val width = max(area.right - left, conditionArea.width())
        val height = max(area.bottom - top, conditionArea.height())

        return Rect(
            left,
            top,
            left + width,
            top + height,
        )
    }

    private fun updateEditedCondition(closure: (oldValue: ScreenCondition.Image) -> ScreenCondition.Image?) {
        editionRepository.editionState.getEditedCondition<ScreenCondition.Image>()?.let { condition ->
            closure(condition)?.let { newValue ->
                editionRepository.updateEditedCondition(newValue)
            }
        }
    }

    private fun Context.getDetectionTypeState(@DetectionType type: Int, area: Rect) = DetectionTypeState(
        type = type,
        areaText = getString(R.string.field_select_detection_area_desc, area.left, area.top, area.right, area.bottom)
    )
}

data class DetectionTypeState(
    @param:DetectionType val type: Int,
    val areaText: String,
)

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f
