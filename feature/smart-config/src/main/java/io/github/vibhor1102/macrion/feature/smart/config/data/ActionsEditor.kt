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
            stopItemEdition()
        } else {
            super.upsertEditedItem()
        }
    }

    override fun deleteEditedItem() {
        val parent = parentSplitAction
        if (parent != null) {
            parentSplitAction = null
            editedSubActionIndex = -1
            val currentList = editedList.value?.toMutableList() ?: return
            val parentIndex = currentList.indexOfAction(parent)
            if (parentIndex != -1) {
                currentList.removeAt(parentIndex)
                updateList(currentList)
            }
            stopItemEdition()
        } else {
            super.deleteEditedItem()
        }
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