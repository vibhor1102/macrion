/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.counter.Counter
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import org.junit.Assert.*
import org.junit.Test

class FiniteNumberInputTests {
    @Test fun invalidTextCannotReplaceTheDomainValueAndRemainsInvalidUntilCorrected() {
        val input = FiniteNumberInput()
        var domainValue = 12.0
        for (text in listOf("", "-", ".", "abc", "NaN", "Infinity", "1e309", "9".repeat(400))) {
            input.update(text) { domainValue = it }
            assertFalse(text, input.isValid)
            assertEquals(text, input.textFor(domainValue))
            assertEquals(12.0, domainValue, 0.0)
        }
        input.update("-2.50") { domainValue = it }
        assertTrue(input.isValid)
        assertEquals(-2.5, domainValue, 0.0)
        assertEquals("-2.50", input.textFor(domainValue))
        input.reset()
        assertFalse(input.hasDraft)
        assertEquals("3", input.textFor(3.0))
    }

    @Test fun operandsRejectNegativeNumbersButAllowZeroAndFractionalValues() {
        val input = FiniteNumberInput(allowNegative = false)
        val values = mutableListOf<Double>()
        input.update("-1", values::add)
        assertFalse(input.isValid)
        assertTrue(values.isEmpty())
        input.update("0", values::add)
        input.update("0.25", values::add)
        assertTrue(input.isValid)
        assertEquals(listOf(0.0, 0.25), values)
    }

    @Test fun domainModelsAlsoRejectNonFiniteNumbers() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFalse(CounterOperationValue.Number(value).isComplete())
            assertFalse(Counter("score", value, Identifier(databaseId = 1)).isComplete())
        }
        assertTrue(Counter("score", -3.0, Identifier(databaseId = 1)).isComplete())
    }
}
