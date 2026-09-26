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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.domain.model.action

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.gesture.*

/**
 * Split action containing two or more sub-actions executed concurrently as a single multi-stroke gesture.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param priority the execution order of this action in the event.
 * @param subActions the list of sub-actions (e.g. two Swipes for a zoom gesture) executed simultaneously.
 */
data class SplitAction(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val subActions: List<Action> = emptyList(),
) : Action() {

    override fun isComplete(): Boolean =
        super.isComplete() && subActions.size in 2..MAX_TOUCH_STROKES && subActions.all { isSubActionComplete(it) }

    override fun hashCodeNoIds(): Int =
        (name?.hashCode() ?: 0) + subActions.sumOf { it.hashCodeNoIds() }

    override fun deepCopy(): SplitAction = copy(
        name = "" + name,
        subActions = subActions.map { it.deepCopy() },
    )

    companion object {
        fun isSubActionComplete(action: Action): Boolean = when (action) {
            is Swipe -> action.isComplete() && isCombinedTouchTimingValid(action.swipeDuration, action.waitBeforeMs, action.waitAfterMs)
            is Click -> action.isComplete() && isCombinedTouchTimingValid(action.pressDuration, action.waitBeforeMs, action.waitAfterMs)
            else -> false
        }
    }
}
