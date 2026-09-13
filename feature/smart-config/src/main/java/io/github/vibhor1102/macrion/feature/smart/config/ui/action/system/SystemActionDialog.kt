/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.system

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class SystemActionDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SYSTEM_ACTION.name

    private val viewModel: SystemActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { systemActionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@SystemActionDialog.Content() } }
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
        val initialName by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val selectedType by viewModel.typeItem.collectAsStateWithLifecycle(SystemActionTypeItem.Back)
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
        val saveEnabled by viewModel.isValidAction.collectAsStateWithLifecycle(false)
        var name by remember { mutableStateOf("") }
        LaunchedEffect(initialName) { initialName?.let { name = it } }

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                TopBar(saveEnabled)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacrionTextField(
                        value = name,
                        onValueChange = { name = it; viewModel.setName(it) },
                        label = context.getString(R.string.generic_name),
                        isError = nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    TypeDropdown(selectedType, viewModel::setType)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    private fun TopBar(saveEnabled: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_system_action),
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TypeDropdown(selected: SystemActionTypeItem, onSelected: (SystemActionTypeItem) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(selected.title), onValueChange = {}, readOnly = true,
                label = { Text(context.getString(R.string.field_dropdown_system_action_type_title)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                systemActionTypeItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.title)) },
                        onClick = { onSelected(item); expanded = false },
                    )
                }
            }
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
            return
        }
        listener.onDismissClicked()
        super.back()
    }

    private fun onSaveButtonClicked() { listener.onConfirmClicked(); super.back() }
    private fun onDeleteButtonClicked() { listener.onDeleteClicked(); super.back() }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing SystemActionDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "SystemActionDialog"
