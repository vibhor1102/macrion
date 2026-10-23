/* Copyright (C) 2026 Vibhor Goel; SPDX-License-Identifier: GPL-3.0-or-later */
package io.github.vibhor1102.macrion.core.ui.compose.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewAnimationDurationTests {
    @Test fun longPausesRemainPositiveInsteadOfWrapping() {
        assertEquals(Int.MAX_VALUE, 2_160_000_000L.toPreviewAnimationDuration(500))
        assertEquals(Int.MAX_VALUE, Long.MAX_VALUE.toPreviewAnimationDuration(500))
    }

    @Test fun ordinaryDurationsKeepTheirTimingAndMinimum() {
        assertEquals(500, 0L.toPreviewAnimationDuration(500))
        assertEquals(500, (-1L).toPreviewAnimationDuration(500))
        assertEquals(250, 1L.toPreviewAnimationDuration(250))
        assertEquals(60_000, 60_000L.toPreviewAnimationDuration(500))
    }
}
