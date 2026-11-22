/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.localservice

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.dumb.engine.DumbEngine
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalServiceStopDuringStartupTest {
    @Test
    fun `stop rejects a launch before teardown and cancels delayed startup`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val overlays = mockk<OverlayManager>(relaxed = true)
        every { overlays.isEmpty() } returns true
        every { overlays.isStackHidden } returns MutableStateFlow(false)
        every { overlays.backStackTopFlow } returns flowOf(null)
        val smart = mockk<SmartProcessingRepository>(relaxed = true)
        every { smart.isFullyStopped() } returns true
        every { smart.getScenarioId() } returns null
        every { smart.detectionState } returns flowOf(DetectionState.INACTIVE)
        val dumb = mockk<DumbEngine>(relaxed = true)
        every { dumb.isRunning } returns MutableStateFlow(false)
        val scenario = mockk<DumbScenario>()
        every { scenario.id } returns Identifier(databaseId = 1L)
        var attemptOnStop = false
        var launchedDuringStop: Boolean? = null
        lateinit var service: LocalService
        service = LocalService(
            context = ApplicationProvider.getApplicationContext<Application>(),
            overlayManager = overlays,
            appComponentsProvider = mockk(),
            settingsRepository = mockk(),
            smartRepository = mockk(),
            smartProcessingRepository = smart,
            dumbEngine = dumb,
            tutorialRepository = mockk(),
            revenueRepository = mockk(),
            debuggingRepository = mockk(),
            onStart = { _, _, _ -> },
            onScenarioChanged = { _, _ -> },
            onScenarioStateChanged = {
                if (attemptOnStop) {
                    attemptOnStop = false
                    launchedDuringStop = service.launchDumbScenario(scenario)
                }
            },
            onStop = {},
            onAccessibilityLoss = {},
        )
        try {
            runCurrent()
            assertTrue(service.launchDumbScenario(scenario))
            runCurrent()
            attemptOnStop = true

            service.stopScenario()
            runCurrent()

            assertEquals(false, launchedDuringStop)
            verify(exactly = 0) { dumb.init(any()) }
            assertTrue(service.launchDumbScenario(scenario))
            advanceTimeBy(501)

            verify(exactly = 1) { overlays.navigateToRoot(any(), any()) }
            verify(exactly = 0) { dumb.init(any()) }
            assertFalse(service.isStarted)
        } finally {
            service.release()
            Dispatchers.resetMain()
        }
    }
}
