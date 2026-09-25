/*
 * Copyright (C) 2025 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.domain.model.action.mapper

import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.CounterOperationValueType
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.database.entity.SplitActionItemEntity


internal fun Action.toEntity(): ActionEntity {
    if (!isComplete()) throw IllegalStateException("Can't transform to entity, action is incomplete: $this")

    return when (this) {
        is Click -> toClickEntity()
        is Swipe -> toSwipeEntity()
        is Pause -> toPauseEntity()
        is Intent -> toIntentEntity()
        is ToggleEvent -> toToggleEventEntity()
        is ChangeCounter -> toChangeCounterEntity()
        is ExternalAction -> toExternalActionEntity()
        is Notification -> toNotificationEntity()
        is SystemAction -> toSystemActionEntity()
        is SetText -> toSetTextEntity()
        is PlaySound -> toPlaySoundEntity()
        is CaptureScreenshot -> toCaptureScreenshotEntity()
        is SplitAction -> toSplitActionEntity()
    }
}

private fun Click.toClickEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.CLICK,
        pressDuration = pressDuration,
        clickPositionType = positionType.toEntity(),
        x = position?.x,
        y = position?.y,
        clickOnConditionId = clickOnConditionId?.databaseId,
        clickOffsetX = clickOffset?.x,
        clickOffsetY = clickOffset?.y,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )

private fun Swipe.toSwipeEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.SWIPE,
        swipeDuration = swipeDuration,
        fromX = from?.x,
        fromY = from?.y,
        toX = to?.x,
        toY = to?.y,
        swipePath = path,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )

private fun Pause.toPauseEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.PAUSE,
        pauseDuration = pauseDuration,
    )

private fun Intent.toIntentEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.INTENT,
        isAdvanced = isAdvanced,
        isBroadcast = isBroadcast,
        intentAction = intentAction,
        componentName = componentName?.flattenToString(),
        flags = flags,
    )

private fun ToggleEvent.toToggleEventEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.TOGGLE_EVENT,
        toggleAllType = toggleAllType?.toEntity(),
        toggleAll = toggleAll,
    )

private fun ChangeCounter.toChangeCounterEntity(): ActionEntity {
    val isNumberValue = operationValue is CounterOperationValue.Number

    return ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.CHANGE_COUNTER,
        counterName = counterName,
        counterOperation = operation.toEntity(),
        counterOperationValueType = if (isNumberValue) CounterOperationValueType.NUMBER else CounterOperationValueType.COUNTER,
        counterOperationValue = if (isNumberValue) operationValue.value else null,
        counterOperationCounterName = if (isNumberValue) null else operationValue.value as String,
    )
}

private fun ExternalAction.toExternalActionEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.EXTERNAL_ACTION,
        externalActionName = externalActionName.trim(),
    )

private fun Notification.toNotificationEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.NOTIFICATION,
        notificationImportance = channelImportance,
        notificationMessageText = messageText,
    )

private fun SystemAction.toSystemActionEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.SYSTEM,
        systemActionType = type.toEntity(),
    )

private fun SetText.toSetTextEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.TEXT,
        textValue = text,
        textValidateInput = validateInput,
    )

private fun PlaySound.toPlaySoundEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.PLAY_SOUND,
        soundUri = soundUri,
        soundTitle = soundTitle,
    )

private fun CaptureScreenshot.toCaptureScreenshotEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.CAPTURE_SCREENSHOT,
        screenshotFolderUri = screenshotFolderUri,
        screenshotFolderName = screenshotFolderName,
    )

private fun SplitAction.toSplitActionEntity(): ActionEntity =
    ActionEntity(
        id = id.databaseId,
        eventId = eventId.databaseId,
        priority = priority,
        name = name!!.trim(),
        type = ActionType.SPLIT_ACTION,
    )

internal fun Action.toSplitItemEntity(actionDbId: Long, itemPriority: Int): SplitActionItemEntity =
    when (this) {
        is Swipe -> SplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = itemPriority,
            name = name,
            type = ActionType.SWIPE,
            fromX = from?.x,
            fromY = from?.y,
            toX = to?.x,
            toY = to?.y,
            swipePath = path,
            duration = swipeDuration,
            startOffset = waitBeforeMs ?: 0L,
            waitAfterMs = waitAfterMs,
        )
        is Click -> SplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = itemPriority,
            name = name,
            type = ActionType.CLICK,
            clickPositionType = positionType.toEntity(),
            clickOnConditionId = clickOnConditionId?.databaseId,
            clickOffsetX = clickOffset?.x,
            clickOffsetY = clickOffset?.y,
            fromX = position?.x,
            fromY = position?.y,
            duration = pressDuration,
            startOffset = waitBeforeMs ?: 0L,
            waitAfterMs = waitAfterMs,
        )
        else -> SplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = itemPriority,
            name = name,
            type = ActionType.SWIPE,
        )
    }
