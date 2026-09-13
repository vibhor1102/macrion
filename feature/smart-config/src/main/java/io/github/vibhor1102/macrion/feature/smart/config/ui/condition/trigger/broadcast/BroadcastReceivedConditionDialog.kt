/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.trigger.broadcast

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.intent.IntentActionsSelectionDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import kotlinx.coroutines.launch

class BroadcastReceivedConditionDialog(private val listener: OnConditionConfigCompleteListener) :
    OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.BROADCAST_RECEIVED_CONDITION.name
    private val viewModel: BroadcastReceivedConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { broadcastReceivedConditionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@BroadcastReceivedConditionDialog.Content() } }
    }
    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect { if (!it) finish() }
        } }
    }

    @Composable private fun Content() {
        val name = viewModel.name.collectAsStateWithLifecycle("").value.orEmpty()
        val action = viewModel.intentAction.collectAsStateWithLifecycle("").value.orEmpty()
        val nameError = viewModel.nameError.collectAsStateWithLifecycle(false).value
        val actionError = viewModel.intentActionError.collectAsStateWithLifecycle(false).value
        val saveEnabled = viewModel.conditionCanBeSaved.collectAsStateWithLifecycle(false).value
        Surface(Modifier.fillMaxWidth().heightIn(max = 560.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(saveEnabled)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(name, viewModel::setName, context.getString(R.string.generic_name),
                        isError = nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    OutlinedTextField(
                        value = action, onValueChange = viewModel::setIntentAction, modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.field_intent_broadcast_action_label)) },
                        trailingIcon = { IconButton(onClick = ::showBroadcastActionSelectionDialog) {
                            Icon(painterResource(R.drawable.ic_search), null)
                        } },
                        isError = actionError, singleLine = true,
                        keyboardOptions = macrionDoneKeyboardOptions(),
                        keyboardActions = macrionDoneKeyboardActions(),
                    )
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_broadcast_received), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge)
            FilledTonalIconButton(onClick = ::delete) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = ::save, enabled = saveEnabled) { Icon(painterResource(R.drawable.ic_save_filled), null) }
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
    private fun showBroadcastActionSelectionDialog() = overlayManager.navigateTo(context,
        IntentActionsSelectionDialog(viewModel.getIntentAction(), viewModel::setIntentAction, true), true)
}
