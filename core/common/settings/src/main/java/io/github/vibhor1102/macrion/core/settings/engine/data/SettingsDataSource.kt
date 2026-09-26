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
package io.github.vibhor1102.macrion.core.settings.engine.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

import io.github.vibhor1102.macrion.core.base.PreferencesDataStore
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.IO
import io.github.vibhor1102.macrion.core.base.workarounds.isImpactedByInputBlock

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class SettingsDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    internal companion object {
        const val PREFERENCES_FILE_NAME = "settings"

        val KEY_TOOLBAR_SCALE_PERCENT: Preferences.Key<Int> =
            intPreferencesKey("toolbarScalePercent")
        val KEY_IS_FILTER_SCENARIO_UI_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isFilterScenarioUiEnabled")
        val KEY_IS_SCENARIO_SWITCHER_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isScenarioSwitcherEnabled")
        val KEY_IS_HOME_BUTTON_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isHomeButtonEnabled")
        val KEY_IS_STOP_CONFIRMATION_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isStopConfirmationEnabled")
        val KEY_IS_LEGACY_NOTIFICATION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyNotificationUiEnabled")
        val KEY_FORCE_ENTIRE_SCREEN: Preferences.Key<Boolean> =
            booleanPreferencesKey("forceEntireScreen")
        val KEY_INPUT_BLOCK_WORKAROUND: Preferences.Key<Boolean> =
            booleanPreferencesKey("inputBlockWorkaround")
        val KEY_ARE_ADVANCED_SETTINGS_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("areAdvancedSettingsEnabled")
        val KEY_HAS_SEEN_ADVANCED_WARNING: Preferences.Key<Boolean> =
            booleanPreferencesKey("hasSeenAdvancedWarning")
        val KEY_MAX_TOLERATED_DIFFERENCE: Preferences.Key<Int> =
            intPreferencesKey("maxToleratedDifference")
        val KEY_IS_TOOLBAR_AUTO_HIDE_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isToolbarAutoHideEnabled")
        val KEY_SHOW_TOUCH_LOCATIONS_IN_PREVIEW: Preferences.Key<Boolean> =
            booleanPreferencesKey("showTouchLocationsInPreview")
        val KEY_TOOLBAR_AUTO_HIDE_DELAY_SECONDS: Preferences.Key<Int> =
            intPreferencesKey("toolbarAutoHideDelaySeconds")
        val KEY_SCREENSHOT_RATE_LIMIT_PER_MINUTE: Preferences.Key<Int> =
            intPreferencesKey("screenshotRateLimitPerMinute")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacySettingsMigration(context, ioDispatcher))
        )

    internal fun isFilterScenarioUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true }

    internal suspend fun toggleFilterScenarioUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] = !(preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true)
        }

    internal fun isScenarioSwitcherEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_SCENARIO_SWITCHER_ENABLED] ?: false }

    internal suspend fun toggleScenarioSwitcher() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_SCENARIO_SWITCHER_ENABLED] = !(preferences[KEY_IS_SCENARIO_SWITCHER_ENABLED] ?: false)
        }

    internal fun isHomeButtonEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_HOME_BUTTON_ENABLED] ?: false }

    internal suspend fun toggleHomeButton() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_HOME_BUTTON_ENABLED] = !(preferences[KEY_IS_HOME_BUTTON_ENABLED] ?: false)
        }

    internal fun isStopConfirmationEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_STOP_CONFIRMATION_ENABLED] ?: false }

    internal suspend fun toggleStopConfirmation() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_STOP_CONFIRMATION_ENABLED] = !(preferences[KEY_IS_STOP_CONFIRMATION_ENABLED] ?: false)
        }

    internal fun isLegacyNotificationUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false }

    internal suspend fun toggleLegacyNotificationUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_LEGACY_NOTIFICATION_UI] = !(preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false)
        }

    internal fun isEntireScreenCaptureForced(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false }

    internal suspend fun toggleForceEntireScreenCapture() =
        dataStore.edit { preferences ->
            preferences[KEY_FORCE_ENTIRE_SCREEN] = !(preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false)
        }

    internal fun isInputBlockWorkaroundEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false }

    internal suspend fun toggleInputBlockWorkaround() {
        if (!isImpactedByInputBlock()) return
        dataStore.edit { preferences ->
            preferences[KEY_INPUT_BLOCK_WORKAROUND] = !(preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false)
        }
    }

    internal fun toolbarScalePercent(): Flow<Int> =
        dataStore.data.map { preferences -> preferences[KEY_TOOLBAR_SCALE_PERCENT] ?: 100 }

    internal suspend fun setToolbarScalePercent(percent: Int) =
        dataStore.edit { preferences ->
            preferences[KEY_TOOLBAR_SCALE_PERCENT] = percent.coerceIn(50, 150)
        }

    internal fun areAdvancedSettingsEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_ARE_ADVANCED_SETTINGS_ENABLED] ?: false }

    internal suspend fun setAdvancedSettingsEnabled(enabled: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_ARE_ADVANCED_SETTINGS_ENABLED] = enabled
        }

    internal fun hasSeenAdvancedWarning(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_HAS_SEEN_ADVANCED_WARNING] ?: false }

    internal suspend fun setHasSeenAdvancedWarning(seen: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_HAS_SEEN_ADVANCED_WARNING] = seen
        }

    internal fun maxToleratedDifference(): Flow<Int> =
        dataStore.data.map { preferences -> preferences[KEY_MAX_TOLERATED_DIFFERENCE] ?: 20 }

    internal suspend fun setMaxToleratedDifference(difference: Int) =
        dataStore.edit { preferences ->
            preferences[KEY_MAX_TOLERATED_DIFFERENCE] = difference.coerceIn(20, 50)
        }

    internal fun isToolbarAutoHideEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_TOOLBAR_AUTO_HIDE_ENABLED] ?: true }

    internal suspend fun setToolbarAutoHideEnabled(enabled: Boolean) =
        dataStore.edit { preferences ->
            preferences[KEY_IS_TOOLBAR_AUTO_HIDE_ENABLED] = enabled
        }

    internal fun showTouchLocationsInPreview(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_SHOW_TOUCH_LOCATIONS_IN_PREVIEW] ?: true }

    internal suspend fun toggleShowTouchLocationsInPreview() =
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_TOUCH_LOCATIONS_IN_PREVIEW] =
                !(preferences[KEY_SHOW_TOUCH_LOCATIONS_IN_PREVIEW] ?: true)
        }

    internal fun toolbarAutoHideDelaySeconds(): Flow<Int> =
        dataStore.data.map { preferences -> preferences[KEY_TOOLBAR_AUTO_HIDE_DELAY_SECONDS] ?: 120 }

    internal suspend fun setToolbarAutoHideDelaySeconds(seconds: Int) =
        dataStore.edit { preferences ->
            preferences[KEY_TOOLBAR_AUTO_HIDE_DELAY_SECONDS] = seconds
        }

    internal fun screenshotRateLimitPerMinute(): Flow<Int> =
        dataStore.data.map { preferences -> preferences[KEY_SCREENSHOT_RATE_LIMIT_PER_MINUTE] ?: 10 }

    internal suspend fun setScreenshotRateLimitPerMinute(limit: Int) =
        dataStore.edit { preferences ->
            preferences[KEY_SCREENSHOT_RATE_LIMIT_PER_MINUTE] = limit.coerceAtLeast(0)
        }
}
