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
package io.github.vibhor1102.macrion.core.domain.model.action

import io.github.vibhor1102.macrion.core.base.gesture.isTouchTimingValid
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import android.graphics.Point
import io.github.vibhor1102.macrion.core.base.identifier.Identifier

/**
 * Swipe action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param swipeDuration the duration between the swipe start and end in milliseconds.
 * @param from the x position of the swipe start.
 * @param to the x position of the swipe end.
 */
data class Swipe(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val swipeDuration: Long? = null,
    val from: Point? = null,
    val to: Point? = null,
    val path: SwipePath? = null,
    val waitBeforeMs: Long? = null,
    val waitAfterMs: Long? = null,
) : Action() {

    override fun isComplete(): Boolean =
        super.isComplete() && isTouchTimingValid(swipeDuration, waitBeforeMs, waitAfterMs) && from != null && to != null &&
            (path == null || (path.isValid() && path.start.toPoint() == from && path.end.toPoint() == to))

    override fun hashCodeNoIds(): Int =
        name.hashCode() + swipeDuration.hashCode() + from.hashCode() + to.hashCode() + path.hashCode() +
                waitBeforeMs.hashCode() + waitAfterMs.hashCode()

    override fun deepCopy(): Swipe = copy(name = "" + name)
}
