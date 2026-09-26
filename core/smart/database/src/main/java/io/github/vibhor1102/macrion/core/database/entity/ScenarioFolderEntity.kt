/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.database.entity

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Folder order is independent of membership, so empty folders also have a position. */
@Serializable
data class ScenarioFolderEntity(val name: String, val eventIndex: Int)

class ScenarioFoldersConverter {
    @TypeConverter
    fun encode(folders: List<ScenarioFolderEntity>): String = Json.encodeToString(folders)

    @TypeConverter
    fun decode(value: String): List<ScenarioFolderEntity> = Json.decodeFromString(value)
}
