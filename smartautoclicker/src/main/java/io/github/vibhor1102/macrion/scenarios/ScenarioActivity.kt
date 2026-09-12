/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.scenarios

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withResumed
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.github.vibhor1102.macrion.crash.CrashReportPrompt
import io.github.vibhor1102.macrion.crash.crashReportStore
import io.github.vibhor1102.macrion.core.base.crash.CrashDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.scenarios.list.ScenarioListHost
import io.github.vibhor1102.macrion.scenarios.list.ScenarioListViewModel
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState
import io.github.vibhor1102.macrion.core.base.extensions.delayDrawUntil
import io.github.vibhor1102.macrion.core.display.recorder.MediaProjectionRequest
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.ui.errors.createNoMediaProjectionDialog
import io.github.vibhor1102.macrion.feature.revenue.UserConsentState
import io.github.vibhor1102.macrion.scenarios.viewmodel.ScenarioViewModel
import io.github.vibhor1102.macrion.core.common.quality.ui.BackgroundLaunchTroubleshootingDialog
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.domain.LocalePluginLaunchFailureStore
import io.github.vibhor1102.macrion.feature.externallaunch.localeplugin.notification.LocalePluginNotificationController

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point activity for the application.
 * Shown when the user clicks on the launcher icon for the application, this activity will displays the list of
 * available scenarios, if any.
 */
@AndroidEntryPoint
class ScenarioActivity : AppCompatActivity() {

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by viewModels()
    private val scenarioListViewModel: ScenarioListViewModel by viewModels()
    private lateinit var scenarioListHost: ScenarioListHost
    @Inject lateinit var localePluginLaunchFailureStore: LocalePluginLaunchFailureStore
    @Inject lateinit var localePluginNotifications: LocalePluginNotificationController

    /** The result launcher for the projection permission dialog. */
    private val mediaProjectionRequest: MediaProjectionRequest = MediaProjectionRequest()

    /** Scenario clicked by the user. */
    private var requestedItem: ScenarioListUiState.Item.ScenarioItem? = null
    private var startupHelpChecked = false
    private var startupCheckJob: Job? = null
    private var checkingCrashReport = false
    private var crashPromptOffered = false
    private val startupConsentFinished = CompletableDeferred<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        CrashDiagnostics.record(CrashDiagnostics.Event.HOME_OPENED)
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                if (f is DialogFragment) window.decorView.post { offerLocalCrashReport() }
            }
        }, false)
        scenarioListHost = ScenarioListHost(this, scenarioListViewModel, ::launchScenario)
        setContentView(scenarioListHost.createView())
        scenarioListHost.start()

        scenarioViewModel.stopScenario()
        scenarioViewModel.requestUserConsentIfNeeded(this) { startupConsentFinished.complete(Unit) }

        mediaProjectionRequest.registerForActivityResult(this)

        // Splash screen is dismissed on first frame drawn, delay it until we have a user consent status
        findViewById<View>(android.R.id.content).delayDrawUntil {
            scenarioViewModel.userConsentState.value != UserConsentState.UNKNOWN
        }
    }

    override fun onResume() {
        super.onResume()
        scenarioViewModel.refreshPurchaseState()
    }

    override fun onDestroy() {
        scenarioListHost.destroy()
        super.onDestroy()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (startupCheckJob?.isActive == true) return
        startupHelpChecked = false
        startupCheckJob = lifecycleScope.launch {
            startupConsentFinished.await()
            val needsHelp = localePluginLaunchFailureStore.consumePendingDirectLaunchFailure()
            lifecycle.withResumed {
                if (needsHelp) showLocalePluginBackgroundLaunchHelp()
                startupHelpChecked = true
                if (!needsHelp) offerLocalCrashReport()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) offerLocalCrashReport()
    }

    private fun canOfferCrashReport() = startupHelpChecked && !crashPromptOffered &&
        !isFinishing && requestedItem == null && hasWindowFocus() &&
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
        !supportFragmentManager.isStateSaved &&
        supportFragmentManager.fragments.none { it is DialogFragment }

    private fun offerLocalCrashReport() {
        if (checkingCrashReport || !canOfferCrashReport()) return
        checkingCrashReport = true
        lifecycleScope.launch {
            try {
                val store = applicationContext.crashReportStore()
                val report = withContext(Dispatchers.IO) { runCatching { store.pending().firstOrNull { !it.prompted } }.getOrNull() }
                if (report != null && canOfferCrashReport()) {
                    crashPromptOffered = true
                    CrashReportPrompt.newInstance(report.id).showNow(supportFragmentManager, CrashReportPrompt.TAG)
                    withContext(Dispatchers.IO) { runCatching { store.markPrompted(report.id) } }
                }
            } finally { checkingCrashReport = false }
        }
    }

    private fun showLocalePluginBackgroundLaunchHelp() {
        if (supportFragmentManager.findFragmentByTag(BackgroundLaunchTroubleshootingDialog.FRAGMENT_TAG) != null) return
        BackgroundLaunchTroubleshootingDialog.newInstance(
            getString(R.string.dialog_title_locale_plugin_background_launch),
            getString(R.string.message_locale_plugin_background_launch),
            DONT_KILL_MY_APP_URL,
        ).show(supportFragmentManager, BackgroundLaunchTroubleshootingDialog.FRAGMENT_TAG)
    }

    private fun launchScenario(item: ScenarioListUiState.Item.ScenarioItem) {
        requestedItem = item

        scenarioViewModel.startPermissionFlowIfNeeded(
            activity = this,
            onAllGranted = ::onMandatoryPermissionsGranted,
        )
    }

    private fun onMandatoryPermissionsGranted() {
        scenarioViewModel.startTroubleshootingFlowIfNeeded(this) {
            when (val scenario = requestedItem?.scenario) {
                is DumbScenario -> launchDumbScenario(scenario)
                is Scenario -> mediaProjectionRequest.showMediaProjectionWarning(
                    context = this,
                    forceEntireScreen = scenarioViewModel.isEntireScreenCaptureForced(),
                    onSuccess = { resultCode, data -> launchSmartScenario(resultCode, data, scenario) },
                    onFailure = { showProjectionDeniedToast() },
                    onError = { showUnsupportedDeviceDialog() },
                )
            }
        }
    }

    /**
     * Some devices messes up too much with Android.
     * Display a dialog in those cases and stop the application.
     */
    private fun showUnsupportedDeviceDialog() {
        createNoMediaProjectionDialog { finish() }.show()
    }

    private fun launchDumbScenario(scenario: DumbScenario) {
        handleScenarioStartResult(scenarioViewModel.loadDumbScenario(
            context = this,
            scenario = scenario,
        ))
    }

    private fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
        handleScenarioStartResult(scenarioViewModel.loadSmartScenario(
            context = this,
            resultCode = resultCode,
            data = data,
            scenario = scenario,
        ))
    }

    private fun handleScenarioStartResult(result: Boolean) {
        if (result) {
            localePluginNotifications.cancelLaunchFallback()
            finish()
        }
        else Toast.makeText(this, R.string.toast_denied_foreground_permission, Toast.LENGTH_SHORT).show()
    }

    private fun showProjectionDeniedToast() {
        Toast.makeText(this, R.string.toast_denied_screen_sharing_permission, Toast.LENGTH_SHORT).show()
    }
}

private const val DONT_KILL_MY_APP_URL = "https://dontkillmyapp.com/?app=Klick%27r"
