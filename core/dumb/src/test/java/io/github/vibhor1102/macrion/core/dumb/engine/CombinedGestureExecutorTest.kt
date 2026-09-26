package io.github.vibhor1102.macrion.core.dumb.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.common.actions.AndroidActionExecutor
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedGestureExecutorTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun preservesExactTimingAndRepeatsWholeGestureWhenRandomizationIsOff() = runTest {
        val androidExecutor = mock<AndroidActionExecutor>()
        val executor = DumbActionExecutor(androidExecutor)
        val scenarioId = Identifier(databaseId = 1)
        val click = DumbAction.DumbClick(
            id = Identifier(databaseId = 2), scenarioId = scenarioId, name = "Hold",
            repeatCount = 99, isRepeatInfinite = true, repeatDelayMs = 500,
            position = Point(100, 100), pressDurationMs = 100, waitAfterMs = 1200,
        )
        val swipe = DumbAction.DumbSwipe(
            id = Identifier(databaseId = 3), scenarioId = scenarioId, name = "Drag",
            repeatCount = 99, isRepeatInfinite = true, repeatDelayMs = 500,
            fromPosition = Point(200, 200), toPosition = Point(300, 300),
            swipeDurationMs = 800, waitBeforeMs = 200,
        )
        val group = DumbAction.DumbSplitAction(
            id = Identifier(databaseId = 4), scenarioId = scenarioId, name = "Combined",
            repeatCount = 2, waitBeforeMs = 100, waitAfterMs = 200,
            subActions = listOf(click, swipe),
        )
        executor.executeDumbAction(group, randomize = false)
        val gestures = argumentCaptor<GestureDescription>()
        verify(androidExecutor, times(2)).dispatchGesture(gestures.capture())
        gestures.allValues.forEach { gesture ->
            assertEquals(2, gesture.strokeCount)
            assertEquals(100L, gesture.getStroke(0).duration)
            assertEquals(800L, gesture.getStroke(1).duration)
            assertEquals(200L, gesture.getStroke(1).startTime)
        }
        // Mock dispatch returns immediately: two 300ms tails plus the parent's waits.
        assertEquals(900L, currentTime)
    }
}
