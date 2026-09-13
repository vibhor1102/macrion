/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.counter.reference

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

class CounterReferenceDialog(
    private val counterName: String,
    private val type: ReferencesType,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COUNTER_REFERENCE.name
    enum class ReferencesType { READ, WRITE }

    private val viewModel: CounterReferenceViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { counterReferenceViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setDialogArgs(counterName, type)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@CounterReferenceDialog.Content() } }
        }
    }

@Composable
    private fun Content() {
        val items by viewModel.uiState.collectAsStateWithLifecycle()
        Surface(
            Modifier.fillMaxWidth().heightIn(max = 620.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
                    Text(counterName, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
                }
                when (val references = items) {
                    null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(references) { _, item -> ReferenceCard(item) }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReferenceCard(item: CounterReferenceUiItem) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconText(item.eventIconRes, item.eventName, MaterialTheme.typography.titleMedium)
                IconText(item.elementIconRes, item.elementName, MaterialTheme.typography.bodyLarge)
                Text(item.referenceDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun IconText(icon: Int, text: String, style: androidx.compose.ui.text.TextStyle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = style)
        }
    }
}
