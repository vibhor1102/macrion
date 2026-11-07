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
 * Entity defining a scenario of events.
 *
 * A scenario has a relation "one to many" with [EventEntity], which is represented by [ScenarioWithEvents].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 * @param computeRate the maximum amount of event loop per seconds. If 0.0, limit is disabled.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param randomize if true, the action values such as timers, positions will be shifted by a small random value in
 *                  order to avoid behaving like a bot.
 * @param keepScreenOn if true, a wakelock will be taken while the scenario is running, preventing the screen to
 *                     turn off.
 */

@Serializable
data class ScenarioEntity(
    val id: Long,
    val name: String,
    val detectionQuality: Int,
    val computeRate: Double = 0.0,
    val randomize: Boolean = false,
    val keepScreenOn: Boolean = false,
)

/**
 * Entity embedding a scenario and its events.
 *
 * Automatically do the junction between event_table and scenario_table, and provide this representation of the one to
 * many relation between scenario and events entity.
 *
 * @param scenario the scenario entity.
 * @param events the list of event entity for this scenario.
 */
data class ScenarioWithEvents(
    val scenario: ScenarioEntity,

    val events: List<EventEntity>,

    val stats: ScenarioStatsEntity?,
)

/**
 * Entity embedding all entities for a scenario.
 * This object can be huge and must be used only for backup purpose.
 *
 * @param scenario the scenario entity.
 * @param events the complete event list for this scenario.
 */
@Serializable
data class CompleteScenario(
    val scenario: ScenarioEntity,

    val events: List<CompleteEventEntity>,

    val counters: List<CountersEntity>,
)
