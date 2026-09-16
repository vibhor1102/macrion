/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.diagnostics

import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.disposeTabCompositionsOnDetach
import android.content.ContextWrapper
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

/** Diagnostic reproduction of the editor's AndroidView-hosted, retained tab composition. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DetachedPopupDiagnosticTest {
    @Test fun disposingDetachedTabPreventsPopupRequestAndAllowsReopening() {
        checkDetachedPopup(true)
        checkDetachedPopup(false)
    }

    private fun checkDetachedPopup(retainComposition: Boolean) {
        org.robolectric.shadows.ShadowChoreographer.setPaused(true)
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val expanded = mutableStateOf(false)
        val popupTokens = mutableListOf<IBinder?>()
        val realManager = activity.getSystemService(WindowManager::class.java)
        val recordingManager = object : WindowManager by realManager {
            override fun addView(view: View, params: ViewGroup.LayoutParams) {
                popupTokens.add((params as WindowManager.LayoutParams).token)
                // Capture the request without asking Robolectric's window server to validate it.
            }
            override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) = Unit
            override fun removeViewImmediate(view: View) = Unit
        }
        val context = object : ContextWrapper(activity) {
            override fun getSystemService(name: String): Any? =
                if (name == WINDOW_SERVICE) recordingManager else super.getSystemService(name)
        }
        val child = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { if (expanded.value) Popup { Box {} } }
        }
        val container = FrameLayout(activity).apply { addView(child) }
        if (!retainComposition) container.disposeTabCompositionsOnDetach()
        val host = ComposeView(activity).apply {
            setContent { AndroidView(factory = { container }) }
        }
        activity.setContentView(host)
        controller.visible()
        pumpFrames()
        assertTrue(child.isAttachedToWindow)
        assertTrue(child.hasComposition)
        assertNotNull(child.applicationWindowToken)
        // Queue opening a popup, then switch tabs before Compose applies that state change.
        expanded.value = true
        container.removeView(child)
        assertNull(child.applicationWindowToken)
        pumpFrames()
        if (retainComposition) {
            assertTrue("Retained tab should still own its composition", child.hasComposition)
            assertEquals("Detached tab attempted to open a popup", 1, popupTokens.size)
            assertNull("Popup has no valid parent window token", popupTokens.single())
        } else {
            assertFalse(child.hasComposition)
            assertTrue(popupTokens.isEmpty())
        }
        if (!retainComposition) {
            expanded.value = false
            container.addView(child)
            child.measure(View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY))
            child.layout(0, 0, 200, 200)
            child.viewTreeObserver.dispatchOnPreDraw()
            pumpFrames()
            assertTrue("Tab composition is recreated on return", child.hasComposition)
            expanded.value = true
            pumpFrames()
            // Snapshot notifications also run on a background coroutine dispatcher.
            // Allow that dispatcher to deliver invalidations while advancing the UI frames.
            val deadline = System.nanoTime() + 2_000_000_000L
            while (popupTokens.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10)
                pumpFrames()
            }
            assertEquals(1, popupTokens.size)
            assertNotNull("Reopened popup uses the attached window", popupTokens.single())
        }
        child.disposeComposition()
        controller.pause().stop().destroy()
    }

    private fun pumpFrames() {
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        repeat(10) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(32)) }
    }
}
