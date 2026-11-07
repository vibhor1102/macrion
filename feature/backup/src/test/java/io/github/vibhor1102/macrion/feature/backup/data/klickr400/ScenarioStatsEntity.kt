/*
 * Copyright (C) 2025 Kevin Buzeau
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

/**
 * Entity defining the usage of a scenario.
 *
 * A scenario has a relation "one to one" with [ScenarioEntity].
 *
 * @param scenarioId the unique identifier for a scenario.
 * @param lastStartTimestampMs timestamp of the last start for this scenario.
 * @param startCount the number of time this scenario has been started.
 */

data class ScenarioStatsEntity(
    val id: Long,
    val scenarioId: Long,
    val lastStartTimestampMs: Long,
    val startCount: Long,
)
