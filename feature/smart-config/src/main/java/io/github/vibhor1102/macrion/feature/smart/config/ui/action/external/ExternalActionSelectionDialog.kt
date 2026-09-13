/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.external

import android.view.ViewGroup
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class ExternalActionSelectionDialog(private val onExternalActionSelected: (String) -> Unit) :
    OverlayDialog(R.style.ScenarioConfigTheme) {
    private val viewModel: ExternalActionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java, creator = { externalActionViewModel() })

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ExternalActionSelectionDialog.Content() } }
    }
@Composable private fun Content() {
        val names = viewModel.knownExternalActionNames.collectAsStateWithLifecycle(emptyList()).value
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
                    Text(context.getString(R.string.dialog_title_external_action_selection), Modifier.weight(1f).padding(8.dp),
                        style = MaterialTheme.typography.titleLarge)
                }
                if (names.isEmpty()) Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(context.getString(R.string.message_empty_external_action_list_title), style = MaterialTheme.typography.titleMedium)
                        Text(context.getString(R.string.message_empty_external_action_list_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(names, key = { it }) { name ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable {
                            onExternalActionSelected(name); back()
                        }.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(context.getString(R.string.field_external_action_selection_desc),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
