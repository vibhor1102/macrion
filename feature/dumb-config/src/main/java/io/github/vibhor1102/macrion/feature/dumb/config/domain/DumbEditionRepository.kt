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
package io.github.vibhor1102.macrion.feature.dumb.config.domain

import android.util.Log

import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEditionRepository @Inject constructor(
    private val dumbRepository: IDumbRepository,
) {

    private val _editedDumbScenario: MutableStateFlow<DumbScenario?> = MutableStateFlow(null)
    val editedDumbScenario: StateFlow<DumbScenario?> = _editedDumbScenario

    private val otherActions: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .flatMapLatest { dumbScenario ->
            dumbRepository.getAllDumbActionsFlowExcept(dumbScenario.id.databaseId)
        }
    val actionsToCopy: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .combine(otherActions) { dumbScenario, otherActions ->
            mutableListOf<DumbAction>().apply {
                addAll(dumbScenario.dumbActions)
                addAll(otherActions)
            }
        }

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = editedDumbScenario.map { it == null }

    val dumbActionBuilder: EditedDumbActionsBuilder = EditedDumbActionsBuilder()

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long): Boolean {
        val scenario = dumbRepository.getDumbScenario(scenarioId) ?: run {
            Log.e(TAG, "Can't start edition, dumb scenario $scenarioId not found")
            return false
        }

        Log.d(TAG, "Start edition of dumb scenario $scenarioId")

        _editedDumbScenario.value = scenario
        dumbActionBuilder.startEdition(scenario.id)

        return true
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        val scenarioToSave = _editedDumbScenario.value ?: return
        Log.d(TAG, "Save editions")

        dumbRepository.updateDumbScenario(scenarioToSave)
        stopEdition()
    }

    fun stopEdition() {
        Log.d(TAG, "Stop editions")

        _editedDumbScenario.value = null
        dumbActionBuilder.clearState()
    }

    fun updateDumbScenario(dumbScenario: DumbScenario) {
        Log.d(TAG, "Updating dumb scenario with $dumbScenario")
        _editedDumbScenario.value = dumbScenario
    }

    fun addNewDumbAction(dumbAction: DumbAction, insertionIndex: Int? = null) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Add dumb action to edited scenario $dumbAction at position $insertionIndex")
        val previousActions = editedScenario.dumbActions
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = previousActions.toMutableList().apply {
                if (insertionIndex == null || insertionIndex == (previousActions.lastIndex + 1)) {
                    add(dumbAction.copyWithNewPriority(previousActions.lastIndex + 1))
                    return@apply
                }

                if (insertionIndex !in editedScenario.dumbActions.indices) {
                    Log.w(TAG, "Invalid insertion index $insertionIndex")
                    return@apply
                }

                add(insertionIndex, dumbAction.copyWithNewPriority(insertionIndex))
                updatePriorities((insertionIndex + 1)..lastIndex)
            }
        )
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val actionIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }
        if (actionIndex == -1) {
            Log.w(TAG, "Can't update action, it is not in the edited scenario.")
            return
        }

        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                set(actionIndex, dumbAction)
            }
        )
    }

    fun deleteDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val deleteIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }
        if (deleteIndex < 0) return

        Log.d(TAG, "Delete dumb action from edited scenario $dumbAction at $deleteIndex")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                removeAt(deleteIndex)

                // Update priority for actions after the deleted one
                if (deleteIndex > lastIndex) return@apply
                updatePriorities(deleteIndex..lastIndex)
            }
        )
    }

    fun updateDumbActions(dumbActions: List<DumbAction>) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Updating dumb action list with $dumbActions")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = dumbActions.toMutableList().apply {
                updatePriorities()
            }
        )
    }

    fun unsplitAction(splitAction: DumbAction.DumbSplitAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val currentActions = editedScenario.dumbActions.toMutableList()
        val index = currentActions.indexOfFirst { it.id == splitAction.id }
        if (index == -1) return
        currentActions.removeAt(index)
        val standaloneActions = splitAction.subActions.mapIndexed { i, sub ->
            when (sub) {
                is DumbAction.DumbSwipe -> sub.copy(
                    id = dumbActionBuilder.generateNewIdentifier(),
                    scenarioId = editedScenario.id,
                    name = sub.name.ifBlank { "Swipe ${i + 1}" },
                    priority = index + i,
                )
                is DumbAction.DumbClick -> sub.copy(
                    id = dumbActionBuilder.generateNewIdentifier(),
                    scenarioId = editedScenario.id,
                    name = sub.name.ifBlank { "Click ${i + 1}" },
                    priority = index + i,
                )
                is DumbAction.DumbPause -> sub.copy(
                    id = dumbActionBuilder.generateNewIdentifier(),
                    scenarioId = editedScenario.id,
                    priority = index + i,
                )
                is DumbAction.DumbSplitAction -> sub.copy(
                    id = dumbActionBuilder.generateNewIdentifier(),
                    scenarioId = editedScenario.id,
                    priority = index + i,
                )
            }
        }
        currentActions.addAll(index, standaloneActions)
        updateDumbActions(currentActions)
    }

    /** Apply a combination only after its parent editor is saved. */
    fun saveCombination(split: DumbAction.DumbSplitAction, sourceIds: Set<Identifier>) {
        val actions = _editedDumbScenario.value?.dumbActions ?: return
        val index = actions.indexOfFirst { it.id in sourceIds }
        if (index < 0 || !split.isValid()) return
        val updated = actions.filterNot { it.id in sourceIds }.toMutableList()
        updated.add(index, split)
        updateDumbActions(updated)
    }

    fun combineActions(actionA: DumbAction, actionB: DumbAction): DumbAction.DumbSplitAction? {
        val editedScenario = _editedDumbScenario.value ?: return null
        val currentList = editedScenario.dumbActions.toMutableList()
        val idxA = currentList.indexOfFirst { it.id == actionA.id }
        val idxB = currentList.indexOfFirst { it.id == actionB.id }
        if (idxA == -1 || idxB == -1 || idxA == idxB) return null
        val children = listOf(minOf(idxA, idxB), maxOf(idxA, idxB)).map { index ->
            val stored = currentList[index]
            if (stored.id == actionA.id) actionA else actionB
        }.flatMap { it.touchActions() }
        if (children.size !in 2..10) return null

        val insertIndex = minOf(idxA, idxB)

        val existingParent = (actionA as? DumbAction.DumbSplitAction) ?: (actionB as? DumbAction.DumbSplitAction)
        val splitAction = DumbAction.DumbSplitAction(
            id = dumbActionBuilder.generateNewIdentifier(),
            scenarioId = editedScenario.id,
            name = existingParent?.name ?: "Multi-touch",
            repeatCount = existingParent?.repeatCount ?: 1,
            isRepeatInfinite = existingParent?.isRepeatInfinite ?: false,
            repeatDelayMs = existingParent?.repeatDelayMs ?: 0L,
            waitBeforeMs = existingParent?.waitBeforeMs,
            waitAfterMs = existingParent?.waitAfterMs,
            priority = insertIndex,
            subActions = children.mapIndexed { index, child -> dumbActionBuilder.createNewDumbActionFrom(child).copyWithNewPriority(index) },
        )

        return splitAction
    }

    fun combineActionWithNew(action: DumbAction, newSubAction: DumbAction): DumbAction.DumbSplitAction? {
        val editedScenario = _editedDumbScenario.value ?: return null
        val currentList = editedScenario.dumbActions.toMutableList()
        val idx = currentList.indexOfFirst { it.id == action.id }
        if (idx == -1) return null
        val children = action.touchActions() + newSubAction.touchActions()
        if (children.size !in 2..10) return null

        val existingParent = action as? DumbAction.DumbSplitAction
        val splitAction = DumbAction.DumbSplitAction(
            id = dumbActionBuilder.generateNewIdentifier(),
            scenarioId = editedScenario.id,
            name = existingParent?.name ?: "Multi-touch",
            repeatCount = existingParent?.repeatCount ?: 1,
            isRepeatInfinite = existingParent?.isRepeatInfinite ?: false,
            repeatDelayMs = existingParent?.repeatDelayMs ?: 0L,
            waitBeforeMs = existingParent?.waitBeforeMs,
            waitAfterMs = existingParent?.waitAfterMs,
            priority = action.priority,
            subActions = children.mapIndexed { index, child -> dumbActionBuilder.createNewDumbActionFrom(child).copyWithNewPriority(index) },
        )

        return splitAction
    }

    private fun DumbAction.touchActions(): List<DumbAction> = when (this) {
        is DumbAction.DumbClick, is DumbAction.DumbSwipe -> listOf(this)
        is DumbAction.DumbSplitAction -> subActions
        else -> emptyList()
    }

    private fun DumbAction.copyWithNewPriority(priority: Int): DumbAction =
        when (this) {
            is DumbAction.DumbClick -> copy(priority = priority)
            is DumbAction.DumbPause -> copy(priority = priority)
            is DumbAction.DumbSwipe -> copy(priority = priority)
            is DumbAction.DumbSplitAction -> copy(priority = priority)
        }

    private fun MutableList<DumbAction>.updatePriorities(range: IntRange = indices) {
        for (index in range) {
            Log.d(TAG, "Updating priority to $index for action ${get(index)}")
            set(index, get(index).copyWithNewPriority(index))
        }
    }
}

/** Tag for logs */
private const val TAG = "DumbEditionRepository"
