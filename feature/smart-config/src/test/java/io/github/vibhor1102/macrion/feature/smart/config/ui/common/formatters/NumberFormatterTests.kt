/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters

import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTests {

    @Test
    fun wholeNumber_removesUselessDecimal() {
        assertEquals("1", 1.0.toNaturalDisplayString())
    }

    @Test
    fun zero_removesUselessDecimal() {
        assertEquals("0", 0.0.toNaturalDisplayString())
    }

    @Test
    fun negativeWholeNumber_removesUselessDecimal() {
        assertEquals("-5", (-5.0).toNaturalDisplayString())
    }

    @Test
    fun decimalValue_keepsMeaningfulDecimal() {
        assertEquals("1.25", 1.25.toNaturalDisplayString())
    }

    @Test
    fun trailingZeroDecimal_removesOnlyUselessZero() {
        assertEquals("1.5", 1.50.toNaturalDisplayString())
    }

    @Test
    fun longDecimal_keepsMeaningfulDecimals() {
        assertEquals("1.234567", 1.234567.toNaturalDisplayString())
    }

    @Test
    fun maxFractionDigits_keepsNaturalDisplayAfterRounding() {
        assertEquals("1.23", 1.234.toNaturalDisplayString(maxFractionDigits = 2))
        assertEquals("1.2", 1.20.toNaturalDisplayString(maxFractionDigits = 2))
        assertEquals("1", 1.0.toNaturalDisplayString(maxFractionDigits = 2))
    }

    @Test
    fun counterOperationNumber_formatsWrappedNumber() {
        assertEquals("1.5", CounterOperationValue.Number(1.50).toNaturalDisplayString())
    }

    @Test
    fun counterOperationCounter_keepsCounterName() {
        assertEquals("Counter A", CounterOperationValue.Counter("Counter A").toNaturalDisplayString())
    }
    @Test
    fun typedExtrasAreCompactWithoutChangingTextPayloads() {
        assertEquals("42", 42.0.toIntentExtraDisplayString())
        assertEquals("1.2", 1.2f.toIntentExtraDisplayString())
        assertEquals("100000000000000000000", 1e20.toIntentExtraDisplayString())
        assertEquals(" 0042.00 ", " 0042.00 ".toIntentExtraDisplayString())
        assertEquals(" ", ' '.toIntentExtraDisplayString())
    }
}
