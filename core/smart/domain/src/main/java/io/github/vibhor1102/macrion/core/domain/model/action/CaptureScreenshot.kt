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
 * Action that captures a screenshot and saves it to storage.
 *
 * @param id the unique identifier for this action.
 * @param eventId the identifier of the parent event.
 * @param name the user-defined name of this action.
 * @param priority the execution priority.
 * @param screenshotFolderUri the optional SAF URI of a custom destination folder (null for default Pictures/Macrion).
 * @param screenshotFolderName the user-friendly display name of the custom folder (cached for display).
 */
data class CaptureScreenshot(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int = 0,
    val screenshotFolderUri: String? = null,
    val screenshotFolderName: String? = null,
) : Action() {

    override fun hashCodeNoIds(): Int =
        (name?.hashCode() ?: 0) + (screenshotFolderUri?.hashCode() ?: 0) + (screenshotFolderName?.hashCode() ?: 0)

    override fun deepCopy(): CaptureScreenshot = copy(
        name = "" + name,
        screenshotFolderUri = screenshotFolderUri?.let { "" + it },
        screenshotFolderName = screenshotFolderName?.let { "" + it },
    )
}
