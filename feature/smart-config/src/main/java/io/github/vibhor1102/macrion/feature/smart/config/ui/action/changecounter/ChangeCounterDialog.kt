/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.changecounter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
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
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection.CounterSelectionDialog
import kotlinx.coroutines.launch

class ChangeCounterDialog(private val listener: OnActionConfigCompleteListener) :
    OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CHANGE_COUNTER.name
    private val viewModel: ChangeCounterViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { changeCounterViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ChangeCounterDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingAction.collect { if (!it) { Log.e(TAG, "Closing dialog because there is no action edited"); finish() } }
        } }
    }

    @Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(ui.canBeSaved)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(ui.name.orEmpty(), viewModel::setName, context.getString(R.string.generic_name),
                        isError = ui.nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    CounterField(ui.counter, ::selectCounterToChange, tutorialMonitored = true)
                    OperandField(ui)
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(context.getString(R.string.field_change_counter_effect_title), style = MaterialTheme.typography.titleSmall)
                                Text(ui.actionEffectText, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_change_counter), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledTonalIconButton(onClick = ::delete) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = ::save,
                enabled = saveEnabled,
                modifier = Modifier.tutorialAnchor(
                    MonitoredViewType.COUNTER_ACTION_DIALOG_BUTTON_SAVE,
                    onClick = ::save,
                    enabled = saveEnabled,
                ),
            ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @Composable private fun CounterField(
        value: UiStaticOrCounterSelection.CounterValue,
        onClick: () -> Unit,
        tutorialMonitored: Boolean = false,
    ) {
        val counter = value.counter
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().then(
                if (tutorialMonitored) {
                    Modifier.tutorialAnchor(
                        MonitoredViewType.COUNTER_ACTION_DIALOG_FIELD_SELECT_COUNTER,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (counter == null) Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                else Icon(painterResource(R.drawable.ic_change_counter), null, Modifier.size(40.dp))
                Column(Modifier.weight(1f).padding(start = 8.dp, end = 16.dp)) {
                    Text(counter?.counterName ?: context.getString(R.string.field_counter_selection_title_empty),
                        style = MaterialTheme.typography.titleSmall)
                    Text(counter?.let { context.getString(R.string.field_counter_selection_desc,
                        it.defaultValue.toNaturalDisplayString(maxFractionDigits = 2)) }
                        ?: context.getString(R.string.field_counter_selection_desc_empty), style = MaterialTheme.typography.bodySmall,
                        color = if (counter == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun OperandField(ui: ChangeCounterUiState) {
        var expanded by remember { mutableStateOf(false) }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.weight(1f)) {
                        OutlinedTextField(stringResource(ui.operator.title), {}, readOnly = true,
                            label = { Text(context.getString(R.string.dropdown_comparison_operator_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            allCounterAffectationOperatorDropdownItems().forEach { item -> DropdownMenuItem(
                                text = { Text(stringResource(item.title)) }, onClick = { viewModel.setOperationItem(item); expanded = false }) }
                        }
                    }
                    SingleChoiceSegmentedButtonRow {
                        listOf(UiOperandType.STATIC to R.drawable.ic_numbers, UiOperandType.COUNTER to R.drawable.ic_change_counter)
                            .forEachIndexed { index, pair -> SegmentedButton(
                                selected = (ui.operandValue is UiStaticOrCounterSelection.StaticValue) == (pair.first == UiOperandType.STATIC),
                                onClick = { viewModel.setOperandType(pair.first) }, shape = SegmentedButtonDefaults.itemShape(index, 2),
                                icon = {}, label = { Icon(painterResource(pair.second), null, Modifier.size(20.dp)) }) }
                    }
                }
                when (val operand = ui.operandValue) {
                    is UiStaticOrCounterSelection.StaticValue -> {
                        var value by remember { mutableStateOf(operand.value.toNaturalDisplayString()) }
                        OutlinedTextField(
                        value = value,
                        onValueChange = { text -> value = text; text.toDoubleOrNull()?.let { number -> viewModel.setOperationValue(CounterOperationValue.Number(number)) } },
                        modifier = Modifier.fillMaxWidth(), label = { Text(context.getString(R.string.field_counter_operation_value_label)) },
                        keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
                        keyboardActions = macrionDoneKeyboardActions(), singleLine = true)
                    }
                    is UiStaticOrCounterSelection.CounterValue -> CounterField(operand, onClick = { showCounterSelectionDialog {
                        viewModel.setOperationValue(CounterOperationValue.Counter(it)) } })
                }
            }
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }; return
        }
        listener.onDismissClicked(); super.back()
    }
    private fun save() { listener.onConfirmClicked(); super.back() }
    private fun delete() { listener.onDeleteClicked(); super.back() }
    private fun selectCounterToChange() = showCounterSelectionDialog(viewModel::setCounterName)
    private fun showCounterSelectionDialog(onSelected: (String) -> Unit) =
        overlayManager.navigateTo(context, CounterSelectionDialog(onSelected), true)
}

private const val TAG = "ChangeCounterDialog"
