/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger

import android.view.ViewGroup
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiTriggerCondition
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.broadcast.BroadcastReceivedConditionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.counter.CounterReachedConditionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.selection.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.timer.TimerReachedConditionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.condition.ConditionCopyDialog

class TriggerConditionListDialog : OverlayDialog(R.style.ScenarioConfigTheme) {
    private val viewModel: TriggerConditionListViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java, creator = { triggerConditionsViewModel() })
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TRIGGER_CONDITION_LIST.name

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@TriggerConditionListDialog.Content() } }
    }
@Composable private fun Content() {
        val conditions = viewModel.configuredTriggerConditions.collectAsStateWithLifecycle(emptyList()).value
        val canCopy = viewModel.canCopyCondition.collectAsStateWithLifecycle(false).value
        Scaffold(
            modifier = Modifier.fillMaxWidth().heightIn(max = dimensionResource(io.github.vibhor1102.macrion.core.ui.R.dimen.bottom_sheet_min_height)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            topBar = {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = ::back,
                        modifier = Modifier.tutorialAnchor(
                            MonitoredViewType.TRIGGER_CONDITION_LIST_DIALOG_BUTTON_CLOSE,
                            onClick = ::back,
                        ),
                    ) { Icon(painterResource(R.drawable.ic_back), null) }
                    Text(context.getString(R.string.dialog_title_trigger_event), Modifier.weight(1f).padding(8.dp),
                        style = MaterialTheme.typography.titleLarge)
                }
            },
            floatingActionButton = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canCopy) FloatingActionButton(onClick = ::showCopyDialog, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(painterResource(R.drawable.ic_copy), null)
                    }
                    FloatingActionButton(
                        onClick = ::showTriggerConditionTypeSelectionDialog,
                        modifier = Modifier.tutorialAnchor(
                            MonitoredViewType.TRIGGER_CONDITION_LIST_DIALOG_BUTTON_CREATE,
                            onClick = ::showTriggerConditionTypeSelectionDialog,
                        ),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), null)
                    }
                }
            },
        ) { padding ->
            if (conditions.isEmpty()) Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(context.getString(R.string.message_empty_trigger_condition_list_title), style = MaterialTheme.typography.titleMedium)
                    Text(context.getString(R.string.message_empty_trigger_condition_list_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(conditions, key = { it.condition.id.toString() }) { condition -> ConditionRow(condition) }
            }
        }
    }

    @Composable private fun ConditionRow(item: UiTriggerCondition) {
        Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { showTriggerConditionDialog(item.condition) }
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall)
                Text(item.description, style = MaterialTheme.typography.bodySmall,
                    color = if (item.haveError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(item.iconRes), null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }

    private fun showTriggerConditionTypeSelectionDialog() = overlayManager.navigateTo(context,
        TriggerConditionTypeSelectionDialog(allTriggerConditionChoices(), onChoiceSelectedListener = {
            showTriggerConditionDialog(viewModel.createNewTriggerCondition(context, it))
        }), false)

    private fun showCopyDialog() = overlayManager.navigateTo(context, ConditionCopyDialog(true) { selected ->
        if (selected.size == 1) (selected[0] as? TriggerCondition)?.let(::showTriggerConditionDialog)
    })

    private fun showTriggerConditionDialog(condition: TriggerCondition) {
        viewModel.startConditionEdition(condition)
        val listener = object : OnConditionConfigCompleteListener {
            override fun onConfirmClicked() { viewModel.upsertEditedCondition() }
            override fun onDeleteClicked() { viewModel.removeEditedCondition() }
            override fun onDismissClicked() { viewModel.dismissEditedCondition() }
        }
        val overlay = when (condition) {
            is TriggerCondition.OnBroadcastReceived -> BroadcastReceivedConditionDialog(listener)
            is TriggerCondition.OnCounterCountReached -> CounterReachedConditionDialog(listener)
            is TriggerCondition.OnTimerReached -> TimerReachedConditionDialog(listener)
        }
        overlayManager.navigateTo(context, overlay, true)
    }
}
