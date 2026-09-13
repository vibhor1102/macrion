/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.settext

import android.os.Build
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionSwitchField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection.CounterSelectionDialog
import kotlinx.coroutines.launch

class SetTextDialog(
    private val listener: OnActionConfigCompleteListener,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.SET_TEXT.name

    private val viewModel: SetTextViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { setTextViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@SetTextDialog.Content() } }
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
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var textFieldValue by remember {
            mutableStateOf(TextFieldValue(ui.textToWrite, TextRange(ui.textToWrite.length)))
        }
        LaunchedEffect(ui.textToWrite) {
            if (ui.textToWrite != textFieldValue.text) {
                textFieldValue = TextFieldValue(
                    text = ui.textToWrite,
                    selection = TextRange(textFieldValue.selection.end.coerceAtMost(ui.textToWrite.length)),
                )
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
                        value = ui.name,
                        onValueChange = viewModel::setName,
                        label = context.getString(R.string.generic_name),
                        isError = ui.nameError,
                        maxLength = context.resources.getInteger(R.integer.name_max_length),
                    )
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { value ->
                            val limited = value.copy(text = value.text.take(
                                context.resources.getInteger(R.integer.set_text_action_max_length)))
                            textFieldValue = limited
                            viewModel.setTextToWrite(limited.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.field_input_set_text_text_to_write_title)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                showCounterSelectionDialog(textFieldValue.selection.end)
                            }) {
                                Icon(painterResource(R.drawable.ic_append_counter), contentDescription = null)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }),
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Card(Modifier.fillMaxWidth()) {
                            MacrionSwitchField(
                                title = context.getString(R.string.field_set_text_validate_title),
                                description = context.getString(
                                    if (ui.validateInput) R.string.field_set_text_validate_desc_enabled
                                    else R.string.field_set_text_validate_desc_disabled,
                                ),
                                checked = ui.validateInput,
                                onClick = viewModel::toggleValidateInput,
                            )
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            text = context.getString(R.string.message_set_text_focus),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
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
            Text(
                context.getString(R.string.dialog_title_set_text_action),
                Modifier.weight(1f).padding(horizontal = 8.dp),
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
            context.showCloseWithoutSavingDialog {
                listener.onDismissClicked()
                super.back()
            }
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

    private fun showCounterSelectionDialog(atIndex: Int) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = CounterSelectionDialog { selectedCounter ->
                viewModel.appendCounterReferenceToTextToWrite(selectedCounter, atIndex)
            },
            hideCurrent = true,
        )
    }

    private fun onActionEditingStateChanged(isEditingAction: Boolean) {
        if (!isEditingAction) {
            Log.e(TAG, "Closing SetTextDialog because there is no action edited")
            finish()
        }
    }
}

private const val TAG = "SetTextDialog"
