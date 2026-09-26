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
package io.github.vibhor1102.macrion.feature.smart.config.data

import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.counter.Counter
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.event.TriggerEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.feature.smart.config.data.events.EventsEditor
import io.github.vibhor1102.macrion.feature.smart.config.data.events.ScreenEventsEditor
import io.github.vibhor1102.macrion.feature.smart.config.data.events.TriggerEventsEditor
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.EditedElementState
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.EditedListState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
internal class ScenarioEditor {

    private val referenceScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)
    private val _editedScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)

    private val _currentEventEditor: MutableStateFlow<EventsEditor<Event, Condition>?> = MutableStateFlow(null)

    val editedScenario: StateFlow<Scenario?> = _editedScenario
    val editedScenarioState: Flow<EditedElementState<Scenario>> = combine(referenceScenario, _editedScenario) { ref, edit ->
        val hasChanged =
            if (ref == null || edit == null) false
            else ref != edit

        val canBeSaved = edit != null && edit.name.isNotBlank()

        EditedElementState(edit, hasChanged, canBeSaved)
    }

    private val imageEventsEditor = ScreenEventsEditor(::deleteAllReferencesToEvent, editedScenario)
    private val triggerEventsEditor = TriggerEventsEditor(::deleteAllReferencesToEvent, editedScenario)
    private val counterListEditor = CountersEditor()

    val currentEventEditor: StateFlow<EventsEditor<Event, Condition>?> = _currentEventEditor

    val allEditedCounters: StateFlow<List<Counter>?> = counterListEditor.editedList
    val editedCountersListState: Flow<EditedListState<Counter>> = counterListEditor.listState

    val allEditedEvents: Flow<List<Event>> =
        combine(imageEventsEditor.allEditedItems, triggerEventsEditor.allEditedItems) { imageEvent, triggerEvents ->
            buildList {
                addAll(imageEvent)
                addAll(triggerEvents)
            }
        }

    val editedEvent: Flow<Event?> = currentEventEditor.flatMapLatest { eventsEditor ->
        eventsEditor?.editedItem ?: emptyFlow()
    }

    val editedScreenEventListState: Flow<EditedListState<ScreenEvent>> = imageEventsEditor.listState
    val editedScreenEventState: Flow<EditedElementState<ScreenEvent>> = imageEventsEditor.editedItemState

    val editedTriggerEventListState: Flow<EditedListState<TriggerEvent>> = triggerEventsEditor.listState
    val editedTriggerEventState: Flow<EditedElementState<TriggerEvent>> = triggerEventsEditor.editedItemState


    fun startEdition(scenario: Scenario, screenEvents: List<ScreenEvent>, triggerEvents: List<TriggerEvent>, counters: List<Counter>) {
        referenceScenario.value = scenario
        _editedScenario.value = scenario

        counterListEditor.startEdition(counters)
        imageEventsEditor.startEdition(screenEvents)
        triggerEventsEditor.startEdition(triggerEvents)
    }

    @Suppress("UNCHECKED_CAST")
    fun startEventEdition(event: Event) {
        _currentEventEditor.value = when (event) {
            is ScreenEvent -> imageEventsEditor
            is TriggerEvent -> triggerEventsEditor
        } as EventsEditor<Event, Condition>

        currentEventEditor.value?.startItemEdition(event)
    }

    fun updateEditedEvent(event: Event) =
        currentEventEditor.value?.updateEditedItem(event)

    fun updateActionsOrder(actions: List<Action>) =
        currentEventEditor.value?.actionsEditor?.updateList(actions)

    fun updateScreenConditionsOrder(screenConditions: List<ScreenCondition>) =
        currentEventEditor.value?.conditionsEditor?.updateList(screenConditions)

    fun upsertEditedEvent() = preservingFolderPositions {
        currentEventEditor.value?.upsertEditedItem()
    }

    fun deleteEditedEvent() = preservingFolderPositions {
        currentEventEditor.value?.deleteEditedItem()
    }

    private inline fun preservingFolderPositions(update: () -> Unit) {
        val before = getScreenEvents()
        update()
        val scenario = _editedScenario.value ?: return
        val layout = reconcileEventLayout(before, scenario.folders, getScreenEvents())
        imageEventsEditor.updateList(layout.events)
        _editedScenario.value = scenario.copy(folders = layout.folders)
    }

    fun deleteScreenEvents(events: List<ScreenEvent>) = preservingFolderPositions {
        events.forEach(::deleteAllReferencesToEvent)
    }

    fun stopEventEdition() {
        currentEventEditor.value?.stopItemEdition()
        _currentEventEditor.value = null
    }

    fun stopEdition() {
        imageEventsEditor.stopEdition()
        triggerEventsEditor.stopEdition()
        counterListEditor.stopEdition()

        referenceScenario.value = null
        _editedScenario.value = null
    }

    fun updateEditedScenario(item: Scenario) {
        _editedScenario.value ?: return
        _editedScenario.value = item
    }

    fun getCounter(name: String): Counter? =
        counterListEditor.getCounter(name)

    fun addCounter(item: Counter) {
        counterListEditor.addCounter(item)
    }

    fun updateCounter(item: Counter) {
        counterListEditor.updateCounter(item)
    }

    fun deleteCounter(item: Counter) {
        counterListEditor.deleteEditedCounter(item)
    }

    fun saveCountersEditionAsReference() {
        counterListEditor.saveEditionAsReference()
    }

    fun updateImageEventsOrder(newEvents: List<ScreenEvent>) {
        imageEventsEditor.updateList(newEvents)
    }

    fun getScreenEvents(): List<ScreenEvent> =
        imageEventsEditor.editedList.value ?: emptyList()

    fun getAllEditedEvents(): List<Event> = buildList {
        imageEventsEditor.editedList.value?.let { addAll(it) }
        triggerEventsEditor.editedList.value?.let { addAll(it) }
    }

    fun getAllEditedCounters(): List<Counter> =
        counterListEditor.editedList.value ?: emptyList()

    fun getEditedImageEventsCount(): Int =
        imageEventsEditor.editedList.value?.size ?: 0

    private fun deleteAllReferencesToEvent(event: Event) {
        imageEventsEditor.deleteAllEventToggleReferencing(event)
        triggerEventsEditor.deleteAllEventToggleReferencing(event)
    }
}
