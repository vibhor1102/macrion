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
package io.github.vibhor1102.macrion.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePathRoomConverter
import io.github.vibhor1102.macrion.core.base.interfaces.EntityWithId
import io.github.vibhor1102.macrion.core.database.SPLIT_ACTION_ITEM_TABLE
import kotlinx.serialization.Serializable

/**
 * Entity defining an item/stroke belonging to a [ActionType.SPLIT_ACTION].
 *
 * Each item represents a simultaneous touch stroke (e.g., a swipe or a click) executed concurrently
 * in a single multi-stroke gesture.
 *
 * @param id unique identifier for the split item. Also the primary key in the table.
 * @param actionId unique identifier of the parent action. References [ActionEntity.id] with CASCADE delete.
 * @param priority order of this item in the split action's sub-actions list.
 * @param type the type of sub-action (e.g., [ActionType.SWIPE] or [ActionType.CLICK]).
 * @param fromX swipe start x or click x.
 * @param fromY swipe start y or click y.
 * @param toX swipe end x (null for click).
 * @param toY swipe end y (null for click).
 * @param duration stroke duration in milliseconds (swipe duration or click press duration).
 * @param startOffset delay in milliseconds from the start of the split action before this stroke begins.
 */
@Entity(
    tableName = SPLIT_ACTION_ITEM_TABLE,
    indices = [Index("action_id")],
    foreignKeys = [
        ForeignKey(
            entity = ActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["action_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
@TypeConverters(SwipePathRoomConverter::class)
@Serializable
data class SplitActionItemEntity(
    @PrimaryKey(autoGenerate = true) override var id: Long = 0,
    @ColumnInfo(name = "action_id") var actionId: Long = 0,
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "click_position_type") val clickPositionType: ClickPositionType? = null,
    @ColumnInfo(name = "click_on_condition_id") var clickOnConditionId: Long? = null,
    @ColumnInfo(name = "click_offset_x") val clickOffsetX: Int? = null,
    @ColumnInfo(name = "click_offset_y") val clickOffsetY: Int? = null,
    @ColumnInfo(name = "priority") var priority: Int = 0,
    @ColumnInfo(name = "type") val type: ActionType = ActionType.SWIPE,
    @ColumnInfo(name = "from_x") val fromX: Int? = null,
    @ColumnInfo(name = "from_y") val fromY: Int? = null,
    @ColumnInfo(name = "to_x") val toX: Int? = null,
    @ColumnInfo(name = "to_y") val toY: Int? = null,
    @ColumnInfo(name = "duration") val duration: Long? = null,
    @ColumnInfo(name = "swipe_path") val swipePath: SwipePath? = null,
    @ColumnInfo(name = "start_offset") val startOffset: Long = 0L,
    @ColumnInfo(name = "wait_after_ms") val waitAfterMs: Long? = null,
) : EntityWithId
