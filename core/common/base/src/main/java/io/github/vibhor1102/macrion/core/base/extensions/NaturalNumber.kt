/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.base.extensions

import java.math.BigDecimal
import java.math.RoundingMode

/** Plain, locale-independent text without redundant decimal zeros; never narrows to an integer. */
fun Double.toNaturalString(maxFractionDigits: Int? = null): String {
    if (!isFinite()) return toString()
    return BigDecimal.valueOf(this).toNaturalString(maxFractionDigits)
}

/** Use the Float's decimal representation instead of widening its binary approximation to Double. */
fun Float.toNaturalString(): String =
    if (!isFinite()) toString() else BigDecimal(toString()).toNaturalString(null)

private fun BigDecimal.toNaturalString(maxFractionDigits: Int?): String =
    (maxFractionDigits?.let { setScale(it, RoundingMode.HALF_UP) } ?: this)
        .stripTrailingZeros().toPlainString()
