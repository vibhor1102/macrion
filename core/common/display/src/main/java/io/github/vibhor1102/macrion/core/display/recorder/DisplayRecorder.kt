/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.display.recorder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import kotlinx.coroutines.delay

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Record the screen and provide [Image] from it.
 *
 * Uses the [MediaProjection] API to create a [VirtualDisplay] not shown to the user and containing a copy of the
 * user device screen content. An [ImageReader] is attached to this display in order to monitor every new frame
 * displayed on the screen, received in the form of an [Image]. Then, process those Image with ScenarioProcessor
 * according to the current mode (capture/detection). All Image processing code is executed on a background thread
 * (methods annotated with [WorkerThread]), and all results callbacks are executed on the main thread (the thread that
 * has instantiated this class).
 *
 * To start recording, call [startProjection] (see method documentation for permission management). This must be done
 * before any other action on this object. Once the recording isn't necessary anymore, you must stop it by calling
 * [stopProjection] in order to release all resources associated with this object.
 */
@MainThread
@Singleton
class DisplayRecorder @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaProjectionProxy: MediaProjectionProxy,
    private val imageReaderProxy: ImageReaderProxy,
    private val displayConfigManager: DisplayConfigManager,
) {

    /** Synchronization mutex. */
    private val mutex = Mutex()

    /** Virtual display capturing the content of the screen. */
    private var virtualDisplay: VirtualDisplay? = null
    @Volatile private var captureSession: CaptureSession? = null
    @Volatile private var readerGeometry: ReaderGeometry? = null

    private class CaptureSession {
        @Volatile var content = CapturedContent()
    }

    private data class CapturedContent(val size: Point? = null, val isVisible: Boolean = true, val revision: Long = 0)
    private data class ReaderGeometry(val displaySize: Point, val rotation: Int, val contentRevision: Long)

    /**
     * Start the media projection.
     *
     * Initialize all values required for screen recording and start the thread managing the processing. This method
     * should be called before anything else in this class. Once you are done with the screen recording, you should
     * call [stopProjection] in order to release all resources.
     *
     * Recording the screen requires the media projection permission code and its data intent, they both can be
     * retrieved using the results of the activity intent provided by [MediaProjectionManager.createScreenCaptureIntent]
     * (this Intent shows the dialog warning about screen recording privacy). Any attempt to call this method without
     * the correct screen capture intent result will leads to a crash.
     *
     * If the screen record was already started, this method will have no effect.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param stoppedListener listener called when the projection have been stopped unexpectedly.
     */
    suspend fun startProjection(
        resultCode: Int,
        data: Intent,
        stoppedListener: () -> Unit,
        onContentResize: (Point) -> Unit = {},
        onContentVisibleAgain: () -> Unit = {},
    ) = mutex.withLock {
        if (mediaProjectionProxy.isMediaProjectionStarted()) {
            Log.w(TAG, "Attempting to start media projection while already started.")
            return@withLock
        }

        Log.d(TAG, "Start media projection")

        val session = CaptureSession().also { captureSession = it }
        val started = mediaProjectionProxy.startMediaProjection(
            context,
            resultCode,
            data,
            onContentResize = { size ->
                val content = session.content
                if (content.size != size) {
                    Log.d(TAG, "Captured content resized to $size")
                    val revision = if (content.size == null) content.revision else content.revision + 1
                    session.content = content.copy(size = size, revision = revision)
                    if (captureSession === session) onContentResize(size)
                }
            },
            onContentVisibilityChanged = { isVisible ->
                val content = session.content
                if (content.isVisible != isVisible) {
                    session.content = content.copy(isVisible = isVisible, revision = content.revision + 1)
                    if (isVisible && captureSession === session) onContentVisibleAgain()
                }
            },
            stopListener = stoppedListener,
        )
        if (!started) {
            captureSession = null
            Log.e(TAG, "Failed to start media projection")
            stoppedListener()
        }
    }

    /**
     * Start the screen record.
     * This method should not be called from the main thread, but the processing thread.
     *
     * @param displaySize the size of the display, in pixels.
     */
    suspend fun startScreenRecord(displaySize: Point): Unit = mutex.withLock {
        if (!mediaProjectionProxy.isMediaProjectionStarted() || virtualDisplay != null) {
            Log.w(TAG, "Attempting to start screen record while already started.")
            return
        }

        Log.i(TAG, "Start screen record with display size $displaySize")

        val content = captureSession?.content ?: return
        val geometry = ReaderGeometry(displaySize, displayConfigManager.displayConfig.rotation, content.revision)
        imageReaderProxy.resize(displaySize)
        virtualDisplay = mediaProjectionProxy.createVirtualDisplay(
            displaySize = displaySize,
            densityDpi = context.resources.configuration.densityDpi,
            surface = imageReaderProxy.surface,
        )
        if (virtualDisplay != null) readerGeometry = geometry
    }

    suspend fun resizeDisplay(displaySize: Point, sourceDisplaySize: Point = displaySize): Unit = mutex.withLock {
        val vDisplay = virtualDisplay ?: return
        val content = captureSession?.content ?: return
        readerGeometry = null
        val geometry = ReaderGeometry(sourceDisplaySize, displayConfigManager.displayConfig.rotation, content.revision)

        Log.i(TAG, "Resizing virtual display to $displaySize")

        imageReaderProxy.resize(displaySize)
        vDisplay.surface = imageReaderProxy.surface
        vDisplay.resize(
            displaySize.x,
            displaySize.y,
            context.resources.configuration.densityDpi,
        )
        readerGeometry = geometry
    }

    fun isCaptureAreaIncompatible(): Boolean {
        val capturedSize = captureSession?.content?.size ?: return false
        val displaySize = displayConfigManager.displayConfig.sizePx
        // MediaProjection can report the new dimensions before DisplayManager reports the rotation.
        val isRotatedDisplaySize = capturedSize.x == displaySize.y && capturedSize.y == displaySize.x
        return capturedSize != displaySize && !isRotatedDisplaySize
    }

    fun isCaptureGeometryReady(): Boolean {
        val display = displayConfigManager.displayConfig
        val geometry = readerGeometry ?: return false
        val content = captureSession?.content ?: return false
        return content.isVisible && geometry.contentRevision == content.revision &&
            geometry.displaySize == display.sizePx && geometry.rotation == display.rotation &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || content.size == display.sizePx)
    }

    suspend fun validateScreenCapture(): Boolean {
        val startTimeMs = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTimeMs + SCREEN_CAPTURE_VALIDATION_TIMEOUT_MS) {
            try {
                if (acquireLatestBitmap() != null) return true
                delay(SCREEN_CAPTURE_VALIDATION_RETRY_DELAY_MS.milliseconds)
            } catch (captureEx: ScreenCaptureException) {
                Log.e(TAG, "Screen capture is not supported on this device", captureEx)
                return false
            }
        }

        return true
    }

    /** @return the last image of the screen, or null if they have been processed. */
    suspend fun acquireLatestBitmap(): Bitmap? = mutex.withLock { acquireLatestBitmapLocked() }

    private fun acquireLatestBitmapLocked(): Bitmap? {
        if (!isCaptureGeometryReady()) return null
        return imageReaderProxy.getLastFrame()?.takeIf { isCaptureGeometryReady() }
    }

    suspend fun takeScreenshot(): Bitmap? {
        var result: Bitmap?
        val startTimeMs: Long = System.currentTimeMillis()

        do {
            result = mutex.withLock {
                // ImageReaderProxy can overwrite the source bitmap on the next acquisition.
                acquireLatestBitmapLocked()?.let { frame ->
                    frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
                }
            }
            if (result == null) delay(40.milliseconds)

        } while (result == null && System.currentTimeMillis() < (startTimeMs + SCREEN_CAPTURE_SCREENSHOT_TIMEOUT_MS))

        return result
    }

    /**
     * Stop the screen recording.
     * This method should not be called from the main thread, but the processing thread.
     */
    suspend fun stopScreenRecord() = mutex.withLock {
        Log.d(TAG, "Stop screen record")
        readerGeometry = null

        virtualDisplay?.apply {
            release()
            virtualDisplay = null
        }
        imageReaderProxy.close()
    }

    /**
     * Stop the media projection previously started with [startProjection].
     *
     * This method will free/close any resources related to screen recording. If a detection was started, it will be
     * stopped. If the screen record wasn't started, this method will have no effect.
     */
    suspend fun stopProjection() {
        Log.d(TAG, "Stop media projection")

        stopScreenRecord()

        mutex.withLock {
            captureSession = null
            mediaProjectionProxy.stopMediaProjection()
        }
    }
}

/** How long to wait for the first valid frame when validating screen capture support, in milliseconds. */
private const val SCREEN_CAPTURE_SCREENSHOT_TIMEOUT_MS = 2000L
/** How long to wait for the first valid frame when validating screen capture support, in milliseconds. */
private const val SCREEN_CAPTURE_VALIDATION_TIMEOUT_MS = 2000L
/** Polling interval between frame acquisition attempts during screen capture validation, in milliseconds. */
private const val SCREEN_CAPTURE_VALIDATION_RETRY_DELAY_MS = 20L

/** Tag for logs. */
private const val TAG = "DisplayRecorder"
