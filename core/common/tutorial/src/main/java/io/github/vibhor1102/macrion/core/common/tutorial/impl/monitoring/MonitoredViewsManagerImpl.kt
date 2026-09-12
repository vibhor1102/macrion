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
package io.github.vibhor1102.macrion.core.common.tutorial.impl.monitoring

import android.graphics.Rect
import android.view.View

import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.IO
import io.github.vibhor1102.macrion.core.common.tutorial.domain.MonitoredViewsManager
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

@Singleton
internal class MonitoredViewsManagerImpl @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val displayConfigManager: DisplayConfigManager,
) : MonitoredViewsManager {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val monitoredViews: MutableMap<MonitoredViewType, ViewMonitor> = mutableMapOf()
    private val monitoredClicks: MutableMap<MonitoredViewType, () -> Unit> = mutableMapOf()
    private val composePositions: MutableMap<MonitoredViewType, MutableStateFlow<Rect>> = mutableMapOf()
    private val composeTexts: MutableMap<MonitoredViewType, MutableStateFlow<String?>> = mutableMapOf()
    private val composeClickHandlers: MutableMap<MonitoredViewType, () -> Unit> = mutableMapOf()

    private var textMonitoringJob: Job? = null
    private var numberMonitoringJob: Job? = null

    private var isViewMonitoringEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    internal fun setViewMonitoringState(isEnabled: Boolean) {
        isViewMonitoringEnabled.update { isEnabled }
    }

    override fun isViewMonitoringEnabled(): StateFlow<Boolean> = isViewMonitoringEnabled

    override fun updatePosition(type: MonitoredViewType, position: Rect) {
        val safeInset = displayConfigManager.displayConfig.safeInsetTopPx
        val adjusted = if (position.isEmpty) position else Rect(
            position.left,
            position.top - safeInset,
            position.right,
            position.bottom - safeInset,
        )
        val flow = composePositions.getOrPut(type) { MutableStateFlow(Rect()) }
        flow.value = adjusted
    }

    override fun setClickHandler(type: MonitoredViewType, onClick: () -> Unit) {
        composeClickHandlers[type] = onClick
    }

    override fun updateText(type: MonitoredViewType, text: String?) {
        val flow = composeTexts.getOrPut(type) { MutableStateFlow(null) }
        flow.value = text
    }

    override fun attach(
        type: MonitoredViewType,
        monitoredView: View,
        positioningType: ViewPositioningType,
    ) {
        if (!isViewMonitoringEnabled.value) return

        val monitor = monitoredViews.getOrPut(type) { ViewMonitor(displayConfigManager) }
        monitor.attachView(monitoredView, positioningType)

        coroutineScopeIo.launch {
            monitor.position.collect { pos ->
                val flow = composePositions.getOrPut(type) { MutableStateFlow(Rect()) }
                flow.value = pos
            }
        }
        coroutineScopeIo.launch {
            monitor.text.collect { txt ->
                updateText(type, txt)
            }
        }
    }

    override fun detach(type: MonitoredViewType) {
        monitoredViews[type]?.detachView()
        composeClickHandlers.remove(type)
        composePositions[type]?.value = Rect()
        composeTexts[type]?.value = null
    }

    override fun notifyClick(type: MonitoredViewType) {
        monitoredClicks[type]?.invoke()
    }

    override fun getViewPosition(type: MonitoredViewType): StateFlow<Rect>? =
        composePositions.getOrPut(type) {
            monitoredViews[type]?.position as? MutableStateFlow<Rect> ?: MutableStateFlow(Rect())
        }

    override fun performClick(type: MonitoredViewType): Boolean {
        notifyClick(type)
        composeClickHandlers[type]?.let { handler ->
            handler.invoke()
            return true
        }
        return monitoredViews[type]?.performClick() ?: false
    }

    fun setExpectedViews(types: Set<MonitoredViewType>) {
        types.forEach { type ->
            if (!composePositions.contains(type)) composePositions[type] = MutableStateFlow(Rect())
            if (!monitoredViews.contains(type)) monitoredViews[type] = ViewMonitor(displayConfigManager)
        }
    }

    fun clearExpectedViews() {
        monitoredViews.clear()
        composePositions.clear()
        composeTexts.clear()
        composeClickHandlers.clear()
    }

    fun monitorNextClick(type: MonitoredViewType, listener: () -> Unit) {
        monitoredClicks[type] = {
            monitoredClicks.remove(type)
            listener()
        }
    }

    fun stopNextClickMonitoring(type: MonitoredViewType) {
        monitoredClicks.remove(type)
    }

    fun monitorText(type: MonitoredViewType, text: String, listener: () -> Unit) {
        textMonitoringJob = coroutineScopeIo.launch {
            val flow = composeTexts.getOrPut(type) { MutableStateFlow(null) }
            flow.collect { viewText ->
                if (text != viewText) return@collect

                monitoredClicks.remove(type)
                listener()

                textMonitoringJob?.cancel()
                textMonitoringJob = null
            }
        }
    }

    fun monitorNumber(type: MonitoredViewType, number: Double, listener: () -> Unit) {
        numberMonitoringJob = coroutineScopeIo.launch {
            val flow = composeTexts.getOrPut(type) { MutableStateFlow(null) }
            flow.collect { viewText ->
                val value = viewText?.toDoubleOrNull()
                if (value != number) return@collect

                monitoredClicks.remove(type)
                listener()

                numberMonitoringJob?.cancel()
                numberMonitoringJob = null
            }
        }
    }

    fun stopMonitoring(type: MonitoredViewType) {
        monitoredClicks.remove(type)

        textMonitoringJob?.cancel()
        textMonitoringJob = null

        numberMonitoringJob?.cancel()
        numberMonitoringJob = null
    }
}