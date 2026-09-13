/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.common.permissions.ui.PermissionsHost
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.externallaunch.R
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginConfiguration
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginContract
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginOperation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalePluginConfigurationActivity : ComponentActivity() {

    private val viewModel: LocalePluginConfigurationViewModel by viewModels()
    private var scenarios by mutableStateOf(emptyList<LocalePluginScenarioItem>())
    private var selectedScenario by mutableStateOf<LocalePluginScenarioItem?>(null)
    private var restoredConfiguration: LocalePluginConfiguration? = null
    private var hasAppliedRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != LocalePluginContract.ACTION_EDIT_SETTING) {
            finish()
            return
        }

        restoredConfiguration = viewModel.decodeConfiguration(LocalePluginContract.readConfigurationJson(intent))
        setContent {
            MacrionTheme {
                LocalePluginConfigurationScreen(
                    scenarios = scenarios,
                    selectedScenario = selectedScenario,
                    restoredScenarioWasDeleted = restoredConfiguration?.operation == LocalePluginOperation.LAUNCH &&
                        restoredConfiguration?.scenarioId != null && selectedScenario == null,
                    onScenarioSelected = { selectedScenario = it },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSave = {
                        viewModel.requestFallbackNotificationPermission(this, ::saveConfiguration)
                    },
                )
                PermissionsHost(viewModel.permissionController)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scenarios.collect(::updateScenarios)
            }
        }
    }

    private fun updateScenarios(scenarios: List<LocalePluginScenarioItem>) {
        this.scenarios = scenarios
        if (!hasAppliedRestore) {
            hasAppliedRestore = true
            restoredConfiguration?.let { restored ->
                selectedScenario = scenarios.find {
                    it.id == restored.scenarioId && it.isSmart == restored.isSmart
                }
            }
            if (restoredConfiguration == null) selectedScenario = scenarios.firstOrNull()
        } else {
            selectedScenario = selectedScenario?.let { selected ->
                scenarios.find { it.id == selected.id && it.isSmart == selected.isSmart }
            }
        }
    }

    private fun saveConfiguration() {
        if (selectedScenario != null) finishSavingConfiguration()
    }

    private fun finishSavingConfiguration() {
        val scenario = selectedScenario ?: return
        val configuration = LocalePluginConfiguration(
            operation = LocalePluginOperation.LAUNCH,
            scenarioId = scenario.id,
            isSmart = scenario.isSmart,
        )
        val blurb = getString(
            R.string.locale_plugin_blurb_launch,
            scenario.name,
            getString(
                if (scenario.isSmart) R.string.locale_plugin_type_smart else R.string.locale_plugin_type_dumb
            ),
        )
        setResult(
            Activity.RESULT_OK,
            LocalePluginContract.createResult(viewModel.encodeConfiguration(configuration), blurb),
        )
        finish()
    }
}
