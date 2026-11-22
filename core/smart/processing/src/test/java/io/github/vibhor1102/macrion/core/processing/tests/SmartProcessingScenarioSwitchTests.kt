/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.processing.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.processing.data.DetectorEngine
import io.github.vibhor1102.macrion.core.processing.data.DetectorState
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartProcessingScenarioSwitchTests {
    @Test
    fun oldPlayCannotStartAReopenedScenarioWithTheSameId() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = mockk<DetectorEngine>(relaxed = true)
        every { detector.state } returns MutableStateFlow(DetectorState.RECORDING)
        every { detector.detectionStopSequence } returns MutableStateFlow(0L)
        every { detector.needsEntireScreenCapture } returns MutableStateFlow(false)
        val scenarios = mockk<IRepository>(relaxed = true)
        val selectedId = Identifier(databaseId = 1)
        val started = CompletableDeferred<Unit>()
        val releaseRead = CompletableDeferred<Unit>()
        coEvery { scenarios.getScenario(selectedId.databaseId) } coAnswers {
            started.complete(Unit)
            releaseRead.await()
            mockk<Scenario>()
        }
        coEvery { scenarios.getScreenEvents(selectedId.databaseId) } returns emptyList()
        coEvery { scenarios.getTriggerEvents(selectedId.databaseId) } returns emptyList()
        coEvery { scenarios.getCounters(selectedId.databaseId) } returns emptyList()
        val repository = SmartProcessingRepositoryImpl(context, StandardTestDispatcher(testScheduler), scenarios, detector)
        repository.setScenarioId(selectedId)

        val pending = async { repository.startDetection(context, liveDebugging = false, generateReport = false) }
        started.await()
        repository.stopScreenRecord()
        repository.setScenarioId(selectedId)
        releaseRead.complete(Unit)

        assertFalse(pending.await())
        verify(exactly = 0) { detector.startDetection(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun acceptedSwitchPersistsAfterStopWithoutReplacingTheNewSession() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = mockk<DetectorEngine>(relaxed = true)
        val state = MutableStateFlow(DetectorState.RECORDING)
        every { detector.state } returns state
        every { detector.detectionStopSequence } returns MutableStateFlow(0L)
        every { detector.needsEntireScreenCapture } returns MutableStateFlow(false)
        every { detector.stopScreenRecord() } answers { state.value = DetectorState.CREATED }
        every { detector.withPausedRecording(any()) } answers {
            firstArg<() -> Unit>().invoke()
            true
        }

        val writes = mockk<IRepository>(relaxed = true)
        val started = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        val old = Identifier(databaseId = 1)
        val target = Identifier(databaseId = 2)
        coEvery { writes.markAsUsed(target) } coAnswers {
            started.complete(Unit)
            releaseWrite.await()
        }
        val repository = SmartProcessingRepositoryImpl(context, StandardTestDispatcher(testScheduler), writes, detector)
        repository.setScenarioId(old)

        val pending = async { repository.setScenarioIdAndMarkAsUsed(old, target) }
        started.await()
        assertEquals(target, repository.getScenarioId())
        repository.stopScreenRecord()
        assertNull(repository.getScenarioId())

        // A new recording can select the same original ID before the old write returns.
        repository.setScenarioId(old)
        state.value = DetectorState.RECORDING
        releaseWrite.complete(Unit)

        assertTrue(pending.await())
        assertEquals(old, repository.getScenarioId())
        coVerify(exactly = 1) { writes.markAsUsed(target) }
    }

    @Test
    fun rejectedSwitchDoesNotRecordUsage() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = mockk<DetectorEngine>(relaxed = true)
        every { detector.state } returns MutableStateFlow(DetectorState.CREATED)
        every { detector.withPausedRecording(any()) } returns false
        val scenarios = mockk<IRepository>(relaxed = true)
        val repository = SmartProcessingRepositoryImpl(context, StandardTestDispatcher(testScheduler), scenarios, detector)
        val old = Identifier(databaseId = 1)
        val target = Identifier(databaseId = 2)
        repository.setScenarioId(old)

        assertFalse(repository.setScenarioIdAndMarkAsUsed(old, target))
        assertEquals(old, repository.getScenarioId())
        coVerify(exactly = 0) { scenarios.markAsUsed(any()) }
    }

    @Test
    fun failedUsageWriteRestoresTheOriginalScenario() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = mockk<DetectorEngine>(relaxed = true)
        every { detector.state } returns MutableStateFlow(DetectorState.RECORDING)
        every { detector.withPausedRecording(any()) } answers {
            firstArg<() -> Unit>().invoke()
            true
        }
        val scenarios = mockk<IRepository>(relaxed = true)
        val repository = SmartProcessingRepositoryImpl(context, StandardTestDispatcher(testScheduler), scenarios, detector)
        val old = Identifier(databaseId = 1)
        val target = Identifier(databaseId = 2)
        val failure = IllegalStateException("database")
        coEvery { scenarios.markAsUsed(target) } throws failure
        repository.setScenarioId(old)

        val error = runCatching { repository.setScenarioIdAndMarkAsUsed(old, target) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertEquals(failure.message, error?.message)
        assertEquals(old, repository.getScenarioId())
    }
}
