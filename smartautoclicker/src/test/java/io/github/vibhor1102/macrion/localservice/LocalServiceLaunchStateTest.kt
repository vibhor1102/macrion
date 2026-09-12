package io.github.vibhor1102.macrion.localservice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalServiceLaunchStateTest {

    @Test
    fun `fresh launch requires every session resource to be clean`() {
        assertTrue(canLaunchFreshScenario(false, true, false, false, false))

        assertFalse(canLaunchFreshScenario(true, true, false, false, false))
        assertFalse(canLaunchFreshScenario(false, false, false, false, false))
        assertFalse(canLaunchFreshScenario(false, true, true, false, false))
        assertFalse(canLaunchFreshScenario(false, true, false, true, false))
        assertFalse(canLaunchFreshScenario(false, true, false, false, true))
    }
}
