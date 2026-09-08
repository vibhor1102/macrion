/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build

import io.github.vibhor1102.macrion.core.common.tutorial.domain.MonitoredViewsManager
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.display.recorder.DisplayRecorder
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditedItemsBuilder
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CaptureViewModelTest {

    // Set main to unconfined so withContext(Dispatchers.Main) in the viewmodel runs
    // directly on the IO thread instead of posting to the blocked test thread.
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockDisplayRecorder: DisplayRecorder = mockk()
    private val mockEditedItemsBuilder: EditedItemsBuilder = mockk()
    private val mockEditionRepository: EditionRepository = mockk {
        every { editedItemsBuilder } returns mockEditedItemsBuilder
    }
    private val mockMonitoredViewsManager: MonitoredViewsManager = mockk(relaxed = true)

    private lateinit var viewModel: CaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CaptureViewModel(mockDisplayRecorder, mockEditionRepository, mockMonitoredViewsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun takeScreenshot_callsCallbackWithBitmapFromRecorder() {
        val bitmap = mockk<Bitmap>()
        coEvery { mockDisplayRecorder.takeScreenshot() } returns bitmap
        val latch = CountDownLatch(1)
        var capturedBitmap: Bitmap? = null

        viewModel.takeScreenshot {
            capturedBitmap = it
            latch.countDown()
        }

        assertTrue("Callback not called within timeout", latch.await(1, TimeUnit.SECONDS))
        assertEquals(bitmap, capturedBitmap)
    }

    @Test
    fun takeScreenshot_callsCallbackWithNull_whenRecorderReturnsNull() {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns null
        val latch = CountDownLatch(1)
        var capturedBitmap: Bitmap? = mockk() // intentionally non-null sentinel

        viewModel.takeScreenshot {
            capturedBitmap = it
            latch.countDown()
        }

        assertTrue("Callback not called within timeout", latch.await(1, TimeUnit.SECONDS))
        assertNull(capturedBitmap)
    }

    @Test
    fun takeScreenshot_notifiesMonitoredViewsManager() {
        coEvery { mockDisplayRecorder.takeScreenshot() } returns mockk()
        val notificationLatch = CountDownLatch(1)
        every {
            mockMonitoredViewsManager.notifyClick(
                MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE,
            )
        } answers { notificationLatch.countDown() }

        viewModel.takeScreenshot { }

        assertTrue("Tutorial notification not sent within timeout", notificationLatch.await(1, TimeUnit.SECONDS))
        verify { mockMonitoredViewsManager.notifyClick(MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE) }
    }

    @Test
    fun createImageCondition_callsCompletedWithCreatedCondition() {
        val context = mockk<Context>()
        val area = mockk<Rect>()
        val bitmap = mockk<Bitmap>()
        val condition = mockk<ScreenCondition.Image>()
        coEvery { mockEditedItemsBuilder.createNewImageCondition(context, area, bitmap) } returns condition
        val latch = CountDownLatch(1)
        var capturedCondition: ScreenCondition.Image? = null

        viewModel.createImageCondition(context, area, bitmap) {
            capturedCondition = it
            latch.countDown()
        }

        assertTrue("Completed callback not called within timeout", latch.await(1, TimeUnit.SECONDS))
        assertEquals(condition, capturedCondition)
    }

    @Test
    fun createImageCondition_delegatesToEditionRepository() {
        val context = mockk<Context>()
        val area = mockk<Rect>()
        val bitmap = mockk<Bitmap>()
        val condition = mockk<ScreenCondition.Image>()
        coEvery { mockEditedItemsBuilder.createNewImageCondition(context, area, bitmap) } returns condition
        val latch = CountDownLatch(1)

        viewModel.createImageCondition(context, area, bitmap) { latch.countDown() }

        assertTrue("Completed callback not called within timeout", latch.await(1, TimeUnit.SECONDS))
        // Verify the builder was called with exact arguments
        io.mockk.coVerify { mockEditedItemsBuilder.createNewImageCondition(context, area, bitmap) }
    }
}
