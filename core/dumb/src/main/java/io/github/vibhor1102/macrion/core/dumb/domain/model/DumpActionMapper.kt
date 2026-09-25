/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.dumb.domain.model

import android.graphics.Point

import io.github.vibhor1102.macrion.core.base.identifier.DATABASE_ID_INSERTION
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType

import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionWithSubActions
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbSplitActionItemEntity

internal fun DumbActionEntity.toDomain(asDomain: Boolean = false): DumbAction = when (type) {
    DumbActionType.CLICK -> toDomainClick(asDomain)
    DumbActionType.SWIPE -> toDomainSwipe(asDomain)
    DumbActionType.PAUSE -> toDomainPause(asDomain)
    DumbActionType.SPLIT_ACTION -> DumbAction.DumbSplitAction(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name.trim(),
        priority = priority,
        repeatCount = repeatCount ?: 1,
        isRepeatInfinite = isRepeatInfinite ?: false,
        repeatDelayMs = repeatDelay ?: 0L,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
        subActions = emptyList(),
    )
}

internal fun DumbActionWithSubActions.toDomain(asDomain: Boolean = false): DumbAction = when (action.type) {
    DumbActionType.SPLIT_ACTION -> DumbAction.DumbSplitAction(
        id = Identifier(id = action.id, asTemporary = asDomain),
        scenarioId = Identifier(id = action.dumbScenarioId, asTemporary = asDomain),
        name = action.name.trim(),
        priority = action.priority,
        repeatCount = action.repeatCount ?: 1,
        isRepeatInfinite = action.isRepeatInfinite ?: false,
        repeatDelayMs = action.repeatDelay ?: 0L,
        waitBeforeMs = action.waitBeforeMs,
        waitAfterMs = action.waitAfterMs,
        subActions = splitItems.sortedBy { it.priority }.map { it.toDomain(asDomain, action.dumbScenarioId) },
    )
    else -> action.toDomain(asDomain)
}

internal fun DumbSplitActionItemEntity.toDomain(asDomain: Boolean, scenarioDbId: Long): DumbAction =
    when (type) {
        DumbActionType.CLICK -> DumbAction.DumbClick(
            id = Identifier(id = id, asTemporary = asDomain),
            scenarioId = Identifier(id = scenarioDbId, asTemporary = asDomain),
            name = name ?: "Tap",
            priority = priority,
            repeatCount = repeatCount,
            isRepeatInfinite = isRepeatInfinite,
            repeatDelayMs = repeatDelayMs,
            position = Point(fromX ?: 0, fromY ?: 0),
            pressDurationMs = duration ?: 50L,
            waitBeforeMs = startOffset.takeIf { it > 0 },
            waitAfterMs = waitAfterMs,
        )
        else -> DumbAction.DumbSwipe(
            id = Identifier(id = id, asTemporary = asDomain),
            scenarioId = Identifier(id = scenarioDbId, asTemporary = asDomain),
            name = name ?: "Swipe",
            priority = priority,
            repeatCount = repeatCount,
            isRepeatInfinite = isRepeatInfinite,
            repeatDelayMs = repeatDelayMs,
            fromPosition = Point(fromX ?: 0, fromY ?: 0),
            toPosition = Point(toX ?: 0, toY ?: 0),
            path = swipePath,
            swipeDurationMs = duration ?: 300L,
            waitBeforeMs = startOffset.takeIf { it > 0 },
            waitAfterMs = waitAfterMs,
        )
    }

internal fun DumbAction.toEntity(scenarioDbId: Long = DATABASE_ID_INSERTION): DumbActionEntity = when (this) {
    is DumbAction.DumbClick -> toClickEntity(scenarioDbId)
    is DumbAction.DumbSwipe -> toSwipeEntity(scenarioDbId)
    is DumbAction.DumbPause -> toPauseEntity(scenarioDbId)
    is DumbAction.DumbSplitAction -> toSplitEntity(scenarioDbId)
}

internal fun DumbAction.toSplitItemEntity(actionDbId: Long, priority: Int): DumbSplitActionItemEntity =
    when (this) {
        is DumbAction.DumbClick -> DumbSplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = priority,
            type = DumbActionType.CLICK,
            name = name, repeatCount = repeatCount, isRepeatInfinite = isRepeatInfinite, repeatDelayMs = repeatDelayMs,
            fromX = position.x,
            fromY = position.y,
            duration = pressDurationMs,
            startOffset = waitBeforeMs ?: 0L,
            waitAfterMs = waitAfterMs,
        )
        is DumbAction.DumbSwipe -> DumbSplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = priority,
            type = DumbActionType.SWIPE,
            name = name, repeatCount = repeatCount, isRepeatInfinite = isRepeatInfinite, repeatDelayMs = repeatDelayMs,
            fromX = fromPosition.x,
            fromY = fromPosition.y,
            toX = toPosition.x,
            toY = toPosition.y,
            swipePath = path,
            duration = swipeDurationMs,
            startOffset = waitBeforeMs ?: 0L,
            waitAfterMs = waitAfterMs,
        )
        else -> DumbSplitActionItemEntity(
            id = id.databaseId,
            actionId = actionDbId,
            priority = priority,
            type = DumbActionType.SWIPE,
        )
    }

private fun DumbAction.DumbSplitAction.toSplitEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, SplitAction is incomplete.")
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name.trim(),
        priority = priority,
        type = DumbActionType.SPLIT_ACTION,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )
}

private fun DumbActionEntity.toDomainClick(asDomain: Boolean): DumbAction.DumbClick =
    DumbAction.DumbClick(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name.trim(),
        priority = priority,
        position = Point(x!!, y!!),
        pressDurationMs = pressDuration!!,
        repeatCount = repeatCount!!,
        isRepeatInfinite = isRepeatInfinite!!,
        repeatDelayMs = repeatDelay!!,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )

private fun DumbActionEntity.toDomainSwipe(asDomain: Boolean): DumbAction.DumbSwipe =
    DumbAction.DumbSwipe(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name.trim(),
        priority = priority,
        fromPosition = Point(fromX!!, fromY!!),
        toPosition = Point(toX!!, toY!!),
        path = swipePath,
        swipeDurationMs = swipeDuration!!,
        repeatCount = repeatCount!!,
        isRepeatInfinite = isRepeatInfinite!!,
        repeatDelayMs = repeatDelay!!,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )

private fun DumbActionEntity.toDomainPause(asDomain: Boolean): DumbAction.DumbPause =
    DumbAction.DumbPause(
        id = Identifier(id = id, asTemporary = asDomain),
        scenarioId = Identifier(id = dumbScenarioId, asTemporary = asDomain),
        name = name.trim(),
        priority = priority,
        pauseDurationMs = pauseDuration!!,
    )

private fun DumbAction.DumbClick.toClickEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name.trim(),
        priority = priority,
        type = DumbActionType.CLICK,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        pressDuration = pressDurationMs,
        x = position.x,
        y = position.y,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )
}

private fun DumbAction.DumbSwipe.toSwipeEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name.trim(),
        priority = priority,
        type = DumbActionType.SWIPE,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        swipeDuration = swipeDurationMs,
        fromX = fromPosition.x,
        fromY = fromPosition.y,
        toX = toPosition.x,
        toY = toPosition.y,
        swipePath = path,
        waitBeforeMs = waitBeforeMs,
        waitAfterMs = waitAfterMs,
    )
}

private fun DumbAction.DumbPause.toPauseEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")

    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = if (scenarioDbId != DATABASE_ID_INSERTION) scenarioDbId else scenarioId.databaseId,
        name = name.trim(),
        priority = priority,
        type = DumbActionType.PAUSE,
        pauseDuration = pauseDurationMs,
    )
}
