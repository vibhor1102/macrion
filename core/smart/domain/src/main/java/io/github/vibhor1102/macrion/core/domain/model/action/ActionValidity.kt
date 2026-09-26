/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.domain.model.action

import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.event.TriggerEvent

/** Action validity also depends on the event that will execute it. */
fun Action.isValidForEvent(event: Event?): Boolean {
    if (!isComplete()) return false
    return when (this) {
        is Click -> when (event) {
            is TriggerEvent -> positionType == Click.PositionType.USER_SELECTED && clickOnConditionId == null
            is ScreenEvent -> positionType != Click.PositionType.ON_DETECTED_CONDITION ||
                event.conditionOperator != AND || event.conditions.any {
                    it.id == clickOnConditionId && it.shouldBeDetected
                }
            else -> true
        }
        is SplitAction -> subActions.all { it.isValidForEvent(event) }
        else -> true
    }
}

/** A child must also fit within the timing and type constraints of one multi-touch gesture. */
fun Action.isValidMultiTouchChildForEvent(event: Event?): Boolean =
    SplitAction.isSubActionComplete(this) && isValidForEvent(event)
