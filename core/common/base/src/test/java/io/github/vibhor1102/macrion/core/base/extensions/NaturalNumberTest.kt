package io.github.vibhor1102.macrion.core.base.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NaturalNumberTest {
    @Test fun formatsNumbersWithoutIntegerNarrowingOrExponentNotation() {
        assertEquals("42", 42.0.toNaturalString())
        assertEquals("-42", (-42.0).toNaturalString())
        assertEquals("0", (-0.0).toNaturalString())
        assertEquals("1.25", 1.25.toNaturalString())
        assertEquals("100000000000000000000", 1e20.toNaturalString())
        assertEquals("0.0000001", 1e-7.toNaturalString())
    }

    @Test fun doesNotRoundUnlessTheCallerRequestsIt() {
        assertEquals("0.30000000000000004", (0.1 + 0.2).toNaturalString())
        assertEquals("1.24", 1.235.toNaturalString(2))
        assertEquals("1", 1.0.toNaturalString(2))
        assertEquals("1.2", 1.2f.toNaturalString())
    }

    @Test fun runtimeNumberTextIsIndependentOfDeviceLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.5", 1.5.toNaturalString())
        } finally { Locale.setDefault(old) }
    }

    @Test fun nonFiniteValuesDoNotCrashFormatting() {
        assertEquals("NaN", Double.NaN.toNaturalString())
        assertEquals("Infinity", Double.POSITIVE_INFINITY.toNaturalString())
        assertEquals("-Infinity", Double.NEGATIVE_INFINITY.toNaturalString())
    }
}
