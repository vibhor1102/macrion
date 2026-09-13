/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.toggleevent

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class ToggleEventDialog(private val listener: OnActionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TOGGLE_EVENT.name
    private val viewModel: ToggleEventViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { toggleEventViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ToggleEventDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingAction.collect {
                if (!it) { Log.e(TAG, "Closing ToggleEventDialog because there is no action edited"); finish() }
            }
        } }
    }

    @Composable private fun Content() {
        val initialName = remember { viewModel.getEditedAction()?.name.orEmpty() }
        var name by rememberSaveable { mutableStateOf(initialName) }
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(initialValue = initialName.isEmpty())
        val checkedIndex by viewModel.toggleAllButtonCheckIndex.collectAsStateWithLifecycle(initialValue = null)
        val selectorState by viewModel.eventToggleSelectorState.collectAsStateWithLifecycle(
            initialValue = EventToggleSelectorState(false, "", 0, 0, 0, R.string.field_select_toggle_events_desc_empty),
        )
        val saveEnabled by viewModel.isValidAction.collectAsStateWithLifecycle(initialValue = false)
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(saveEnabled)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                        isError = nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ToggleAllField(checkedIndex)
                            HorizontalDivider()
                            EventSelector(selectorState)
                        }
                    }
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_toggle_event), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledTonalIconButton(onClick = ::delete) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = ::save, enabled = saveEnabled) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @Composable private fun ToggleAllField(checkedIndex: Int?) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.getString(R.string.field_change_all_title), style = MaterialTheme.typography.titleSmall)
                Text(context.getString(when (checkedIndex) {
                    BUTTON_ENABLE_EVENT -> R.string.field_change_all_desc_enable_all
                    BUTTON_TOGGLE_EVENT -> R.string.field_change_all_desc_invert_all
                    BUTTON_DISABLE_EVENT -> R.string.field_change_all_desc_disable_all
                    else -> R.string.field_change_all_desc_manual
                }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            EventToggleButtons(checkedIndex, viewModel::setToggleAllType)
        }
    }

    @Composable private fun EventSelector(state: EventToggleSelectorState) {
        val openSelector = { if (state.isEnabled) showEventTogglesDialog() }
        Row(
            Modifier
                .fillMaxWidth()
                .tutorialAnchor(
                    MonitoredViewType.TOGGLE_EVENT_DIALOG_SELECT_TOGGLES,
                    onClick = openSelector,
                    enabled = state.isEnabled,
                )
                .clickable(enabled = state.isEnabled, onClick = openSelector)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleSmall,
                    color = if (state.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                if (state.emptyText != null) Text(context.getString(state.emptyText), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) else ToggleCounts(state)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null)
        }
    }

    @Composable private fun ToggleCounts(state: EventToggleSelectorState) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleCount(R.drawable.ic_confirm, state.enableCount)
            ToggleCount(R.drawable.ic_invert, state.toggleCount)
            ToggleCount(R.drawable.ic_cancel, state.disableCount)
        }
    }
    @Composable private fun ToggleCount(icon: Int, count: Int) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(icon), null, Modifier.size(16.dp)); Text(count.toString(), style = MaterialTheme.typography.bodySmall)
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
    private fun showEventTogglesDialog() {
        val action = viewModel.getEditedAction() ?: return
        overlayManager.navigateTo(context, EventTogglesDialog(action, viewModel.getScenarioEvents(), viewModel::setNewEventToggles))
    }
}
private const val TAG = "ToggleEventDialog"
