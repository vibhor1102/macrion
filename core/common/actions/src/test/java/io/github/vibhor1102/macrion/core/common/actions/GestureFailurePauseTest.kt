package io.github.vibhor1102.macrion.core.common.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import io.github.vibhor1102.macrion.core.base.gesture.GestureInteractionGuard
import io.github.vibhor1102.macrion.core.common.actions.gesture.GestureExecutor
import io.github.vibhor1102.macrion.core.common.actions.notification.NotificationRequestExecutor
import io.github.vibhor1102.macrion.core.common.actions.screenshot.ScreenshotExecutor
import io.github.vibhor1102.macrion.core.common.actions.sound.SoundExecutor
import io.github.vibhor1102.macrion.core.common.actions.text.TextExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureFailurePauseTest {
    private lateinit var service: AccessibilityService
    private lateinit var executor: AndroidActionExecutorImpl
    private var pauseCount = 0

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        service = mock(AccessibilityService::class.java)
        executor = AndroidActionExecutorImpl(
            GestureExecutor(GestureInteractionGuard()),
            mock(NotificationRequestExecutor::class.java),
            mock(TextExecutor::class.java),
            mock(SoundExecutor::class.java),
            mock(ScreenshotExecutor::class.java),
        )
        executor.init(service) { pauseCount++ }
    }

    @After fun teardown() = Dispatchers.resetMain()

    @Test fun repeatedRejectionPausesAndNeverRunsNextAction() = runTest {
        var nextActionRan = false
        val job = launch { executor.dispatchGesture(gesture()); nextActionRan = true }
        advanceTimeBy(501)
        runCurrent()
        assertTrue(job.isCancelled)
        assertFalse(nextActionRan)
        assertEquals(1, pauseCount)
        verify(service, times(2)).dispatchGesture(any(), any(), any())
    }

    @Test fun unknownTimeoutPausesWithoutRetryOrNextAction() = runTest {
        `when`(service.dispatchGesture(any(), any(), any())).thenReturn(true)
        var nextActionRan = false
        val job = launch { executor.dispatchGesture(gesture()); nextActionRan = true }
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(job.isCancelled)
        assertFalse(nextActionRan)
        assertEquals(1, pauseCount)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    @Test fun userStopCancelsRetryWithoutReportingAnotherFailure() = runTest {
        val job = launch { executor.dispatchGesture(gesture()) }
        runCurrent()
        job.cancel()
        advanceTimeBy(1_000)
        assertEquals(0, pauseCount)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    private fun gesture(): GestureDescription = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(Path().apply { moveTo(1f, 1f) }, 0L, 50L))
        .build()
}
