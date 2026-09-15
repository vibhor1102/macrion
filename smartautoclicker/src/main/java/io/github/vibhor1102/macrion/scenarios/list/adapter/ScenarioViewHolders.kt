/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.scenarios.list.adapter

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbScenario as DumbScenarioModel
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toEffectDescription
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.formatters.toNaturalDisplayString
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState
import io.github.vibhor1102.macrion.scenarios.list.model.getTimeSinceString
import kotlinx.coroutines.Job

private typealias ScenarioItem = ScenarioListUiState.Item.ScenarioItem
private typealias ValidScenario = ScenarioListUiState.Item.ScenarioItem.Valid
private typealias EmptyScenario = ScenarioListUiState.Item.ScenarioItem.Empty
private typealias DumbScenarioItem = ScenarioListUiState.Item.ScenarioItem.Valid.Dumb
private typealias SmartScenarioItem = ScenarioListUiState.Item.ScenarioItem.Valid.Smart
private typealias EventItem = ScenarioListUiState.Item.ScenarioItem.Valid.Smart.EventItem

@Composable
private fun ScenarioCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        ) { content() }
    }
}

@Composable
private fun ScenarioTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ValidScenarioCard(
    item: ValidScenario,
    typeIcon: Int,
    onLaunch: () -> Unit,
    onExpand: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
    details: @Composable () -> Unit,
) {
    ScenarioCard(onClick = if (item.showExportCheckbox) onExport else onLaunch, modifier = modifier) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(typeIcon), null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                ScenarioTitle(item.displayName, Modifier.weight(1f))
                if (item.showExportCheckbox) {
                    RadioButton(selected = item.checkedForExport, onClick = onExport)
                } else {
                    VerticalDivider(Modifier.height(32.dp).padding(start = 16.dp, end = 8.dp))
                    IconButton(onClick = onExpand) {
                        Icon(
                            painterResource(if (item.expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
                            null,
                        )
                    }
                }
            }
            if (!item.showExportCheckbox) {
                AnimatedVisibility(
                    visible = item.expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) { details() }
            }
        }
    }
}

@Composable
private fun DumbDetails(item: DumbScenarioItem, onCopy: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stat(R.drawable.ic_most_used, item.startCount.toString(), Modifier.width(72.dp))
                Spacer(Modifier.width(8.dp))
                Stat(R.drawable.ic_sort_recent, context.getTimeSinceString(item.lastStartTimestamp))
            }
            Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(R.drawable.ic_click_small, item.clickCount.toString())
                Stat(R.drawable.ic_swipe_small, item.swipeCount.toString())
                Stat(R.drawable.ic_wait_small, item.pauseCount.toString())
            }
            Text(item.repeatText, style = MaterialTheme.typography.bodyLarge)
            Text(item.maxDurationText, style = MaterialTheme.typography.bodyLarge)
        }
        ActionButtons(onCopy, onDelete)
    }
}

@Composable
private fun SmartDetails(
    item: SmartScenarioItem,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(R.drawable.ic_most_used, item.startCount.toString(), Modifier.width(72.dp))
            Spacer(Modifier.width(8.dp))
            Stat(R.drawable.ic_sort_recent, context.getTimeSinceString(item.lastStartTimestamp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(R.drawable.ic_detection_quality, item.detectionQuality.toString(), Modifier.width(72.dp))
            Spacer(Modifier.width(8.dp))
            Stat(R.drawable.ic_trigger_event, item.triggerEventCount.toString())
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.eventsItems.isEmpty()) {
                Text(
                    stringResource(R.string.message_empty_image_events_scenario_list),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(item.eventsItems, key = { it.id }) { EventCard(it, bitmapProvider) }
                }
            }
            Spacer(Modifier.width(16.dp))
            ActionButtons(onCopy, onDelete)
        }
    }
}

@Composable
private fun ActionButtons(onCopy: () -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(onClick = onCopy) { Icon(painterResource(R.drawable.ic_copy), null) }
        FilledTonalIconButton(onClick = onDelete) { Icon(painterResource(R.drawable.ic_delete), null) }
    }
}

@Composable
private fun Stat(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EventCard(
    item: EventItem,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
) {
    OutlinedCard(Modifier.width(100.dp).height(100.dp)) {
        ConditionPreview(item.firstCondition, bitmapProvider, Modifier.fillMaxWidth().weight(1f))
        HorizontalDivider()
        Text(
            item.eventName,
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            EventStat(R.drawable.ic_click_small, item.actionsCount.toString())
            EventStat(R.drawable.ic_condition, item.conditionsCount.toString())
        }
    }
}

@Composable
private fun EventStat(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(2.dp))
        Icon(painterResource(icon), null, Modifier.size(14.dp))
    }
}

@Composable
private fun ConditionPreview(
    condition: ScreenCondition?,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
    modifier: Modifier,
) {
    val context = LocalContext.current
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(2.dp), Alignment.Center) {
        when (condition) {
            is ScreenCondition.Color -> ConditionColorPreview(condition.color)
            is ScreenCondition.Image -> {
                var bitmap by remember(condition) { mutableStateOf<Bitmap?>(null) }
                var loaded by remember(condition) { mutableStateOf(false) }
                DisposableEffect(condition) {
                    val job = bitmapProvider(condition) { bitmap = it; loaded = true }
                    onDispose { job?.cancel() }
                }
                if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else if (loaded) ErrorPreview()
            }
            is ScreenCondition.Number -> Text(
                condition.comparisonOperation.toEffectDescription(
                    context,
                    operand = condition.counterValue.toNaturalDisplayString(),
                ),
                textAlign = TextAlign.Center,
            )
            is ScreenCondition.Text -> Text(condition.text, textAlign = TextAlign.Center)
            null -> ErrorPreview()
        }
    }
}

@Composable
private fun ConditionColorPreview(color: Int) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.3f
        drawCircle(Color(color), radius = radius)
        drawCircle(outline, radius = radius * 1.15f, style = Stroke(size.minDimension * 0.06f))
    }
}

@Composable private fun ErrorPreview() {
    Icon(painterResource(R.drawable.ic_cancel), null, Modifier.size(32.dp), tint = Color.Red)
}

/**
 * Shared scenario-row content for Compose screens.
 *
 * Keeping this dispatch here ensures the scenario card layout has one source of truth for the
 * Compose scenario list.
 */
@Composable
internal fun ScenarioListItem(
    item: ScenarioListUiState.Item.ScenarioItem,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
    onLaunch: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    onExpand: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onExport: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onCopy: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onDelete: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is EmptyScenario -> ScenarioCard(onClick = { onLaunch(item) }, modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (item.scenario is DumbScenarioModel) R.drawable.ic_dumb else R.drawable.ic_smart),
                    null,
                    Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                ScenarioTitle(item.displayName, Modifier.weight(1f))
                IconButton(onClick = { onDelete(item) }) {
                    Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                FilledIconButton(onClick = { onLaunch(item) }) {
                    Icon(painterResource(R.drawable.ic_play_arrow), null, Modifier.size(20.dp))
                }
            }
        }

        is DumbScenarioItem -> ValidScenarioCard(
            item, R.drawable.ic_dumb, { onLaunch(item) }, { onExpand(item) }, { onExport(item) }, modifier,
        ) { DumbDetails(item, { onCopy(item) }, { onDelete(item) }) }

        is SmartScenarioItem -> ValidScenarioCard(
            item, R.drawable.ic_smart, { onLaunch(item) }, { onExpand(item) }, { onExport(item) }, modifier,
        ) { SmartDetails(item, bitmapProvider, { onCopy(item) }, { onDelete(item) }) }
    }
}
