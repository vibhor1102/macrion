/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.external

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class ExternalActionDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    private val viewModel: ExternalActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { externalActionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ExternalActionDialog.Content() } }
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
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                TopBar(ui.canBeSaved)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacrionTextField(
                        value = ui.name.orEmpty(), onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name), isError = ui.nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MacrionTextField(
                                value = ui.externalActionName,
                                onValueChange = viewModel::setExternalActionName,
                                label = context.getString(R.string.field_external_action_name_label),
                                isError = ui.externalActionNameError,
                                maxLength = context.resources.getInteger(R.integer.name_max_length),
                            )
                            OutlinedButton(onClick = ::showExternalActionSelectionDialog,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(context.getString(R.string.dialog_title_external_action_selection))
                            }
                            Text(context.getString(R.string.field_external_action_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_external_action),
                Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
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
        listener.onDismissClicked(); super.back()
    }

    private fun onSaveButtonClicked() { listener.onConfirmClicked(); super.back() }
    private fun onDeleteButtonClicked() { listener.onDeleteClicked(); super.back() }

    private fun showExternalActionSelectionDialog() {
        overlayManager.navigateTo(
            context, ExternalActionSelectionDialog(viewModel::setExternalActionName), hideCurrent = true)
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) { Log.e(TAG, "Closing ExternalActionDialog because there is no action edited"); finish() }
    }
}

private const val TAG = "ExternalActionDialog"
