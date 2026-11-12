package io.github.vibhor1102.macrion.feature.tutorial.ui.dialogs

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.base.gesture.GestureInteractionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class GestureCoordinatedTipDialogTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun heldDialogTouchBlocksInjectionAndDismissalEstablishesQuietPeriod() = runTest {
        val guard = GestureInteractionGuard()
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), android.R.style.Theme_Material_Light_Dialog)
        val dialog = GestureCoordinatedTipDialog(context, guard)
        dialog.setContentView(View(context))
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        touch(dialog, MotionEvent.ACTION_DOWN)
        val pending = async { guard.awaitIdle() }
        runCurrent()
        assertFalse(pending.isCompleted)
        dialog.dismiss()
        runCurrent()
        assertFalse(pending.isCompleted)
        ShadowSystemClock.advanceBy(Duration.ofMillis(150))
        advanceTimeBy(150)
        runCurrent()
        assertTrue(pending.isCompleted)
    }

    @Test fun cancelReleasesHeldDialogTouch() = runTest {
        val guard = GestureInteractionGuard()
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), android.R.style.Theme_Material_Light_Dialog)
        val dialog = GestureCoordinatedTipDialog(context, guard)
        dialog.setContentView(View(context))
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        touch(dialog, MotionEvent.ACTION_DOWN)
        val pending = async { guard.awaitIdle(quietPeriodMs = 0) }
        runCurrent()
        assertFalse(pending.isCompleted)
        touch(dialog, MotionEvent.ACTION_CANCEL)
        runCurrent()
        assertTrue(pending.isCompleted)
        dialog.dismiss()
    }

    private fun touch(dialog: GestureCoordinatedTipDialog, action: Int) {
        val event = MotionEvent.obtain(0L, 0L, action, 1f, 1f, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        try { dialog.dispatchTouchEvent(event) } finally { event.recycle() }
    }
}
