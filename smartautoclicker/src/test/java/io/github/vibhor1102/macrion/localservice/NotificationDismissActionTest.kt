/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.localservice

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDismissActionTest {

    @Test
    fun `visible overlay does nothing when notification is dismissed`() {
        assertEquals(
            NotificationDismissAction.DO_NOTHING,
            determineNotificationDismissAction(
                isOverlayHidden = false,
                isScenarioRunning = false,
                hasOverlayAboveRoot = false,
            ),
        )
        assertEquals(
            NotificationDismissAction.DO_NOTHING,
            determineNotificationDismissAction(
                isOverlayHidden = false,
                isScenarioRunning = true,
                hasOverlayAboveRoot = false,
            ),
        )
        assertEquals(
            NotificationDismissAction.DO_NOTHING,
            determineNotificationDismissAction(
                isOverlayHidden = false,
                isScenarioRunning = false,
                hasOverlayAboveRoot = true,
            ),
        )
        assertEquals(
            NotificationDismissAction.DO_NOTHING,
            determineNotificationDismissAction(
                isOverlayHidden = false,
                isScenarioRunning = true,
                hasOverlayAboveRoot = true,
            ),
        )
    }

    @Test
    fun `hidden overlay with active running scenario restores overlay`() {
        assertEquals(
            NotificationDismissAction.RESTORE_OVERLAY,
            determineNotificationDismissAction(
                isOverlayHidden = true,
                isScenarioRunning = true,
                hasOverlayAboveRoot = false,
            ),
        )
    }

    @Test
    fun `hidden overlay with child overlay or config above root restores overlay`() {
        assertEquals(
            NotificationDismissAction.RESTORE_OVERLAY,
            determineNotificationDismissAction(
                isOverlayHidden = true,
                isScenarioRunning = false,
                hasOverlayAboveRoot = true,
            ),
        )
    }

    @Test
    fun `hidden overlay with both running scenario and child overlay restores overlay`() {
        assertEquals(
            NotificationDismissAction.RESTORE_OVERLAY,
            determineNotificationDismissAction(
                isOverlayHidden = true,
                isScenarioRunning = true,
                hasOverlayAboveRoot = true,
            ),
        )
    }

    @Test
    fun `hidden overlay when paused and at root stops scenario`() {
        assertEquals(
            NotificationDismissAction.STOP_SCENARIO,
            determineNotificationDismissAction(
                isOverlayHidden = true,
                isScenarioRunning = false,
                hasOverlayAboveRoot = false,
            ),
        )
    }
}
