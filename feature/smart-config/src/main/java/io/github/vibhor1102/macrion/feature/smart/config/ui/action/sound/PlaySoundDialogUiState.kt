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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.sound

import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.feature.smart.config.domain.sound.NotificationSoundItem

data class PlaySoundDialogUiState(
    val name: String,
    val selectedUri: String?,
    val selectedTitle: String?,
    val sounds: List<NotificationSoundItem>,
    val playingPreviewUri: String?,
    val canBeSaved: Boolean,
    val nameError: Boolean,
    val hasUnsavedModifications: Boolean,
)

internal fun PlaySound.toUiState(
    hasChanged: Boolean,
    sounds: List<NotificationSoundItem>,
    playingPreviewUri: String?,
): PlaySoundDialogUiState {
    val isNameBlank = name.isNullOrBlank()
    val isSoundBlank = soundUri.isNullOrBlank()

    return PlaySoundDialogUiState(
        name = name ?: "",
        selectedUri = soundUri,
        selectedTitle = soundTitle,
        sounds = sounds,
        playingPreviewUri = playingPreviewUri,
        canBeSaved = !isNameBlank && !isSoundBlank,
        nameError = isNameBlank,
        hasUnsavedModifications = hasChanged,
    )
}
