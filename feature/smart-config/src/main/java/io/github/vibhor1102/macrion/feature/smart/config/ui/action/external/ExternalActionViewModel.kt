/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
class ExternalActionViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val smartRepository: IRepository,
) : ViewModel() {

    private val configuredExternalAction = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<ExternalAction>()

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    private val editedScenarioEventsId: Flow<Set<Identifier>> =
        editionRepository.editionState.allEditedEventsFlow
            .map { events -> events.mapNotNull { it.id }.toSet() }

    val knownExternalActionNames: Flow<List<String>> =
        combine(
            smartRepository.allActions,
            editionRepository.editionState.allEditedEventsFlow,
            editedScenarioEventsId,
        ) { dbActions, editedEvents, currentEventIds ->
            val fromOtherScenariosInDb = dbActions.asSequence()
                .filter { action -> action.eventId !in currentEventIds }
                .filterIsInstance<ExternalAction>()
                .map { it.externalActionName.trim() }

            val fromCurrentEditedScenario = editedEvents.asSequence()
                .flatMap { it.actions }
                .filterIsInstance<ExternalAction>()
                .map { it.externalActionName.trim() }

            (fromOtherScenariosInDb + fromCurrentEditedScenario)
                .filter { it.isNotEmpty() }
                .distinct()
                .sortedBy { it.lowercase() }
                .toList()
        }

    val uiState: StateFlow<ExternalActionUiState?> = combine(
        configuredExternalAction,
        editionRepository.editionState.editedActionState.map { it.hasChanged },
        editionRepository.editionState.editedActionState.map { it.canBeSaved },
    ) { action, hasChanged, canBeSaved ->
        ExternalActionUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasChanged,
            name = action.name,
            nameError = action.name?.isEmpty() ?: true,
            externalActionName = action.externalActionName,
            externalActionNameError = action.externalActionName.isBlank(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedExternalAction { old -> old.copy(name = "" + name) }
    }

    fun setExternalActionName(name: String) {
        updateEditedExternalAction { old -> old.copy(externalActionName = name.trim()) }
    }

    private fun updateEditedExternalAction(closure: (old: ExternalAction) -> ExternalAction) {
        editionRepository.editionState.getEditedAction<ExternalAction>()?.let { old ->
            editionRepository.updateEditedAction(closure(old))
        }
    }
}
