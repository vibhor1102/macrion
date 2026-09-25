/*
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
package io.github.vibhor1102.macrion.core.dumb.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePathRoomConverter
import io.github.vibhor1102.macrion.core.base.interfaces.EntityWithId
import kotlinx.serialization.Serializable

const val DUMB_SPLIT_ACTION_ITEM_TABLE = "dumb_split_action_item_table"

@Entity(
    tableName = DUMB_SPLIT_ACTION_ITEM_TABLE,
    indices = [Index("action_id")],
    foreignKeys = [
        ForeignKey(
            entity = DumbActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["action_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
@TypeConverters(SwipePathRoomConverter::class)
@Serializable
data class DumbSplitActionItemEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long = 0,
    @ColumnInfo(name = "action_id") var actionId: Long = 0,
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "repeat_count", defaultValue = "1") val repeatCount: Int = 1,
    @ColumnInfo(name = "is_repeat_infinite", defaultValue = "0") val isRepeatInfinite: Boolean = false,
    @ColumnInfo(name = "repeat_delay_ms", defaultValue = "0") val repeatDelayMs: Long = 0L,
    @ColumnInfo(name = "priority") var priority: Int = 0,
    @ColumnInfo(name = "type") val type: DumbActionType = DumbActionType.SWIPE,
    @ColumnInfo(name = "from_x") val fromX: Int? = null,
    @ColumnInfo(name = "from_y") val fromY: Int? = null,
    @ColumnInfo(name = "to_x") val toX: Int? = null,
    @ColumnInfo(name = "to_y") val toY: Int? = null,
    @ColumnInfo(name = "duration") val duration: Long? = null,
    @ColumnInfo(name = "swipe_path") val swipePath: SwipePath? = null,
    @ColumnInfo(name = "start_offset") val startOffset: Long = 0L,
    @ColumnInfo(name = "wait_after_ms") val waitAfterMs: Long? = null,
) : EntityWithId
