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
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import javax.inject.Inject


class ReplaceMissingScreenConditionReferenceUseCase @Inject constructor() {

    operator fun invoke(
        event: Event,
        itemToEdit: ItemWithMissingReferences.ActionItem,
        missingReference: MissingCopyReference.ScreenConditionReference,
        replacement: ScreenCondition,
    ): Event {

        val newAction = itemToEdit.item.replaceScreenConditionReference(missingReference, replacement)
            ?: return event

        val actionIndex = event.actions.indexOf(itemToEdit.item)
        if (actionIndex !in event.actions.indices) {
            Log.e(TAG, "Can't find action to edit in provided event.")
            return event
        }

        return event.copyBase(
            actions = event.actions.toMutableList().apply {
                removeAt(actionIndex)
                add(actionIndex, newAction)
            }
        )
    }

    private fun Action.replaceScreenConditionReference(
        missingReference: MissingCopyReference.ScreenConditionReference,
        replacement: ScreenCondition,
    ): Action? {

        when (this) {
            is Click -> {
                if (clickOnConditionId == missingReference.conditionId) {
                    return copy(clickOnConditionId = replacement.id)
                }

                Log.e(TAG, "Can't replace, can't find missing reference to replace")
                return null
            }

            is ChangeCounter,
            is ExternalAction,
            is Intent,
            is Notification,
            is Pause,
            is PlaySound,
            is CaptureScreenshot,
            is SetText,
            is Swipe,
            is SystemAction,
            is ToggleEvent -> {
                Log.e(TAG, "Can't replace, action type is not supported.")
                return null
            }
        }
    }
}

private const val TAG = "ReplaceMissingScreenConditionReferenceUseCase"
