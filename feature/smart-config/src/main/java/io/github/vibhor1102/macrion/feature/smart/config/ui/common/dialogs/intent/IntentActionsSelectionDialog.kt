/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.intent

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters.newWebBrowserStarterOverlay

class IntentActionsSelectionDialog(
    private val currentAction: String?,
    private val onConfigComplete: (action: String?) -> Unit,
    private val forBroadcastReception: Boolean = false,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.INTENT_ACTIONS_SELECTION.name

    private val viewModel: IntentActionsSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { intentActionsSelectionViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.initialize(currentAction, forBroadcastReception)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@IntentActionsSelectionDialog.Content() } }
        }
    }

@Composable
    private fun Content() {
        val actions by viewModel.actionsItems.collectAsStateWithLifecycle(emptyList())
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::finishSelection) {
                        Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
                    }
                    Text(
                        text = context.getString(R.string.dialog_title_intent_actions),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(actions, key = { it.action.value }) { item ->
                        IntentActionRow(item)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    @Composable
    private fun IntentActionRow(item: ItemAction) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { viewModel.setActionSelectionState(item.action.value, !item.isSelected) }
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.action.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            IconButton(onClick = { onActionHelpClicked(item.action.helpUri) }) {
                Icon(painterResource(R.drawable.ic_help), contentDescription = null)
            }
            RadioButton(
                selected = item.isSelected,
                onClick = { viewModel.setActionSelectionState(item.action.value, !item.isSelected) },
            )
        }
    }

    private fun finishSelection() {
        debounceUserInteraction {
            onConfigComplete(viewModel.getSelectedAction())
            back()
        }
    }

    private fun onActionHelpClicked(uri: Uri) {
        debounceUserInteraction {
            overlayManager.navigateTo(
                context = context,
                newOverlay = newWebBrowserStarterOverlay(uri),
                hideCurrent = true,
            )
        }
    }
}
