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
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

import io.github.vibhor1102.macrion.core.common.permissions.ui.PermissionsHost
import io.github.vibhor1102.macrion.core.display.recorder.MediaProjectionRequest
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.errors.createNoMediaProjectionDialog
import io.github.vibhor1102.macrion.feature.externallaunch.R

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QSTileLauncherActivity : ComponentActivity() {

    companion object {

        private const val EXTRA_SCENARIO_ID =
            "io.github.vibhor1102.macrion.feature.externallaunch.qstile.ui.EXTRA_SCENARIO_ID"
        private const val EXTRA_IS_SMART_SCENARIO =
            "io.github.vibhor1102.macrion.feature.externallaunch.qstile.ui.EXTRA_IS_SMART_SCENARIO"

        fun getLaunchIntent(context: Context, scenarioId: Long, isSmartScenario: Boolean): Intent =
            Intent(context, QSTileLauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_SCENARIO_ID, scenarioId)
                .putExtra(EXTRA_IS_SMART_SCENARIO, isSmartScenario)
    }

    private val viewModel: QSTileLauncherViewModel by viewModels()

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MacrionTheme {
                PermissionsHost(viewModel.permissionController)
            }
        }

        val scenarioId = intent?.getLongExtra(EXTRA_SCENARIO_ID, -1)
        val isSmartScenario = intent?.getBooleanExtra(EXTRA_IS_SMART_SCENARIO, false)
        if (scenarioId == null || scenarioId == -1L || isSmartScenario == null) {
            Log.e(TAG, "Invalid start parameter, finish activity")
            finish()
            return
        }

        Log.i(TAG, "Start scenario from tile...")
        if (isSmartScenario) onCreateSmartScenarioLauncher(scenarioId)
        else onCreateDumbScenarioLauncher(scenarioId)
    }

    private fun onCreateDumbScenarioLauncher(scenarioId: Long) {
        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = {
                Log.i(TAG, "All permissions are granted, start scenario")
                viewModel.launchDumbScenario(scenarioId)
                finish()
            }
        )
    }

    private fun onCreateSmartScenarioLauncher(scenarioId: Long) {
        mediaProjectionRequest.registerForActivityResult(this)

        viewModel.startPermissionFlowIfNeeded(
            activity = this,
            onMandatoryDenied = ::finish,
            onAllGranted = { showMediaProjectionWarning(scenarioId) },
        )
    }

    /** Show the media projection start warning. */
    private fun showMediaProjectionWarning(scenarioId: Long) {
        Log.i(TAG, "All permissions are granted, request media projection")

        mediaProjectionRequest.showMediaProjectionWarning(
            context = this,
            forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
            onSuccess = { resultCode, data -> launchScenario(resultCode, data, scenarioId) },
            onFailure = { showProjectionDeniedToast() },
            onError = { showUnsupportedDeviceDialog() },
        )
    }

    private fun launchScenario(resultCode: Int, data: Intent, scenarioId: Long) {
        viewModel.launchSmartScenario(resultCode, data, scenarioId)
        finish()
    }

    private fun showProjectionDeniedToast() {
        Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showUnsupportedDeviceDialog() {
        createNoMediaProjectionDialog { finish() }.show()
    }
}

private const val TAG = "QSTileLauncherActivity"
