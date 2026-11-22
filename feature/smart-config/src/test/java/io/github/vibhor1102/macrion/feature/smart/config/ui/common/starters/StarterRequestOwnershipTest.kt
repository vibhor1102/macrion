/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters

import android.app.Application
import android.content.Intent
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StarterRequestOwnershipTest {
    @Test
    fun onlyTheCurrentStarterAcceptsItsActivityResult() {
        val manager = mockk<OverlayManager>()
        val old = OwnedActivityStarterOverlay { Intent() }
        val replacement = OwnedActivityStarterOverlay { Intent() }

        every { manager.getBackStackTop() } returns old
        every { manager.isCurrentOverlay(old) } returns true
        assertTrue(manager.ownsStarterRequest(old.requestId))

        every { manager.isCurrentOverlay(old) } returns false
        assertFalse(manager.ownsStarterRequest(old.requestId))

        every { manager.getBackStackTop() } returns replacement
        every { manager.isCurrentOverlay(replacement) } returns true
        assertFalse(manager.ownsStarterRequest(old.requestId))
        assertTrue(manager.ownsStarterRequest(replacement.requestId))
    }
}
