/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.common.overlays.dialog

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.MoveToDialog
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.display.di.DisplayEntryPoint
import dagger.hilt.EntryPoints
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MoveToDialogRotationTests {
    @Test
    fun oldDialogDismissalDoesNotDestroyTheRecreatedOverlay() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val entryPoint = mockk<DisplayEntryPoint>()
        every { entryPoint.displayMetrics() } returns DisplayConfigManager(activity)
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(activity.applicationContext, DisplayEntryPoint::class.java) } returns entryPoint

        var dismissals = 0
        val overlay = MoveToDialog(io.github.vibhor1102.macrion.core.ui.R.style.AppTheme, 1, 4) {}
        try {
            overlay.create(activity) { _, _ -> dismissals++ }
            overlay.resume()
            overlay.changeOrientation()
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(Lifecycle.State.RESUMED, overlay.lifecycle.currentState)
            assertEquals(0, dismissals)
        } finally {
            overlay.destroy()
            unmockkStatic(EntryPoints::class)
            controller.pause().stop().destroy()
        }
    }
}
