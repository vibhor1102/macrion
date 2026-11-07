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
 * Entity defining an event.
 *
 * An event is a combination of a list of actions (click, swipe, pause) and a list of conditions to fulfill in order to
 * execute those actions. It has a relation "one to many" with [ActionEntity], which is represented in
 * [CompleteEventEntity]. It also as another "one to many" relation with [ConditionEntity], also represented in
 * [CompleteEventEntity].
 *
 * It is always associated to a [ScenarioEntity] via its foreign key [EventEntity.scenarioId]. Deletion of a
 * scenario will lead to the automatic deletion of all events with the same scenarioId.
 *
 * To get the complete event with its actions and conditions, you can use [CompleteEventEntity].
 *
 * @param id unique identifier for an event. Also the primary key in the table.
 * @param scenarioId identifier of this event's scenario. Reference the key [ScenarioEntity.id] in scenario_table.
 * @param name name of this event.
 * @param conditionOperator the operator to apply to all [ConditionEntity] related to this click. Can be any value
 *                          of [com.buzbuz.smartautoclicker.domain.ConditionOperator].
 * @param priority the order in the scenario. Lowest priority will always be checked first when detecting.
 * @param enabledOnStart if true, the event will be evaluated while the scenario is playing. If false, it must be
 *                       enabled via an action TOGGLE_EVENT to be evaluated.
 * @param type the [EventType] of this event.
 * @param keepDetecting only for [EventType.IMAGE_EVENT]. If true, keep interpreting the next events in the list with
 *                      the current screen frame. If false, stops and start over the event list with the next frame.
 * @param detectionCooldownMs only for [EventType.IMAGE_EVENT]. If >= 0 or null, cooldown is disabled. If > 0, event will be
 * ignored for the next detectionCooldownMs after being fulfilled.
 */

@Serializable
data class EventEntity(
    var id: Long,
    var scenarioId: Long,
    val name: String,
    val conditionOperator: Int,
    var priority: Int,
    var enabledOnStart: Boolean = true,
    val type: EventType,
    val keepDetecting: Boolean? = null,
    val detectionCooldownMs: Long? = null,
)

/**
 * Entity embedding an event, its actions and its conditions.
 *
 * Automatically do the junction between event_table, action_table and condition_table, and provide this
 * representation of the one to many relations between scenario to actions and conditions entities.
 *
 * @param event the event entity.
 * @param actions the list of actions entity for this event.
 * @param conditions the list of conditions entity for this event.
 */
@Serializable
data class CompleteEventEntity(
    val event: EventEntity,

    val actions: List<CompleteActionEntity>,

    val conditions: List<ConditionEntity>,
)
