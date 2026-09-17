/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.references

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.actions.text.findCounterReferences
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction

import javax.inject.Inject


/**
 * For a given Action, get all possible missing references to another item in the current scenario.
 */
class GetActionMissingReferencesUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
    private val smartRepository: IRepository,
) {

    suspend operator fun invoke(
        action: Action,
        eventsToCopy: List<Event> = emptyList(),
    ): ItemWithMissingReferences.ActionItem {

        // We want to check for conflict on the resulting list, allowing cross-referenced items within the same copy
        val copyResultEvents: Map<Identifier, Event> = buildMap {
            putAll(editionRepository.editionState.getAllEditedEvents().map { event -> event.id to event })
            putAll(eventsToCopy.map { event -> event.id to event })
        }

        val missingReferences = when (action) {
            is ChangeCounter -> action.getMissingReferences()
            is Click -> action.getMissingReferences(copyResultEvents)
            is Notification -> action.getMissingReferences()
            is SetText -> action.getMissingReferences()
            is ToggleEvent -> action.getMissingReferences(copyResultEvents)

            // Nothing is referenced in those actions
            is ExternalAction,
            is Intent,
            is Pause,
            is PlaySound,
            is Swipe,
            is SystemAction -> emptyList()
        }

        return ItemWithMissingReferences.ActionItem(
            item = action,
            missingReferences = missingReferences,
        )
    }

    private fun ChangeCounter.getMissingReferences(): List<MissingCopyReference> =
        buildList {
            if (editionRepository.editionState.getCounter(counterName) == null) {
                add(MissingCopyReference.CounterReference(counterName))
            }

            if (operationValue is CounterOperationValue.Counter) {
                val valueCounterName = (operationValue as CounterOperationValue.Counter).value
                if (editionRepository.editionState.getCounter(valueCounterName) == null) {
                    add(MissingCopyReference.CounterReference(valueCounterName))
                }
            }
        }

    private suspend fun Click.getMissingReferences(copyResultEvents: Map<Identifier, Event>): List<MissingCopyReference> {
        if (positionType != Click.PositionType.ON_DETECTED_CONDITION) return emptyList()

        val conditionId = clickOnConditionId ?: return emptyList()
        val isFound = copyResultEvents[eventId]?.conditions
            ?.find { condition -> condition.id == conditionId } != null
        if (isFound) return emptyList()

        val name = smartRepository.getConditionName(conditionId) ?: return emptyList()
        return listOf(MissingCopyReference.ScreenConditionReference(name = name, conditionId = conditionId))
    }

    private fun Notification.getMissingReferences(): List<MissingCopyReference> =
        messageText.findCounterReferences()
            .filter { counterName -> editionRepository.editionState.getCounter(counterName) == null }
            .map { counterName -> MissingCopyReference.CounterReference(counterName) }

    private fun SetText.getMissingReferences(): List<MissingCopyReference> =
        text.findCounterReferences()
            .filter { counterName -> editionRepository.editionState.getCounter(counterName) == null }
            .map { counterName -> MissingCopyReference.CounterReference(counterName) }

    private suspend fun ToggleEvent.getMissingReferences(copyResultEvents: Map<Identifier, Event>): List<MissingCopyReference> {
        if (toggleAll) return emptyList()

        // We don't want one error per toggle, as if one is valid, all them of will most likely be
        // Just take the first one found, UI forces an update in batches anyway
        val invalidToggle = eventToggles.find { toggle ->
            toggle.targetEventId?.let { targetEventId ->
                !copyResultEvents.contains(targetEventId)
            } ?: false
        } ?: return emptyList()

        val name = invalidToggle.targetEventId
            ?.let { eventId -> smartRepository.getEventName(eventId) }
            ?: "Unknown"

        return listOf(MissingCopyReference.EventToggleReference(name))
    }
}
