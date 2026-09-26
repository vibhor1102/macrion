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
package io.github.vibhor1102.macrion.core.database.serialization

import android.util.Log

import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.serialization.compat.CompatDeserializer
import io.github.vibhor1102.macrion.core.database.serialization.compat.CompatV11Deserializer
import io.github.vibhor1102.macrion.core.database.serialization.compat.CompatV13Deserializer
import io.github.vibhor1102.macrion.core.database.serialization.compat.CompatV20Deserializer

import kotlinx.serialization.json.JsonObject


object DeserializerFactory {

    fun create(databaseVersion: Int): Deserializer? =
        when {
            databaseVersion < VERSION_MINIMUM -> {
                Log.w(TAG, "Json object version not supported, minimum=$VERSION_MINIMUM, actual=$databaseVersion")
                null
            }

            databaseVersion < VERSION_DETECTION_QUALITY_UPDATE -> CompatV11Deserializer()
            databaseVersion < VERSION_ADVANCED_AUTOMATION_UPDATE -> CompatV13Deserializer()
            databaseVersion < VERSION_COUNTER_TYPE_UPDATE -> CompatV20Deserializer()
            // These versions already use the current complete-action JSON structure.
            // Kotlin defaults fill newly added columns without dropping split children or delays.
            databaseVersion in 31 until VERSION_UP_TO_DATE -> KotlinDeserializer()
            databaseVersion < VERSION_UP_TO_DATE -> CompatDeserializer()
            databaseVersion == VERSION_UP_TO_DATE -> KotlinDeserializer()

            else -> {
                Log.w(TAG, "Json object version not supported, maximum=$VERSION_UP_TO_DATE, actual=$databaseVersion")
                null
            }
        }

    /** Maximum json object version supported. */
    private const val VERSION_UP_TO_DATE = DATABASE_VERSION
    /** Number & Text detection update. */
    private const val VERSION_COUNTER_TYPE_UPDATE = 20
    /** Trigger events update version. */
    private const val VERSION_ADVANCED_AUTOMATION_UPDATE = 13
    /** Scenario detection quality revision update version. */
    private const val VERSION_DETECTION_QUALITY_UPDATE = 11
    /** Minimal supported json object version. */
    private const val VERSION_MINIMUM = 8

    /** Tag for logs. */
    private const val TAG = "ScenarioDeserializerFactory"
}

interface Deserializer {
    fun deserializeCompleteScenario(jsonCompleteScenario: JsonObject): CompleteScenario
}