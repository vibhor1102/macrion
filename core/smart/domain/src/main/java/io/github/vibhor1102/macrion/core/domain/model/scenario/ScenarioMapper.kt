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
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.entity.ScenarioFolderEntity
import io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity
import io.github.vibhor1102.macrion.core.database.entity.ScenarioStatsEntity
import io.github.vibhor1102.macrion.core.database.entity.ScenarioWithEvents
import io.github.vibhor1102.macrion.core.domain.model.counter.Counter
import io.github.vibhor1102.macrion.core.domain.model.counter.toDomain
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.toDomain

/** @return the entity equivalent of this scenario. */
internal fun Scenario.toEntity() = ScenarioEntity(
    id = id.databaseId,
    name = name.trim(),
    detectionQuality = detectionQuality,
    randomize = randomize,
    keepScreenOn = keepScreenOn,
    computeRate = computeRate,
    folders = folders.map { ScenarioFolderEntity(it.name, it.eventIndex) },
)

/** @return the scenario for this entity. */
internal fun ScenarioWithEvents.toDomain(asDomain: Boolean = false) = Scenario(
    id = Identifier(id = scenario.id, asTemporary = asDomain),
    name = scenario.name,
    detectionQuality = scenario.detectionQuality,
    computeRate = scenario.computeRate,
    randomize = scenario.randomize,
    keepScreenOn = scenario.keepScreenOn,
    folders = scenario.folders.map { ScenarioFolder(it.name, it.eventIndex) },
    eventCount = events.size,
    stats = stats.toDomain(),
)

/** @return the scenario for this entity. */
internal fun CompleteScenario.toDomain(cleanIds: Boolean = false): Triple<Scenario, List<Event>, List<Counter>> =
    Triple(
        scenario.toDomain(cleanIds),
        events.map { completeEventEntity -> completeEventEntity.toDomain(cleanIds) },
        counters.map { counter -> counter.toDomain() }
    )

/** @return the scenario for this entity. */
private fun ScenarioEntity.toDomain(cleanIds: Boolean = false) = Scenario(
    id = Identifier(id = id, asTemporary = cleanIds),
    name = name,
    detectionQuality = detectionQuality,
    randomize = randomize,
    keepScreenOn = keepScreenOn,
    computeRate = computeRate,
    folders = folders.map { ScenarioFolder(it.name, it.eventIndex) },
)

private fun ScenarioStatsEntity?.toDomain() =
    if (this == null) ScenarioStats(
        lastStartTimestampMs = 0,
        startCount = 0,
    ) else ScenarioStats(
        lastStartTimestampMs = lastStartTimestampMs,
        startCount = startCount,
    )
