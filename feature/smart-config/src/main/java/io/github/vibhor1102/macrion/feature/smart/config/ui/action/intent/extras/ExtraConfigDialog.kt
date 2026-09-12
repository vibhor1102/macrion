/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.intent.extras

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.DropdownItem
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import kotlinx.coroutines.launch

class ExtraConfigDialog(
    private val onConfigComplete: () -> Unit,
    private val onDeleteClicked: () -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.EXTRA_CONFIG.name

    private val viewModel: ExtraConfigModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { extraConfigViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ExtraConfigDialog.Content() } }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingExtra.collect(::onExtraEditingStateChanged)
            }
        }
    }

    @Composable
    private fun Content() {
        val key by viewModel.key.collectAsStateWithLifecycle(initialValue = null)
        val keyError by viewModel.keyError.collectAsStateWithLifecycle(initialValue = false)
        val valueState by viewModel.valueInputState.collectAsStateWithLifecycle(initialValue = null)
        val valueError by viewModel.valueError.collectAsStateWithLifecycle(initialValue = false)
        val canSave by viewModel.isExtraValid.collectAsStateWithLifecycle(initialValue = false)

        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column {
                TopBar(canSave)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MacrionTextField(
                        value = key.orEmpty(),
                        onValueChange = viewModel::setKey,
                        label = context.getString(R.string.field_intent_extra_key_label),
                        isError = keyError,
                    )
                    valueState?.let { state ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = context.getString(R.string.field_intent_extra_value_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                DropdownField(
                                    label = context.getString(R.string.dropdown_intent_extra_type_title),
                                    selected = state.typeItem,
                                    items = viewModel.extraTypeDropdownItems,
                                    onSelected = viewModel::setType,
                                )
                                when (state) {
                                    is ExtraValueInputState.BooleanInputTypeSelected -> DropdownField(
                                        label = context.getString(R.string.field_intent_extra_value_title),
                                        selected = state.value,
                                        items = viewModel.booleanItems,
                                        onSelected = viewModel::setBooleanValue,
                                    )
                                    is ExtraValueInputState.TextInputTypeSelected -> ValueField(state, valueError)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::onDismissButtonClicked) {
                Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
            }
            Text(
                text = context.getString(R.string.dialog_intent_extra_title),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            FilledTonalIconButton(onClick = ::onDeleteButtonClicked) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = ::onSaveButtonClicked, enabled = saveEnabled) {
                Icon(painterResource(R.drawable.ic_save_filled), contentDescription = null)
            }
        }
    }

    @Composable
    private fun ValueField(state: ExtraValueInputState.TextInputTypeSelected, isError: Boolean) {
        var text by remember(state.value::class) { mutableStateOf(state.valueStr) }
        OutlinedTextField(
            value = text,
            onValueChange = { proposed ->
                val accepted = acceptedExtraValue(state.value, proposed)
                if (accepted != null) {
                    text = accepted
                    viewModel.setValue(accepted)
                }
            },
            label = { Text(context.getString(R.string.field_intent_extra_value_title)) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            singleLine = true,
            keyboardOptions = macrionDoneKeyboardOptions(keyboardType(state.value)),
            keyboardActions = macrionDoneKeyboardActions(),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DropdownField(
        label: String,
        selected: DropdownItem,
        items: List<DropdownItem>,
        onSelected: (DropdownItem) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(selected.title),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(stringResource(item.title)) },
                        onClick = { onSelected(item); expanded = false },
                    )
                }
            }
        }
    }

    private fun onDismissButtonClicked() { onDismissClicked(); back() }
    private fun onSaveButtonClicked() { onConfigComplete(); back() }
    private fun onDeleteButtonClicked() { onDeleteClicked(); back() }

    private fun onExtraEditingStateChanged(isEditingExtra: Boolean) {
        if (!isEditingExtra) {
            Log.e(TAG, "Closing ExtraConfigDialog because there is no intent extra edited")
            finish()
        }
    }
}

internal fun acceptedExtraValue(currentValue: Any, proposed: String): String? = when (currentValue) {
    is Char -> proposed.take(1)
    is String -> proposed
    is Byte -> proposed.takeIf { it.isTransientNumber() || it.toLongOrNull()?.let { value -> value in Byte.MIN_VALUE..Byte.MAX_VALUE } == true }
    is Short -> proposed.takeIf { it.isTransientNumber() || it.toLongOrNull()?.let { value -> value in Short.MIN_VALUE..Short.MAX_VALUE } == true }
    is Int -> proposed.takeIf { it.isTransientNumber() || it.toLongOrNull()?.let { value -> value in Int.MIN_VALUE..Int.MAX_VALUE } == true }
    is Float -> proposed.takeIf { it.isTransientNumber() || it.toDoubleOrNull()?.let { value -> value in -Float.MAX_VALUE..Float.MAX_VALUE } == true }
    is Double -> proposed.takeIf { it.isTransientNumber() || it.toDoubleOrNull()?.let { value -> value in -Double.MAX_VALUE..Double.MAX_VALUE } == true }
    else -> null
}

private fun String.isTransientNumber(): Boolean = isEmpty() || this == "-"

private fun keyboardType(value: Any): KeyboardType = when (value) {
    is Float, is Double -> KeyboardType.Decimal
    is Byte, is Short, is Int -> KeyboardType.Number
    else -> KeyboardType.Text
}

private const val TAG = "ExtraConfigDialog"
