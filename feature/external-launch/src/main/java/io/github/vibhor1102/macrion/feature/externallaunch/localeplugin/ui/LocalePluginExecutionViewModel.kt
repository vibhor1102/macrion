/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionAccessibilityService
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionOverlay
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchRepository
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginActionExecutor
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationCodec
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.ResolvedLocalePluginAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LocalePluginExecutionViewModel @Inject constructor(
    private val codec: LocalePluginConfigurationCodec,
    private val executor: LocalePluginActionExecutor,
    val permissionController: PermissionsController,
    private val externalLaunchRepository: ExternalLaunchRepository,
    private val appComponentsProvider: AppComponentsProvider,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun resolve(configurationJson: String?, onResult: (ResolvedLocalePluginAction?) -> Unit) {
        val configuration = codec.decode(configurationJson)
        if (configuration == null) {
            onResult(null)
            return
        }
        viewModelScope.launch { onResult(executor.resolve(configuration)) }
    }

    fun requestPermissions(
        activity: Context,
        onAllGranted: () -> Unit,
        onMandatoryDenied: () -> Unit,
    ) {
        permissionController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.macrionServiceComponentName,
                    isServiceRunning = { externalLaunchRepository.isAccessibilityServiceStarted() },
                ),
            ),
            onAllGranted = onAllGranted,
            onMandatoryDenied = onMandatoryDenied,
        )
    }

    fun launchDumb(action: ResolvedLocalePluginAction.LaunchDumb) = executor.launchDumb(action)

    fun launchSmart(resultCode: Int, data: Intent, action: ResolvedLocalePluginAction.LaunchSmart) =
        executor.launchSmart(resultCode, data, action)

    fun executeStop() = executor.executeStop()

    fun executeRunCurrent() = executor.executeRunCurrent()

    fun isEntireScreenCaptureForced(): Boolean = settingsRepository.isEntireScreenCaptureForced()
}
