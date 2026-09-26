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

import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventType
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.ConditionOperator
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioTestsData
import io.github.vibhor1102.macrion.core.domain.utils.asIdentifier

internal object EventTestsData {

    const val EVENT_SCENARIO_ID = ScenarioTestsData.SCENARIO_ID
    const val EVENT_ID = 1667L
    const val EVENT_NAME = "EventName"
    const val EVENT_CONDITION_OPERATOR = AND
    const val EVENT_ENABLED_ON_START = true

    fun getNewImageEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        scenarioId: Long,
        priority: Int = 0,
        cooldownMs: Long = 0,
        folderName: String? = null,
    ) = EventEntity(id, scenarioId, name, conditionOperator, priority, enabledOnStart, EventType.IMAGE_EVENT, detectionCooldownMs = cooldownMs, keepDetecting = false, folderName = folderName)

    fun getNewTriggerEventEntity(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        scenarioId: Long,
    ) = EventEntity(id, scenarioId, name, conditionOperator, -1, enabledOnStart, EventType.TRIGGER_EVENT)

    fun getNewImageEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        actions: List<Action> = emptyList(),
        conditions: List<ScreenCondition.Image> = emptyList(),
        scenarioId: Long,
        priority: Int = 0,
        cooldownMs: Long = 0,
        folder: String? = null,
    ) = ScreenEvent(id.asIdentifier(), scenarioId.asIdentifier(), name, conditionOperator, actions, conditions, enabledOnStart, priority, cooldownMs = cooldownMs, keepDetecting = false, folder = folder)

    fun getNewTriggerEvent(
        id: Long = EVENT_ID,
        name: String = EVENT_NAME,
        @ConditionOperator conditionOperator: Int = EVENT_CONDITION_OPERATOR,
        enabledOnStart: Boolean = EVENT_ENABLED_ON_START,
        actions: List<Action> = emptyList(),
        conditions: List<TriggerCondition> = emptyList(),
        scenarioId: Long,
    ) = TriggerEvent(id.asIdentifier(), scenarioId.asIdentifier(), name, conditionOperator, actions, conditions, enabledOnStart)
}