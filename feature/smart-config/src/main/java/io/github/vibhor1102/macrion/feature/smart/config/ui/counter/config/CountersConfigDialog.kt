/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.counter.config

import android.view.ViewGroup
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteConfirmationDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.creation.CounterCreationDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.reference.CounterReferenceDialog

class CountersConfigDialog : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTERS_CONFIG.name

    private val viewModel: CountersConfigViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { countersViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@CountersConfigDialog.Content() } }
    }

@Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val canDismiss = state is CountersUiState.Loaded || state is CountersUiState.Empty
        Surface(
            Modifier.fillMaxWidth().heightIn(max = 680.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = ::back, enabled = canDismiss) {
                            Icon(painterResource(R.drawable.ic_back), null)
                        }
                        Text(
                            context.getString(R.string.dialog_title_counters_config),
                            Modifier.weight(1f).padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    StateContent(state)
                }
                FloatingActionButton(
                    onClick = ::showCounterCreationDialog,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) { Icon(painterResource(R.drawable.ic_add), null) }
            }
        }
    }

    @Composable
    private fun ColumnScope.StateContent(state: CountersUiState?) {
        when (state) {
            null, CountersUiState.Loading -> Box(
                Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            CountersUiState.Empty -> Box(
                Modifier.fillMaxWidth().height(220.dp).padding(24.dp), contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(context.getString(R.string.message_empty_counter_name_list_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        context.getString(R.string.message_empty_counter_name_list_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is CountersUiState.Loaded -> CounterList(state.counterItems)
            is CountersUiState.Replacing -> CounterList(state.counterItems)
        }
    }

    @Composable
    private fun ColumnScope.CounterList(counters: List<CounterUiItem>) {
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(counters, key = { "counter:${it.counterName}" }) { CounterCard(it) }
        }
    }

    @Composable
    private fun CounterCard(counter: CounterUiItem) {
        val shape = RoundedCornerShape(10.dp)
        val chevronRotation by animateFloatAsState(
            targetValue = if (counter.isExpanded) 180f else 0f,
            label = "counter chevron",
        )
        Box(Modifier.fillMaxWidth().clip(shape)) {
            ElevatedCard(
                onClick = { onCounterClicked(counter) },
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = shape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(counter.counterName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                counter.counterDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onExpandClicked(counter) }) {
                            Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.rotate(chevronRotation))
                        }
                    }
                    if (counter.isExpanded) ExpandedCounterContent(counter)
                }
            }
            if (counter.selectedForReplacement) {
                Box(
                    Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .clickable { viewModel.cancelReplacement() },
                    contentAlignment = Alignment.Center,
                ) { Text(context.getString(R.string.message_replace_by), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }

    @Composable
    private fun ExpandedCounterContent(counter: CounterUiItem) {
        var value by rememberSaveable(counter.counterName) { mutableStateOf(counter.startingValue.toNaturalDisplayString()) }
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                    value = newValue
                    newValue.toDoubleOrNull()?.let { viewModel.setStartingValue(counter, it) }
                }
            },
            label = { Text(context.getString(R.string.field_label_counter_starting_value)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Decimal),
            keyboardActions = macrionDoneKeyboardActions(),
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { showSetByDialog(counter) },
                enabled = !counter.setByButtonIsEmpty,
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_write), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(counter.setByButtonText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
                onClick = { showReadByDialog(counter) },
                enabled = !counter.readByButtonIsEmpty,
                modifier = Modifier.weight(1f),
            ) {
                Icon(painterResource(R.drawable.ic_read), null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(counter.readByButtonText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Button(onClick = { onDeleteClicked(counter) }, enabled = counter.deleteButtonEnabled, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(counter.deleteButtonText)
        }
    }

    override fun back() {
        if (viewModel.getUiState() is CountersUiState.Replacing) return
        super.back()
    }

    private fun onExpandClicked(counter: CounterUiItem) = when (viewModel.getUiState()) {
        is CountersUiState.Loaded -> viewModel.expandCollapseItem(counter)
        is CountersUiState.Replacing -> viewModel.replaceAndDelete(counter)
        else -> Unit
    }

    private fun onCounterClicked(counter: CounterUiItem) = when (viewModel.getUiState()) {
        is CountersUiState.Replacing -> viewModel.replaceAndDelete(counter)
        is CountersUiState.Loaded -> if (!counter.isExpanded) viewModel.expandCollapseItem(counter) else Unit
        else -> Unit
    }

    private fun onDeleteClicked(counter: CounterUiItem) {
        if (viewModel.getUiState() !is CountersUiState.Loaded) return
        if (counter.readByButtonIsEmpty && counter.setByButtonIsEmpty) {
            context.showDeleteConfirmationDialog { viewModel.deleteCounter(counter) }
        } else viewModel.selectForReplacement(counter)
    }

    private fun showSetByDialog(counter: CounterUiItem) = showReferenceDialog(counter, CounterReferenceDialog.ReferencesType.WRITE)
    private fun showReadByDialog(counter: CounterUiItem) = showReferenceDialog(counter, CounterReferenceDialog.ReferencesType.READ)

    private fun showReferenceDialog(counter: CounterUiItem, type: CounterReferenceDialog.ReferencesType) {
        if (viewModel.getUiState() !is CountersUiState.Loaded) return
        overlayManager.navigateTo(context, CounterReferenceDialog(counter.counterName, type), hideCurrent = false)
    }

    private fun showCounterCreationDialog() {
        overlayManager.navigateTo(context, CounterCreationDialog(), hideCurrent = false)
    }
}
