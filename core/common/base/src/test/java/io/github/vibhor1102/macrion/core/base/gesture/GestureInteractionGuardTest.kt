package io.github.vibhor1102.macrion.core.base.gesture

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestureInteractionGuardTest {
    @Test fun idlePlaybackHasNoDelay() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        guard.awaitIdle()
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun heldTouchWaitsWithoutTimingOutThenSettles() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        val owner = Any()
        guard.onTouchStarted(owner)
        val pending = async { guard.awaitIdle() }
        advanceTimeBy(10_000)
        assertFalse(pending.isCompleted)
        guard.onTouchFinished(owner)
        advanceTimeBy(149)
        assertFalse(pending.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(pending.isCompleted)
    }

    @Test fun repeatedTouchesExtendQuietPeriod() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        val owner = Any()
        guard.onTouchStarted(owner)
        guard.onTouchFinished(owner)
        val pending = async { guard.awaitIdle() }
        advanceTimeBy(100)
        guard.onTouchStarted(owner)
        advanceTimeBy(100)
        guard.onTouchFinished(owner)
        advanceTimeBy(149)
        assertFalse(pending.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(pending.isCompleted)
    }

    @Test fun finishingOneOverlayDoesNotReleaseAnother() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        val first = Any()
        val second = Any()
        guard.onTouchStarted(first)
        guard.onTouchStarted(second)
        val pending = async { guard.awaitIdle() }
        guard.onTouchFinished(first)
        advanceTimeBy(1_000)
        assertFalse(pending.isCompleted)
        guard.onTouchFinished(second)
        advanceTimeBy(150)
        runCurrent()
        assertTrue(pending.isCompleted)
    }

    @Test fun unknownFinishesDoNotDelayIdlePlayback() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        guard.onTouchFinished(Any())
        guard.awaitIdle()
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun recoveryWaitsForFiveHundredMillisecondsAfterRelease() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        val owner = Any()
        guard.onTouchStarted(owner)
        val pending = async { guard.awaitIdle(quietPeriodMs = 500) }
        advanceTimeBy(1_000)
        guard.onTouchFinished(owner)
        advanceTimeBy(499)
        assertFalse(pending.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(pending.isCompleted)
    }

    @Test fun stopCancelsPendingWaitWithoutSchedulingPlayback() = runTest {
        val guard = GestureInteractionGuard { testScheduler.currentTime }
        val owner = Any()
        guard.onTouchStarted(owner)
        var submitted = false
        val pending = async { guard.awaitIdle(); submitted = true }
        runCurrent()
        pending.cancel()
        guard.onTouchFinished(owner)
        advanceTimeBy(1_000)
        assertFalse(submitted)
    }
}
