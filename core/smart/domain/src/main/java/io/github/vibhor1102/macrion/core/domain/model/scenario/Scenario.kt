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
package io.github.vibhor1102.macrion.core.domain.model.scenario

import io.github.vibhor1102.macrion.core.base.ScenarioStats
import io.github.vibhor1102.macrion.core.base.interfaces.Identifiable
import io.github.vibhor1102.macrion.core.base.identifier.Identifier

/**
 * Scenario of events.
 *
 * @param id the unique identifier for the scenario.
 * @param name the name of the scenario.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param randomize tells if the actions values should be randomized a bit.
 * @param computeRate the maximum amount of event loop per seconds. If 0.0, limit is disabled.
 * @param eventCount the number of events in this scenario. Default value is 0.
 */
data class Scenario(
    override val id: Identifier,
    val name: String,
    val detectionQuality: Int,
    val randomize: Boolean = false,
    val keepScreenOn: Boolean = false,
    val computeRate: Double = 0.0,
    val eventCount: Int = 0,
    val stats: ScenarioStats? = null,
    val folders: List<ScenarioFolder> = emptyList(),
): Identifiable

/** Number of screen events before this folder, including hidden events in collapsed folders. */
data class ScenarioFolder(val name: String, val eventIndex: Int)
