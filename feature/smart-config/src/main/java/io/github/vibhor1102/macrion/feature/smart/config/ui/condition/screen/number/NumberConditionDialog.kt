/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.number

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialTextAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.allNumberFormatDropdownItems
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.counter.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.areaselector.ConditionAreaSelectorMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image.MAX_THRESHOLD
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection.CounterSelectionDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class NumberConditionDialog(private val listener: OnConditionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.NUMBER_CONDITION.name
    private val viewModel: NumberConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { numberConditionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@NumberConditionDialog.Content() } }
    }
    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect { if (!it) { Log.e(TAG, "Closing NumberConditionDialog because there is no condition edited"); finish() } }
        } }
    }

    @Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var name by rememberSaveable { mutableStateOf(ui.name) }
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(ui.canBeSaved)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                        isError = ui.nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    OperandCard(ui)
                    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(context.getString(R.string.field_number_condition_effect_title), style = MaterialTheme.typography.titleSmall)
                        Text(ui.conditionEffectDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    DetectionCard(ui)
                    ThresholdCard(ui.detectionThreshold)
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_condition_config), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledTonalIconButton(onClick = ::onDeleteClicked) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = ::save,
                enabled = saveEnabled,
                modifier = Modifier.tutorialAnchor(
                    MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    onClick = ::save,
                    enabled = saveEnabled,
                ),
            ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun OperandCard(ui: NumberConditionUiState) {
        var expanded by remember { mutableStateOf(false) }
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .tutorialAnchor(
                            MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN,
                            onClick = { expanded = true },
                        )
                ) {
                    ExposedDropdownMenuBox(expanded, { expanded = it }) {
                        OutlinedTextField(stringResource(ui.selectorOperatorDropdownItem.title), {}, readOnly = true,
                            label = { Text(context.getString(R.string.dropdown_comparison_operator_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            allCounterComparisonOperatorDropdownItems().forEach { item ->
                                val isGreater = item is UiCounterOperatorDropdownItem.Comparison.GreaterItem
                                DropdownMenuItem(
                                    text = { Text(stringResource(item.title)) },
                                    onClick = {
                                        viewModel.setComparisonOperator(item)
                                        expanded = false
                                    },
                                    modifier = if (isGreater) {
                                        Modifier.tutorialAnchor(
                                            MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_ITEM_GREATER,
                                            onClick = {
                                                viewModel.setComparisonOperator(item)
                                                expanded = false
                                            },
                                        )
                                    } else Modifier,
                                )
                            }
                        }
                    }
                }
                OperandTypeButtons(ui.operandValue)
            }
            when (val operand = ui.operandValue) {
                is UiStaticOrCounterSelection.StaticValue -> StaticValueField(operand)
                is UiStaticOrCounterSelection.CounterValue -> CounterField(operand)
            }
        } }
    }

    @Composable private fun OperandTypeButtons(operand: UiStaticOrCounterSelection) {
        val selected = if (operand is UiStaticOrCounterSelection.StaticValue) 0 else 1
        val shape = RoundedCornerShape(20.dp)
        Row(Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
            listOf(UiOperandType.STATIC to R.drawable.ic_numbers, UiOperandType.COUNTER to R.drawable.ic_change_counter)
                .forEachIndexed { index, (type, icon) ->
                    if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                    Box(Modifier.width(44.dp).fillMaxHeight()
                        .background(if (selected == index) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.setOperandType(type) }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(18.dp), tint = if (selected == index)
                            MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                    }
                }
        }
    }

    @Composable private fun StaticValueField(operand: UiStaticOrCounterSelection.StaticValue) {
        var text by rememberSaveable { mutableStateOf(operand.value.toNaturalDisplayString()) }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                newText.toDoubleOrNull()?.let { viewModel.setOperationValue(CounterOperationValue.Number(it)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .tutorialAnchor(
                    MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
                    onClick = { focusRequester.requestFocus() },
                )
                .tutorialTextAnchor(
                    MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
                    text = text,
                ),
            label = { Text(context.getString(R.string.field_counter_operation_value_label)) },
            keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
            keyboardActions = macrionDoneKeyboardActions(),
            singleLine = true,
        )
    }

    @Composable private fun CounterField(operand: UiStaticOrCounterSelection.CounterValue) {
        ElevatedCard(onClick = { showCounterSelectionDialog { viewModel.setOperationValue(CounterOperationValue.Counter(it)) } },
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_change_counter), null, Modifier.size(40.dp))
                Column(Modifier.weight(1f).padding(start = 8.dp, end = 16.dp)) {
                    Text(operand.counter?.counterName ?: context.getString(R.string.field_counter_selection_title_empty), style = MaterialTheme.typography.titleSmall)
                    Text(operand.counter?.let { context.getString(R.string.field_counter_selection_desc,
                        it.defaultValue.toNaturalDisplayString(maxFractionDigits = 2)) } ?: context.getString(R.string.field_counter_selection_desc_empty),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun DetectionCard(ui: NumberConditionUiState) {
        var formatExpanded by remember { mutableStateOf(false) }
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 8.dp)) {
            ExposedDropdownMenuBox(formatExpanded, { formatExpanded = it }, Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(stringResource(ui.numberFormatDropdownItem.title), {}, readOnly = true,
                    label = { Text(context.getString(R.string.field_number_condition_number_format_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(formatExpanded, { formatExpanded = false }) {
                    allNumberFormatDropdownItems().forEach { item -> DropdownMenuItem(text = {
                        Column { Text(stringResource(item.title)); item.helperText?.let { Text(stringResource(it),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }, onClick = { viewModel.setNumberFormat(item); formatExpanded = false }) }
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .tutorialAnchor(
                        MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_AREA_SELECTOR,
                        onClick = ::showDetectionAreaSelector,
                    )
                    .clickable(onClick = ::showDetectionAreaSelector)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(context.getString(R.string.generic_detection_area_title), style = MaterialTheme.typography.titleSmall)
                    Text(ui.detectionAreaDescription, style = MaterialTheme.typography.bodySmall,
                        color = if (ui.detectionAreaError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        } }
    }

    @Composable private fun ThresholdCard(value: Int) {
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(context.getString(R.string.generic_condition_threshold_title), style = MaterialTheme.typography.titleSmall)
                Text("$value%", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { viewModel.setThreshold(it.roundToInt()) },
                valueRange = 0f..MAX_THRESHOLD,
                steps = MAX_THRESHOLD.roundToInt() - 1,
            )
        } }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) { context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }; return }
        listener.onDismissClicked(); super.back()
    }
    private fun save() { listener.onConfirmClicked(); super.back() }
    private fun onDeleteClicked() {
        if (viewModel.isConditionRelatedToClick()) context.showDeleteConditionsWithAssociatedActionsDialog(::onConfirmDelete)
        else onConfirmDelete()
    }
    private fun onConfirmDelete() { listener.onDeleteClicked(); super.back() }
    private fun showCounterSelectionDialog(onSelected: (String) -> Unit) =
        overlayManager.navigateTo(context, CounterSelectionDialog(onSelected), true)
    private fun showDetectionAreaSelector() = overlayManager.navigateTo(context,
        ConditionAreaSelectorMenu(onHelpClicked = { context.getTutorialNavigator().showTipDialog(context, Tip.NUMBER_DETECTION_AREA) },
            onAreaSelected = viewModel::setDetectionArea), true)
}

private const val TAG = "NumberConditionDialog"
