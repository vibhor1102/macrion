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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.screenshot

import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot

data class CaptureScreenshotDialogUiState(
    val name: String,
    val folderUri: String?,
    val folderName: String?,
    val canBeSaved: Boolean,
    val nameError: Boolean,
    val hasUnsavedModifications: Boolean,
)

internal fun CaptureScreenshot.toUiState(hasChanged: Boolean): CaptureScreenshotDialogUiState {
    val isNameBlank = name.isNullOrBlank()
    return CaptureScreenshotDialogUiState(
        name = name ?: "",
        folderUri = screenshotFolderUri,
        folderName = screenshotFolderName,
        canBeSaved = !isNameBlank,
        nameError = isNameBlank,
        hasUnsavedModifications = hasChanged,
    )
}
