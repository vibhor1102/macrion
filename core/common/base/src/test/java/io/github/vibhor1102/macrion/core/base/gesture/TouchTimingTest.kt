package io.github.vibhor1102.macrion.core.base.gesture

import org.junit.Assert.*
import org.junit.Test

class TouchTimingTest {
    @Test fun waitAfterOverlapsOtherFingers() {
        assertEquals(0L, combinedTouchTailDelay(listOf(100, 1000), listOf(500, 0)))
        assertEquals(300L, combinedTouchTailDelay(listOf(100, 1000), listOf(1200, 0)))
        assertEquals(500L, combinedTouchTailDelay(listOf(100, 1000), listOf(1200, 500)))
    }
    @Test fun invalidOffsetsDurationsAndOverflowAreRejected() {
        assertFalse(isCombinedTouchTimingValid(100, -1, null))
        assertFalse(isCombinedTouchTimingValid(100, Long.MAX_VALUE, null))
        assertFalse(isCombinedTouchTimingValid(0, null, null))
        assertFalse(isCombinedTouchTimingValid(100, 59900, null))
        assertTrue(isCombinedTouchTimingValid(100, 59899, 500))
    }
}
