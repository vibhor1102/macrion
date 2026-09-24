/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.processing.domain.model

import io.github.vibhor1102.macrion.core.processing.data.DetectorState

/** Current engine phase, including setup and cleanup that [DetectionState] intentionally hides. */
enum class DetectionPhase {
    INACTIVE,
    PROJECTION_TRANSITION,
    RECORDING,
    STARTING,
    DETECTING,
    STOPPING,
    ERROR,
}

internal fun DetectorState.toDetectionPhase(): DetectionPhase = when (this) {
    DetectorState.CREATED -> DetectionPhase.INACTIVE
    DetectorState.TRANSITIONING -> DetectionPhase.PROJECTION_TRANSITION
    DetectorState.RECORDING -> DetectionPhase.RECORDING
    DetectorState.STARTING_DETECTION -> DetectionPhase.STARTING
    DetectorState.DETECTING -> DetectionPhase.DETECTING
    DetectorState.STOPPING_DETECTION -> DetectionPhase.STOPPING
    DetectorState.ERROR_NATIVE_DETECTOR_LIB_NOT_FOUND,
    DetectorState.ERROR_OCR_MODEL_NOT_FOUND,
    DetectorState.ERROR_SCREEN_IMAGE_CAPTURE_FAILED -> DetectionPhase.ERROR
}
