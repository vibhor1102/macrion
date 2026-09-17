/*
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
package io.github.vibhor1102.macrion.core.common.actions.screenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.display.recorder.DisplayRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ScreenshotExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val displayRecorder: DisplayRecorder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun captureScreenshot(folderUri: String?, folderName: String?) {
        val screenshot = displayRecorder.takeScreenshot()
        if (screenshot == null) {
            Log.w(TAG, "Cannot capture screenshot: DisplayRecorder returned null frame")
            return
        }

        val snapshot = try {
            screenshot.copy(screenshot.config ?: Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy screenshot frame", e)
            return
        } ?: return

        scope.launch {
            try {
                saveBitmap(snapshot, folderUri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save screenshot", e)
            } finally {
                snapshot.recycle()
            }
        }
    }

    private fun saveBitmap(bitmap: Bitmap, folderUri: String?) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Screenshot_$timeStamp.jpg"

        if (!folderUri.isNullOrBlank()) {
            val savedToCustom = saveToCustomFolder(bitmap, folderUri, fileName)
            if (savedToCustom) return
            Log.w(TAG, "Saving to custom folder failed or was denied, falling back to default Pictures/Macrion")
        }

        saveToDefaultFolder(bitmap, fileName)
    }

    private fun saveToCustomFolder(bitmap: Bitmap, folderUri: String, fileName: String): Boolean {
        return try {
            val treeUri = Uri.parse(folderUri)
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val newDocUri = DocumentsContract.createDocument(
                context.contentResolver,
                parentDocUri,
                "image/jpeg",
                fileName,
            ) ?: return false

            context.contentResolver.openOutputStream(newDocUri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
            } ?: return false
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error saving screenshot to custom folder URI: $folderUri", e)
            false
        }
    }

    private fun saveToDefaultFolder(bitmap: Bitmap, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Macrion")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing screenshot via MediaStore", e)
                    context.contentResolver.delete(uri, null, null)
                }
            } else {
                Log.e(TAG, "Failed to insert screenshot into MediaStore")
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val macrionDir = File(picturesDir, "Macrion")
            if (!macrionDir.exists()) {
                macrionDir.mkdirs()
            }
            val file = File(macrionDir, fileName)
            try {
                FileOutputStream(file).use { os ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving screenshot file on pre-Q device", e)
            }
        }
    }
}

private const val TAG = "ScreenshotExecutor"
