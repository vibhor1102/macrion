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
package io.github.vibhor1102.macrion

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

import io.github.vibhor1102.macrion.core.base.Dumpable
import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.core.base.extensions.requestFilterKeyEvents
import io.github.vibhor1102.macrion.core.base.extensions.startForegroundMediaProjectionServiceCompat
import io.github.vibhor1102.macrion.core.base.notifications.NotificationIds
import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import io.github.vibhor1102.macrion.core.common.actions.AndroidActionExecutor
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.common.quality.domain.QualityMetricsMonitor
import io.github.vibhor1102.macrion.core.common.quality.domain.QualityRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.dumb.engine.DumbEngine
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.core.smart.debugging.domain.DebuggingRepository
import io.github.vibhor1102.macrion.feature.revenue.IRevenueRepository
import io.github.vibhor1102.macrion.feature.review.ReviewRepository
import io.github.vibhor1102.macrion.localservice.LocalService
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchActionHandler
import io.github.vibhor1102.macrion.feature.externallaunch.domain.ExternalLaunchRepository

import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.PrintWriter
import javax.inject.Inject

/**
 * AccessibilityService implementation for the SmartAutoClicker.
 *
 * Started automatically by Android once the user has defined this service has an accessibility service, it provides
 * an API to start and stop the DetectorEngine correctly in order to display the overlay UI and record the screen for
 * clicks detection.
 * This API is offered through the [LocalService] class, which is instantiated in the
 * [LocalAccessibilityServiceConnection] object. This system is used instead of the usual binder interface because
 * an [AccessibilityService] already has its own binder, and it can't be changed. To access this local service,
 * use [LocalAccessibilityServiceConnection].
 *
 * We need this service to be an accessibility service in order to inject the detected event on the currently
 * displayed activity. This injection is made by the [dispatchGesture] method, which is called everytime an event has
 * been detected.
 */
@AndroidEntryPoint
class SmartAutoClickerService : AccessibilityService() {

    @Inject lateinit var localServiceConnection: LocalAccessibilityServiceConnection
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var displayConfigManager: DisplayConfigManager
    @Inject lateinit var smartProcessingRepository: SmartProcessingRepository
    @Inject lateinit var smartRepository: IRepository
    @Inject lateinit var dumbEngine: DumbEngine
    @Inject lateinit var bitmapManager: BitmapRepository
    @Inject lateinit var qualityRepository: QualityRepository
    @Inject lateinit var qualityMetricsMonitor: QualityMetricsMonitor
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var revenueRepository: IRevenueRepository
    @Inject lateinit var externalLaunchRepository: ExternalLaunchRepository
    @Inject lateinit var reviewRepository: ReviewRepository
    @Inject lateinit var appComponentsProvider: AppComponentsProvider
    @Inject lateinit var actionExecutor: AndroidActionExecutor
    @Inject lateinit var debuggingRepository: DebuggingRepository
    @Inject lateinit var tutorialRepository: TutorialRepository

    override fun onServiceConnected() {
        super.onServiceConnected()

        // A previous accessibility-service instance can disappear while the application process (and therefore its
        // singleton engines) survives. Never publish a fresh LocalService on top of that stale session.
        if (localServiceConnection.getLocalService() == null &&
            (!overlayManager.isEmpty() || smartProcessingRepository.getScenarioId() != null ||
                !smartProcessingRepository.isFullyStopped() || dumbEngine.isInitialized())
        ) {
            Log.w(TAG, "Resetting stale scenario state after accessibility service reconnect")
            dumbEngine.release()
            overlayManager.closeAll(this)
            smartProcessingRepository.stopScreenRecord()
            onAccessibilityServiceLost()
        }

        qualityMetricsMonitor.onServiceConnected()
        actionExecutor.init(this)

        externalLaunchRepository.setActionHandler(
            object : ExternalLaunchActionHandler {
                override fun isRunning(): Boolean = localServiceConnection.isServiceStarted()
                override fun isScenarioRunning(): Boolean =
                    localServiceConnection.getLocalService()?.isScenarioRunning() ?: false
                override fun isOverlayVisible(): Boolean = overlayManager.isOverlayStackVisible()
                override fun isOverlayHidden(): Boolean = overlayManager.isOverlayStackHidden()
                override fun isScenarioConfigurationOpen(): Boolean =
                    overlayManager.hasOverlayAboveRoot()
                override fun isSmartScreenRecordActive(): Boolean =
                    localServiceConnection.getLocalService()?.isSmartScreenRecordActive() ?: false
                override fun getSmartScenarioId(): Long? =
                    localServiceConnection.getLocalService()?.getSmartScenarioId()
                override fun getDumbScenarioId(): Long? =
                    localServiceConnection.getLocalService()?.getDumbScenarioId()
                override fun launchDumbScenario(dumbScenario: DumbScenario) {
                    localServiceConnection.getLocalService()?.launchDumbScenario(dumbScenario)
                }
                override fun launchSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
                    localServiceConnection.getLocalService()?.launchSmartScenario(resultCode, data, scenario)
                }
                override fun replaceDumbScenario(dumbScenario: DumbScenario) {
                    localServiceConnection.getLocalService()?.replaceDumbScenario(dumbScenario)
                }
                override fun replaceSmartScenario(resultCode: Int, data: Intent, scenario: Scenario) {
                    localServiceConnection.getLocalService()?.replaceSmartScenario(resultCode, data, scenario)
                }
                override fun replaceSmartScenarioWithCurrentProjection(scenario: Scenario) {
                    localServiceConnection.getLocalService()?.replaceSmartScenarioWithCurrentProjection(scenario)
                }
                override fun runCurrentScenario() {
                    localServiceConnection.getLocalService()?.runCurrentScenario()
                }
                override fun stop() {
                    localServiceConnection.getLocalService()?.stopScenario()
                }
            }
        )

        localServiceConnection.onAccessibilityServiceStarted(
            LocalService(
                context = this,
                overlayManager = overlayManager,
                appComponentsProvider = appComponentsProvider,
                smartProcessingRepository = smartProcessingRepository,
                smartRepository = smartRepository,
                dumbEngine = dumbEngine,
                revenueRepository = revenueRepository,
                settingsRepository = settingsRepository,
                debuggingRepository = debuggingRepository,
                tutorialRepository = tutorialRepository,
                onStart = ::onLocalServiceStarted,
                onScenarioChanged = ::onLocalScenarioChanged,
                onScenarioStateChanged = externalLaunchRepository::notifyScenarioStateChanged,
                onStop = ::onLocalServiceStopped,
                onAccessibilityLoss = ::onAccessibilityServiceLost,
            )
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        localServiceConnection.getLocalService()?.shutdownForAccessibilityLoss()
        localServiceConnection.onAccessibilityServiceStopped()
        externalLaunchRepository.notifyScenarioStateChanged()

        qualityMetricsMonitor.onServiceUnbind()
        actionExecutor.clear()
        return super.onUnbind(intent)
    }

    private fun onLocalServiceStarted(scenarioId: Long, isSmart: Boolean, serviceNotification: Notification?) {
        reviewRepository.onUserSessionStarted()
        qualityMetricsMonitor.onServiceForegroundStart()

        serviceNotification?.let {
            startForegroundMediaProjectionServiceCompat(NotificationIds.FOREGROUND_SERVICE_NOTIFICATION_ID, it)
        }
        requestFilterKeyEvents(true)

        displayConfigManager.startMonitoring(this)
        externalLaunchRepository.setTileScenario(scenarioId = scenarioId, isSmart = isSmart)
    }

    private fun onLocalServiceStopped() {
        qualityMetricsMonitor.onServiceForegroundEnd()
        reviewRepository.onUserSessionStopped()
        actionExecutor.resetState()

        if (reviewRepository.isUserCandidateForReview()) {
            Log.i(TAG, "User is candidate for review")

            reviewRepository.getReviewActivityIntent(this)?.let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }

        requestFilterKeyEvents(false)
        stopForeground(STOP_FOREGROUND_REMOVE)

        displayConfigManager.stopMonitoring()
        bitmapManager.clearCache()
    }

    /** Release service-owned resources without treating an abnormal accessibility loss as a completed user session. */
    private fun onAccessibilityServiceLost() {
        qualityMetricsMonitor.onServiceForegroundEnd()
        actionExecutor.resetState()
        requestFilterKeyEvents(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        displayConfigManager.stopMonitoring()
        bitmapManager.clearCache()
    }

    private fun onLocalScenarioChanged(scenarioId: Long, isSmart: Boolean) {
        externalLaunchRepository.setTileScenario(scenarioId = scenarioId, isSmart = isSmart)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean =
        (localServiceConnection.getLocalService() as? LocalService)?.onKeyEvent(event) ?: super.onKeyEvent(event)

    /**
     * Dump the state of the service via adb.
     * adb shell "dumpsys activity service io.github.vibhor1102.macrion"
     */
    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        if (writer == null) return

        writer.append("* SmartAutoClickerService:").println()
        writer.append(Dumpable.DUMP_DISPLAY_TAB)
            .append("- isStarted=").append("${localServiceConnection.isServiceStarted()}; ")
            .println()

        displayConfigManager.dump(writer)
        bitmapManager.dump(writer)
        overlayManager.dump(writer)
        smartProcessingRepository.dump(writer)
        dumbEngine.dump(writer)
        actionExecutor.dump(writer)
        qualityRepository.dump(writer)

        revenueRepository.dump(writer)
        reviewRepository.dump(writer)
    }

    override fun onInterrupt() { /* Unused */ }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Unused */ }
}

/** Tag for the logs. */
private const val TAG = "SmartAutoClickerService"
