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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import javax.inject.Inject

@AndroidEntryPoint
class PickFolderActivity : ComponentActivity() {

    companion object {
        fun getStartIntent(context: Context): Intent =
            Intent(context, PickFolderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    @Inject lateinit var editionRepository: EditionRepository
    @Inject lateinit var overlayManager: OverlayManager

    private val openDocumentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, takeFlags)

                val folderName = getFolderName(treeUri)

                val currentAction = editionRepository.editionState.getEditedAction<CaptureScreenshot>()
                if (currentAction != null) {
                    editionRepository.updateEditedAction(
                        currentAction.copy(
                            screenshotFolderUri = treeUri.toString(),
                            screenshotFolderName = folderName,
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist URI permission or update folder", e)
            }
        }
        finishActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.TRANSPARENT) })
        try {
            openDocumentTreeLauncher.launch(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch OPEN_DOCUMENT_TREE", e)
            finishActivity()
        }
    }

    private fun finishActivity() {
        overlayManager.navigateUp(this)
        finish()
    }

    private fun getFolderName(treeUri: Uri): String {
        return try {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            } ?: treeUri.lastPathSegment ?: "Custom folder"
        } catch (e: Exception) {
            treeUri.lastPathSegment ?: "Custom folder"
        }
    }
}

private const val TAG = "PickFolderActivity"
