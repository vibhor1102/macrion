/*
 * Copyright (C) 2025 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.common.actions.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.util.Log

import io.github.vibhor1102.macrion.core.base.Dumpable
import io.github.vibhor1102.macrion.core.base.addDumpTabulationLvl
import io.github.vibhor1102.macrion.core.base.gesture.GestureInteractionGuard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds


@Singleton
internal class GestureExecutor @Inject constructor(
    private val interactionGuard: GestureInteractionGuard,
) : Dumpable {

    private val dispatchMutex = Mutex()

    private var completedGestures: Long = 0L
    private var cancelledGestures: Long = 0L
    private var errorGestures: Long = 0L
    private var retriedGestures: Long = 0L


    fun clear() {
        completedGestures = 0L
        cancelledGestures = 0L
        errorGestures = 0L
        retriedGestures = 0L
    }

    suspend fun dispatchGesture(service: AccessibilityService, gesture: GestureDescription): GestureDispatchResult =
        withContext(Dispatchers.Main.immediate) {
            dispatchMutex.withLock {
                interactionGuard.awaitIdle()
                val first = dispatchOnce(service, gesture)
                if (!first.canRetry) return@withLock first

                Log.i(TAG, "Gesture $first; waiting for recovery before one retry")
                delay(GESTURE_RECOVERY_DELAY_MS)
                interactionGuard.awaitIdle(quietPeriodMs = GESTURE_RECOVERY_DELAY_MS)
                currentCoroutineContext().ensureActive()
                retriedGestures++
                dispatchOnce(service, gesture)
            }
        }

    private suspend fun dispatchOnce(service: AccessibilityService, gesture: GestureDescription): GestureDispatchResult {
        val result = withTimeoutOrNull(gesture.timeoutDurationMs().milliseconds) {
            suspendCancellableCoroutine<GestureDispatchResult> { continuation ->
                fun complete(result: GestureDispatchResult) {
                    if (continuation.isActive) continuation.resume(result)
                }
                try {
                    val accepted = service.dispatchGesture(
                        /* gesture = */ gesture,
                        /* callback = */ object : GestureResultCallback() {
                            override fun onCompleted(g: GestureDescription?) = complete(GestureDispatchResult.COMPLETED)
                            override fun onCancelled(g: GestureDescription?) = complete(GestureDispatchResult.CANCELLED)
                        },
                        /* handler = */ null,
                    )
                    if (!accepted) complete(GestureDispatchResult.REJECTED)
                } catch (rEx: RuntimeException) {
                    Log.w(TAG, "Gesture submission failed", rEx)
                    complete(GestureDispatchResult.ERROR)
                }
            }
        } ?: GestureDispatchResult.TIMED_OUT

        when (result) {
            GestureDispatchResult.COMPLETED -> completedGestures++
            GestureDispatchResult.CANCELLED -> cancelledGestures++
            else -> errorGestures++
        }
        if (result != GestureDispatchResult.COMPLETED) Log.w(TAG, "Gesture result: $result")
        return result
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = "${prefix.addDumpTabulationLvl()}- "

        writer.apply {
            append(prefix).println("* GestureExecutor:")
            append(contentPrefix).append("Completed=$completedGestures").println()
            append(contentPrefix).append("Cancelled=$cancelledGestures").println()
            append(contentPrefix).append("Error=$errorGestures").println()
            append(contentPrefix).append("Retried=$retriedGestures").println()
        }
    }
}

internal enum class GestureDispatchResult(val canRetry: Boolean = false) {
    COMPLETED, REJECTED(true), CANCELLED(true), TIMED_OUT, ERROR,
}

private fun GestureDescription.durationMs(): Long {
    var maxEndTime = 0L
    for (i in 0 until strokeCount) {
        val stroke = getStroke(i)
        maxEndTime = maxOf(maxEndTime, stroke.startTime + stroke.duration)
    }

    return maxEndTime
}

private fun GestureDescription.timeoutDurationMs(): Long {
    val duration = durationMs()
    // Short gestures also need room for system scheduling and the completion callback. Doubling a 50 ms press
    // alone can time out while Android is still delivering its UP event under ordinary load.
    return maxOf(duration * GESTURE_CALLBACK_TIMEOUT_DURATION_RATIO_MS, duration + GESTURE_CALLBACK_GRACE_MS)
        .coerceAtMost(GESTURE_CALLBACK_TIMEOUT_MAX_MS)
}

private const val GESTURE_CALLBACK_GRACE_MS = 500L
private const val GESTURE_CALLBACK_TIMEOUT_MAX_MS = 65_000L
private const val GESTURE_CALLBACK_TIMEOUT_DURATION_RATIO_MS = 2
private const val GESTURE_RECOVERY_DELAY_MS = 500L

private const val TAG = "GestureExecutor"
