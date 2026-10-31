package io.github.vibhor1102.macrion.core.smart.debugging.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DebugValuesFormatterTest {
    @Test fun confidenceIsCompactClampedAndStillLocalized() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("100 %", 1.0.formatDebugConfidenceRate())
            assertEquals("0 %", (-0.1).formatDebugConfidenceRate())
            assertEquals("95.5 %", 0.955.formatDebugConfidenceRate())
            assertEquals("12.35 %", 0.123456.formatDebugConfidenceRate())
            Locale.setDefault(Locale.GERMANY)
            assertEquals("95,5 %", 0.955.formatDebugConfidenceRate())
        } finally { Locale.setDefault(old) }
    }
}
