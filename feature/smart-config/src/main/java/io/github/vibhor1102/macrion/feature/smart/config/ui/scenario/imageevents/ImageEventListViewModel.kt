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
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import androidx.lifecycle.ViewModel

import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder
import io.github.vibhor1102.macrion.feature.smart.config.data.completeFolders
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.MonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.availability.IsScreenEventCopyAvailableUseCase
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.toUiImageEvent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import javax.inject.Inject

class ImageEventListViewModel @Inject constructor(
    isScreenEventCopyAvailableUseCase: IsScreenEventCopyAvailableUseCase,
    private val editionRepository: EditionRepository,
    internal val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** Read both parts of the draft together after each synchronous editor operation. */
    internal val listState = combine(
        editionRepository.editionState.editedScreenEventsState,
        editionRepository.editionState.scenarioState,
    ) { _, _ ->
        ImageEventListState(
            editionRepository.getScreenEvents().map { it.toUiImageEvent(inError = !it.isComplete()) },
            editionRepository.editionState.getScenario()?.folders.orEmpty(),
        )
    }

    val copyButtonIsVisible: Flow<Boolean> = isScreenEventCopyAvailableUseCase()

    /** Create an event. */
    fun createNewEvent(context: Context): ScreenEvent =
        editionRepository.editedItemsBuilder.createNewImageEvent(context)

    fun startEventEdition(event: ScreenEvent) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.upsertEditedEvent()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEvent()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedEvent() = editionRepository.stopEventEdition()

    /** Update the priority of the events in the scenario. */
    fun updateEventsPriority(uiEvents: List<UiImageEvent>) =
        editionRepository.updateImageEventsOrder(
            uiEvents.map { it.event }
        )

    /** Store events and header positions in the scenario draft; Save persists both together. */
    fun updateLayout(items: List<ScenarioListItem>, source: List<UiImageEvent>) {
        val events = ScenarioFolderReorderHelper.reconstructEvents(items, source)
        val folders = ScenarioFolderReorderHelper.reconstructFolders(items, source)
        editionRepository.updateImageEventsOrder(events)
        updateFolders(folders)
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        val events = editionRepository.getScreenEvents()
        val folders = currentFolders()
        if (trimmed.isEmpty() || folders.any { it.name == trimmed }) return
        updateFolders(folders + ScenarioFolder(trimmed, events.size))
    }

    fun renameFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        val folders = currentFolders()
        if (trimmed.isEmpty() || trimmed == oldName || folders.any { it.name == trimmed }) return
        editionRepository.updateImageEventsOrder(editionRepository.getScreenEvents().map {
            if (it.folder == oldName) it.copy(folder = trimmed) else it
        })
        updateFolders(folders.map { if (it.name == oldName) it.copy(name = trimmed) else it })
    }

    fun deleteFolder(folderName: String, deleteEvents: Boolean) {
        val current = editionRepository.getScreenEvents()
        val folders = currentFolders()
        if (deleteEvents) {
            // Use normal deletion to clean up Toggle Event references, including trigger events.
            editionRepository.deleteScreenEvents(current.filter { it.folder == folderName })
            updateFolders(currentFolders().filterNot { it.name == folderName })
        } else {
            editionRepository.updateImageEventsOrder(current.map {
                if (it.folder == folderName) it.copy(folder = null) else it
            })
            updateFolders(folders.filterNot { it.name == folderName })
        }
    }

    private fun currentFolders(): List<ScenarioFolder> = completeFolders(
        editionRepository.getScreenEvents(), editionRepository.editionState.getScenario()?.folders.orEmpty(),
    )

    private fun updateFolders(folders: List<ScenarioFolder>) {
        val scenario = editionRepository.editionState.getScenario() ?: return
        editionRepository.updateEditedScenario(scenario.copy(folders = folders))
    }

    /** Create a new event assigned to a specific folder. */
    fun createNewEventInFolder(context: Context, folderName: String): ScreenEvent =
        editionRepository.editedItemsBuilder.createNewImageEvent(context).copy(folder = folderName)
}

internal data class ImageEventListState(
    val events: List<UiImageEvent>,
    val folders: List<ScenarioFolder>,
)
