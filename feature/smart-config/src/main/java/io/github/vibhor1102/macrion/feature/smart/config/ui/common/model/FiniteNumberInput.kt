/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString

/** Keep incomplete text separate from the last valid domain value, including across rotation. */
class FiniteNumberInput(private val allowNegative: Boolean = true) {
    private var draft by mutableStateOf<String?>(null)

    val isValid: Boolean get() = draft == null || parse(draft!!) != null
    val hasDraft: Boolean get() = draft != null

    fun textFor(value: Double): String = draft ?: value.toNaturalDisplayString()

    fun update(text: String, onValidValue: (Double) -> Unit) {
        draft = text
        parse(text)?.let(onValidValue)
    }

    fun reset() { draft = null }

    private fun parse(text: String): Double? = text.toDoubleOrNull()
        ?.takeIf { it.isFinite() && (allowNegative || it >= 0.0) }
}
