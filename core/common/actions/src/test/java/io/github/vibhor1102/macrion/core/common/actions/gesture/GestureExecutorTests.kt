/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.common.actions.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import io.github.vibhor1102.macrion.core.base.gesture.GestureInteractionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureExecutorTests {
    private lateinit var service: AccessibilityService
    private lateinit var guard: GestureInteractionGuard
    private lateinit var executor: GestureExecutor
    private lateinit var callbacks: ArgumentCaptor<GestureResultCallback>

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        service = mock(AccessibilityService::class.java)
        `when`(service.dispatchGesture(any(), any(), any())).thenReturn(true)
        guard = GestureInteractionGuard()
        executor = GestureExecutor(guard)
        callbacks = ArgumentCaptor.forClass(GestureResultCallback::class.java)
    }

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun completedCallbackReturnsSuccessWithoutRetry() = runTest {
        val pending = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, pending.await())
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    @Test fun rejectionIsRecognizedImmediatelyAndRetriedOnce() = runTest {
        `when`(service.dispatchGesture(any(), any(), any())).thenReturn(false, true)
        val pending = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        advanceTimeBy(499)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
        advanceTimeBy(1)
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, pending.await())
        assertEquals(500L, testScheduler.currentTime)
    }

    @Test fun cancelledGestureWaitsForRecoveryAndRetriesSameGesture() = runTest {
        val gesture = gesture()
        val pending = async { executor.dispatchGesture(service, gesture) }
        runCurrent()
        verify(service).dispatchGesture(eq(gesture), callbacks.capture(), any())
        callbacks.value.onCancelled(null)
        runCurrent()
        advanceTimeBy(499)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
        advanceTimeBy(1)
        runCurrent()
        verify(service, times(2)).dispatchGesture(eq(gesture), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, pending.await())
    }

    @Test fun secondRejectionDoesNotRetryAgain() = runTest {
        `when`(service.dispatchGesture(any(), any(), any())).thenReturn(false)
        assertEquals(GestureDispatchResult.REJECTED, executor.dispatchGesture(service, gesture()))
        verify(service, times(2)).dispatchGesture(any(), any(), any())
    }

    @Test fun secondCancellationDoesNotRetryAgain() = runTest {
        doAnswer {
            it.getArgument<GestureResultCallback>(1).onCancelled(null)
            true
        }.`when`(service).dispatchGesture(any(), any(), any())
        assertEquals(GestureDispatchResult.CANCELLED, executor.dispatchGesture(service, gesture()))
        verify(service, times(2)).dispatchGesture(any(), any(), any())
    }

    @Test fun missingCallbackTimesOutWithoutRetryingUnknownOutcome() = runTest {
        assertEquals(GestureDispatchResult.TIMED_OUT, executor.dispatchGesture(service, gesture()))
        verify(service, times(1)).dispatchGesture(any(), any(), any())
        assertEquals(200L, testScheduler.currentTime)
    }

    @Test fun longGestureIsNotDeclaredFailedAtFiveHundredMilliseconds() = runTest {
        val pending = async { executor.dispatchGesture(service, gesture(durationMs = 1_000)) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbacks.capture(), any())
        advanceTimeBy(500)
        assertFalse(pending.isCompleted)
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, pending.await())
    }

    @Test fun submissionExceptionDoesNotBlindlyRetry() = runTest {
        doThrow(IllegalStateException("Accessibility service unavailable"))
            .`when`(service).dispatchGesture(any(), any(), any())
        assertEquals(GestureDispatchResult.ERROR, executor.dispatchGesture(service, gesture()))
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    @Test fun stopDuringRecoveryCancelsRetryAndReleasesDispatchLock() = runTest {
        val pending = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCancelled(null)
        runCurrent()
        pending.cancel()
        advanceTimeBy(1_000)
        verify(service, times(1)).dispatchGesture(any(), any(), any())

        val next = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, next.await())
    }

    @Test fun waitingForToolbarDoesNotSubmitOrStartCallbackTimeout() = runTest {
        val owner = Any()
        guard.onTouchStarted(owner)
        val pending = async { executor.dispatchGesture(service, gesture()) }
        advanceTimeBy(10_000)
        assertFalse(pending.isCompleted)
        verify(service, never()).dispatchGesture(any(), any(), any())
        pending.cancel()
        guard.onTouchFinished(owner)
        advanceTimeBy(1_000)
        verify(service, never()).dispatchGesture(any(), any(), any())
    }

    @Test fun retryRemainsPendingWhileToolbarIsHeld() = runTest {
        val pending = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCancelled(null)
        runCurrent()
        val owner = Any()
        guard.onTouchStarted(owner)
        advanceTimeBy(5_000)
        assertFalse(pending.isCompleted)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
        pending.cancel()
        guard.onTouchFinished(owner)
    }

    @Test fun lateCallbackCannotCompleteNextSubmission() = runTest {
        val first = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbacks.capture(), any())
        val lateCallback = callbacks.value
        assertEquals(GestureDispatchResult.TIMED_OUT, first.await())
        val second = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbacks.capture(), any())
        lateCallback.onCompleted(null)
        runCurrent()
        assertFalse(second.isCompleted)
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, second.await())
    }

    @Test fun submissionsAreSerializedUntilPreviousGestureCompletes() = runTest {
        val first = async { executor.dispatchGesture(service, gesture()) }
        val second = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service, times(1)).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbacks.capture(), any())
        callbacks.value.onCompleted(null)
        assertEquals(GestureDispatchResult.COMPLETED, first.await())
        assertEquals(GestureDispatchResult.COMPLETED, second.await())
    }

    private fun gesture(durationMs: Long = 100L): GestureDescription = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(
            Path().apply { moveTo(0f, 0f); lineTo(1f, 1f) }, 0L, durationMs,
        )).build()
}
