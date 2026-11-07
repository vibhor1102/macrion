/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.more

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.viewModels
import io.github.vibhor1102.macrion.core.ui.bindings.dialogs.DialogNavigationButton
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.counter.config.CountersConfigDialog
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.DebugReportDialog

class MoreContent(appContext: Context) : NavBarDialogContent(appContext) {
    private val viewModel: MoreViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { moreViewModel() },
    )

    override fun onCreateView(container: ViewGroup): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@MoreContent.Content() } }
    }

    override fun onViewCreated() = Unit

    @Composable private fun Content() {
        val debugView = viewModel.isDebugViewEnabled.collectAsStateWithLifecycle(false).value
        val debugReport = viewModel.isDebugReportEnabled.collectAsStateWithLifecycle(false).value
        val reportAvailable = viewModel.showDebugReportEnabled.collectAsStateWithLifecycle(false).value
        val counters = viewModel.counterFieldDescription.collectAsStateWithLifecycle("").value
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    SelectorField(context.getString(R.string.field_counters_title), counters, true, ::showCountersConfigDialog)
                }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    SwitchField(context.getString(R.string.field_show_debug_view_title),
                        context.getString(R.string.field_show_debug_view_desc), debugView, viewModel::toggleIsDebugViewEnabled)
                }
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SwitchField(context.getString(R.string.item_title_debug_generate_report),
                        context.getString(R.string.item_desc_debug_generate_report), debugReport, viewModel::toggleIsDebugReportEnabled,
                        contentPadding = PaddingValues(0.dp))
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    SelectorField(context.getString(R.string.field_show_debug_report_title),
                        context.getString(if (reportAvailable) R.string.field_show_debug_report_desc_available
                        else R.string.field_show_debug_report_desc_not_available), reportAvailable, ::showDebugReport,
                        contentPadding = PaddingValues(0.dp))
                } }
            }
        }
    }

    @Composable private fun SwitchField(title: String, description: String, checked: Boolean, onToggle: () -> Unit,
        contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(contentPadding), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(Modifier.height(48.dp)); Spacer(Modifier.width(12.dp))
            Switch(checked, { onToggle() })
        }
    }

    @Composable private fun SelectorField(title: String, description: String, enabled: Boolean, onClick: () -> Unit,
        contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
        Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clickable(enabled, onClick = onClick).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
                Text(description, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.75f))
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = color)
        }
    }

    override fun onDialogButtonClicked(buttonType: DialogNavigationButton) {
        if (buttonType == DialogNavigationButton.SAVE) viewModel.saveConfig()
    }
    private fun showDebugReport() = debounceUserInteraction {
        dialogController.overlayManager.navigateTo(context, DebugReportDialog())
    }

    private fun showCountersConfigDialog() = debounceUserInteraction {
        dialogController.overlayManager.navigateTo(context, CountersConfigDialog())
    }
}
