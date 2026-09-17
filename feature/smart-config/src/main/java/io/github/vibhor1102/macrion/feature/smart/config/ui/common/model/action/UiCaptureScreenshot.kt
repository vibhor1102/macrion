/*
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.feature.smart.config.R

@DrawableRes
internal fun getCaptureScreenshotIconRes(): Int = R.drawable.ic_action_screenshot

internal fun CaptureScreenshot.getDescription(context: Context, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)
    val folder = screenshotFolderName
    return if (!folder.isNullOrBlank()) {
        context.getString(R.string.item_capture_screenshot_details_custom, folder)
    } else {
        context.getString(R.string.item_capture_screenshot_details_default)
    }
}
