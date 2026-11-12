/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.base.gesture

import android.os.SystemClock
import androidx.annotation.MainThread
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates toolbar touches with gesture submission. All access is confined to the main thread. */
@Singleton
@MainThread
class GestureInteractionGuard internal constructor(private val uptimeMillis: () -> Long) {
    @Inject constructor() : this(SystemClock::uptimeMillis)

    private val owners = mutableSetOf<Any>()
    private val touching = MutableStateFlow(false)
    private var lastReleaseUptimeMs: Long? = null

    fun onTouchStarted(owner: Any) {
        owners.add(owner)
        touching.value = true
    }

    /** Also called on cancel, hiding, or detaching a toolbar so abandoned touches cannot block playback. */
    fun onTouchFinished(owner: Any) {
        if (!owners.remove(owner)) return
        lastReleaseUptimeMs = uptimeMillis()
        touching.value = owners.isNotEmpty()
    }

    suspend fun awaitIdle(quietPeriodMs: Long = 150L) {
        while (true) {
            currentCoroutineContext().ensureActive()
            if (touching.value) {
                touching.first { !it }
                continue
            }
            val remaining = lastReleaseUptimeMs?.let { it + quietPeriodMs - uptimeMillis() } ?: 0L
            if (remaining <= 0L) return
            // Only pending injections wait; there is no idle worker or periodic monitoring.
            delay(remaining)
        }
    }
}
