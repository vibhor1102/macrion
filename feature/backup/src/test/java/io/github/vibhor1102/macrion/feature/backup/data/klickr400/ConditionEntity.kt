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
package io.github.vibhor1102.macrion.feature.backup.data.klickr400

import kotlinx.serialization.Serializable

/**
 * Entity defining a condition from an event.
 *
 * A condition is composed of a path and size of a bitmap to be matched on the screen, and the position of this matching
 * on the screen.
 *
 * It has a one to many relation with [EventEntity], meaning that one event can have several conditions. If the event is
 * deleted, this condition will be deleted as well.
 *
 * @param id unique identifier for a condition. Also the primary key in the table.
 * @param eventId identifier of this condition's event. Reference the key [EventEntity.id] in event_table.
 * @param name the name of the condition.
 * @param path the path on the application appData directory for the bitmap representing the condition. Also the
 *             primary key for this entity.
 * @param areaLeft the left coordinate of the rectangle defining the matching area.
 * @param areaTop the top coordinate of the rectangle defining the matching area.
 * @param areaRight the right coordinate of the rectangle defining the matching area.
 * @param areaBottom the bottom coordinate of the rectangle defining the matching area.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 * @param detectionType the type of detection. Can be any of the values defined in
 *                      [com.buzbuz.smartautoclicker.domain.DetectionType].
 * @param shouldBeDetected true if this condition should be detected to be true, false if it should not be found.
 */

@Serializable
data class ConditionEntity(
      var id: Long,
    var eventId: Long,
    val name: String,
    val type: ConditionType,
    val priority: Int,
    val shouldBeDetected: Boolean? = null,

    // ConditionType.ON_IMAGE_DETECTED
    var path: String? = null,
    val areaLeft: Int? = null,
    val areaTop: Int? = null,
    val areaRight: Int? = null,
    val areaBottom: Int? = null,
    val threshold: Int? = null,
    val detectionType: Int? = null,
    val detectionAreaLeft: Int? = null,
    val detectionAreaTop: Int? = null,
    val detectionAreaRight: Int? = null,
    val detectionAreaBottom: Int? = null,

    // ConditionType.ON_BROADCAST_RECEIVED
    val broadcastAction: String? = null,

    // ConditionType.ON_COUNTER_REACHED
    val counterName: String? = null,
    val counterComparisonOperation: CounterComparisonOperation? = null,
    val counterOperationValueType: CounterOperationValueType? = null,
    val counterValue: Double? = null,
    val counterOperationCounterName: String? = null,

    // ConditionType.ON_TIMER_REACHED
    val timerValueMs: Long? = null,
    val restartWhenReached: Boolean? = null,

    // ConditionType.ON_COLOR_DETECTED
    val colorRgba: Int? = null,

    // ConditionType.ON_NUMBER_DETECTED
    val numberCounterComparisonOperation: CounterComparisonOperation? = null,
    val numberCounterOperationValueType: CounterOperationValueType? = null,
    val numberCounterValue: Double? = null,
    val numberCounterOperationCounterName: String? = null,
    val numberFormatType: NumberFormatType? = null,

    // ConditionType.ON_TEXT_DETECTED
    val textToDetect: String? = null,
    val textAlphabet: String? = null,
)
