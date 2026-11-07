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
 * Entity defining an event toggle.
 *
 * An intent extra is linked with an action of type [ActionType.TOGGLE_EVENT] and contains the toggle to apply to an event.
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param actionId unique identifier the action containing this toggle. Reference the key [ActionEntity.id] in action_table.
 * @param type the type of manipulation to apply to the event.
 * @param eventId unique identifier of the event to apply the toggle to. Reference the key [EventEntity.id] in event_table.
 */

@Serializable
data class EventToggleEntity(
    var id: Long,
    var actionId: Long,
    val type: EventToggleType,
    var toggleEventId: Long,
)
