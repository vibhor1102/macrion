/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays

import android.os.Looper
import androidx.lifecycle.Lifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class OverlayDebounceTests {
    private class TestOverlay : BaseOverlay(recreateOnRotation = true) {
        fun click(action: () -> Unit) = debounceUserInteraction(action)
    }

    @Test fun rotationDuringDebounceAllowsClicksOnTheRecreatedOverlay() {
        val overlay = TestOverlay()
        overlay.create(RuntimeEnvironment.getApplication())
        overlay.resume()
        var clicks = 0
        overlay.click { clicks++ }
        overlay.click { clicks++ }
        assertEquals(1, clicks)
        overlay.changeOrientation()
        assertEquals(Lifecycle.State.RESUMED, overlay.lifecycle.currentState)
        overlay.click { clicks++ }
        assertEquals(2, clicks)
        overlay.click { clicks++ }
        assertEquals(2, clicks)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(501))
        overlay.click { clicks++ }
        assertEquals(3, clicks)
        overlay.destroy()
    }

    @Test fun synchronousRotationInsideClickDoesNotRestoreACancelledGate() {
        val overlay = TestOverlay()
        overlay.create(RuntimeEnvironment.getApplication())
        overlay.resume()
        var clicks = 0
        overlay.click { clicks++; overlay.changeOrientation() }
        overlay.click { clicks++ }
        assertEquals(2, clicks)
        overlay.destroy()
    }
}
