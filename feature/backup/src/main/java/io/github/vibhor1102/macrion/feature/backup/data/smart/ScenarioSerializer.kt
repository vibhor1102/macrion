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
package io.github.vibhor1102.macrion.feature.backup.data.smart

import android.util.Log

import io.github.vibhor1102.macrion.core.base.extensions.getInt
import io.github.vibhor1102.macrion.core.base.extensions.getJsonObject
import io.github.vibhor1102.macrion.core.base.extensions.getString
import io.github.vibhor1102.macrion.core.database.serialization.DeserializerFactory
import io.github.vibhor1102.macrion.core.database.entity.normalizeUserEnteredNames
import io.github.vibhor1102.macrion.feature.backup.data.base.validateBackupSchema
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.withoutMacrionSubExtension
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_FORMAT_NAME
import io.github.vibhor1102.macrion.feature.backup.data.base.MalformedBackupArchiveException
import io.github.vibhor1102.macrion.feature.backup.data.base.ScenarioBackupSerializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject

import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer/Deserializer for database scenarios (json).
 * It (tries to) handles the compatibility by deserializing manually if the version isn't the same.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ScenarioSerializer : ScenarioBackupSerializer<ScenarioBackup> {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Serialize a scenario.
     *
     * @param scenarioBackup the scenario to serialize.
     * @param outputStream the stream to serialize into.
     */
    override fun serialize(scenarioBackup: ScenarioBackup, outputStream: OutputStream) =
        Json.encodeToStream(scenarioBackup, outputStream)

    /**
     * Deserialize a scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param jsonStream the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    override fun deserialize(jsonStream: InputStream, format: BackupArchiveFormat): ScenarioBackup? {
        Log.d(TAG, "Deserializing smart scenario")

        val jsonBackup = json.parseToJsonElement(jsonStream.readBytes().toString(Charsets.UTF_8)).jsonObject
        val declaredFormat = jsonBackup.getString("format")
        if (
            (format == BackupArchiveFormat.MACRION_NATIVE && declaredFormat != MACRION_FORMAT_NAME) ||
            (format == BackupArchiveFormat.KLICKR_COMPATIBLE && declaredFormat == MACRION_FORMAT_NAME)
        ) {
            throw MalformedBackupArchiveException()
        }
        validateBackupSchema(jsonBackup, format, isDumb = false)
        val version = jsonBackup.getInt("version", true) ?: -1

        val scenario = jsonBackup.getJsonObject("scenario", true)?.let { scenario ->
            DeserializerFactory.create(version)?.deserializeCompleteScenario(scenario)
                ?.withRestoredInternalPaths(format)
                ?.normalizeUserEnteredNames()
        }
        if (scenario == null) {
            Log.w(TAG, "Can't deserialize scenario.")
            return null
        }

        return ScenarioBackup(
            format = declaredFormat,
            version = version,
            screenWidth = jsonBackup.getInt("screenWidth") ?: 0,
            screenHeight = jsonBackup.getInt("screenHeight") ?: 0,
            scenario = scenario,
        )
    }
}

private fun io.github.vibhor1102.macrion.core.database.entity.CompleteScenario.withRestoredInternalPaths(
    format: BackupArchiveFormat,
): io.github.vibhor1102.macrion.core.database.entity.CompleteScenario =
    if (format != BackupArchiveFormat.MACRION_NATIVE) this else copy(
        events = events.map { event ->
            event.copy(
                conditions = event.conditions.map { condition ->
                    condition.copy(path = condition.path?.withoutMacrionSubExtension())
                }
            )
        }
    )

/** Tag for logs. */
private const val TAG = "ScenarioDeserializer"
