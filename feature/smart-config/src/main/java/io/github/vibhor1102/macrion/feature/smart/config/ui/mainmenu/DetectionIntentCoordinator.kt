/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reconciles every tap with the engine's safe phases, retaining only the latest requested result. */
internal class DetectionIntentCoordinator(
    private val scope: CoroutineScope,
    phases: Flow<DetectionPhase>,
    private val stopSequence: StateFlow<Long>,
    initiallyRunning: Boolean,
    private val start: suspend () -> Boolean,
    private val stop: () -> Unit,
    private val onStarted: () -> Unit,
) {
    private val _requestedRunning = MutableStateFlow(initiallyRunning)
    val requestedRunning: StateFlow<Boolean> = _requestedRunning

    private var phase: DetectionPhase? = null
    private var startJob: Job? = null
    private var startInitiated = false
    private var startReachedEngine = false
    private var startAccepted = false
    private var stopRequested = false
    private var acknowledgedStopSequence = stopSequence.value

    init {
        scope.launch {
            stopSequence.collect { sequence ->
                if (sequence != acknowledgedStopSequence) {
                    acknowledgedStopSequence = sequence
                    stopRequested = phase == DetectionPhase.STARTING || phase == DetectionPhase.DETECTING ||
                        phase == DetectionPhase.STOPPING
                    _requestedRunning.value = false
                    reconcile()
                }
            }
        }
        scope.launch {
            phases.collect { next ->
                val previous = phase
                phase = next

                // Action/event tests and notification controls can start detection without
                // this menu's Play button. Adopt them as soon as setup begins so reconciliation
                // does not immediately cancel their STARTING phase.
                if (next == DetectionPhase.STARTING && !startInitiated && !stopRequested &&
                    !_requestedRunning.value
                ) _requestedRunning.value = true

                // An event end, auto-stop, or projection failure is authoritative. Do not
                // restart it merely because the last button tap had requested Play.
                if (!stopRequested && (next == DetectionPhase.STOPPING ||
                        (next == DetectionPhase.RECORDING &&
                            (previous == DetectionPhase.STARTING || previous == DetectionPhase.DETECTING)))) {
                    _requestedRunning.value = false
                }
                if (next == DetectionPhase.ERROR || next == DetectionPhase.INACTIVE ||
                    next == DetectionPhase.PROJECTION_TRANSITION) {
                    _requestedRunning.value = false
                    startInitiated = false
                    startReachedEngine = false
                    startAccepted = false
                    stopRequested = false
                }
                if (next == DetectionPhase.RECORDING) {
                    startInitiated = false
                    startReachedEngine = false
                    stopRequested = false
                }
                if (next == DetectionPhase.DETECTING) {
                    val startedHere = startAccepted
                    startInitiated = false
                    startReachedEngine = false
                    startAccepted = false
                    if (!startedHere && !stopRequested && !_requestedRunning.value) {
                        // Notification and volume-key Play can start detection outside this menu.
                        _requestedRunning.value = true
                    }
                    if (startedHere && !stopRequested && previous != DetectionPhase.DETECTING &&
                        _requestedRunning.value) onStarted()
                }
                reconcile()
            }
        }
    }

    fun toggle() {
        _requestedRunning.update { !it }
        reconcile()
    }

    fun requestStop(): Boolean {
        if (!_requestedRunning.value && phase != DetectionPhase.STARTING &&
            phase != DetectionPhase.DETECTING) return false
        _requestedRunning.value = false
        reconcile()
        return true
    }

    private fun reconcile() {
        if (!_requestedRunning.value) {
            startJob?.cancel()
            startAccepted = false
            if ((phase == DetectionPhase.STARTING || phase == DetectionPhase.DETECTING) && !stopRequested) {
                stopRequested = true
                stop()
                acknowledgedStopSequence = stopSequence.value
            }
            return
        }

        if (phase != DetectionPhase.RECORDING || startAccepted || startJob != null) return
        startInitiated = true
        startReachedEngine = false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val accepted = try {
                start()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (accepted) {
                startReachedEngine = true
                startAccepted = true
            } else if (phase != DetectionPhase.DETECTING) {
                if (phase == DetectionPhase.RECORDING) startInitiated = false
                _requestedRunning.value = false
            }
        }
        startJob = job
        job.invokeOnCompletion {
            scope.launch {
                if (startJob === job) {
                    startJob = null
                    // A paused start may have been accepted by the engine even if its
                    // STARTING emission has not reached this collector yet.
                    if (phase == DetectionPhase.RECORDING && !startReachedEngine) startInitiated = false
                }
                reconcile()
            }
        }
        job.start()
    }
}
