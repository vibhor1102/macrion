/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.database.entity.*

/** OR clicks use the detected result, not a stored condition id. AND targets belong to this event. */
internal fun CompleteEventEntity.hasValidClickTarget(type: ClickPositionType?, conditionId: Long?): Boolean {
    if (type != ClickPositionType.ON_DETECTED_CONDITION) return true
    if (event.type != EventType.IMAGE_EVENT) return false
    return when (event.conditionOperator) {
        2 -> true
        1 -> conditions.any { it.id == conditionId && it.shouldBeDetected != false && it.type in backupScreenConditionTypes }
        else -> false
    }
}

/** Remove ignored ids before copying: they must not become mandatory database references. */
internal fun CompleteEventEntity.normalizeIgnoredClickTargets(): CompleteEventEntity = copy(
    actions = actions.map { complete ->
        fun clearTarget(type: ActionType, positionType: ClickPositionType?): Boolean =
            type == ActionType.CLICK && (positionType != ClickPositionType.ON_DETECTED_CONDITION || event.conditionOperator == 2)
        complete.copy(
            action = complete.action.let { action ->
                if (clearTarget(action.type, action.clickPositionType)) action.copy(clickOnConditionId = null) else action
            },
            splitItems = complete.splitItems.map { item ->
                if (clearTarget(item.type, item.clickPositionType)) item.copy(clickOnConditionId = null) else item
            },
        )
    },
)

internal fun ActionEntity.positionCounterNames(): List<String> = when (type) {
    ActionType.CLICK -> positionCounters?.names().orEmpty()
    ActionType.SWIPE -> fromCounters?.names().orEmpty() + toCounters?.names().orEmpty()
    else -> emptyList()
}

internal fun SplitActionItemEntity.positionCounterNames(): List<String> = when (type) {
    ActionType.CLICK -> positionCounters?.names().orEmpty()
    ActionType.SWIPE -> fromCounters?.names().orEmpty() + toCounters?.names().orEmpty()
    else -> emptyList()
}

internal val backupScreenConditionTypes = setOf(ConditionType.ON_IMAGE_DETECTED, ConditionType.ON_COLOR_DETECTED,
    ConditionType.ON_TEXT_DETECTED, ConditionType.ON_NUMBER_DETECTED)

/** Evaluate only fields used by this action, never stale metadata from another action type. */
internal fun ActionEntity.isUnsupportedByLegacyKlickr(): Boolean =
    type in setOf(ActionType.EXTERNAL_ACTION, ActionType.PLAY_SOUND, ActionType.CAPTURE_SCREENSHOT, ActionType.SPLIT_ACTION) ||
        positionCounterNames().isNotEmpty() ||
        (type == ActionType.CHANGE_COUNTER && counterOperationValueType == CounterOperationValueType.CONDITION) ||
        (type == ActionType.SWIPE && swipePath?.isCurved == true)
