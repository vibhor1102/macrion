/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionPhase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetectionIntentCoordinatorTest {

    @Test
    fun `pause during scenario loading cancels obsolete start`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        val loading = CompletableDeferred<Boolean>()
        var starts = 0
        var stops = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { starts++; loading.await() }, stop = { stops++ }, onStarted = {})
        runCurrent()

        coordinator.toggle()
        runCurrent()
        coordinator.toggle()
        runCurrent()

        assertEquals(1, starts)
        assertEquals(0, stops)
        assertFalse(coordinator.requestedRunning.value)
        assertTrue(loading.isActive)
    }

    @Test
    fun `late engine startup from a paused menu start is stopped`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        var stops = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { true }, stop = { stops++; phases.value = DetectionPhase.STOPPING }, onStarted = {})
        runCurrent()

        coordinator.toggle()
        runCurrent()
        coordinator.toggle()
        runCurrent()

        phases.value = DetectionPhase.STARTING
        runCurrent()

        assertFalse(coordinator.requestedRunning.value)
        assertEquals(1, stops)
    }

    @Test
    fun `play during cleanup waits then starts once`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        var starts = 0
        var stops = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { starts++; phases.value = DetectionPhase.STARTING; true },
            stop = { stops++; phases.value = DetectionPhase.STOPPING }, onStarted = {})
        runCurrent()

        coordinator.toggle()
        runCurrent()
        coordinator.toggle()
        runCurrent()
        coordinator.toggle()
        runCurrent()

        assertTrue(coordinator.requestedRunning.value)
        assertEquals(1, stops)
        assertEquals(1, starts)

        phases.value = DetectionPhase.RECORDING
        runCurrent()
        assertEquals(2, starts)
        assertEquals(1, stops)
    }

    @Test
    fun `external stop cannot restart an ended scenario`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.DETECTING)
        var starts = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), true,
            start = { starts++; true }, stop = {}, onStarted = {})
        runCurrent()

        phases.value = DetectionPhase.STOPPING
        runCurrent()
        phases.value = DetectionPhase.RECORDING
        runCurrent()

        assertFalse(coordinator.requestedRunning.value)
        assertEquals(0, starts)
    }

    @Test
    fun `direct detecting to recording emission also counts as external stop`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.DETECTING)
        var starts = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), true,
            start = { starts++; true }, stop = {}, onStarted = {})
        runCurrent()

        phases.value = DetectionPhase.RECORDING
        runCurrent()

        assertFalse(coordinator.requestedRunning.value)
        assertEquals(0, starts)
    }

    @Test
    fun `external stop signal survives conflated phase updates`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        val stops = MutableStateFlow(0L)
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, stops, false,
            start = { true }, stop = {}, onStarted = {})
        runCurrent()

        coordinator.toggle()
        runCurrent()
        stops.value++
        runCurrent()

        assertFalse(coordinator.requestedRunning.value)
    }

    @Test
    fun `external test setup remains running without consuming menu trial`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        var stops = 0
        var trialStarts = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { true }, stop = { stops++; phases.value = DetectionPhase.STOPPING },
            onStarted = { trialStarts++ })
        runCurrent()

        // Action, event, and condition tests all enter STARTING before DETECTING.
        phases.value = DetectionPhase.STARTING
        runCurrent()
        assertTrue(coordinator.requestedRunning.value)
        assertEquals(0, stops)

        phases.value = DetectionPhase.DETECTING
        runCurrent()
        assertTrue(coordinator.requestedRunning.value)
        assertEquals(0, trialStarts)

        coordinator.toggle()
        runCurrent()
        assertEquals(1, stops)
    }

    @Test
    fun `external test can be paused during setup`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        var stops = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { true }, stop = { stops++; phases.value = DetectionPhase.STOPPING }, onStarted = {})
        runCurrent()

        phases.value = DetectionPhase.STARTING
        runCurrent()
        assertTrue(coordinator.requestedRunning.value)

        assertTrue(coordinator.requestStop())
        runCurrent()
        assertFalse(coordinator.requestedRunning.value)
        assertEquals(1, stops)
    }

    @Test
    fun `own stop signal cannot discard a quick resume`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.DETECTING)
        val stops = MutableStateFlow(0L)
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, stops, true,
            start = { true },
            stop = { stops.value++; phases.value = DetectionPhase.STOPPING }, onStarted = {})
        runCurrent()

        coordinator.toggle()
        coordinator.toggle()
        runCurrent()

        assertTrue(coordinator.requestedRunning.value)
        phases.value = DetectionPhase.RECORDING
        runCurrent()
        assertTrue(coordinator.requestedRunning.value)
    }

    @Test
    fun `a failed start clears play intent`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { false }, stop = {}, onStarted = {})
        runCurrent()

        coordinator.toggle()
        runCurrent()

        assertFalse(coordinator.requestedRunning.value)
    }

    @Test
    fun `trial starts once when detection actually begins`() = runTest {
        val phases = MutableStateFlow(DetectionPhase.RECORDING)
        var consumed = 0
        val coordinator = DetectionIntentCoordinator(backgroundScope, phases, MutableStateFlow(0L), false,
            start = { phases.value = DetectionPhase.STARTING; true }, stop = {},
            onStarted = { consumed++ })
        runCurrent()

        coordinator.toggle()
        runCurrent()
        assertEquals(0, consumed)
        phases.value = DetectionPhase.DETECTING
        runCurrent()
        assertEquals(1, consumed)
        phases.value = DetectionPhase.DETECTING
        runCurrent()
        assertEquals(1, consumed)
    }
}
