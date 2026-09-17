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
package io.github.vibhor1102.macrion.core.settings.domain

import io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortSettings
import io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val isLegacyActionUiEnabledFlow: Flow<Boolean>
    fun isLegacyActionUiEnabled(): Boolean
    fun toggleLegacyActionUi()

    val isLegacyNotificationUiEnabledFlow: Flow<Boolean>
    fun isLegacyNotificationUiEnabled(): Boolean
    fun toggleLegacyNotificationUi()

    val isEntireScreenCaptureForcedFlow: Flow<Boolean>
    fun isEntireScreenCaptureForced(): Boolean
    fun toggleForceEntireScreenCapture()

    val isFilterScenarioUiEnabledFlow: Flow<Boolean>
    fun toggleFilterScenarioUi()

    val isScenarioSwitcherEnabledFlow: Flow<Boolean>
    suspend fun isScenarioSwitcherEnabled(): Boolean
    fun toggleScenarioSwitcher()

    val isHomeButtonEnabledFlow: Flow<Boolean>
    suspend fun isHomeButtonEnabled(): Boolean
    fun toggleHomeButton()

    val isStopConfirmationEnabledFlow: Flow<Boolean>
    suspend fun isStopConfirmationEnabled(): Boolean
    fun toggleStopConfirmation()

    val isInputBlockWorkaroundEnabledFlow: Flow<Boolean>
    fun isInputBlockWorkaroundEnabled(): Boolean
    fun toggleInputBlockWorkaround()

    val toolbarScalePercentFlow: Flow<Int>
    fun getToolbarScalePercent(): Int
    fun setToolbarScalePercent(percent: Int)

    val areAdvancedSettingsEnabledFlow: Flow<Boolean>
    fun areAdvancedSettingsEnabled(): Boolean
    fun setAdvancedSettingsEnabled(enabled: Boolean)

    val hasSeenAdvancedWarningFlow: Flow<Boolean>
    fun hasSeenAdvancedWarning(): Boolean
    fun setHasSeenAdvancedWarning(seen: Boolean)

    val maxToleratedDifferenceFlow: Flow<Int>
    fun getMaxToleratedDifference(): Int
    fun setMaxToleratedDifference(difference: Int)

    val screenshotRateLimitPerMinuteFlow: Flow<Int>
    fun getScreenshotRateLimitPerMinute(): Int
    fun setScreenshotRateLimitPerMinute(limit: Int)

    val isToolbarAutoHideEnabledFlow: Flow<Boolean>
    fun isToolbarAutoHideEnabled(): Boolean
    fun setToolbarAutoHideEnabled(enabled: Boolean)
    fun toggleToolbarAutoHide()

    val toolbarAutoHideDelaySecondsFlow: Flow<Int>
    fun getToolbarAutoHideDelaySeconds(): Int
    fun setToolbarAutoHideDelaySeconds(seconds: Int)


    val scenarioSortSettings: Flow<ScenarioSortSettings>
    fun setScenarioSortType(type: ScenarioSortType)
    fun setScenarioSortOrder(invertSortOrder: Boolean)
    fun setScenarioSortShowDumb(show: Boolean)
    fun setScenarioSortShowSmart(show: Boolean)
}
