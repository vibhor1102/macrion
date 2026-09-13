/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.copy.condition

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toEffectDescription
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.*
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

class ConditionCopyDialog(
    private val requestTriggerConditions: Boolean,
    private val onConditionsCopied: (List<Condition>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    private val viewModel: ConditionCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { conditionCopyViewModel() },
    )

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CONDITION_COPY.name
    override fun onCreateView(): ViewGroup {
        viewModel.setCopyListType(requestTriggerConditions)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@ConditionCopyDialog.Content() } }
        }
    }
@Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        var query by rememberSaveable { mutableStateOf("") }
        val list = state?.items.orEmpty()
        CopyPickerContent(
            context.getString(R.string.dialog_overlay_title_copy_from),
            context.getString(R.string.search_view_hint_condition_copy),
            context.getString(R.string.message_empty_copy), query,
            loading = state == null, empty = state != null && list.isEmpty(),
            copyEnabled = list.any { it is ConditionCopyItem.ConditionItem },
            onQueryChanged = { query = it; viewModel.updateSearchQuery(it) },
            onDismiss = ::back, onCopy = ::onCopyClicked,
        ) {
            var index = 0
            while (index < list.size) {
                when (val entry = list[index]) {
                    is ConditionCopyItem.HeaderItem -> item(key = "header:${entry.title}:$index") {
                        CopySectionHeader(context.getString(entry.title))
                    }
                    is ConditionCopyItem.ConditionItem.Trigger -> {
                        val triggerIndex = index
                        item(key = entry.uiCondition.condition.id.toString()) {
                        CopyListItem(entry.uiCondition.iconRes, entry.uiCondition.name, entry.uiCondition.description,
                            entry.isChecked, entry.uiCondition.haveError) {
                            viewModel.toggleCheckedForCopy(entry.uiCondition.condition, triggerIndex)
                        }
                    }
                    }
                    is ConditionCopyItem.ConditionItem.Screen -> {
                        val firstIndex = index
                        val secondIndex = index + 1
                        val second = list.getOrNull(secondIndex) as? ConditionCopyItem.ConditionItem.Screen
                        item(key = "screen-row:${entry.uiCondition.condition.id}") {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
                                ScreenConditionCard(entry.uiCondition, entry.isChecked, Modifier.weight(1f)) {
                                    viewModel.toggleCheckedForCopy(entry.uiCondition.condition, firstIndex)
                                }
                                if (second != null) ScreenConditionCard(second.uiCondition, second.isChecked, Modifier.weight(1f)) {
                                    viewModel.toggleCheckedForCopy(second.uiCondition.condition, secondIndex)
                                } else Spacer(Modifier.weight(1f))
                            }
                        }
                        if (second != null) index++
                    }
                }
                index++
            }
        }
    }

    @Composable
    private fun ScreenConditionCard(condition: UiScreenCondition, checked: Boolean, modifier: Modifier, onClick: () -> Unit) {
        val shape = RoundedCornerShape(10.dp)
        Card(
            modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                .border(if (checked) 2.dp else 1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(onClick = onClick), shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.height(100.dp)) {
                ConditionPreview(condition, Modifier.fillMaxWidth().height(55.dp))
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(condition.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(condition.shouldBeVisibleIconRes), null, Modifier.size(14.dp))
                        if (condition.condition is ScreenCondition.Image) {
                            Icon(painterResource(condition.detectionTypeIconRes), null, Modifier.size(14.dp))
                        }
                        Text(condition.thresholdText, style = MaterialTheme.typography.bodySmall,
                            color = if (condition.haveError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Checkbox(checked, onCheckedChange = { onClick() }, Modifier.size(38.dp))
                }
            }
        }
    }

    @Composable
    private fun ConditionPreview(condition: UiScreenCondition, modifier: Modifier) {
        val source = condition.condition
        var bitmap by remember(source.id) { mutableStateOf<Bitmap?>(null) }
        DisposableEffect(source.id) {
            val job = if (source is ScreenCondition.Image) viewModel.getConditionBitmap(source) { bitmap = it } else null
            onDispose { job?.cancel() }
        }
        Box(modifier.clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            when (source) {
                is ScreenCondition.Color -> Box(Modifier.size(40.dp).background(Color(source.color), CircleShape))
                is ScreenCondition.Image -> bitmap?.let {
                    Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } ?: Icon(painterResource(if (condition.haveError) R.drawable.ic_cancel else condition.iconRes), null, Modifier.size(40.dp),
                    tint = if (condition.haveError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                is ScreenCondition.Number -> Text(
                    source.comparisonOperation.toEffectDescription(context, operand = source.counterValue.toNaturalDisplayString()),
                    Modifier.padding(6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2,
                )
                is ScreenCondition.Text -> Text(source.text, Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    private fun onCopyClicked() {
        val conditions = viewModel.getConditionsCopy()
        if (viewModel.conditionCopyShouldWarnUser(conditions)) {
            val args = viewModel.getFixEventDialogArgument(conditions) ?: return
            overlayManager.navigateTo(context, FixEventChildrenCopyDialog(args) { notifySelectionAndDestroy(it.conditions) }, false)
        } else notifySelectionAndDestroy(conditions)
    }
    private fun notifySelectionAndDestroy(conditions: List<Condition>) {
        viewModel.saveCopyConditions(conditions); back(); onConditionsCopied(conditions)
    }
}
