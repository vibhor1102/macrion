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
package io.github.vibhor1102.macrion.core.dumb.domain.model

import android.graphics.Point
import io.github.vibhor1102.macrion.core.base.gesture.*
import io.github.vibhor1102.macrion.core.base.interfaces.Identifiable
import io.github.vibhor1102.macrion.core.base.identifier.Identifier

sealed class DumbAction : Identifiable {

    /** The identifier of the dumb scenario for this dumb action. */
    abstract val scenarioId: Identifier
    /** The name of the dumb action. */
    abstract val name: String?

    abstract val priority: Int

    abstract fun isValid(): Boolean

    fun copyWithNewScenarioId(scenarioId: Identifier): DumbAction =
        when (this) {
            is DumbClick -> copy(scenarioId = scenarioId)
            is DumbPause -> copy(scenarioId = scenarioId)
            is DumbSwipe -> copy(scenarioId = scenarioId)
            is DumbSplitAction -> copy(
                scenarioId = scenarioId,
                subActions = subActions.map { it.copyWithNewScenarioId(scenarioId) },
            )
        }

    data class DumbClick(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val position: Point,
        val pressDurationMs: Long,
        val waitBeforeMs: Long? = null,
        val waitAfterMs: Long? = null,
    ) : DumbAction(), RepeatableWithDelay {

        override fun isValid(): Boolean =
            name.isNotBlank() && isTouchTimingValid(pressDurationMs, waitBeforeMs, waitAfterMs) && position.x >= 0 && position.y >= 0 && isRepeatCountValid() && isRepeatDelayValid()
    }

    data class DumbSwipe(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        override val repeatCount: Int,
        override val isRepeatInfinite: Boolean,
        override val repeatDelayMs: Long,
        val fromPosition: Point,
        val toPosition: Point,
        val swipeDurationMs: Long,
        val waitBeforeMs: Long? = null,
        val waitAfterMs: Long? = null,
    ) : DumbAction(), RepeatableWithDelay {
        override fun isValid(): Boolean =
            name.isNotBlank() && isTouchTimingValid(swipeDurationMs, waitBeforeMs, waitAfterMs) && fromPosition.x >= 0 && fromPosition.y >= 0 && toPosition.x >= 0 && toPosition.y >= 0 && isRepeatCountValid() && isRepeatDelayValid()
    }

    data class DumbSplitAction(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        override val repeatCount: Int = 1,
        override val isRepeatInfinite: Boolean = false,
        override val repeatDelayMs: Long = 0L,
        val subActions: List<DumbAction> = emptyList(),
        val waitBeforeMs: Long? = null,
        val waitAfterMs: Long? = null,
    ) : DumbAction(), RepeatableWithDelay {

        override fun isValid(): Boolean =
            name.isNotBlank() && subActions.size in 2..MAX_TOUCH_STROKES && subActions.all {
                it.isValid() && when (it) {
                    is DumbClick -> isCombinedTouchTimingValid(it.pressDurationMs, it.waitBeforeMs, it.waitAfterMs)
                    is DumbSwipe -> isCombinedTouchTimingValid(it.swipeDurationMs, it.waitBeforeMs, it.waitAfterMs)
                    else -> false
                }
            } && (waitBeforeMs ?: 0L) in 0..MAX_TOUCH_DURATION_MS && (waitAfterMs ?: 0L) in 0..MAX_TOUCH_DURATION_MS && isRepeatCountValid() && isRepeatDelayValid()
    }

    data class DumbPause(
        override val id: Identifier,
        override val scenarioId: Identifier,
        override val name: String,
        override val priority: Int = 0,
        val pauseDurationMs: Long,
    ) : DumbAction() {

        override fun isValid(): Boolean = name.isNotBlank()
    }
}
