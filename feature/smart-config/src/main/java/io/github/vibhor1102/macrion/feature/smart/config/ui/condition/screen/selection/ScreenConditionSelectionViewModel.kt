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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selection

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.utils.getImageConditionBitmap
import kotlinx.coroutines.Job
import javax.inject.Inject


class ScreenConditionSelectionViewModel @Inject constructor(
    private val bitmapRepository: BitmapRepository,
) : ViewModel() {

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)
}