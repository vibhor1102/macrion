/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.base.compat

/** Interpret legacy multi-touch names as the current default without changing stored user data. */
fun String.normalizedMultiTouchName(): String = when (this) {
    "Simultaneous click/swipe", "Simultaneous Touch" -> "Multi-touch"
    else -> this
}
