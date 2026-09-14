/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.overlays

import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.state.TutorialState
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppOverlayScaleProviderTest {

    private val settingsRepository: SettingsRepository = mockk()
    private val tutorialRepository: TutorialRepository = mockk()

    @Test
    fun `when tutorial is running scale is always 1_0f`() = runTest {
        val tutorialFlow = MutableStateFlow<TutorialState>(
            TutorialState.Started(mockk(), isCompleted = false, isCurrentStepStarted = true, currentStep = null),
        )
        every { tutorialRepository.tutorialState } returns tutorialFlow
        every { tutorialRepository.isTutorialStarted() } returns true
        every { settingsRepository.toolbarScalePercentFlow } returns flowOf(150)
        every { settingsRepository.getToolbarScalePercent() } returns 150

        val provider = AppOverlayScaleProvider(settingsRepository, tutorialRepository)

        assertEquals(1.0f, provider.getScale(), 0.001f)
        assertEquals(1.0f, provider.scaleFlow.first(), 0.001f)
    }

    @Test
    fun `when tutorial is stopped scale follows settings`() = runTest {
        val tutorialFlow = MutableStateFlow<TutorialState>(TutorialState.Stopped)
        every { tutorialRepository.tutorialState } returns tutorialFlow
        every { tutorialRepository.isTutorialStarted() } returns false
        every { settingsRepository.toolbarScalePercentFlow } returns flowOf(140)
        every { settingsRepository.getToolbarScalePercent() } returns 140

        val provider = AppOverlayScaleProvider(settingsRepository, tutorialRepository)

        assertEquals(1.4f, provider.getScale(), 0.001f)
        assertEquals(1.4f, provider.scaleFlow.first(), 0.001f)
    }

    @Test
    fun `when tutorial is stopped 50 percent scale returns 0_5f`() = runTest {
        val tutorialFlow = MutableStateFlow<TutorialState>(TutorialState.Stopped)
        every { tutorialRepository.tutorialState } returns tutorialFlow
        every { tutorialRepository.isTutorialStarted() } returns false
        every { settingsRepository.toolbarScalePercentFlow } returns flowOf(50)
        every { settingsRepository.getToolbarScalePercent() } returns 50

        val provider = AppOverlayScaleProvider(settingsRepository, tutorialRepository)

        assertEquals(0.5f, provider.getScale(), 0.001f)
        assertEquals(0.5f, provider.scaleFlow.first(), 0.001f)
    }
}
