package io.github.vibhor1102.macrion.core.common.overlays.menu

import android.content.Context
import android.app.Activity
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.base.gesture.GestureInteractionGuard
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayGestureInteractionTest {
    @Test fun physicalTouchIsObservedWithoutConsumingTheControl() {
        val guard = mock(GestureInteractionGuard::class.java)
        val host = host(guard)
        var clicked = false
        host.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                verify(guard, never()).onTouchFinished(host)
                clicked = true
            }
            true
        }
        dispatch(host, MotionEvent.ACTION_DOWN)
        verify(guard).onTouchStarted(host)
        dispatch(host, MotionEvent.ACTION_UP)
        assertTrue(clicked)
        verify(guard).onTouchFinished(host)
    }

    @Test fun syntheticTouchesAndOutsideNotificationsDoNotBlockPlayback() {
        val guard = mock(GestureInteractionGuard::class.java)
        val host = host(guard)
        dispatch(host, MotionEvent.ACTION_DOWN, deviceId = -1)
        dispatch(host, MotionEvent.ACTION_UP, deviceId = -1)
        dispatch(host, MotionEvent.ACTION_OUTSIDE)
        verifyNoInteractions(guard)
    }

    @Test fun cancelledTouchReleasesGuard() {
        val guard = mock(GestureInteractionGuard::class.java)
        val host = host(guard)
        dispatch(host, MotionEvent.ACTION_DOWN)
        dispatch(host, MotionEvent.ACTION_CANCEL)
        verify(guard).onTouchFinished(host)
    }

    @Test fun hidingToolbarReleasesAbandonedTouch() = withAttachedHost { host, guard ->
        dispatch(host, MotionEvent.ACTION_DOWN)
        host.visibility = View.GONE
        verify(guard, atLeastOnce()).onTouchFinished(host)
    }

    @Test fun removingToolbarReleasesAbandonedTouch() = withAttachedHost { host, guard ->
        dispatch(host, MotionEvent.ACTION_DOWN)
        (host.parent as ViewGroup).removeView(host)
        verify(guard, atLeastOnce()).onTouchFinished(host)
    }

    private fun withAttachedHost(check: (ComposeOverlayMenuHost, GestureInteractionGuard) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup().visible()
        val guard = mock(GestureInteractionGuard::class.java)
        val host = host(guard, controller.get())
        controller.get().setContentView(host)
        clearInvocations(guard)
        try { check(host, guard) } finally { controller.pause().stop().destroy() }
    }

    private fun host(
        guard: GestureInteractionGuard,
        context: Context = ApplicationProvider.getApplicationContext(),
    ) = ComposeOverlayMenuHost(context).apply {
        gestureInteractionGuard = guard
        setOnTouchListener { _, _ -> true }
        clearInvocations(guard)
    }

    private fun dispatch(host: View, action: Int, deviceId: Int = 4) {
        val event = MotionEvent.obtain(
            0L, 1L, action, 1,
            arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }),
            arrayOf(MotionEvent.PointerCoords().apply { x = 10f; y = 10f }),
            0, 0, 1f, 1f, deviceId, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        try { host.dispatchTouchEvent(event) } finally { event.recycle() }
    }
}
