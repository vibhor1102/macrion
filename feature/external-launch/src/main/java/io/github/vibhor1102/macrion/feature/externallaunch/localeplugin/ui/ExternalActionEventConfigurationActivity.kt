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
import io.github.vibhor1102.macrion.core.common.actions.external.ExternalActionEventContract
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.externallaunch.R
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.externalaction.ExternalActionEventConfigurationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExternalActionEventConfigurationActivity : ComponentActivity() {

    private val viewModel: ExternalActionEventConfigurationViewModel by viewModels()
    private var names by mutableStateOf(emptyList<String>())
    private var restoredName: String? = null
    private var selectedName by mutableStateOf<String?>(null)
    private var hasAppliedRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != ExternalActionEventContract.ACTION_EDIT_EVENT) {
            finish()
            return
        }

        restoredName = viewModel
            .decodeConfiguration(ExternalActionEventContract.readConfigurationJson(intent))
            ?.externalActionName

        setContent {
            MacrionTheme {
                ExternalActionEventConfigurationScreen(
                    names = names,
                    selectedName = selectedName,
                    restoredNameIsMissing = selectedName == restoredName &&
                        restoredName != null && restoredName !in viewModel.knownExternalActionNames.value,
                    onNameSelected = { selectedName = it },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSave = ::saveConfiguration,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.knownExternalActionNames.collect(::updateNames)
            }
        }
    }

    private fun updateNames(knownNames: List<String>) {
        if (!hasAppliedRestore) {
            hasAppliedRestore = true
            selectedName = restoredName ?: knownNames.firstOrNull()
        } else if (selectedName == null) {
            selectedName = knownNames.firstOrNull()
        } else if (selectedName != restoredName && selectedName !in knownNames) {
            selectedName = knownNames.firstOrNull()
        }

        val namesForDisplay = buildList {
            if (restoredName != null && restoredName !in knownNames) add(restoredName!!)
            addAll(knownNames)
        }

        names = namesForDisplay
    }

    private fun saveConfiguration() {
        val name = selectedName?.trim()?.takeIf { it.isNotEmpty() } ?: return
        setResult(
            Activity.RESULT_OK,
            ExternalActionEventContract.createConfigurationResult(
                configurationJson = viewModel.encodeConfiguration(name),
                blurb = getString(R.string.external_action_event_blurb, name),
            ),
        )
        finish()
    }
}
