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
package io.github.vibhor1102.macrion.core.dumb.engine

import android.accessibilityservice.GestureDescription
import io.github.vibhor1102.macrion.core.base.gesture.combinedTouchTailDelay
import io.github.vibhor1102.macrion.core.common.actions.gesture.addStroke
import android.graphics.Path
import android.util.Log

import io.github.vibhor1102.macrion.core.base.workarounds.UnblockGestureScheduler
import io.github.vibhor1102.macrion.core.base.workarounds.buildUnblockGesture
import io.github.vibhor1102.macrion.core.common.actions.AndroidActionExecutor
import io.github.vibhor1102.macrion.core.common.actions.gesture.buildSingleStroke
import io.github.vibhor1102.macrion.core.common.actions.gesture.line
import io.github.vibhor1102.macrion.core.common.actions.gesture.moveTo
import io.github.vibhor1102.macrion.core.common.actions.utils.getPauseDurationMs
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.domain.model.Repeatable

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DumbActionExecutor @Inject constructor(
    private val androidExecutor: AndroidActionExecutor,
) {

    private val randomSource = Random(System.currentTimeMillis())
    private val random: Random?
        get() = randomSource.takeIf { randomize }
    private var randomize: Boolean = false

    private var unblockWorkaroundEnabled: Boolean = false

    private val unblockGestureScheduler: UnblockGestureScheduler? =
        if (unblockWorkaroundEnabled) UnblockGestureScheduler()
        else null

    fun setUnblockWorkaround(isEnabled: Boolean) {
        unblockWorkaroundEnabled = isEnabled
    }

    suspend fun onScenarioLoopFinished() {
        if (unblockGestureScheduler?.shouldTrigger() == true) {
            withContext(Dispatchers.Main) {
                Log.i(TAG, "Injecting unblock gesture")
                androidExecutor.dispatchGesture(
                    GestureDescription.Builder().buildUnblockGesture()
                )
            }
        }
    }

    suspend fun executeDumbAction(action: DumbAction, randomize: Boolean) {
        this.randomize = randomize
        when (action) {
            is DumbAction.DumbClick -> executeDumbClick(action)
            is DumbAction.DumbSwipe -> executeDumbSwipe(action)
            is DumbAction.DumbPause -> executeDumbPause(action)
            is DumbAction.DumbSplitAction -> executeDumbSplitAction(action)
        }
    }

    private suspend fun executeDumbClick(dumbClick: DumbAction.DumbClick) {
        dumbClick.waitBeforeMs?.takeIf { it > 0 }?.let { delay(it) }
        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply { moveTo(dumbClick.position, random) },
            durationMs = dumbClick.pressDurationMs,
            random = random,
        )

        executeRepeatableGesture(clickGesture, dumbClick)
        dumbClick.waitAfterMs?.takeIf { it > 0 }?.let { delay(it) }
    }

    private suspend fun executeDumbSwipe(dumbSwipe: DumbAction.DumbSwipe) {
        dumbSwipe.waitBeforeMs?.takeIf { it > 0 }?.let { delay(it) }
        val swipeGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply {
                line(
                    from = dumbSwipe.fromPosition,
                    to = dumbSwipe.toPosition,
                    random = random,
                )
            },
            durationMs = dumbSwipe.swipeDurationMs,
            random = random,
        )

        executeRepeatableGesture(swipeGesture, dumbSwipe)
        dumbSwipe.waitAfterMs?.takeIf { it > 0 }?.let { delay(it) }
    }

    private suspend fun executeDumbSplitAction(splitAction: DumbAction.DumbSplitAction) {
        if (!splitAction.isValid()) return
        splitAction.waitBeforeMs?.takeIf { it > 0 }?.let { delay(it) }
        val builder = GestureDescription.Builder()
        for (sub in splitAction.subActions) {
            val startOffset = when (sub) {
                is DumbAction.DumbClick -> sub.waitBeforeMs ?: 0L
                is DumbAction.DumbSwipe -> sub.waitBeforeMs ?: 0L
                else -> 0L
            }
            val path = Path()
            val duration: Long
            when (sub) {
                is DumbAction.DumbClick -> {
                    path.moveTo(sub.position, random)
                    duration = sub.pressDurationMs
                }
                is DumbAction.DumbSwipe -> {
                    path.line(from = sub.fromPosition, to = sub.toPosition, random = random)
                    duration = sub.swipeDurationMs
                }
                else -> continue
            }
            builder.addStroke(
                path = path, startTime = startOffset, durationMs = duration, random = random
            )
        }

        val gesture = builder.build()
        val tailDelay = combinedTouchTailDelay(
            (0 until gesture.strokeCount).map { gesture.getStroke(it).let { stroke -> stroke.startTime + stroke.duration } },
            splitAction.subActions.map {
                when (it) {
                    is DumbAction.DumbClick -> it.waitAfterMs ?: 0L
                    is DumbAction.DumbSwipe -> it.waitAfterMs ?: 0L
                    else -> 0L
                }
            },
        )
        executeRepeatableGesture(gesture, splitAction, tailDelay)
        splitAction.waitAfterMs?.takeIf { it > 0 }?.let { delay(it) }
    }

    private suspend fun executeDumbPause(dumbPause: DumbAction.DumbPause) {
        delay(dumbPause.pauseDurationMs.getPauseDurationMs(random))
    }

    private suspend fun executeRepeatableGesture(gesture: GestureDescription, repeatable: Repeatable, tailDelay: Long = 0L) {
        repeatable.repeat {
            withContext(Dispatchers.Main) {
                androidExecutor.dispatchGesture(gesture)
            }
            if (tailDelay > 0L) delay(tailDelay)
        }
    }
}

private const val TAG = "DumbActionExecutor"