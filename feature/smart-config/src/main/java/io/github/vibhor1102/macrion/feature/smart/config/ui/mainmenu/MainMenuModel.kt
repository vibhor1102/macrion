/*
 * Copyright (C) 2025 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import android.content.Context
import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionState
import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionPhase
import io.github.vibhor1102.macrion.core.smart.debugging.domain.DebuggingRepository
import io.github.vibhor1102.macrion.feature.revenue.IRevenueRepository
import io.github.vibhor1102.macrion.feature.revenue.UserBillingState
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.alphabet.AreRequiredAlphabetModelsInstalledUseCase
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val smartProcessingRepository: SmartProcessingRepository,
    settingsRepository: SettingsRepository,
    private val editionRepository: EditionRepository,
    private val tutorialRepository: TutorialRepository,
    private val revenueRepository: IRevenueRepository,
    private val debuggingRepository: DebuggingRepository,
    areRequiredAlphabetModelsInstalledUseCase: AreRequiredAlphabetModelsInstalledUseCase,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = smartProcessingRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private var paywallResultJob: Job? = null
    private var paywallRequestId = 0L
    private var paywallPending = false
    private var startContext: Context? = null
    private var startScenarioId: Identifier? = null

    /** Tells if the paywall is currently displayed. */
    val paywallIsVisible: Flow<Boolean> =
        revenueRepository.isBillingFlowInProgress

    val shouldShowStopWithVolumeDownTip: StateFlow<Boolean> = tutorialRepository
        .shouldShowTip(Tip.STOP_WITH_VOLUME_DOWN)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = smartProcessingRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState.Idle,
        )

    val isMediaProjectionStarted: StateFlow<Boolean> = smartProcessingRepository.detectionState
        .map { it == DetectionState.RECORDING || it == DetectionState.DETECTING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val enginePhase: StateFlow<DetectionPhase> = smartProcessingRepository.detectionPhase
        .stateIn(viewModelScope, SharingStarted.Eagerly, when {
            smartProcessingRepository.isRunning() -> DetectionPhase.DETECTING
            smartProcessingRepository.isScreenRecordActive() -> DetectionPhase.RECORDING
            else -> DetectionPhase.INACTIVE
        })

    private val detectionIntent = DetectionIntentCoordinator(
        scope = viewModelScope,
        phases = enginePhase,
        stopSequence = smartProcessingRepository.detectionStopSequence,
        initiallyRunning = smartProcessingRepository.isRunning(),
        start = {
            val context = startContext
            if (context == null || startScenarioId == null ||
                smartProcessingRepository.getScenarioId() != startScenarioId) false
            else smartProcessingRepository.startDetection(
                context = context,
                liveDebugging = debuggingRepository.isDebugViewEnabled(),
                generateReport = debuggingRepository.isDebugReportEnabled(),
            )
        },
        stop = smartProcessingRepository::stopDetection,
        onStarted = { smartProcessingRepository.scheduleAutoStop(revenueRepository.consumeTrial()) },
    )

    /** The latest button choice, independent of detector setup and cleanup. */
    val requestedDetectionRunning: StateFlow<Boolean> = detectionIntent.requestedRunning

    val toolbarDetectionState: Flow<UiState> = combine(requestedDetectionRunning, enginePhase) { requested, phase ->
        if (requested || phase == DetectionPhase.STARTING || phase == DetectionPhase.DETECTING ||
            phase == DetectionPhase.STOPPING) UiState.Detecting else UiState.Idle
    }.distinctUntilChanged()

    val isTutorial: Boolean
        get() = tutorialRepository.isTutorialStarted()

    val isSwitchButtonVisible: StateFlow<Boolean> = combine(
        detectionState,
        isMediaProjectionStarted,
        settingsRepository.isScenarioSwitcherEnabledFlow,
        tutorialRepository.tutorialState,
    ) { state, isProjectionStarted, isEnabled, _ ->
        !isTutorial && state == UiState.Idle && isProjectionStarted && isEnabled
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isToolbarAutoHideEnabled: Flow<Boolean> = settingsRepository.isToolbarAutoHideEnabledFlow
    val toolbarAutoHideDelaySeconds: Flow<Int> = settingsRepository.toolbarAutoHideDelaySecondsFlow

    /** The condition being configured by the user. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allModelsInstalled: StateFlow<Boolean> = scenarioDbId
        .flatMapLatest { identifier ->
            identifier?.let { dbId -> areRequiredAlphabetModelsInstalledUseCase(dbId) } ?: flowOf(true)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Tells if the scenario can be started. Edited scenario must be synchronized and engine should allow it. */
    private val canStartNormally: Flow<Boolean> = combine(
        smartProcessingRepository.canStartDetection,
        editionRepository.isEditionSynchronized,
        isMediaProjectionStarted,
        detectionState,
    ) { canStartDetection, isSynchronized, isProjectionStarted, state ->
        canUsePlayPauseButton(state, canStartDetection, isSynchronized, isProjectionStarted)
    }

    val isStartButtonEnabled: Flow<Boolean> = combine(
        canStartNormally, requestedDetectionRunning, enginePhase,
    ) { canStart, requested, phase ->
        canStart || requested || phase == DetectionPhase.STARTING ||
            phase == DetectionPhase.DETECTING || phase == DetectionPhase.STOPPING
    }.distinctUntilChanged()

    /** Tells if the detector can't work due to a native library load error. */
    val nativeLibError: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    /** Tells if the device's GPU driver can't expose screen capture buffers for CPU access. */
    val screenCaptureError: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { it == DetectionState.ERROR_SCREEN_IMAGE_CAPTURE_FAILED }
        .distinctUntilChanged()

    val screenshotRateLimitError: Flow<Int> = smartProcessingRepository.screenshotRateLimitError

    /** Load an advertisement, if needed. Should be called before showing the paywall to reduce user waiting time. */
    fun loadAdIfNeeded(context: Context) {
        revenueRepository.loadAdIfNeeded(context)
    }

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context) {
        if (requestedDetectionRunning.value || paywallPending) {
            stopDetection()
        } else if (shouldStartPaywall()) {
            startPaywall(context)
        } else {
            requestStart(context)
        }
    }

    /** A second button tap may request Play while the previous Pause is still cleaning up. */
    fun pauseIfRequested(): Boolean {
        if (!requestedDetectionRunning.value && !paywallPending) return false
        return stopDetection()
    }

    /** Stop the detection. Returns true if it was started, false if not. */
    fun stopDetection(): Boolean {
        val wasPending = paywallPending
        paywallRequestId++
        paywallPending = false
        paywallResultJob?.cancel()
        paywallResultJob = null
        return detectionIntent.requestStop() || wasPending
    }

    private fun shouldStartPaywall(): Boolean =
        revenueRepository.userBillingState.value.isAdRequested() &&
                !tutorialRepository.isTutorialStarted()

    private fun startPaywall(context: Context) {
        paywallPending = true
        val requestId = ++paywallRequestId
        revenueRepository.startPaywallUiFlow(context)

        var sawPaywall = false
        paywallResultJob = combine(revenueRepository.isBillingFlowInProgress, revenueRepository.userBillingState) { inProgress, state ->
            if (inProgress) sawPaywall = true
            if (inProgress || !sawPaywall || requestId != paywallRequestId) return@combine

            Log.d(TAG, "onPaywall finished")

            paywallPending = false
            if (!state.isAdRequested()) requestStart(context)
            paywallResultJob?.cancel()
            paywallResultJob = null
        }.launchIn(viewModelScope)
    }

    private fun requestStart(context: Context) {
        startContext = context
        startScenarioId = smartProcessingRepository.getScenarioId()
        detectionIntent.toggle()
    }

    fun startScenarioEdition(onEditionStarted: () -> Unit) {
        scenarioDbId.value?.let { scenarioDatabaseId ->
            viewModelScope.launch(Dispatchers.IO) {
                if (editionRepository.startEdition(scenarioDatabaseId)) {
                    withContext(Dispatchers.Main) { onEditionStarted() }
                }
            }
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges(onCompleted: (success: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = editionRepository.saveEditions()

            withContext(Dispatchers.Main) {
                onCompleted(result)
            }
        }
    }

    /** Cancel all changes made by the user. */
    fun cancelScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.stopEdition()
        }
    }

    fun shouldDownloadModels(): Boolean =
        !allModelsInstalled.value

    fun shouldRestartMediaProjection(): Boolean =
        !isMediaProjectionStarted.value

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        detectionState.value == UiState.Idle && shouldShowStopWithVolumeDownTip.value

    private fun UserBillingState.isAdRequested(): Boolean =
        this == UserBillingState.AD_REQUESTED
}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

internal fun canUsePlayPauseButton(
    state: UiState,
    canStartDetection: Boolean,
    isSynchronized: Boolean,
    isProjectionStarted: Boolean,
): Boolean = state == UiState.Detecting || (canStartDetection || !isProjectionStarted) && isSynchronized

private const val TAG = "MainMenuViewModel"
