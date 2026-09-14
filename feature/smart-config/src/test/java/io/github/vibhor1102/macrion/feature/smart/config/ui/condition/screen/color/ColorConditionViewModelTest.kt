/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color

import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.EditedElementState
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.IEditionState
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.hsvToColorInt
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.toHsv
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ColorConditionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHsvConversionRoundtrip() {
        val red = 0xFFFF0000.toInt()
        val redHsv = red.toHsv()
        assertEquals(0f, redHsv[0], 0.01f)
        assertEquals(1f, redHsv[1], 0.01f)
        assertEquals(1f, redHsv[2], 0.01f)
        assertEquals(red, hsvToColorInt(redHsv[0], redHsv[1], redHsv[2]))

        val green = 0xFF00FF00.toInt()
        val greenHsv = green.toHsv()
        assertEquals(120f, greenHsv[0], 0.01f)
        assertEquals(1f, greenHsv[1], 0.01f)
        assertEquals(1f, greenHsv[2], 0.01f)
        assertEquals(green, hsvToColorInt(greenHsv[0], greenHsv[1], greenHsv[2]))

        val blue = 0xFF0000FF.toInt()
        val blueHsv = blue.toHsv()
        assertEquals(240f, blueHsv[0], 0.01f)
        assertEquals(1f, blueHsv[1], 0.01f)
        assertEquals(1f, blueHsv[2], 0.01f)
        assertEquals(blue, hsvToColorInt(blueHsv[0], blueHsv[1], blueHsv[2]))
    }

    @Test
    fun testViewModelInitializesWithHsvAndAllowsSlidersToUpdateColor() = runTest {
        val initialCondition = ScreenCondition.Color(
            id = Identifier(tempId = 1L),
            eventId = Identifier(databaseId = 10L),
            name = "Test Color Condition",
            detectionArea = Rect(10, 20, 11, 21),
            color = 0xFFFF0000.toInt(),
            threshold = 10,
            priority = 0,
            shouldBeDetected = true,
        )

        val editedConditionStateFlow = MutableStateFlow(
            EditedElementState<ScreenCondition>(
                value = initialCondition,
                hasChanged = false,
                canBeSaved = true,
            )
        )
        val isEditingFlow = MutableStateFlow(true)

        val mockEditionState = mockk<IEditionState>()
        every { mockEditionState.editedScreenConditionState } returns editedConditionStateFlow
        every { mockEditionState.getEditedCondition<ScreenCondition.Color>() } answers {
            editedConditionStateFlow.value.value as? ScreenCondition.Color
        }


        val mockRepository = mockk<EditionRepository>()
        every { mockRepository.editionState } returns mockEditionState
        every { mockRepository.isEditingCondition } returns isEditingFlow
        every { mockRepository.updateEditedCondition(any<Condition>()) } answers {
            val updated = firstArg<Condition>()
            editedConditionStateFlow.value = EditedElementState(value = updated as ScreenCondition, hasChanged = true, canBeSaved = true)
        }

        val viewModel = ColorConditionViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialUiState = viewModel.uiState.filterNotNull().first()
        assertEquals(0f, initialUiState.hue, 0.01f)
        assertEquals(1f, initialUiState.saturation, 0.01f)
        assertEquals(1f, initialUiState.value, 0.01f)

        // Change hue to green (120°)
        viewModel.setHue(120f)
        testDispatcher.scheduler.advanceUntilIdle()

        val greenUiState = viewModel.uiState.filterNotNull().first()
        assertEquals(120f, greenUiState.hue, 0.01f)
        assertEquals(0xFF00FF00.toInt(), greenUiState.conditionColor)

        // Set value to 0 (black) and verify hue is preserved across updates
        viewModel.setValue(0f)
        testDispatcher.scheduler.advanceUntilIdle()

        val blackUiState = viewModel.uiState.filterNotNull().first()
        assertEquals(0f, blackUiState.value, 0.01f)
        assertEquals(Color.BLACK, blackUiState.conditionColor)
        assertEquals(120f, blackUiState.hue, 0.01f)

        // Change hue while value is 0 (color remains black, but hue slider retains position)
        viewModel.setHue(240f)
        testDispatcher.scheduler.advanceUntilIdle()

        val blueHueBlackUiState = viewModel.uiState.filterNotNull().first()
        assertEquals(240f, blueHueBlackUiState.hue, 0.01f)
        assertEquals(Color.BLACK, blueHueBlackUiState.conditionColor)

        // Now restore value to 1f and verify it becomes pure blue (240°)
        viewModel.setValue(1f)
        testDispatcher.scheduler.advanceUntilIdle()

        val restoredBlueUiState = viewModel.uiState.filterNotNull().first()
        assertEquals(240f, restoredBlueUiState.hue, 0.01f)
        assertEquals(1f, restoredBlueUiState.value, 0.01f)
        assertEquals(0xFF0000FF.toInt(), restoredBlueUiState.conditionColor)
    }
}
