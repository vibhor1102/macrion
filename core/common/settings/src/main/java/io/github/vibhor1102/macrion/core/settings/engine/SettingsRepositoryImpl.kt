/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.settings.engine

import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortSettings
import io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortType
import io.github.vibhor1102.macrion.core.settings.engine.data.ScenarioSortSettingsDataSource
import io.github.vibhor1102.macrion.core.settings.engine.data.SettingsDataSource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

// TODO: Cleanup those state flows, this should be done by the caller
@Singleton
internal class SettingsRepositoryImpl @Inject constructor(
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val dataSource: SettingsDataSource,
    private val scenarioSortSettingsDatasource: ScenarioSortSettingsDataSource,
) : SettingsRepository {

    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _isLegacyNotificationUiEnabledFlow: StateFlow<Boolean> = dataSource.isLegacyNotificationUiEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isLegacyNotificationUiEnabledFlow: Flow<Boolean> = _isLegacyNotificationUiEnabledFlow

    private val _isEntireScreenCaptureForcedFlow: StateFlow<Boolean> = dataSource.isEntireScreenCaptureForced()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isEntireScreenCaptureForcedFlow: Flow<Boolean> = _isEntireScreenCaptureForcedFlow

    private val _isFilterScenarioUiEnabled: StateFlow<Boolean> = dataSource.isFilterScenarioUiEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isFilterScenarioUiEnabledFlow: Flow<Boolean> = _isFilterScenarioUiEnabled

    private val _isScenarioSwitcherEnabled: StateFlow<Boolean> = dataSource.isScenarioSwitcherEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isScenarioSwitcherEnabledFlow: Flow<Boolean> = _isScenarioSwitcherEnabled

    override suspend fun isScenarioSwitcherEnabled(): Boolean =
        dataSource.isScenarioSwitcherEnabled().first()

    private val _isHomeButtonEnabled: StateFlow<Boolean> = dataSource.isHomeButtonEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isHomeButtonEnabledFlow: Flow<Boolean> = _isHomeButtonEnabled

    override suspend fun isHomeButtonEnabled(): Boolean =
        dataSource.isHomeButtonEnabled().first()

    private val _isStopConfirmationEnabled: StateFlow<Boolean> = dataSource.isStopConfirmationEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isStopConfirmationEnabledFlow: Flow<Boolean> = _isStopConfirmationEnabled

    override suspend fun isStopConfirmationEnabled(): Boolean =
        dataSource.isStopConfirmationEnabled().first()

    private val _isInputBlockWorkaroundEnabledFlow: StateFlow<Boolean> = dataSource.isInputBlockWorkaroundEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isInputBlockWorkaroundEnabledFlow: Flow<Boolean> = _isInputBlockWorkaroundEnabledFlow

    private val _toolbarScalePercentFlow: StateFlow<Int> = dataSource.toolbarScalePercent()
        .stateIn(coroutineScope, SharingStarted.Eagerly, 100)
    override val toolbarScalePercentFlow: Flow<Int> = _toolbarScalePercentFlow

    override fun getToolbarScalePercent(): Int = _toolbarScalePercentFlow.value

    override fun setToolbarScalePercent(percent: Int) {
        coroutineScope.launch {
            dataSource.setToolbarScalePercent(percent)
        }
    }

    private val _areAdvancedSettingsEnabledFlow: StateFlow<Boolean> = dataSource.areAdvancedSettingsEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val areAdvancedSettingsEnabledFlow: Flow<Boolean> = _areAdvancedSettingsEnabledFlow

    override fun areAdvancedSettingsEnabled(): Boolean = _areAdvancedSettingsEnabledFlow.value

    override fun setAdvancedSettingsEnabled(enabled: Boolean) {
        coroutineScope.launch {
            dataSource.setAdvancedSettingsEnabled(enabled)
        }
    }

    private val _hasSeenAdvancedWarningFlow: StateFlow<Boolean> = dataSource.hasSeenAdvancedWarning()
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val hasSeenAdvancedWarningFlow: Flow<Boolean> = _hasSeenAdvancedWarningFlow

    override fun hasSeenAdvancedWarning(): Boolean = _hasSeenAdvancedWarningFlow.value

    override fun setHasSeenAdvancedWarning(seen: Boolean) {
        coroutineScope.launch {
            dataSource.setHasSeenAdvancedWarning(seen)
        }
    }

    private val _maxToleratedDifferenceFlow: StateFlow<Int> = combine(
        dataSource.areAdvancedSettingsEnabled(),
        dataSource.maxToleratedDifference(),
    ) { areAdvancedEnabled, maxDiff ->
        if (areAdvancedEnabled) maxDiff else 20
    }.stateIn(coroutineScope, SharingStarted.Eagerly, 20)
    override val maxToleratedDifferenceFlow: Flow<Int> = _maxToleratedDifferenceFlow

    override fun getMaxToleratedDifference(): Int = _maxToleratedDifferenceFlow.value

    override fun setMaxToleratedDifference(difference: Int) {
        coroutineScope.launch {
            dataSource.setMaxToleratedDifference(difference)
        }
    }

    private val _screenshotRateLimitPerMinuteFlow: StateFlow<Int> = combine(
        dataSource.areAdvancedSettingsEnabled(),
        dataSource.screenshotRateLimitPerMinute(),
    ) { areAdvancedEnabled, limit ->
        if (areAdvancedEnabled) limit else 10
    }.stateIn(coroutineScope, SharingStarted.Eagerly, 10)
    override val screenshotRateLimitPerMinuteFlow: Flow<Int> = _screenshotRateLimitPerMinuteFlow

    override fun getScreenshotRateLimitPerMinute(): Int = _screenshotRateLimitPerMinuteFlow.value

    override fun setScreenshotRateLimitPerMinute(limit: Int) {
        coroutineScope.launch {
            dataSource.setScreenshotRateLimitPerMinute(limit)
        }
    }

    private val _isToolbarAutoHideEnabledFlow: StateFlow<Boolean> = dataSource.isToolbarAutoHideEnabled()
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)
    override val isToolbarAutoHideEnabledFlow: Flow<Boolean> = _isToolbarAutoHideEnabledFlow

    override fun isToolbarAutoHideEnabled(): Boolean = _isToolbarAutoHideEnabledFlow.value

    override fun setToolbarAutoHideEnabled(enabled: Boolean) {
        coroutineScope.launch {
            dataSource.setToolbarAutoHideEnabled(enabled)
        }
    }

    override fun toggleToolbarAutoHide() {
        coroutineScope.launch {
            dataSource.setToolbarAutoHideEnabled(!_isToolbarAutoHideEnabledFlow.value)
        }
    }

    override val allowPreviewHandleEditingFlow: Flow<Boolean> =
        dataSource.allowPreviewHandleEditing()

    override fun togglePreviewHandleEditing() {
        coroutineScope.launch { dataSource.togglePreviewHandleEditing() }
    }

    private val _toolbarAutoHideDelaySecondsFlow: StateFlow<Int> = dataSource.toolbarAutoHideDelaySeconds()
        .stateIn(coroutineScope, SharingStarted.Eagerly, 120)
    override val toolbarAutoHideDelaySecondsFlow: Flow<Int> = _toolbarAutoHideDelaySecondsFlow

    override fun getToolbarAutoHideDelaySeconds(): Int = _toolbarAutoHideDelaySecondsFlow.value

    override fun setToolbarAutoHideDelaySeconds(seconds: Int) {
        coroutineScope.launch {
            dataSource.setToolbarAutoHideDelaySeconds(seconds)
        }
    }

    override val scenarioSortSettings: Flow<ScenarioSortSettings> = scenarioSortSettingsDatasource.getSortConfig()



    override fun toggleFilterScenarioUi() {
        coroutineScope.launch {
            dataSource.toggleFilterScenarioUi()
        }
    }

    override fun toggleScenarioSwitcher() {
        coroutineScope.launch {
            dataSource.toggleScenarioSwitcher()
        }
    }

    override fun toggleHomeButton() {
        coroutineScope.launch {
            dataSource.toggleHomeButton()
        }
    }

    override fun toggleStopConfirmation() {
        coroutineScope.launch {
            dataSource.toggleStopConfirmation()
        }
    }


    override fun isLegacyNotificationUiEnabled(): Boolean =
        _isLegacyNotificationUiEnabledFlow.value

    override fun toggleLegacyNotificationUi() {
        coroutineScope.launch {
            dataSource.toggleLegacyNotificationUi()
        }
    }


    override fun isEntireScreenCaptureForced(): Boolean =
        _isEntireScreenCaptureForcedFlow.value

    override fun toggleForceEntireScreenCapture() {
        coroutineScope.launch {
            dataSource.toggleForceEntireScreenCapture()
        }
    }


    override fun isInputBlockWorkaroundEnabled(): Boolean =
        _isInputBlockWorkaroundEnabledFlow.value

    override fun toggleInputBlockWorkaround() {
        coroutineScope.launch {
            dataSource.toggleInputBlockWorkaround()
        }
    }

    override fun setScenarioSortType(type: ScenarioSortType) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setSortType(type) }
    }

    override fun setScenarioSortOrder(invertSortOrder: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setSortOrder(invertSortOrder) }
    }

    override fun setScenarioSortShowDumb(show: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setShowDumb(show) }
    }

    override fun setScenarioSortShowSmart(show: Boolean) {
        coroutineScope.launch { scenarioSortSettingsDatasource.setShowSmart(show) }
    }
}
