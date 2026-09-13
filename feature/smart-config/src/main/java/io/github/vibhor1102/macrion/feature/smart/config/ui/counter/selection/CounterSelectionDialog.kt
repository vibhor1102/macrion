/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.counter.selection

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.LocalMonitoredViewsManager
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.creation.CounterCreationDialog

class CounterSelectionDialog(private val onCounterSelected: (String) -> Unit) :
    OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_SELECTION.name
    private val viewModel: CounterSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterSelectionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@CounterSelectionDialog.Content() } }
    }
@Composable private fun Content() {
        CompositionLocalProvider(LocalMonitoredViewsManager provides viewModel.monitoredViewsManager) {
            val counters = viewModel.counterNames.collectAsStateWithLifecycle(emptyList()).value
            Scaffold(
                modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                topBar = {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
                        Text(context.getString(R.string.generic_counters), Modifier.weight(1f).padding(8.dp),
                            style = MaterialTheme.typography.titleLarge)
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = ::showCounterCreationDialog,
                        modifier = Modifier.tutorialAnchor(
                            MonitoredViewType.COUNTER_SELECTION_DIALOG_BUTTON_CREATE,
                            onClick = ::showCounterCreationDialog,
                        ),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), null)
                    }
                },
            ) { padding ->
            if (counters.isEmpty()) Box(Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(context.getString(R.string.message_empty_counter_name_list_title), style = MaterialTheme.typography.titleMedium)
                    Text(context.getString(R.string.message_empty_counter_name_list_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(counters, key = { it.counterName }) { counter ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable {
                        onCounterSelected(counter.counterName); back()
                    }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(counter.counterName, style = MaterialTheme.typography.titleSmall)
                            Text(counter.counterStartingValueDesc, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(24.dp))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        }
    }

    private fun showCounterCreationDialog() = overlayManager.navigateTo(context, CounterCreationDialog(), false)
}
