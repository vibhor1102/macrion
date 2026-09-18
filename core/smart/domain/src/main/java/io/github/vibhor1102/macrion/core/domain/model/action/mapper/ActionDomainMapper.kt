package io.github.vibhor1102.macrion.core.domain.model.action.mapper

import android.content.ComponentName
import android.graphics.Point
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ChangeCounterOperationType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.EventToggleType
import io.github.vibhor1102.macrion.core.database.entity.SystemActionType
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
import io.github.vibhor1102.macrion.core.domain.model.action.intent.toDomainIntentExtra
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.toDomain
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction

/** Convert an Action entity into a Domain Action. */
internal fun CompleteActionEntity.toDomain(cleanIds: Boolean = false): Action = when (action.type) {
    ActionType.CLICK -> toDomainClick(cleanIds)
    ActionType.SWIPE -> toDomainSwipe(cleanIds)
    ActionType.PAUSE -> toDomainPause(cleanIds)
    ActionType.INTENT -> toDomainIntent(cleanIds)
    ActionType.TOGGLE_EVENT -> toDomainToggleEvent(cleanIds)
    ActionType.CHANGE_COUNTER -> toDomainChangeCounter(cleanIds)
    ActionType.EXTERNAL_ACTION -> toDomainExternalAction(cleanIds)
    ActionType.NOTIFICATION -> toDomainNotification(cleanIds)
    ActionType.SYSTEM -> toDomainSystem(cleanIds)
    ActionType.TEXT -> toDomainSetText(cleanIds)
    ActionType.PLAY_SOUND -> toDomainPlaySound(cleanIds)
    ActionType.CAPTURE_SCREENSHOT -> toDomainCaptureScreenshot(cleanIds)
    ActionType.SPLIT_ACTION -> toDomainSplitAction(cleanIds)
}

private fun CompleteActionEntity.toDomainClick(cleanIds: Boolean = false) = Click(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pressDuration = action.pressDuration!!,
    positionType = action.clickPositionType!!.toDomain(),
    position = getPositionIfValid(action.x, action.y),
    clickOnConditionId = action.clickOnConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
    clickOffset =
        if (action.clickOffsetX != null && action.clickOffsetY != null) Point(action.clickOffsetX!!, action.clickOffsetY!!)
        else null,
    waitBeforeMs = action.waitBeforeMs,
    waitAfterMs = action.waitAfterMs,
)

private fun CompleteActionEntity.toDomainSwipe(cleanIds: Boolean = false) = Swipe(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    swipeDuration = action.swipeDuration!!,
    from = getPositionIfValid(action.fromX, action.fromY),
    to = getPositionIfValid(action.toX, action.toY),
    waitBeforeMs = action.waitBeforeMs,
    waitAfterMs = action.waitAfterMs,
)

private fun CompleteActionEntity.toDomainPause(cleanIds: Boolean = false) = Pause(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    pauseDuration = action.pauseDuration!!,
)

private fun CompleteActionEntity.toDomainIntent(cleanIds: Boolean = false) = Intent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    isAdvanced = action.isAdvanced,
    isBroadcast = action.isBroadcast ?: false,
    intentAction = action.intentAction,
    componentName = action.componentName.toComponentName(),
    flags = action.flags,
    extras = intentExtras.map { it.toDomainIntentExtra(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainToggleEvent(cleanIds: Boolean = false) = ToggleEvent(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    toggleAll = action.toggleAll == true,
    toggleAllType = action.toggleAllType?.toDomain(),
    eventToggles = eventsToggle.map { it.toDomain(cleanIds) }.toMutableList(),
)

private fun CompleteActionEntity.toDomainChangeCounter(cleanIds: Boolean = false) = ChangeCounter(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    counterName = action.counterName!!,
    operation = action.counterOperation!!.toDomain(),
    operationValue = CounterOperationValue.getCounterOperationValue(
        type = action.counterOperationValueType,
        numberValue = action.counterOperationValue,
        counterName = action.counterOperationCounterName,
    ),
)

private fun CompleteActionEntity.toDomainExternalAction(cleanIds: Boolean = false) = ExternalAction(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    externalActionName = action.externalActionName ?: "",
)

private fun CompleteActionEntity.toDomainNotification(cleanIds: Boolean = false) = Notification(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    channelImportance = action.notificationImportance!!,
    messageText = action.notificationMessageText!!,
)

private fun CompleteActionEntity.toDomainSystem(cleanIds: Boolean = false) = SystemAction(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    type = action.systemActionType?.toDomain()!!,
)

private fun CompleteActionEntity.toDomainSetText(cleanIds: Boolean = false) = SetText(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    text = action.textValue ?: "",
    validateInput = action.textValidateInput ?: false,
)

private fun CompleteActionEntity.toDomainPlaySound(cleanIds: Boolean = false) = PlaySound(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    soundUri = action.soundUri,
    soundTitle = action.soundTitle,
)

private fun CompleteActionEntity.toDomainCaptureScreenshot(cleanIds: Boolean = false) = CaptureScreenshot(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    screenshotFolderUri = action.screenshotFolderUri,
    screenshotFolderName = action.screenshotFolderName,
)

private fun ClickPositionType.toDomain(): Click.PositionType =
    Click.PositionType.valueOf(name)

private fun EventToggleType.toDomain(): ToggleEvent.ToggleType =
    ToggleEvent.ToggleType.valueOf(name)

private fun ChangeCounterOperationType.toDomain(): ChangeCounter.OperationType =
    ChangeCounter.OperationType.valueOf(name)

private fun SystemActionType.toDomain(): SystemAction.Type =
    SystemAction.Type.valueOf(name)

private fun String?.toComponentName(): ComponentName? = this?.let {
    ComponentName.unflattenFromString(it)
}

private fun getPositionIfValid(x: Int?, y: Int?): Point? =
    if (x != null && y != null) Point(x, y) else null

private fun CompleteActionEntity.toDomainSplitAction(cleanIds: Boolean = false) = SplitAction(
    id = Identifier(id = action.id, asTemporary = cleanIds),
    eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
    name = action.name,
    priority = action.priority,
    subActions = splitItems.sortedBy { it.priority }.map { item ->
        when (item.type) {
            ActionType.SWIPE -> Swipe(
                id = Identifier(id = item.id, asTemporary = cleanIds),
                eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
                name = item.name ?: "Swipe ${item.priority + 1}",
                priority = item.priority,
                swipeDuration = item.duration,
                from = getPositionIfValid(item.fromX, item.fromY),
                to = getPositionIfValid(item.toX, item.toY),
                waitBeforeMs = item.startOffset.takeIf { it > 0 },
                waitAfterMs = item.waitAfterMs,
            )
            ActionType.CLICK -> Click(
                id = Identifier(id = item.id, asTemporary = cleanIds),
                eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
                name = item.name ?: "Click ${item.priority + 1}",
                priority = item.priority,
                pressDuration = item.duration ?: 50L,
                positionType = item.clickPositionType?.let { Click.PositionType.valueOf(it.name) } ?: Click.PositionType.USER_SELECTED,
                clickOnConditionId = item.clickOnConditionId?.let { Identifier(id = it, asTemporary = cleanIds) },
                clickOffset = getPositionIfValid(item.clickOffsetX, item.clickOffsetY),
                position = getPositionIfValid(item.fromX, item.fromY),
                waitBeforeMs = item.startOffset.takeIf { it > 0 },
                waitAfterMs = item.waitAfterMs,
            )
            else -> Swipe(
                id = Identifier(id = item.id, asTemporary = cleanIds),
                eventId = Identifier(id = action.eventId, asTemporary = cleanIds),
                name = item.name ?: "Swipe ${item.priority + 1}",
                priority = item.priority,
                swipeDuration = item.duration,
                from = getPositionIfValid(item.fromX, item.fromY),
                to = getPositionIfValid(item.toX, item.toY),
                waitBeforeMs = item.startOffset.takeIf { it > 0 },
                waitAfterMs = item.waitAfterMs,
            )
        }
    }
)
