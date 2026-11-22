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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.withContext


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

    internal data class ProjectionGrant(val resultCode: Int, val data: Intent, val scenarioId: Long)

    private val _projectionGrant = MutableStateFlow<ProjectionGrant?>(null)
    internal val projectionGrant = _projectionGrant.asStateFlow()

    fun startPermissionFlowIfNeeded(activity: Context, onAllGranted: () -> Unit, onMandatoryDenied: () -> Unit, resumeExisting: Boolean = false) {
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
            resumeExisting = resumeExisting,
        )
    }

    internal fun storeProjectionGrant(resultCode: Int, data: Intent, scenarioId: Long) {
        _projectionGrant.value = ProjectionGrant(resultCode, data, scenarioId)
    }

    internal suspend fun launchGrantedScenario(grant: ProjectionGrant): Boolean {
        val scenario = withContext(ioDispatcher) { smartRepository.getScenario(grant.scenarioId) }
        if (!_projectionGrant.compareAndSet(grant, null)) return false
        if (scenario != null) qsTileRepository.launchSmartScenario(grant.resultCode, grant.data, scenario)
        return true
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
