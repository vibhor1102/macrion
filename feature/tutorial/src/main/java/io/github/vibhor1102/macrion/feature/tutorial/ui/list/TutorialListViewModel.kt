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
package io.github.vibhor1102.macrion.feature.tutorial.ui.list

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionAccessibilityService
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionOverlay
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.feature.tutorial.data.mapping.toTutorialItem
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategory
import io.github.vibhor1102.macrion.feature.tutorial.domain.GetTutorialCategoryUseCase
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategoryUiItems
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategoryUiState

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TutorialListViewModel @Inject constructor(
    private val appComponentsProvider: AppComponentsProvider,
    private val accessibilityServiceConnection: LocalAccessibilityServiceConnection,
    val permissionsController: PermissionsController,
    private val tutorialRepository: TutorialRepository,
    private val getTutorialCategoryUseCase: GetTutorialCategoryUseCase,
) : ViewModel() {

    private val categoryStates = mutableMapOf<TutorialCategory.Type, StateFlow<TutorialCategoryUiState>>()

    fun uiState(categoryType: TutorialCategory.Type): StateFlow<TutorialCategoryUiState> =
        categoryStates.getOrPut(categoryType) {
            getTutorialCategoryUseCase(categoryType)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), TutorialCategoryUiState.Loading)
        }

    fun startPermissionFlowIfNeeded(activity: Context, onAllGranted: () -> Unit) {
        permissionsController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.macrionServiceComponentName,
                    isServiceRunning = { accessibilityServiceConnection.isServiceStarted() },
                ),
                PermissionPostNotification(optional = true),
            ),
            onAllGranted = onAllGranted,
        )
    }

    fun startTutorial(item: TutorialCategoryUiItems.Item.Tutorial, resultCode: Int, data: Intent) {
        tutorialRepository.startTutorial(item.type.toTutorialItem().getTutorial(), resultCode, data)
    }

    fun stopTutorial() {
        tutorialRepository.stopTutorial()
    }
}
