package io.github.vibhor1102.macrion.core.ui.compose.overlay

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vibhor1102.macrion.core.base.gesture.MAX_TOUCH_DURATION_MS
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class GestureRecordingSessionTests {
    private fun session() = GestureRecordingSession(100L, 1080f, 2400f, 40f, 4f)

    @Test fun motionlessFingersFinishAtPlatformDeadline() {
        val session = session()
        session.update(1, 10f, 20f, 100, true)
        session.update(2, 30f, 40f, 200, true)
        session.finishAtDeadline()
        val result = session.gesture(true) as RecordedGesture.Split
        assertEquals(MAX_TOUCH_DURATION_MS, result.durationMs)
        assertTrue(result.subGestures.all { it.durationMs + it.startOffsetMs <= MAX_TOUCH_DURATION_MS })
        assertTrue(session.isExpired(100 + MAX_TOUCH_DURATION_MS))
    }

    @Test fun repeatedContactWithRecycledIdDoesNotOverwriteEarlierStroke() {
        val session = session()
        session.update(1, 10f, 20f, 100, true)
        session.update(2, 30f, 40f, 200, true)
        session.update(2, 30f, 40f, 250, false)
        session.update(2, 50f, 60f, 300, true)
        session.update(2, 50f, 60f, 350, false)
        session.update(1, 10f, 20f, 400, false)
        val result = session.gesture(true) as RecordedGesture.Split
        assertEquals(listOf(0L, 100L, 200L), result.subGestures.map { it.startOffsetMs })
        assertEquals(listOf(300L, 50L, 50L), result.subGestures.map { it.durationMs })
    }

    @Test fun tooManyContactsOrLateEventsAreRejected() {
        val session = session()
        repeat(10) { assertTrue(session.update(it.toLong(), 10f, 20f, 100 + it.toLong(), true)) }
        assertFalse(session.update(11, 30f, 40f, 200, true))
        assertFalse(session.update(1, 100f, 200f, 100 + MAX_TOUCH_DURATION_MS, true))
    }
}
