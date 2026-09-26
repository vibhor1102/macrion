/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.actions.screenshot

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.core.display.recorder.DisplayRecorder
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScreenshotExecutorTests {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockDisplayRecorder: DisplayRecorder = mock(DisplayRecorder::class.java)
    private val mockSettingsRepository: SettingsRepository = mock(SettingsRepository::class.java)

    private lateinit var executor: ScreenshotExecutor

    @Before
    fun setUp() {
        executor = ScreenshotExecutor(context, mockDisplayRecorder, mockSettingsRepository)
    }

    @Test
    fun captureScreenshot_underLimit_returnsTrue() = runTest {
        mockWhen(mockSettingsRepository.getScreenshotRateLimitPerMinute()).thenReturn(3)

        assertTrue(executor.captureScreenshot(null, null))
        assertTrue(executor.captureScreenshot(null, null))
        assertTrue(executor.captureScreenshot(null, null))
    }

    @Test
    fun captureScreenshot_exceedsLimit_returnsFalse() = runTest {
        mockWhen(mockSettingsRepository.getScreenshotRateLimitPerMinute()).thenReturn(2)

        assertTrue(executor.captureScreenshot(null, null))
        assertTrue(executor.captureScreenshot(null, null))
        assertFalse("Third capture within 60s should be rejected", executor.captureScreenshot(null, null))
    }

    @Test
    fun captureScreenshot_afterReset_allowsNewCaptures() = runTest {
        mockWhen(mockSettingsRepository.getScreenshotRateLimitPerMinute()).thenReturn(2)

        assertTrue(executor.captureScreenshot(null, null))
        assertTrue(executor.captureScreenshot(null, null))
        assertFalse(executor.captureScreenshot(null, null))

        executor.resetState()

        assertTrue("After reset, fresh run should allow captures again", executor.captureScreenshot(null, null))
    }

    @Test
    fun captureScreenshot_limitZero_disablesLimit() = runTest {
        mockWhen(mockSettingsRepository.getScreenshotRateLimitPerMinute()).thenReturn(0)

        for (i in 1..25) {
            assertTrue("Capture $i should succeed when rate limit is disabled (0)", executor.captureScreenshot(null, null))
        }
    }
}
