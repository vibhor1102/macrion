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
package io.github.vibhor1102.macrion.core.domain.model.event

import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.interfaces.sortedByPriority
import io.github.vibhor1102.macrion.core.database.entity.EventType
import io.github.vibhor1102.macrion.core.domain.model.action.mapper.toDomain
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.toDomain

internal fun Event.toEntity(): EventEntity =
    when (this) {
        is ScreenEvent -> toEntity()
        is TriggerEvent -> toEntity()
    }

/** @return the entity equivalent of this event. */
private fun ScreenEvent.toEntity() = EventEntity(
    id = id.databaseId,
    scenarioId = scenarioId.databaseId,
    name = name.trim(),
    conditionOperator = conditionOperator,
    priority = priority,
    keepDetecting = keepDetecting,
    enabledOnStart = enabledOnStart,
    type = EventType.IMAGE_EVENT,
    detectionCooldownMs = cooldownMs,
    folderName = folder,
)

private fun TriggerEvent.toEntity() : EventEntity =
    EventEntity(
        id = id.databaseId,
        scenarioId = scenarioId.databaseId,
        name = name.trim(),
        conditionOperator = conditionOperator,
        enabledOnStart = enabledOnStart,
        priority = -1,
        type = EventType.TRIGGER_EVENT,
    )


/** @return the complete event for this entity. */
internal fun CompleteEventEntity.toDomain(cleanIds: Boolean = false): Event =
    when (event.type) {
        EventType.IMAGE_EVENT -> toDomainScreenEvent(cleanIds)
        EventType.TRIGGER_EVENT -> toDomainTriggerEvent(cleanIds)
    }

/** @return the complete event for this entity. */
internal fun CompleteEventEntity.toDomainScreenEvent(cleanIds: Boolean = false): ScreenEvent =
    ScreenEvent(
        id = Identifier(id = event.id, asTemporary = cleanIds),
        scenarioId = Identifier(id = event.scenarioId, asTemporary = cleanIds),
        name= event.name,
        conditionOperator = event.conditionOperator,
        priority = event.priority,
        enabledOnStart = event.enabledOnStart,
        keepDetecting = event.keepDetecting == true,
        actions = actions.map { it.toDomain(cleanIds) }.sortedByPriority().toMutableList(),
        conditions = conditions.mapNotNull { it.toDomain(cleanIds) as? ScreenCondition }.sortedByPriority().toMutableList(),
        cooldownMs = event.detectionCooldownMs ?: 0L,
        folder = event.folderName,
    )

/** @return the complete trigger event for this entity. */
internal fun CompleteEventEntity.toDomainTriggerEvent(cleanIds: Boolean = false): TriggerEvent =
    TriggerEvent(
        id = Identifier(id = event.id, asTemporary = cleanIds),
        scenarioId = Identifier(id = event.scenarioId, asTemporary = cleanIds),
        name= event.name,
        conditionOperator = event.conditionOperator,
        enabledOnStart = event.enabledOnStart,
        actions = actions.map { it.toDomain(cleanIds) }.sortedByPriority().toMutableList(),
        conditions = conditions.mapNotNull { it.toDomain(cleanIds) as? TriggerCondition }.toMutableList(),
    )
