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
package io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.counter

import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.counter.model.CounterReference
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCounterWriteReferencesUseCase @Inject constructor(
    private val editionRepository: EditionRepository,
) {

    operator fun invoke(): Flow<Map<String, Set<CounterReference>>> =
        editionRepository.editionState.allEditedEventsFlow
            .map { events -> events.findCounterReferences() }

    private fun List<Event>.findCounterReferences(): Map<String, Set<CounterReference>> =
        buildMap {
            this@findCounterReferences.forEach { event ->
                event.actions.getActionsCounterReferences(event).forEach { (counterName, references) ->
                    addReferences(counterName, references)
                }
            }
        }

    private fun List<Action>.getActionsCounterReferences(event: Event): Map<String, Set<CounterReference>> =
        buildMap {
            this@getActionsCounterReferences.forEach { action ->
                when (action) {
                    is ChangeCounter -> {
                        addReference(event, action.counterName, action)
                    }

                    is Notification,
                    is PlaySound,
                    is CaptureScreenshot,
                    is SetText,
                    is Click,
                    is ExternalAction,
                    is Intent,
                    is Pause,
                    is SystemAction,
                    is Swipe,
                    is SplitAction,
                    is ToggleEvent -> Unit
                }
            }
        }

    private fun MutableMap<String, Set<CounterReference>>.addReference(event: Event, counterName: String, action: Action) {
        put(
            counterName,
            getOrDefault(counterName, emptySet()) + CounterReference.ActionElement(event, action)
        )
    }

    private fun MutableMap<String, Set<CounterReference>>.addReferences(counterName: String, references: Set<CounterReference>) {
        put(
            counterName,
            getOrDefault(counterName, emptySet()) + references
        )
    }
}
