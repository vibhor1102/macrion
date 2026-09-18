/*
 * Copyright (C) 2024 Kevin Buzeau
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

import io.github.vibhor1102.macrion.core.base.interfaces.Identifiable
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.interfaces.Completable
import io.github.vibhor1102.macrion.core.base.interfaces.Prioritizable

/** Base for for all possible actions for an Event. */
sealed class Action : Identifiable, Completable, Prioritizable {

    /** The identifier of the event for this action. */
    abstract val eventId: Identifier
    /** The name of the action. */
    abstract val name: String?

    /** @return true if this action is complete and can be transformed into its entity. */
    override fun isComplete(): Boolean = !name.isNullOrBlank()

    abstract fun hashCodeNoIds(): Int

    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    fun copyBase(
        id: Identifier = this.id,
        eventId: Identifier = this.eventId,
        name: String? = this.name,
        priority: Int = this.priority,
    ): Action =
        when (this) {
            is Click -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ChangeCounter -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ExternalAction -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Intent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Pause -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Swipe -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ToggleEvent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Notification -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is SystemAction -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is SetText -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is PlaySound -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is CaptureScreenshot -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is SplitAction -> copy(id = id, eventId = eventId, name = name, priority = priority)
        }
}
