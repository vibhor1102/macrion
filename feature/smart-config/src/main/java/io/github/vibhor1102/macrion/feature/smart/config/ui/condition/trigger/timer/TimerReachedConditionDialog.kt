/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.timer

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.TimeUnitDropDownItem
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.timeUnitDropdownItems
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.LocalMonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialTextAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import kotlinx.coroutines.launch

class TimerReachedConditionDialog(private val listener: OnConditionConfigCompleteListener) :
    OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TIMER_REACHED_CONDITION.name
    private val viewModel: TimerReachedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { timerReachedConditionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@TimerReachedConditionDialog.Content() } }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect {
                if (!it) { Log.e(TAG, "Closing TimerReachedConditionDialog because there is no condition edited"); finish() }
            }
        } }
    }

    @Composable private fun Content() {
        CompositionLocalProvider(LocalMonitoredViewsManager provides viewModel.monitoredViewsManager) {
            val initialName by viewModel.name.collectAsStateWithLifecycle(null)
            val displayedDuration by viewModel.duration.collectAsStateWithLifecycle(null)
            val unit by viewModel.selectedUnitItem.collectAsStateWithLifecycle(TimeUnitDropDownItem.Milliseconds)
            val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
            val durationError by viewModel.durationError.collectAsStateWithLifecycle(false)
            val restart by viewModel.restartWhenReached.collectAsStateWithLifecycle(false)
            val saveEnabled by viewModel.conditionCanBeSaved.collectAsStateWithLifecycle(false)
            var name by rememberSaveable { mutableStateOf("") }
            var duration by rememberSaveable { mutableStateOf("") }
            val durationFocusRequester = remember { FocusRequester() }
            LaunchedEffect(initialName) { initialName?.let { name = it } }
            LaunchedEffect(displayedDuration) { displayedDuration?.let { duration = it } }

            Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column {
                    TopBar(saveEnabled)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                            isError = nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            OutlinedTextField(
                                duration,
                                {
                                    val filtered = it.filter(Char::isDigit)
                                    duration = filtered
                                    viewModel.setDuration(filtered.toLongOrNull())
                                },
                                Modifier
                                    .weight(0.7f)
                                    .focusRequester(durationFocusRequester)
                                    .tutorialAnchor(
                                        MonitoredViewType.TIMER_REACHED_CONDITION_FIELD_AFTER,
                                        onClick = durationFocusRequester::requestFocus,
                                    )
                                    .tutorialTextAnchor(
                                        MonitoredViewType.TIMER_REACHED_CONDITION_FIELD_AFTER,
                                        duration,
                                    ),
                                label = { Text(context.getString(R.string.input_field_label_timer_duration_no_unit)) },
                                isError = durationError,
                                singleLine = true,
                                keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
                                keyboardActions = macrionDoneKeyboardActions()
                            )
                            Spacer(Modifier.width(16.dp))
                            TimeUnitDropdown(unit, Modifier.weight(0.3f))
                        }
                        RestartCard(restart)
                    }
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_timer_reached), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledTonalIconButton(onClick = ::delete) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = ::save,
                enabled = saveEnabled,
                modifier = Modifier.tutorialAnchor(
                    MonitoredViewType.TIMER_REACHED_CONDITION_BUTTON_SAVE,
                    onClick = ::save,
                    enabled = saveEnabled,
                ),
            ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun TimeUnitDropdown(selected: TimeUnitDropDownItem, modifier: Modifier) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
            OutlinedTextField(stringResource(selected.title), {},
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), readOnly = true,
                label = { Text(context.getString(R.string.dropdown_label_time_unit)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, singleLine = true)
            ExposedDropdownMenu(expanded, { expanded = false }) {
                timeUnitDropdownItems.forEach { item -> DropdownMenuItem({ Text(stringResource(item.title)) }, {
                    viewModel.setTimeUnit(item); expanded = false
                }) }
            }
        }
    }

    @Composable private fun RestartCard(restart: Boolean) {
        ElevatedCard(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .tutorialAnchor(
                    MonitoredViewType.TIMER_REACHED_CONDITION_FIELD_RESTART,
                    onClick = viewModel::toggleRestartWhenReached,
                )
        ) {
            Row(Modifier.fillMaxWidth().clickable(onClick = viewModel::toggleRestartWhenReached)
                .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(context.getString(R.string.field_timer_restart_title), style = MaterialTheme.typography.titleSmall)
                    Text(context.getString(if (restart) R.string.field_timer_restart_desc_on else R.string.field_timer_restart_desc_off),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(restart, { viewModel.toggleRestartWhenReached() })
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
    private fun save() { listener.onConfirmClicked(); super.back() }
    private fun delete() { listener.onDeleteClicked(); super.back() }
}

private const val TAG = "TimerReachedConditionDialog"
