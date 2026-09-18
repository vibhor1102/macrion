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
package io.github.vibhor1102.macrion.feature.dumb.config.domain

import android.content.Context
import android.graphics.Point

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.identifier.IdentifierCreator
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction

class EditedDumbActionsBuilder {

    private val dumbActionsIdCreator = IdentifierCreator()

    private var dumbScenarioId: Identifier? = null

    internal fun startEdition(scenarioId: Identifier) {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = scenarioId
    }

    internal fun clearState() {
        dumbActionsIdCreator.resetIdCount()
        dumbScenarioId = null
    }

    fun generateNewIdentifier(): Identifier = dumbActionsIdCreator.generateNewIdentifier()

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        DumbAction.DumbClick(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbClickName(),
            position = position,
            pressDurationMs = context.getDefaultDumbClickDurationMs(),
            repeatCount = context.getDefaultDumbClickRepeatCount(),
            isRepeatInfinite = false,
            repeatDelayMs = context.getDefaultDumbClickRepeatDelay(),
        )

    fun createNewDumbSwipe(context: Context, from: Point, to: Point): DumbAction.DumbSwipe =
        DumbAction.DumbSwipe(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbSwipeName(),
            fromPosition = from,
            toPosition = to,
            swipeDurationMs = context.getDefaultDumbSwipeDurationMs(),
            repeatCount = context.getDefaultDumbSwipeRepeatCount(),
            isRepeatInfinite = false,
            repeatDelayMs = context.getDefaultDumbSwipeRepeatDelay(),
        )

    fun createNewDumbPause(context: Context): DumbAction.DumbPause =
        DumbAction.DumbPause(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getDefaultDumbPauseName(),
            pauseDurationMs = context.getDefaultDumbPauseDurationMs(),
        )

    fun createNewDumbZoomInOut(context: Context): DumbAction.DumbSplitAction {
        val swipe1 = createNewDumbSwipe(context, Point(-1, -1), Point(-1, -1)).copy(
            name = "Swipe 1",
            priority = 0,
        )
        val swipe2 = createNewDumbSwipe(context, Point(-1, -1), Point(-1, -1)).copy(
            name = "Swipe 2",
            priority = 1,
        )
        return DumbAction.DumbSplitAction(
            id = dumbActionsIdCreator.generateNewIdentifier(),
            scenarioId = getEditedScenarioIdOrThrow(),
            name = context.getString(io.github.vibhor1102.macrion.feature.dumb.config.R.string.action_type_zoom),
            subActions = listOf(swipe1, swipe2),
        )
    }

    fun createNewDumbActionFrom(from: DumbAction): DumbAction =
        when (from) {
            is DumbAction.DumbClick -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbSwipe -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbPause -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
            )
            is DumbAction.DumbSplitAction -> from.copy(
                id = dumbActionsIdCreator.generateNewIdentifier(),
                scenarioId = getEditedScenarioIdOrThrow(),
                subActions = from.subActions.map { createNewDumbActionFrom(it) },
            )
        }

    private fun getEditedScenarioIdOrThrow(): Identifier = dumbScenarioId
        ?: throw IllegalStateException("Can't create items without an edited dumb scenario")
}