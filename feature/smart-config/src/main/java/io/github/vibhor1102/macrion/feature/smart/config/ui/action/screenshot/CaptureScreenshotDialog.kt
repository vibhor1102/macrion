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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.screenshot

import android.app.Dialog
import android.content.Intent
import androidx.core.net.toUri
import android.provider.DocumentsContract
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters.newPickFolderStarterOverlay
import kotlinx.coroutines.launch

class CaptureScreenshotDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CAPTURE_SCREENSHOT.name

    private val viewModel: CaptureScreenshotViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { captureScreenshotViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@CaptureScreenshotDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingAction.collect(::onActionEditingStateChanged)
            }
        }
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                TopBar(ui.canBeSaved)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacrionTextField(
                        value = ui.name,
                        onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name),
                        isError = ui.nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )

                    LocationCard(
                        folderName = ui.folderName,
                        hasCustomFolder = ui.folderUri != null,
                        canPickFolder = canPickFolder(),
                        onChooseFolder = ::showPickFolderDialog,
                        onResetFolder = viewModel::resetFolderToDefault,
                        onOpenFolder = { openFolderInFileManager(ui.folderUri) },
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.capture_screenshot_storage_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    @Composable
    private fun LocationCard(
        folderName: String?,
        hasCustomFolder: Boolean,
        canPickFolder: Boolean,
        onChooseFolder: () -> Unit,
        onResetFolder: () -> Unit,
        onOpenFolder: () -> Unit,
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.capture_screenshot_location_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = folderName?.ifEmpty { null }
                                ?: stringResource(R.string.capture_screenshot_location_default),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canPickFolder) {
                        FilledTonalButton(
                            onClick = onChooseFolder,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.capture_screenshot_button_choose_folder),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }
                    }

                    if (hasCustomFolder) {
                        TextButton(
                            onClick = onResetFolder,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.capture_screenshot_button_reset_folder),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenFolder,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.capture_screenshot_button_open_folder),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    @Composable
    private fun TopBar(saveEnabled: Boolean) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(
                text = context.getString(R.string.dialog_title_capture_screenshot),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            FilledTonalIconButton(onClick = ::onDeleteButtonClicked) {
                Icon(painterResource(R.drawable.ic_delete), null)
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = ::onSaveButtonClicked, enabled = saveEnabled) {
                Icon(painterResource(R.drawable.ic_save_filled), null)
            }
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }
            return
        }
        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveButtonClicked() {
        listener.onConfirmClicked()
        super.back()
    }

    private fun onDeleteButtonClicked() {
        listener.onDeleteClicked()
        super.back()
    }

    private fun canPickFolder(): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        return intent.resolveActivity(context.packageManager) != null
    }

    private fun showPickFolderDialog() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = newPickFolderStarterOverlay(context),
            hideCurrent = true,
        )
    }

    private fun openFolderInFileManager(folderUri: String?) {
        try {
            val intent = if (!folderUri.isNullOrBlank()) {
                val treeUri = folderUri.toUri()
                val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                val defaultTreeDocUri = "content://com.android.externalstorage.documents/document/primary%3APictures%2FMacrion".toUri()
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(defaultTreeDocUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open folder directly, attempting fallback files intent", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory("android.intent.category.APP_FILES")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Log.w(TAG, "No file manager found", fallbackEx)
                Toast.makeText(context, "No file manager found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing CaptureScreenshotDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "CaptureScreenshotDialog"
