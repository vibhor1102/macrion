package io.github.vibhor1102.macrion.core.database.entity

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Persisted coordinate-counter references; runtime models live in the domain module. */
@Serializable
data class PositionCountersEntity(val x: String? = null, val y: String? = null) {
    fun names(): List<String> = listOfNotNull(x, y)
}

class PositionCountersConverter {
    @TypeConverter fun encode(value: PositionCountersEntity?): String? = value?.let { Json.encodeToString(it) }
    @TypeConverter fun decode(value: String?): PositionCountersEntity? = value?.let { Json.decodeFromString(it) }
}
