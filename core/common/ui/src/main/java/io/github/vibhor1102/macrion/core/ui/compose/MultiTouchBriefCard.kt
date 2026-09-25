/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.utils.formatDuration
import kotlin.math.roundToInt

enum class MultiTouchKind { TAP, SWIPE, OTHER }

data class MultiTouchBriefChild(
    val name: String,
    @DrawableRes val icon: Int,
    val kind: MultiTouchKind,
    val durationMs: Long?,
    val needsSetup: Boolean,
)

private data class TouchLabel(val title: String, val detail: String)

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
    val childLabels = children.map { child ->
        val title = child.name.ifBlank { when (child.kind) {
            MultiTouchKind.TAP -> tapLabel
            MultiTouchKind.SWIPE -> swipeLabel
            MultiTouchKind.OTHER -> touchLabel
        } }
        val detail = if (child.needsSetup || child.durationMs == null) setupLabel else formatDuration(child.durationMs)
        TouchLabel(title, detail)
    }
    val accessibilityText = buildString {
        append(parentName)
        childLabels.forEachIndexed { childIndex, (title, detail) ->
            val type = when (children[childIndex].kind) {
                MultiTouchKind.TAP -> tapLabel
                MultiTouchKind.SWIPE -> swipeLabel
                MultiTouchKind.OTHER -> touchLabel
            }
            append(". ")
            append(childIndex + 1)
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
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold)
    val detailStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic)

    ElevatedCard(onClick = onClick, modifier = modifier.semantics { contentDescription = accessibilityText }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val paneWidth = if (portrait) maxWidth / 2 else maxWidth
            val paneHeight = if (portrait) maxHeight else maxHeight / 2
            val paneWidthPx = with(density) { paneWidth.toPx() }
            val paneHeightPx = with(density) { paneHeight.toPx() }
            val titleWidthPx = with(density) {
                paneWidthPx - (if (portrait) {
                    16.dp.toPx() + 32.dp.toPx() + 6.dp.toPx() + 2.dp.toPx()
                } else {
                    16.dp.toPx()
                })
            }
            val detailWidthPx = with(density) { paneWidthPx - 16.dp.toPx() }
            val canSplit = children.size == 2 &&
                children.all { it.kind != MultiTouchKind.OTHER } &&
                (!needsSetup || children.any { it.needsSetup }) &&
                paneWidth >= 112.dp &&
                childLabels.all { (title, detail) ->
                    val titleLayout = textMeasurer.measure(
                        AnnotatedString(title), style = titleStyle,
                        constraints = Constraints(maxWidth = titleWidthPx.roundToInt().coerceAtLeast(1)),
                        maxLines = if (portrait) 1 else 2,
                        softWrap = !portrait, overflow = TextOverflow.Ellipsis,
                    )
                    val detailLayout = textMeasurer.measure(
                        AnnotatedString(detail), style = detailStyle,
                        constraints = Constraints(maxWidth = detailWidthPx.roundToInt().coerceAtLeast(1)),
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    )
                    val requiredHeightPx = with(density) {
                        if (portrait) {
                            8.dp.toPx() + maxOf(32.dp.toPx(), titleLayout.size.height.toFloat()) +
                                4.dp.toPx() + detailLayout.size.height
                        } else {
                            16.dp.toPx() + 32.dp.toPx() + 8.dp.toPx() +
                                titleLayout.size.height + 4.dp.toPx() + detailLayout.size.height
                        }
                    }
                    !titleLayout.hasVisualOverflow && !detailLayout.hasVisualOverflow &&
                        requiredHeightPx <= paneHeightPx
                }

            if (canSplit) {
                if (portrait) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        TouchSection(childLabels[0], children[0], true, titleStyle, detailStyle,
                            Modifier.weight(1f).fillMaxHeight())
                        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant))
                        TouchSection(childLabels[1], children[1], true, titleStyle, detailStyle,
                            Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        TouchSection(childLabels[0], children[0], false, titleStyle, detailStyle,
                            Modifier.weight(1f).fillMaxWidth())
                        Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant))
                        TouchSection(childLabels[1], children[1], false, titleStyle, detailStyle,
                            Modifier.weight(1f).fillMaxWidth())
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
                SummarySection(parentName, summary, icon, portrait, hasProblem, titleStyle, detailStyle)
            }
        }
    }
}

@Composable
private fun TouchSection(
    label: TouchLabel,
    child: MultiTouchBriefChild,
    portrait: Boolean,
    titleStyle: TextStyle,
    detailStyle: TextStyle,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = if (portrait) 4.dp else 8.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (portrait) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(painterResource(child.icon), contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(label.title, style = titleStyle, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
        } else {
            Icon(painterResource(child.icon), contentDescription = null,
                modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(label.title, style = titleStyle, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
        Text(label.detail, style = detailStyle, textAlign = TextAlign.Center,
            color = if (child.needsSetup) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SummarySection(name: String, summary: String, @DrawableRes icon: Int, portrait: Boolean,
    hasProblem: Boolean, titleStyle: TextStyle, detailStyle: TextStyle) {
    val summaryColor = if (hasProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    if (portrait) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(name, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary, style = detailStyle,
                    color = summaryColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
        }
    } else {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(name, style = titleStyle, maxLines = 2,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(summary, style = detailStyle,
                color = summaryColor, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
