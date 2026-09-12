package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainMenuModelStateTest {

    @Test
    fun `detecting always keeps pause available`() {
        assertTrue(
            canUsePlayPauseButton(
                state = UiState.Detecting,
                canStartDetection = false,
                isSynchronized = false,
                isProjectionStarted = true,
            )
        )
    }

    @Test
    fun `idle still observes normal start requirements`() {
        assertFalse(canUsePlayPauseButton(UiState.Idle, false, true, true))
        assertFalse(canUsePlayPauseButton(UiState.Idle, true, false, true))
        assertTrue(canUsePlayPauseButton(UiState.Idle, true, true, true))
    }
}
