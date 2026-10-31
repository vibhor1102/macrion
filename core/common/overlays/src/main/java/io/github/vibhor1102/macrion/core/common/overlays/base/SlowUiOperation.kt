/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.base

import android.os.SystemClock
import android.util.Log

/** Selected synchronous operations only: no polling, per-frame callbacks, stack capture or I/O. */
internal object SlowUiOperation {
    private val reporter = SlowOperationReporter { component, operation, duration ->
        Log.w("MacrionSlowUi", "$component.$operation took ${duration}ms on the UI thread")
    }

    fun begin(): Long = SystemClock.uptimeMillis()

    fun end(startMs: Long, component: String, operation: String) {
        val now = SystemClock.uptimeMillis()
        reporter.report(now - startMs, now, component, operation)
    }
}

/** One warning at most per minute across all overlay operations, including nested rotation/create. */
internal class SlowOperationReporter(
    private val warn: (component: String, operation: String, durationMs: Long) -> Unit,
) {
    private var lastWarningMs: Long? = null

    fun report(durationMs: Long, nowMs: Long, component: String, operation: String) {
        if (durationMs < 1_000) return
        synchronized(this) {
            val previous = lastWarningMs
            if (previous != null && nowMs - previous < 60_000) return
            lastWarningMs = nowMs
        }
        warn(component, operation, durationMs)
    }
}
