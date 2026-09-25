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
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
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
    private var parentReference: Action? = null
    private var pendingCombination: Set<Identifier> = emptySet()
    private var sourceBeforeCombination: Pair<Action, Action>? = null

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
        val currentParent = (editedItem.value as? SplitAction)?.takeIf { it.id == parent.id } ?: parent
        val subAction = currentParent.subActions.getOrNull(subIndex) ?: return
        parentReference = referenceEditedItem.value?.takeIf { it.id == parent.id } ?: parent
        parentSplitAction = currentParent
        editedSubActionIndex = subIndex
        restoreItemEdition(subAction, subAction)
    }

    override fun startItemEdition(item: Action) {
        // The UI opens the parent after creating a pending combination.
        if (pendingCombination.isNotEmpty() && editedItem.value?.id == item.id) return
        pendingCombination = emptySet()
        sourceBeforeCombination = null
        parentSplitAction = null
        parentReference = null
        editedSubActionIndex = -1
        super.startItemEdition(item)
        when (val current = editedItem.value) {
            is Intent -> intentExtraEditor.startEdition(current.extras ?: emptyList())
            is ToggleEvent -> eventToggleEditor.startEdition(current.eventToggles)
            else -> Unit
        }
    }

    private fun returnToParent(parent: SplitAction) {
        val reference = parentReference ?: parent
        parentSplitAction = null
        parentReference = null
        editedSubActionIndex = -1
        restoreItemEdition(reference, parent)
    }

    override fun stopEdition() {
        parentSplitAction = null
        parentReference = null
        editedSubActionIndex = -1
        pendingCombination = emptySet()
        sourceBeforeCombination = null
        super.stopEdition()
    }

    override fun stopItemEdition() {
        parentSplitAction?.let {
            returnToParent(it)
            return
        }
        sourceBeforeCombination?.takeIf { pendingCombination.isNotEmpty() }?.let { (reference, draft) ->
            pendingCombination = emptySet()
            sourceBeforeCombination = null
            restoreItemEdition(reference, draft)
            return
        }
        pendingCombination = emptySet()
        intentExtraEditor.stopEdition()
        super.stopItemEdition()
    }

    override fun upsertEditedItem() {
        parentSplitAction?.let { parent ->
            val child = editedItem.value ?: return
            val children = parent.subActions.toMutableList()
            children[editedSubActionIndex] = child
            returnToParent(parent.copy(subActions = children))
            return
        }
        if (pendingCombination.isNotEmpty()) {
            val parent = editedItem.value ?: return
            val current = editedList.value ?: return
            val index = current.indexOfFirst { it.id in pendingCombination }
            if (index < 0) return
            val updated = current.filterNot { it.id in pendingCombination }.toMutableList()
            updated.add(index, parent)
            updateList(updated)
            sourceBeforeCombination = null
            stopItemEdition()
        } else {
            super.upsertEditedItem()
        }
    }

    override fun deleteEditedItem() {
        parentSplitAction?.let { parent ->
            // A child editor must never delete the whole gesture.
            val children = if (parent.subActions.size > 2) {
                parent.subActions.filterIndexed { index, _ -> index != editedSubActionIndex }
                    .mapIndexed { index, action -> action.copyBase(priority = index) }
            } else parent.subActions
            returnToParent(parent.copy(subActions = children))
            return
        }
        if (pendingCombination.isNotEmpty()) {
            stopItemEdition()
            return
        }
        super.deleteEditedItem()
    }

    fun addSubAction(newSubAction: Action) {
        val current = editedItem.value as? SplitAction ?: return
        if (current.subActions.size >= 10 || (newSubAction !is Click && newSubAction !is Swipe)) return
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

    fun combineActions(actionA: Action, actionB: Action, newId: Identifier, childId: () -> Identifier): SplitAction? {
        val currentList = editedList.value?.toMutableList() ?: return null
        val idxA = currentList.indexOfFirst { it.id == actionA.id }
        val idxB = currentList.indexOfFirst { it.id == actionB.id }
        if (idxA == -1 || idxB == -1 || idxA == idxB) return null
        val children = listOf(minOf(idxA, idxB), maxOf(idxA, idxB)).map { index ->
            val stored = currentList[index]
            if (stored.id == actionA.id) actionA else if (stored.id == actionB.id) actionB else stored
        }.flatMap { it.touchActions() }
        if (children.size !in 2..10) return null

        val insertIndex = minOf(idxA, idxB)

        val splitAction = SplitAction(
            id = newId,
            eventId = actionA.eventId,
            name = (actionA as? SplitAction)?.name ?: (actionB as? SplitAction)?.name ?: "Simultaneous click/swipe",
            priority = insertIndex,
            subActions = children.mapIndexed { index, child -> child.copyBase(id = childId(), priority = index) },
        )

        val originalEdition = combinationSource(actionA)
        startItemEdition(splitAction)
        sourceBeforeCombination = originalEdition
        pendingCombination = setOf(actionA.id, actionB.id)
        return splitAction
    }

    fun combineActionWithNew(action: Action, newSubAction: Action, newId: Identifier, childId: () -> Identifier): SplitAction? {
        val currentList = editedList.value?.toMutableList() ?: return null
        val idx = currentList.indexOfFirst { it.id == action.id }
        if (idx == -1) return null
        val children = action.touchActions() + newSubAction.touchActions()
        if (children.size !in 2..10) return null

        val splitAction = SplitAction(
            id = newId,
            eventId = action.eventId,
            name = (action as? SplitAction)?.name ?: "Simultaneous click/swipe",
            priority = action.priority,
            subActions = children.mapIndexed { index, child -> child.copyBase(id = childId(), priority = index) },
        )

        val originalEdition = combinationSource(action)
        startItemEdition(splitAction)
        sourceBeforeCombination = originalEdition
        pendingCombination = setOf(action.id)
        return splitAction
    }

    override fun buildAllItemList(editedList: List<Action>?, editedItem: Action?): List<Action> {
        val draft = parentSplitAction ?: editedItem
        if (pendingCombination.isNotEmpty() && draft != null) {
            val source = editedList.orEmpty()
            val index = source.indexOfFirst { it.id in pendingCombination }
            return source.filterNot { it.id in pendingCombination }.toMutableList().apply {
                add(index.coerceIn(0, size), draft)
            }
        }
        return super.buildAllItemList(editedList, draft)
    }

    private fun Action.touchActions(): List<Action> = when (this) {
        is Click, is Swipe -> listOf(this)
        is SplitAction -> subActions
        else -> emptyList()
    }

    private fun combinationSource(source: Action): Pair<Action, Action>? {
        val draft = editedItem.value?.takeIf { it.id == source.id } ?: return null
        val reference = referenceEditedItem.value ?: return null
        return reference to draft
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
