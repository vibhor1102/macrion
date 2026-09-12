/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.notification

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters.newNotificationSettingsStarterOverlay
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection.CounterSelectionDialog
import kotlinx.coroutines.launch

class NotificationDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.NOTIFICATION.name

    private val viewModel: NotificationViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { notificationViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@NotificationDialog.Content() } }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
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
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var message by remember {
            mutableStateOf(TextFieldValue(ui.message, TextRange(ui.message.length)))
        }
        LaunchedEffect(ui.message) {
            if (message.text != ui.message) {
                message = TextFieldValue(ui.message, TextRange(message.selection.end.coerceAtMost(ui.message.length)))
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
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
                        value = ui.name, onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name), isError = ui.nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    Card(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { value ->
                                val limited = value.copy(text = value.text.take(MAX_MESSAGE_LENGTH))
                                message = limited
                                viewModel.setNotificationMessage(limited.text)
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            label = { Text(context.getString(R.string.field_notification_message_text_label)) },
                            trailingIcon = {
                                IconButton(onClick = { showCounterSelectionDialog(message.selection.end) }) {
                                    Icon(painterResource(R.drawable.ic_append_counter), null)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                        )
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ImportanceDropdown(ui.importance, viewModel::setNotificationImportance)
                            HorizontalDivider()
                            Text(context.getString(R.string.message_notification_config),
                                style = MaterialTheme.typography.bodyMedium)
                            if (viewModel.shouldShowSettingsButton()) {
                                Button(onClick = ::showNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                                    Text(context.getString(R.string.button_notification_config))
                                }
                            }
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
            Text(context.getString(R.string.dialog_title_notification),
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
    private fun ImportanceDropdown(
        selected: NotificationImportanceItem,
        onSelected: (NotificationImportanceItem) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(selected.title), onValueChange = {}, readOnly = true,
                label = { Text(context.getString(R.string.field_dropdown_notification_importance_title)) },
                supportingText = { selected.helperText?.let { Text(stringResource(it)) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                notificationImportanceItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Column { Text(stringResource(item.title)); item.helperText?.let {
                            Text(stringResource(it), style = MaterialTheme.typography.bodySmall)
                        } } },
                        onClick = { onSelected(item); expanded = false },
                    )
                }
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

    private fun showCounterSelectionDialog(atIndex: Int) {
        overlayManager.navigateTo(context, CounterSelectionDialog {
            viewModel.appendCounterReference(it, atIndex)
        }, hideCurrent = true)
    }

    private fun showNotificationSettings() {
        if (viewModel.shouldShowSettingsButton()) {
            overlayManager.navigateTo(context, newNotificationSettingsStarterOverlay(), hideCurrent = true)
        }
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) { Log.e(TAG, "Closing NotificationDialog because there is no action edited"); finish() }
    }
}

private const val TAG = "NotificationDialog"
private const val MAX_MESSAGE_LENGTH = 300
