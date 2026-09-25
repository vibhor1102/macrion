/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.utils.formatDuration

enum class MultiTouchKind { TAP, SWIPE, OTHER }

data class MultiTouchBriefChild(
    val name: String,
    @DrawableRes val icon: Int,
    val kind: MultiTouchKind,
    val durationMs: Long?,
    val needsSetup: Boolean,
)

private data class TouchLabel(val order: Int, val title: String, val detail: String)

/** One carousel card for the whole action; its two visual sections are not separate controls. */
@Composable
fun MultiTouchBriefCard(
    name: String,
    @DrawableRes icon: Int,
    children: List<MultiTouchBriefChild>,
    needsSetup: Boolean,
    portrait: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tapLabel = stringResource(R.string.multi_touch_brief_tap)
    val swipeLabel = stringResource(R.string.multi_touch_brief_swipe)
    val touchLabel = stringResource(R.string.multi_touch_brief_touch)
    val setupLabel = stringResource(R.string.multi_touch_brief_needs_setup)
    val emptyLabel = stringResource(R.string.multi_touch_brief_no_touches)
    val parentName = name.ifBlank { stringResource(R.string.multi_touch_brief_default_name) }
    val childLabels = children.mapIndexed { index, child ->
        val title = child.name.ifBlank { when (child.kind) {
            MultiTouchKind.TAP -> tapLabel
            MultiTouchKind.SWIPE -> swipeLabel
            MultiTouchKind.OTHER -> touchLabel
        } }
        val detail = if (child.needsSetup || child.durationMs == null) setupLabel else formatDuration(child.durationMs)
        TouchLabel(index + 1, title, detail)
    }
    val accessibilityText = buildString {
        append(parentName)
        childLabels.forEachIndexed { childIndex, (index, title, detail) ->
            val type = when (children[childIndex].kind) {
                MultiTouchKind.TAP -> tapLabel
                MultiTouchKind.SWIPE -> swipeLabel
                MultiTouchKind.OTHER -> touchLabel
            }
            append(". ")
            append(index)
            append(": ")
            if (!title.equals(type, ignoreCase = true)) {
                append(type)
                append(", ")
            }
            append(title)
            append(", ")
            append(detail)
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleStyle = MaterialTheme.typography.titleSmall
    val detailStyle = MaterialTheme.typography.labelMedium
    val orderStyle = MaterialTheme.typography.labelSmall

    ElevatedCard(onClick = onClick, modifier = modifier.semantics { contentDescription = accessibilityText }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val paneWidth = if (portrait) maxWidth / 2 else maxWidth
            val paneHeight = if (portrait) maxHeight else maxHeight / 2
            val availableNameWidth = with(density) {
                paneWidth.toPx() - 22.dp.toPx() - 18.dp.toPx() - 8.dp.toPx() -
                    textMeasurer.measure(AnnotatedString("2"), style = orderStyle, maxLines = 1).size.width
            }
            val availableDetailWidth = with(density) { paneWidth.toPx() - 20.dp.toPx() }
            val requiredHeight = with(density) {
                18.dp.toPx() + 2.dp.toPx() +
                    maxOf(
                        textMeasurer.measure(AnnotatedString("Swipe"), style = titleStyle, maxLines = 1).size.height,
                        textMeasurer.measure(AnnotatedString("2"), style = orderStyle, maxLines = 1).size.height,
                    ) + textMeasurer.measure(AnnotatedString(setupLabel), style = detailStyle, maxLines = 1).size.height
            }
            val canSplit = children.size == 2 &&
                children.all { it.kind != MultiTouchKind.OTHER } &&
                (!needsSetup || children.any { it.needsSetup }) &&
                paneWidth >= 112.dp && with(density) { paneHeight.toPx() >= requiredHeight } &&
                childLabels.all { (_, title, detail) ->
                    textWidth(textMeasurer, title, titleStyle) <= availableNameWidth &&
                        textWidth(textMeasurer, detail, detailStyle) <= availableDetailWidth
                }

            if (canSplit) {
                if (portrait) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        TouchSection(childLabels[0], children[0], Modifier.weight(1f).fillMaxHeight())
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TouchSection(childLabels[1], children[1], Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        TouchSection(childLabels[0], children[0], Modifier.weight(1f).fillMaxWidth())
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TouchSection(childLabels[1], children[1], Modifier.weight(1f).fillMaxWidth())
                    }
                }
            } else {
                val count = children.size
                val touchCount = pluralStringResource(R.plurals.multi_touch_brief_touch_count, count, count)
                val invalidCount = children.count { it.needsSetup }
                val hasProblem = invalidCount > 0 || needsSetup
                val summary = when {
                    count == 0 -> emptyLabel
                    hasProblem -> {
                        val problemCount = invalidCount.coerceAtLeast(1)
                        "$touchCount · ${pluralStringResource(R.plurals.multi_touch_brief_setup_count, problemCount, problemCount)}"
                    }
                    else -> {
                        val swipes = children.count { it.kind == MultiTouchKind.SWIPE }
                        val taps = children.count { it.kind == MultiTouchKind.TAP }
                        val types = buildList {
                            if (swipes > 0) add(pluralStringResource(R.plurals.multi_touch_brief_swipe_count, swipes, swipes))
                            if (taps > 0) add(pluralStringResource(R.plurals.multi_touch_brief_tap_count, taps, taps))
                        }.joinToString(", ")
                        if (types.isEmpty()) touchCount else "$touchCount · $types"
                    }
                }
                SummarySection(parentName, summary, icon, portrait, hasProblem)
            }
        }
    }
}

private fun textWidth(measurer: androidx.compose.ui.text.TextMeasurer, text: String, style: TextStyle): Int =
    measurer.measure(AnnotatedString(text), style = style, maxLines = 1, softWrap = false).size.width

@Composable
private fun TouchSection(
    label: TouchLabel,
    child: MultiTouchBriefChild,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.order.toString(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(child.icon), contentDescription = null,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Text(label.title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(2.dp))
        Text(label.detail, style = MaterialTheme.typography.labelMedium,
            color = if (child.needsSetup) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SummarySection(name: String, summary: String, @DrawableRes icon: Int, portrait: Boolean, hasProblem: Boolean) {
    val summaryColor = if (hasProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    if (portrait) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = summaryColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        }
    } else {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(summary, style = MaterialTheme.typography.bodySmall,
                color = summaryColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
