/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain

import android.content.Context
import android.content.Intent
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.IO
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionOverlay
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.dumb.domain.DumbRepository
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalePluginActionExecutor @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
    private val externalLaunchRepository: ExternalLaunchRepository,
) {
    suspend fun resolve(configuration: LocalePluginConfiguration): ResolvedLocalePluginAction? =
        withContext(ioDispatcher) {
            when (configuration.operation) {
                LocalePluginOperation.STOP -> ResolvedLocalePluginAction.Stop
                LocalePluginOperation.RUN_CURRENT -> ResolvedLocalePluginAction.RunCurrent
                LocalePluginOperation.LAUNCH -> {
                    val id = configuration.scenarioId ?: return@withContext null
                    if (configuration.isSmart == true) {
                        smartRepository.getScenario(id)?.let(ResolvedLocalePluginAction::LaunchSmart)
                    } else {
                        dumbRepository.getDumbScenario(id)?.let(ResolvedLocalePluginAction::LaunchDumb)
                    }
                }
            }
        }

    fun areBasePermissionsReady(context: Context): Boolean =
        PermissionOverlay().checkIfGranted(context) && externalLaunchRepository.isAccessibilityServiceStarted()

    fun isScenarioConfigurationOpen(): Boolean = externalLaunchRepository.isScenarioConfigurationOpen()

    fun executeStop() = externalLaunchRepository.stopScenarios()

    fun executeRunCurrent() = externalLaunchRepository.runCurrentScenario()

    fun launchDumb(action: ResolvedLocalePluginAction.LaunchDumb) {
        if (externalLaunchRepository.isDumbScenarioRunning(action.scenario.id.databaseId)) return
        externalLaunchRepository.replaceDumbScenario(action.scenario)
    }

    fun launchSmart(
        resultCode: Int,
        data: Intent,
        action: ResolvedLocalePluginAction.LaunchSmart,
        autoRun: Boolean = false,
    ) =
        externalLaunchRepository.replaceSmartScenario(resultCode, data, action.scenario, autoStart = autoRun)

    fun launchSmartWithCurrentProjection(action: ResolvedLocalePluginAction.LaunchSmart): Boolean {
        if (!externalLaunchRepository.isSmartScreenRecordActive()) return false

        val currentScenarioId = externalLaunchRepository.getSmartScenarioId()
        if (currentScenarioId == action.scenario.id.databaseId &&
            externalLaunchRepository.isAccessibilityServiceStarted()
        ) {
            return true
        }

        externalLaunchRepository.replaceSmartScenarioWithCurrentProjection(action.scenario)
        return true
    }
}

internal sealed interface ResolvedLocalePluginAction {
    data object Stop : ResolvedLocalePluginAction
    data object RunCurrent : ResolvedLocalePluginAction
    data class LaunchSmart(val scenario: Scenario) : ResolvedLocalePluginAction
    data class LaunchDumb(val scenario: DumbScenario) : ResolvedLocalePluginAction

    val scenarioName: String?
        get() = when (this) {
            Stop -> null
            RunCurrent -> null
            is LaunchSmart -> scenario.name
            is LaunchDumb -> scenario.name
        }
}
