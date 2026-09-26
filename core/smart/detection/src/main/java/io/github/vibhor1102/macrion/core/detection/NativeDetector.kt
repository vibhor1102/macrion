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
package io.github.vibhor1102.macrion.core.detection

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.annotation.Keep
import io.github.vibhor1102.macrion.core.base.crash.DetectionCrashContext
import io.github.vibhor1102.macrion.core.base.crash.throwWithContext

/**
 * Native implementation of the image detector.
 * It uses OpenCv template matching algorithms to achieve condition detection on the screen.
 *
 * Debug flavour of the library is build against build artifacts of OpenCv in the debug folder.
 * Release flavour of the library is build against the sources of the OpenCv project, downloaded from GitHub.
 */
class NativeDetector private constructor() : ImageDetector {

    companion object {
        fun newInstance(): ImageDetector? = try {
            System.loadLibrary("smartautoclicker")
            NativeDetector()
        } catch (_: UnsatisfiedLinkError) {
            null
        }
    }

    /** Native pointer of the detector object. */
    @Keep
    private var nativePtr: Long = 0

    private var isClosed: Boolean = false
    private var screenDimensions: Point = Point(0, 0)

    override fun init() {
        if (isClosed || nativePtr != 0L) return
        nativePtr = newDetector()
    }

    override fun close() {
        if (isClosed) return

        isClosed = true
        // Detection may be stopped before its queued initialization runs.
        if (nativePtr != 0L) {
            deleteDetector()
            nativePtr = 0L
        }
    }

    override fun loadTextDetectionModels(detectionModelPath: String, recognitionModels: Map<String, String>): Boolean {
        if (isClosed) return false

        return loadDetectionModels(
            detectionModelPath = detectionModelPath,
            recognitionModelIds = recognitionModels.keys.toTypedArray(),
            recognitionModelsPaths = recognitionModels.values.toTypedArray(),
        )
    }


    override fun setScreenBitmap(screenBitmap: Bitmap, metadata: String) {
        if (isClosed) return

        screenDimensions.x = screenBitmap.width
        screenDimensions.y = screenBitmap.height
        setScreenImage(screenBitmap, metadata)
    }

    override fun detectImage(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectImageNative(
                conditionBitmap,
                conditionWidth,
                conditionHeight,
                detectionArea.left,
                detectionArea.top,
                detectionArea.width(),
                detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithContext(crashContext(DetectionCrashContext.Operation.IMAGE, detectionArea, threshold).copy(
                originalWidth = conditionBitmap.width, originalHeight = conditionBitmap.height,
                scaledWidth = conditionWidth, scaledHeight = conditionHeight,
            ))
        }
    }

    override fun detectColor(conditionColor: Int, detectionArea: Rect, threshold: Int): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectColorNative(
                conditionColor,
                detectionArea.left,
                detectionArea.top,
                detectionArea.width(),
                detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithContext(crashContext(DetectionCrashContext.Operation.COLOR, detectionArea, threshold).copy(color = conditionColor))
        }
    }

    override fun detectText(
        conditionText: String,
        recognitionModelId: String,
        detectionArea: Rect,
        threshold: Int,
    ): DetectionResult {

        if (isClosed) return DetectionResult()

        return try {
            detectTextNative(
                conditionText = conditionText,
                recognitionModelId = recognitionModelId,
                x = detectionArea.left,
                y = detectionArea.top,
                width = detectionArea.width(),
                height = detectionArea.height(),
                threshold
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithContext(crashContext(DetectionCrashContext.Operation.TEXT, detectionArea, threshold).copy(
                textLength = conditionText.length, modelId = recognitionModelId,
            ))
        }
    }

    override fun detectNumber(detectionArea: Rect, threshold: Int, numberFormatType: NumberFormatType): DetectionResult {
        if (isClosed) return DetectionResult()

        return try {
            detectNumberNative(
                x = detectionArea.left,
                y = detectionArea.top,
                width = detectionArea.width(),
                height = detectionArea.height(),
                threshold = threshold,
                numberFormat = numberFormatType.ordinal,
            ).toDetectionResult()
        } catch (ex: Exception) {
            ex.throwWithContext(crashContext(DetectionCrashContext.Operation.NUMBER, detectionArea, threshold).copy(numberFormat = numberFormatType.ordinal))
        }
    }

    private fun crashContext(operation: DetectionCrashContext.Operation, area: Rect, threshold: Int) =
        DetectionCrashContext(operation, screenDimensions.x, screenDimensions.y,
            area.left, area.top, area.width(), area.height(), threshold)

    override fun releaseScreenBitmap(screenBitmap: Bitmap) {
        if (isClosed) return
        releaseScreenImage(screenBitmap)
    }

    /**
     * Creates the detector. Must be called before any other methods.
     * Call [close] to release resources once the detection process is finished.
     *
     * @return the pointer of the native detector object.
     */
    private external fun newDetector(): Long

    /**
     * Deletes the native detector.
     * Once called, this object can't be used anymore.
     */
    private external fun deleteDetector()

    /**
     *
     */
    private external fun loadDetectionModels(
        detectionModelPath: String,
        recognitionModelIds: Array<String>,
        recognitionModelsPaths: Array<String>,
    ): Boolean

    /**
     * Native method for detection setup.
     *
     * @param screenBitmap the content of the screen as a bitmap.
     */
    private external fun setScreenImage(screenBitmap: Bitmap, metricsTag: String)

    /**
     * Native method for detecting if the bitmap is at a specific position in the current screen bitmap.
     *
     * @param conditionBitmap the condition to detect in the screen.
     * @param conditionWidth the expected width of the condition at detection time.
     * @param conditionHeight the expected height of the condition at detection time.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectImageNative(
        conditionBitmap: Bitmap,
        conditionWidth: Int,
        conditionHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if the color is at a specific position in the current screen bitmap.
     *
     * @param conditionColor the condition to detect in the screen.
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectColorNative(
        conditionColor: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if the text is at a specific position in the current screen bitmap.
     *
     * @param conditionText the condition to detect in the screen.
     * @param recognitionModelId the identifier of the recognition model specified with [init].
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectTextNative(
        conditionText: String,
        recognitionModelId: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
    ): DoubleArray?

    /**
     * Native method for detecting if a number is at a specific position in the current screen bitmap.
     *
     * @param x the horizontal position of the condition.
     * @param y the vertical position of the condition.
     * @param width the width of the condition.
     * @param height the height of the condition.
     * @param threshold the allowed error threshold allowed for the condition.
     */
    private external fun detectNumberNative(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        threshold: Int,
        numberFormat: Int,
    ): DoubleArray?

    /** Native method for releasing the screen image resources set with [setScreenImage]. */
    private external fun releaseScreenImage(screenBitmap: Bitmap)
}
