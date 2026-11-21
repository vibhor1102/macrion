/*
 * Copyright (C) 2024 Kevin Buzeau
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.display.recorder.DisplayRecorder
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.common.tutorial.domain.MonitoredViewsManager
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class CaptureViewModel @Inject constructor(
    private val displayRecorder: DisplayRecorder,
    private val smartProcessingRepository: SmartProcessingRepository,
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel()  {

    fun needsScreenCapture(): Boolean = smartProcessingRepository.isFullyStopped()

    fun takeScreenshot(resultCallback: (Bitmap?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(200L.milliseconds)
            val screenshot = displayRecorder.takeScreenshot()

            withContext(Dispatchers.Main) {
                resultCallback(screenshot)
                monitoredViewsManager.notifyClick(MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE)
            }
        }
    }

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createImageCondition(context: Context, area: Rect, bitmap: Bitmap, completed: (ScreenCondition.Image) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val condition = editionRepository.editedItemsBuilder.createNewImageCondition(context, area, bitmap)
            withContext(Dispatchers.Main) { completed(condition) }
        }
    }
}