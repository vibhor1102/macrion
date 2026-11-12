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
package io.github.vibhor1102.macrion.core.dumb.engine

import android.util.Log

import io.github.vibhor1102.macrion.core.base.Dumpable
import io.github.vibhor1102.macrion.core.base.addDumpTabulationLvl
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.IO
import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEngine @Inject constructor(
    private val dumbRepository: IDumbRepository,
    private val dumbActionExecutor: DumbActionExecutor,
    private val settingsRepository: SettingsRepository,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
): Dumpable {

    /** Coroutine scope for the dumb scenario processing. */
    private var processingScope: CoroutineScope? = null
    private var startJob: Job? = null
    private var executionGeneration = 0L
    /** Job for the scenario auto stop. */
    private var timeoutJob: Job? = null
    /** Job for the scenario execution. */
    private var executionJob: Job? = null
    /** Completion listener on dumb actions tries.*/
    private var onTryCompletedListener: (() -> Unit)? = null

    private val dumbScenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val dumbScenario: Flow<DumbScenario?> =
        dumbScenarioDbId.flatMapLatest { dbId ->
            if (dbId == null) flowOf(null)
            else dumbRepository.getDumbScenarioFlow(dbId)
        }

    fun isInitialized(): Boolean = dumbScenarioDbId.value != null

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    @Synchronized
    fun init(dumbScenario: DumbScenario) {
        release()
        dumbActionExecutor.setUnblockWorkaround(settingsRepository.isInputBlockWorkaroundEnabled())
        dumbScenarioDbId.value = dumbScenario.id.databaseId

        processingScope = CoroutineScope(ioDispatcher)
        processingScope?.launch {
            dumbRepository.markAsUsed(dumbScenario.id)
        }
    }

    @Synchronized
    fun startDumbScenario() {
        if (_isRunning.value) return
        val scope = processingScope ?: return
        val dbId = dumbScenarioDbId.value ?: return
        val generation = ++executionGeneration
        // Pending loading is part of playback ownership: Pause must cancel it, too.
        _isRunning.value = true
        startJob = scope.launch(start = CoroutineStart.LAZY) {
            var started = false
            try {
                val scenario = dumbRepository.getDumbScenario(dbId)
                synchronized(this@DumbEngine) {
                    if (generation == executionGeneration && scenario != null) {
                        startJob = null
                        startEngine(scenario, generation)
                        started = true
                    }
                }
            } finally {
                if (!started) stopIfCurrent(generation)
            }
        }
        startJob?.start()
    }

    @Synchronized
    fun tryDumbAction(dumbAction: DumbAction, completionListener: () -> Unit): Boolean {
        if (_isRunning.value || processingScope == null) return false
        Log.i(TAG, "Trying dumb action: $dumbAction")
        onTryCompletedListener = completionListener
        _isRunning.value = true
        startEngine(dumbAction.toDumbScenarioTry(), ++executionGeneration)
        return true
    }

    @Synchronized
    fun stopDumbScenario() {
        ++executionGeneration
        _isRunning.value = false
        startJob?.cancel()
        startJob = null
        timeoutJob?.cancel()
        timeoutJob = null
        executionJob?.cancel()
        executionJob = null
        val completed = onTryCompletedListener
        onTryCompletedListener = null
        completed?.invoke()
    }

    @Synchronized
    fun release() {
        stopDumbScenario()
        dumbScenarioDbId.value = null
        processingScope?.cancel()
        processingScope = null
    }

    /** Called under this engine's monitor; no database or gesture work runs inside it. */
    private fun startEngine(scenario: DumbScenario, generation: Long) {
        if (scenario.dumbActions.isEmpty()) {
            stopDumbScenario()
            return
        }
        Log.d(TAG, "startDumbScenario ${scenario.id} with ${scenario.dumbActions.size} actions")
        if (!scenario.isDurationInfinite) timeoutJob = startTimeoutJob(scenario.maxDurationMin, generation)
        executionJob = startScenarioExecutionJob(scenario, generation)
    }

    @Synchronized
    private fun stopIfCurrent(generation: Long) {
        if (generation == executionGeneration) stopDumbScenario()
    }

    private fun startTimeoutJob(timeoutDurationMinutes: Int, generation: Long): Job? =
        processingScope?.launch {
            Log.d(TAG, "startTimeoutJob: timeoutDurationMinutes=$timeoutDurationMinutes")
            delay(timeoutDurationMinutes.minutes.inWholeMilliseconds)

            stopIfCurrent(generation)
        }

    private fun startScenarioExecutionJob(dumbScenario: DumbScenario, generation: Long): Job? =
        processingScope?.launch {
            dumbScenario.repeat {
                dumbScenario.dumbActions.forEach { dumbAction ->
                    dumbActionExecutor.executeDumbAction(dumbAction, dumbScenario.randomize)
                }

                dumbActionExecutor.onScenarioLoopFinished()
            }

            stopIfCurrent(generation)
        }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DumbEngine:")

            append(contentPrefix)
                .append("- scenarioId=${dumbScenarioDbId.value}; ")
                .append("isRunning=${isRunning.value}; ")
                .println()
        }
    }
}

private const val TAG = "DumbEngine"
