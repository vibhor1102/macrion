package io.github.vibhor1102.macrion.core.common.actions.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCountersTest {
    @Test fun textOutputUsesWholeNumbersAndRetainsRealFractions() {
        assertEquals(" 42 / -5 / 1.25 / 0 ",
            " {A} / {B} / {C} / {D} ".replaceCounterReferences(
                mapOf("A" to 42.0, "B" to -5.0, "C" to 1.25, "D" to -0.0)))
    }

    @Test fun replacementPreservesLiteralTextUnknownReferencesAndCounterNames() {
        assertEquals("0042.00 3 {Missing} 3", "0042.00 {A:F2} {Missing} {A:F2}"
            .replaceCounterReferences(mapOf("A:F2" to 3.0)))
        assertEquals(listOf("A:F2", "Missing", "A:F2"),
            "0042.00 {A:F2} {Missing} {A:F2}".findCounterReferences())
    }

    @Test fun largeAndSmallValuesAreWrittenWithoutExponents() {
        assertEquals("100000000000000000000 0.0000001", "{Big} {Small}"
            .replaceCounterReferences(mapOf("Big" to 1e20, "Small" to 1e-7)))
    }
}
