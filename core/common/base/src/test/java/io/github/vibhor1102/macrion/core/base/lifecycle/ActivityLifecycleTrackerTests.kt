package io.github.vibhor1102.macrion.core.base.lifecycle

import android.app.Activity
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class ActivityLifecycleTrackerTests {
    @Test fun aCoveredOrStoppedActivityStillPreventsAnUnsolicitedReview() {
        val tracker = ActivityLifecycleTracker()
        val first = mock(Activity::class.java)
        val second = mock(Activity::class.java)
        assertFalse(tracker.hasActiveActivity())
        tracker.onActivityCreated(first, null)
        tracker.onActivityStopped(first)
        assertTrue(tracker.hasActiveActivity())
        tracker.onActivityCreated(second, null)
        tracker.onActivityDestroyed(first)
        assertTrue(tracker.hasActiveActivity())
        `when`(second.isFinishing).thenReturn(true)
        assertFalse(tracker.hasActiveActivity())
        tracker.onActivityDestroyed(second)
        assertFalse(tracker.hasActiveActivity())
    }
}
