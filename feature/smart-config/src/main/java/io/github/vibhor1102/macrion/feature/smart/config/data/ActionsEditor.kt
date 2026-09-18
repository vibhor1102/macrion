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

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.EventToggle
import io.github.vibhor1102.macrion.core.domain.model.action.intent.IntentExtra
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.event.TriggerEvent
import io.github.vibhor1102.macrion.feature.smart.config.data.base.ListEditor

import kotlinx.coroutines.flow.StateFlow

internal class ActionsEditor<Parent>(
    onListUpdated: (List<Action>) -> Unit,
    parentItem: StateFlow<Parent?>,
): ListEditor<Action, Parent>(onListUpdated, parentItem = parentItem) {

    private var parentSplitAction: SplitAction? = null
    private var editedSubActionIndex: Int = -1

    val intentExtraEditor: ListEditor<IntentExtra<out Any>, Action> = ListEditor(
        onListUpdated = ::onEditedActionIntentExtraUpdated,
        canBeEmpty = true,
        parentItem = editedItem,
    )

    val eventToggleEditor: ListEditor<EventToggle, Action> = ListEditor(
        onListUpdated = ::onEditedActionEventToggleUpdated,
        canBeEmpty = true,
        parentItem = editedItem,
    )

    fun startSubActionEdition(parent: SplitAction, subIndex: Int) {
        val currentList = editedList.value ?: return
        val currentParent = currentList.find { it.id == parent.id } as? SplitAction ?: parent
        val subAction = currentParent.subActions.getOrNull(subIndex) ?: return

        parentSplitAction = currentParent
        editedSubActionIndex = subIndex
        super.startItemEdition(subAction)
    }

    override fun startItemEdition(item: Action) {
        parentSplitAction = null
        editedSubActionIndex = -1
        val currentList = editedList.value ?: return
        val currentItem = currentList.find { it.id == item.id } ?: item
        super.startItemEdition(currentItem)

        when (currentItem) {
            is Intent -> intentExtraEditor.startEdition(currentItem.extras ?: emptyList())
            is ToggleEvent -> eventToggleEditor.startEdition(currentItem.eventToggles)
            else -> Unit
        }
    }

    override fun stopItemEdition() {
        val parent = parentSplitAction
        if (parent != null) {
            parentSplitAction = null
            editedSubActionIndex = -1
            super.startItemEdition(parent)
            return
        }
        parentSplitAction = null
        editedSubActionIndex = -1
        intentExtraEditor.stopEdition()
        super.stopItemEdition()
    }

    override fun upsertEditedItem() {
        val parent = parentSplitAction
        val subIndex = editedSubActionIndex
        if (parent != null && subIndex >= 0) {
            val updatedSubAction = editedItem.value ?: return
            val updatedSubActions = parent.subActions.toMutableList()
            if (subIndex in updatedSubActions.indices) {
                updatedSubActions[subIndex] = updatedSubAction
            }
            val updatedParent = parent.copy(subActions = updatedSubActions)
            parentSplitAction = null
            editedSubActionIndex = -1

            val currentList = editedList.value?.toMutableList() ?: return
            val parentIndex = currentList.indexOfAction(updatedParent)
            if (parentIndex != -1) {
                currentList[parentIndex] = updatedParent
            } else {
                currentList.add(updatedParent)
            }
            updateList(currentList)
            super.startItemEdition(updatedParent)
        } else {
            super.upsertEditedItem()
        }
    }

    override fun deleteEditedItem() {
        val parent = parentSplitAction
        val subIndex = editedSubActionIndex
        if (parent != null && subIndex >= 0) {
            parentSplitAction = null
            editedSubActionIndex = -1
            val currentList = editedList.value?.toMutableList() ?: return
            val parentIndex = currentList.indexOfAction(parent)
            if (parent.subActions.size > 2 && subIndex in parent.subActions.indices) {
                val updatedSubActions = parent.subActions.toMutableList()
                updatedSubActions.removeAt(subIndex)
                val reindexed = updatedSubActions.mapIndexed { idx, act -> act.copyBase(priority = idx) }
                val updatedParent = parent.copy(subActions = reindexed)
                if (parentIndex != -1) {
                    currentList[parentIndex] = updatedParent
                }
                updateList(currentList)
                super.startItemEdition(updatedParent)
            } else {
                if (parentIndex != -1) {
                    currentList.removeAt(parentIndex)
                    updateList(currentList)
                }
                stopItemEdition()
            }
        } else {
            super.deleteEditedItem()
        }
    }

    fun addSubAction(newSubAction: Action) {
        val current = editedItem.value as? SplitAction ?: return
        val updatedSubActions = current.subActions + newSubAction.copyBase(priority = current.subActions.size)
        val updated = current.copy(subActions = updatedSubActions)
        updateEditedItem(updated)
    }

    fun removeSubAction(subIndex: Int) {
        val current = editedItem.value as? SplitAction ?: return
        if (current.subActions.size <= 2) return
        val list = current.subActions.toMutableList()
        if (subIndex in list.indices) {
            list.removeAt(subIndex)
            val reindexed = list.mapIndexed { idx, act -> act.copyBase(priority = idx) }
            val updated = current.copy(subActions = reindexed)
            updateEditedItem(updated)
        }
    }

    fun unsplitAction(splitAction: SplitAction, idGenerator: () -> Identifier) {
        val currentList = editedList.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == splitAction.id }
        if (index == -1) return
        currentList.removeAt(index)
        val standaloneActions = splitAction.subActions.mapIndexed { i, sub ->
            sub.copyBase(
                id = idGenerator(),
                eventId = splitAction.eventId,
                name = sub.name ?: "Touch ${i + 1}",
                priority = index + i,
            )
        }
        currentList.addAll(index, standaloneActions)
        updateList(currentList)
        stopItemEdition()
    }

    fun combineActions(actionA: Action, actionB: Action, newId: Identifier): SplitAction? {
        val currentList = editedList.value?.toMutableList() ?: return null
        val idxA = currentList.indexOfFirst { it.id == actionA.id }
        val idxB = currentList.indexOfFirst { it.id == actionB.id }
        if (idxA == -1 || idxB == -1) return null

        val insertIndex = minOf(idxA, idxB)
        currentList.removeAll { it.id == actionA.id || it.id == actionB.id }

        val splitAction = SplitAction(
            id = newId,
            eventId = actionA.eventId,
            name = "Zoom",
            priority = insertIndex,
            subActions = listOf(
                actionA.copyBase(priority = 0),
                actionB.copyBase(priority = 1),
            ),
        )

        currentList.add(insertIndex, splitAction)
        updateList(currentList)
        startItemEdition(splitAction)
        return splitAction
    }

    fun combineActionWithNew(action: Action, newSubAction: Action, newId: Identifier): SplitAction? {
        val currentList = editedList.value?.toMutableList() ?: return null
        val idx = currentList.indexOfFirst { it.id == action.id }
        if (idx == -1) return null

        val splitAction = SplitAction(
            id = newId,
            eventId = action.eventId,
            name = "Zoom",
            priority = action.priority,
            subActions = listOf(
                action.copyBase(priority = 0),
                newSubAction.copyBase(priority = 1),
            ),
        )

        currentList[idx] = splitAction
        updateList(currentList)
        startItemEdition(splitAction)
        return splitAction
    }

    override fun itemCanBeSaved(item: Action?, parent: Parent?): Boolean =
        when (item) {
            is Click -> {
                when (parent) {
                    is TriggerEvent ->
                        item.isComplete() && item.positionType != Click.PositionType.ON_DETECTED_CONDITION

                    is ScreenEvent ->
                        if (item.isComplete()) !(parent.conditionOperator == AND && !item.isClickOnConditionValid())
                        else false

                    else -> item.isComplete()
                }
            }
            is SplitAction -> item.isComplete() && item.subActions.all { itemCanBeSaved(it, parent) }
            else -> item?.isComplete() ?: false
        }

    private fun List<Action>.indexOfAction(action: Action): Int {
        if (action.id != null) return indexOfFirst { it.id == action.id }
        return indexOfFirst { it.name == action.name }
    }

    private fun onEditedActionIntentExtraUpdated(extras: List<IntentExtra<out Any>>) {
        val action = editedItem.value
        if (action == null || action !is Intent) return

        updateEditedItem(action.copy(extras = extras))
    }

    private fun onEditedActionEventToggleUpdated(eventToggles: List<EventToggle>) {
        val action = editedItem.value
        if (action == null || action !is ToggleEvent) return

        updateEditedItem(action.copy(eventToggles = eventToggles))
    }
}