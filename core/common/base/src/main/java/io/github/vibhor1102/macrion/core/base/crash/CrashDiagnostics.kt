/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.core.base.crash

import java.util.WeakHashMap

/** Only technical state belongs here. Never pass scenario objects or automation payloads. */
data class DetectionCrashContext(
    val operation: Operation,
    val screenWidth: Int,
    val screenHeight: Int,
    val areaLeft: Int,
    val areaTop: Int,
    val areaWidth: Int,
    val areaHeight: Int,
    val threshold: Int,
    val textLength: Int? = null,
    val modelId: String? = null,
    val originalWidth: Int? = null,
    val originalHeight: Int? = null,
    val scaledWidth: Int? = null,
    val scaledHeight: Int? = null,
    val color: Int? = null,
    val numberFormat: Int? = null,
) {
    enum class Operation { IMAGE, COLOR, TEXT, NUMBER }
}

object CrashDiagnostics {
    enum class Event {
        HOME_OPENED, SETTINGS_OPENED, OVERLAY_CREATED, OVERLAY_SHOWN, OVERLAY_HIDDEN,
        OVERLAY_DESTROYED, ROTATION_CHANGED, TAB_SHOWN, TAB_HIDDEN, VIEW_ATTACHED, VIEW_DETACHED,
        SCENARIO_START_REQUESTED, SCENARIO_STOP_REQUESTED, ACTION_FAILED,
        CLICK, SWIPE, PAUSE, INTENT, TOGGLE_EVENT, CHANGE_COUNTER, EXTERNAL_ACTION,
        NOTIFICATION, SYSTEM_ACTION, SET_TEXT, PLAY_SOUND, CAPTURE_SCREENSHOT, SPLIT_ACTION, ACTIVITY_RESUMED, ACTIVITY_PAUSED, DROPDOWN_OPENED, DROPDOWN_CLOSED,
    }
    data class Entry(val event: Event, val elapsedNanos: Long, val component: String?,
        val count: Int, val state: Int?, val attached: Boolean?)
    private val contexts = WeakHashMap<Throwable, DetectionCrashContext>()
    private const val CAPACITY = 128
    private val types = arrayOfNulls<Event>(CAPACITY)
    private val times = LongArray(CAPACITY)
    private val components = arrayOfNulls<String>(CAPACITY)
    private val counts = IntArray(CAPACITY)
    private val states = arrayOfNulls<Int>(CAPACITY)
    private val attachments = arrayOfNulls<Boolean>(CAPACITY)
    private var size = 0
    private var next = 0

    /** Component must be a code class name, never a user label or action payload. No I/O or stack capture. */
    @Synchronized fun record(event: Event, component: String? = null, state: Int? = null, attached: Boolean? = null) {
        val now = System.nanoTime()
        val previous = (next + CAPACITY - 1) % CAPACITY
        if (size > 0 && types[previous] == event && components[previous] == component &&
            states[previous] == state && attachments[previous] == attached &&
            now - times[previous] < 1_000_000_000L) {
            times[previous] = now
            if (counts[previous] < Int.MAX_VALUE) counts[previous]++
            return
        }
        types[next] = event; times[next] = now; components[next] = component
        states[next] = state; attachments[next] = attached; counts[next] = 1
        next = (next + 1) % CAPACITY
        if (size < CAPACITY) size++
    }

    @Synchronized fun snapshot(): List<Entry> = List(size) { offset ->
        val index = (next - size + offset + CAPACITY) % CAPACITY
        Entry(types[index]!!, times[index], components[index], counts[index], states[index], attachments[index])
    }
    data class CaughtError(val type: String, val message: String?, val frames: List<StackTraceElement>, val elapsedNanos: Long,
        val redactionCount: Int, val truncated: Boolean)
    private val errors = ArrayDeque<CaughtError>()

    /** Rare failure path only: never retain Throwable objects or their application object graphs. */
    @Synchronized fun recordFailure(error: Exception) {
        val redactor = CrashRedactor()
        if (errors.size == 4) errors.removeFirst()
        errors.addLast(CaughtError(redactor.redact(error.javaClass.name, 256)!!,
            redactor.redact(error.message, 2048), error.stackTrace.take(16), System.nanoTime(),
            redactor.replacements, redactor.truncated || error.stackTrace.size > 16))
        record(Event.ACTION_FAILED)
    }
    @Synchronized fun errorSnapshot(): List<CaughtError> = errors.toList()

    @Synchronized fun context(error: Throwable): DetectionCrashContext? = contexts[error]
    @Synchronized fun attach(error: Throwable, context: DetectionCrashContext) {
        if (contexts.size >= 8) contexts.clear()
        contexts[error] = context
    }
}

/** Preserve the original exception type and trace, without logging user content. */
fun Exception.throwWithContext(context: DetectionCrashContext): Nothing {
    runCatching { CrashDiagnostics.attach(this, context) }
    throw this
}
