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
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.conditiontry

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionState
import io.github.vibhor1102.macrion.core.smart.debugging.domain.model.live.DebugLiveEventConditionResult
import io.github.vibhor1102.macrion.core.smart.debugging.domain.usecase.GetDebugLiveDetectionResultUseCase
import io.github.vibhor1102.macrion.core.smart.debugging.utils.formatDebugConfidenceRate
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.uistate.ScreenConditionResultUiState
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live.uistate.mapping.toConditionUiState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TryImageConditionViewModel @Inject constructor(
    detectionResultUseCase: GetDebugLiveDetectionResultUseCase,
    private val smartProcessingRepository: SmartProcessingRepository,
) : ViewModel() {

    private var tryStartJob: Job? = null

    private val isPlaying: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { state -> state == DetectionState.DETECTING }
        .distinctUntilChanged()

    private val userThreshold: MutableStateFlow<Int> = MutableStateFlow(0)
    private val useUserThreshold: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val detectionResult: Flow<ScreenConditionResultUiState?> = detectionResultUseCase(filterNotFulfilled = false)
        .combine(isPlaying) { results, playing -> if (playing) results else null }
        .map { results ->
            if (results == null || results.conditionsResults.isEmpty()) null
            else (results.conditionsResults.first() as DebugLiveEventConditionResult.Screen).toConditionUiState()
        }

    val displayResults: Flow<ScreenConditionResultUiState?> =
        combine(userThreshold, useUserThreshold, detectionResult) { userThreshold, useUserThreshold, result ->
            result?.copy(
                positive =
                    if (useUserThreshold) (1.0 - (userThreshold / 100.0)) < result.confidenceRate
                    else result.positive
            )
        }

    val thresholdText: Flow<String> =
        userThreshold.map { threshold -> (1 - (threshold / 100.0)).formatDebugConfidenceRate() }


    fun setThreshold(newThreshold: Int) {
        viewModelScope.launch {
            userThreshold.value = newThreshold
        }
    }

    fun startTry(context: Context, scenario: Scenario, screenCondition: ScreenCondition) {
        tryStartJob?.cancel()
        tryStartJob = viewModelScope.launch {
            useUserThreshold.value = screenCondition !is ScreenCondition.Number
            userThreshold.value = screenCondition.threshold

            delay(500.milliseconds)
            smartProcessingRepository.tryScreenCondition(context, scenario, screenCondition)
        }
    }

    fun stopTry() {
        tryStartJob?.cancel()
        tryStartJob = null
        smartProcessingRepository.stopDetection()
    }

    fun getSelectedThreshold(): Int = userThreshold.value
}

/** The minimum threshold value selectable by the user. */
internal const val MIN_THRESHOLD = 0f
/** The maximum threshold value selectable by the user. */
internal const val MAX_THRESHOLD = 20f
