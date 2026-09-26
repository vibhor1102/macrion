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
package io.github.vibhor1102.macrion.core.settings.engine.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import io.github.vibhor1102.macrion.core.base.PreferencesDataStore

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first


internal class LegacySettingsMigration(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
) : DataMigration<Preferences> {

    private val legacyDataStore: PreferencesDataStore =
        PreferencesDataStore(context, ioDispatcher, LEGACY_PREFERENCES_FILE_NAME)

    override suspend fun shouldMigrate(currentData: Preferences) = false

    override suspend fun migrate(currentData: Preferences): Preferences = currentData

    // Once the migration is over, clean up the old storage
    override suspend fun cleanUp() {
        legacyDataStore.edit { it.clear() }
    }
}

private const val LEGACY_PREFERENCES_FILE_NAME = "smartConfig"
private const val TAG = "LegacySettingsMigration"