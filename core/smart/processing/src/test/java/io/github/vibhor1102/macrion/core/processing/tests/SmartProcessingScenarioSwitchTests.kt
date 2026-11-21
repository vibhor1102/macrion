/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.processing.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.processing.data.DetectorEngine
import io.github.vibhor1102.macrion.core.processing.data.DetectorState
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartProcessingScenarioSwitchTests {
    @Test
    fun persistenceFinishingAfterStopCannotRestoreOrReplaceTheLoadedScenario() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val detector = mockk<DetectorEngine>(relaxed = true)
        val state = MutableStateFlow(DetectorState.RECORDING)
        every { detector.state } returns state
        every { detector.detectionStopSequence } returns MutableStateFlow(0L)
        every { detector.needsEntireScreenCapture } returns MutableStateFlow(false)
        every { detector.stopScreenRecord() } answers { state.value = DetectorState.CREATED }

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
        repository.stopScreenRecord()
        assertNull(repository.getScenarioId())

        // A new recording can select the same original ID before the old write returns.
        repository.setScenarioId(old)
        state.value = DetectorState.RECORDING
        releaseWrite.complete(Unit)

        assertFalse(pending.await())
        assertEquals(old, repository.getScenarioId())
    }
}
