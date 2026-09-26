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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
class CaptureScreenshotViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val configuredScreenshot = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<CaptureScreenshot>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    val uiState: StateFlow<CaptureScreenshotDialogUiState?> =
        combine(configuredScreenshot, editedActionHasChanged) { screenshot, hasChanged ->
            screenshot.toUiState(hasChanged)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedScreenshot { old -> old.copy(name = "" + name) }
    }

    fun resetFolderToDefault() {
        updateEditedScreenshot { old ->
            old.copy(
                screenshotFolderUri = null,
                screenshotFolderName = null,
            )
        }
    }

    private fun updateEditedScreenshot(updater: (old: CaptureScreenshot) -> CaptureScreenshot) {
        editionRepository.editionState.getEditedAction<CaptureScreenshot>()?.let { old ->
            editionRepository.updateEditedAction(updater(old))
        }
    }
}
