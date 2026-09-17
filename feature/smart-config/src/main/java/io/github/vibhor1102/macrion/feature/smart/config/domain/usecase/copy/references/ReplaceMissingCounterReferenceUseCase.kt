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

import android.util.Log

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
import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction

import javax.inject.Inject

class ReplaceMissingCounterReferenceUseCase @Inject constructor() {

    operator fun invoke(
        event: Event,
        itemToEdit: ItemWithMissingReferences,
        missingReference: MissingCopyReference.CounterReference,
        replacement: String,
    ): Event = when (itemToEdit) {
        is ItemWithMissingReferences.ActionItem ->
            event.replaceInAction(itemToEdit, missingReference, replacement)
        is ItemWithMissingReferences.ConditionItem ->
            event.replaceInCondition(itemToEdit, missingReference, replacement)
    }

    private fun Event.replaceInAction(
        itemToEdit: ItemWithMissingReferences.ActionItem,
        missingReference: MissingCopyReference.CounterReference,
        replacement: String,
    ): Event {
        val newAction = itemToEdit.item.replaceCounterReference(missingReference.name, replacement)
            ?: return this

        val actionIndex = actions.indexOf(itemToEdit.item)
        if (actionIndex !in actions.indices) {
            Log.e(TAG, "Can't find action to edit in provided event.")
            return this
        }

        return copyBase(
            actions = actions.toMutableList().apply {
                removeAt(actionIndex)
                add(actionIndex, newAction)
            }
        )
    }

    private fun Event.replaceInCondition(
        itemToEdit: ItemWithMissingReferences.ConditionItem,
        missingReference: MissingCopyReference.CounterReference,
        replacement: String,
    ): Event {
        val newCondition = itemToEdit.item.replaceCounterReference(missingReference.name, replacement)
            ?: return this

        val conditionIndex = conditions.indexOf(itemToEdit.item)
        if (conditionIndex !in conditions.indices) {
            Log.e(TAG, "Can't find condition to edit in provided event.")
            return this
        }

        return copyBase(
            conditions = conditions.toMutableList().apply {
                removeAt(conditionIndex)
                add(conditionIndex, newCondition)
            }
        )
    }

    private fun Action.replaceCounterReference(oldName: String, newName: String): Action? =
        when (this) {
            is ChangeCounter -> replaceCounterReference(oldName, newName)
            is Notification -> replaceCounterReference(oldName, newName)
            is SetText -> replaceCounterReference(oldName, newName)

            is Click,
            is ExternalAction,
            is Intent,
            is Pause,
            is PlaySound,
            is CaptureScreenshot,
            is Swipe,
            is SystemAction,
            is ToggleEvent -> {
                Log.e(TAG, "Can't replace counter reference, action type is not supported.")
                null
            }
        }

    private fun ChangeCounter.replaceCounterReference(oldName: String, newName: String): ChangeCounter =
        copy(
            counterName = if (counterName == oldName) newName else counterName,
            operationValue =
                if ((operationValue as? CounterOperationValue.Counter)?.value != oldName) operationValue
                else CounterOperationValue.Counter(newName),
        )

    private fun Notification.replaceCounterReference(oldName: String, newName: String): Notification =
        copy(messageText = messageText.replaceCounterName(oldName, newName))

    private fun SetText.replaceCounterReference(oldName: String, newName: String): SetText =
        copy(text = text.replaceCounterName(oldName, newName))

    private fun Condition.replaceCounterReference(oldName: String, newName: String): Condition? =
        when (this) {
            is TriggerCondition.OnCounterCountReached -> replaceCounterReference(oldName, newName)
            is ScreenCondition.Number -> replaceCounterReference(oldName, newName)

            is ScreenCondition.Color,
            is ScreenCondition.Image,
            is ScreenCondition.Text,
            is TriggerCondition.OnBroadcastReceived,
            is TriggerCondition.OnTimerReached -> {
                Log.e(TAG, "Can't replace counter reference, condition type is not supported.")
                null
            }
        }

    private fun TriggerCondition.OnCounterCountReached.replaceCounterReference(
        oldName: String,
        newName: String,
    ): TriggerCondition.OnCounterCountReached =
        copy(
            counterName = if (counterName == oldName) newName else counterName,
            counterValue =
                if ((counterValue as? CounterOperationValue.Counter)?.value != oldName) counterValue
                else CounterOperationValue.Counter(newName),
        )

    private fun ScreenCondition.Number.replaceCounterReference(
        oldName: String,
        newName: String,
    ): ScreenCondition.Number =
        copy(
            counterValue =
                if ((counterValue as? CounterOperationValue.Counter)?.value != oldName) counterValue
                else CounterOperationValue.Counter(newName),
        )
}

/** Replace all occurrences of `{oldName}` with `{newName}` in a counter-reference text template. */
private fun String.replaceCounterName(oldName: String, newName: String): String =
    replace(oldValue = "{$oldName}", newValue = "{$newName}")

private const val TAG = "ReplaceMissingCounterReferenceUseCase"
