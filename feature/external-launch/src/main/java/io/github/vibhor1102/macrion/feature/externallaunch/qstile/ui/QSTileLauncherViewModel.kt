/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.externallaunch.qstile.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.IO
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.dumb.domain.DumbRepository
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionAccessibilityService
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionOverlay
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class QSTileLauncherViewModel @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val qsTileRepository: ExternalLaunchRepository,
    val permissionController: PermissionsController,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
    private val settingsRepository: SettingsRepository,
    private val appComponentsProvider: AppComponentsProvider,
) : ViewModel() {


    fun startPermissionFlowIfNeeded(activity: Context, onAllGranted: () -> Unit, onMandatoryDenied: () -> Unit) {
        permissionController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.macrionServiceComponentName,
                    isServiceRunning = { qsTileRepository.isAccessibilityServiceStarted() },
                ),
                PermissionPostNotification(optional = true),
            ),
            onAllGranted = onAllGranted,
            onMandatoryDenied = onMandatoryDenied,
        )
    }

    fun launchSmartScenario(resultCode: Int, data: Intent, scenarioId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val scenario = smartRepository.getScenario(scenarioId) ?: return@launch
            qsTileRepository.launchSmartScenario(resultCode, data, scenario)
        }
    }

    fun launchDumbScenario(scenarioId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val scenario = dumbRepository.getDumbScenario(scenarioId) ?: return@launch
            qsTileRepository.launchDumbScenario(scenario)
        }
    }

    fun isEntireScreenCaptureForced(): Boolean =
        settingsRepository.isEntireScreenCaptureForced()
}
