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

/**
 * Action that plays an audio notification sound.
 *
 * @param id the unique identifier for this action.
 * @param eventId the identifier of the parent event.
 * @param name the user-defined name of this action.
 * @param priority the execution priority.
 * @param soundUri the URI of the notification sound to play.
 * @param soundTitle the user-friendly title of the sound (cached for display).
 */
data class PlaySound(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int = 0,
    val soundUri: String? = null,
    val soundTitle: String? = null,
) : Action() {

    override fun hashCodeNoIds(): Int =
        (name?.hashCode() ?: 0) + (soundUri?.hashCode() ?: 0) + (soundTitle?.hashCode() ?: 0)

    override fun deepCopy(): PlaySound = copy(
        name = "" + name,
        soundUri = soundUri?.let { "" + it },
        soundTitle = soundTitle?.let { "" + it },
    )

    override fun isComplete(): Boolean =
        super.isComplete() && !soundUri.isNullOrBlank()
}
