/* Copyright (C) 2024 Kevin Buzeau — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.feature.smart.config.R

@Composable
internal fun SmartActionBriefItem(
    details: UiAction,
    orientation: Int,
    onClick: () -> Unit,
    combinableActions: List<UiAction> = emptyList(),
    onCombineWithNewClick: (() -> Unit)? = null,
    onCombineWithNewSwipe: (() -> Unit)? = null,
    onCombineWithAction: ((Action) -> Unit)? = null,
    onUnsplit: (() -> Unit)? = null,
) {
    val portrait = orientation == Configuration.ORIENTATION_PORTRAIT
    val isSplit = details.subUiActions.isNotEmpty()
    val touchCount = details.subUiActions.size.coerceAtLeast(1)
    val isTouchAction = details.action is Click || details.action is Swipe || details.action is SplitAction
    var menuExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), if (portrait) Alignment.BottomCenter else Alignment.CenterStart) {
        if (isSplit) {
            ElevatedCard(
                onClick = onClick,
                modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp)
                    else Modifier.width(200.dp).fillMaxHeight(),
            ) {
                Row(Modifier.fillMaxSize().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(details.name, style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.combined_touch_count, details.subUiActions.size),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(details.subUiActions.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(painterResource(UiR.drawable.ic_more), stringResource(R.string.action_edit_gestures))
                    }
                }
            }
        } else {
            ElevatedCard(
                onClick = onClick,
                modifier = if (portrait) Modifier.fillMaxWidth().height(80.dp) else Modifier.width(136.dp).fillMaxHeight(),
            ) {
                if (portrait) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            BriefText(details.name, 17, true, 1)
                            BriefText(details.description, 14, false, 1)
                        }
                        BriefIcon(details, Modifier.padding(end = 4.dp))
                        if (isTouchAction) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    painter = painterResource(UiR.drawable.ic_more),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            BriefText(details.name, 17, true, 2, TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            BriefText(details.description, 14, false, 2, TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            BriefIcon(details)
                        }
                        if (isTouchAction) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                            ) {
                                Icon(
                                    painter = painterResource(UiR.drawable.ic_more),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            if (isSplit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit_gestures)) },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_unsplit)) },
                    onClick = {
                        menuExpanded = false
                        onUnsplit?.invoke()
                    },
                )
            }
            if (isTouchAction) {
                DropdownMenuItem(
                    enabled = touchCount < 10,
                    text = { Text(stringResource(R.string.action_combine_with_new_swipe)) },
                    onClick = {
                        menuExpanded = false
                        onCombineWithNewSwipe?.invoke()
                    },
                )
                DropdownMenuItem(
                    enabled = touchCount < 10,
                    text = { Text(stringResource(R.string.action_combine_with_new_click)) },
                    onClick = {
                        menuExpanded = false
                        onCombineWithNewClick?.invoke()
                    },
                )
                val others = combinableActions.filter { it.action.id != details.action.id && touchCount + it.subUiActions.size.coerceAtLeast(1) <= 10 }
                others.forEach { other ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_combine_with, other.name)) },
                        onClick = {
                            menuExpanded = false
                            onCombineWithAction?.invoke(other.action)
                        },
                    )
                }
            }
        }
    }
}

@Composable private fun SmartActionPortrait(details: UiAction) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(start = 16.dp)) { BriefText(details.name, 17, true, 1); BriefText(details.description, 14, false, 1) }
        BriefIcon(details, Modifier.padding(end = 16.dp))
    }
}
@Composable private fun SmartActionLandscape(details: UiAction) {
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BriefText(details.name, 17, true, 2, TextAlign.Center); Spacer(Modifier.height(8.dp)); BriefText(details.description, 14, false, 2, TextAlign.Center); Spacer(Modifier.height(8.dp)); BriefIcon(details)
    }
}
@Composable private fun BriefText(text: String, size: Int, title: Boolean, lines: Int, align: TextAlign? = null) = Text(text, fontSize = size.sp, fontWeight = if (title) FontWeight.Bold else FontWeight.Normal, fontStyle = if (title) FontStyle.Normal else FontStyle.Italic, maxLines = lines, overflow = TextOverflow.Ellipsis, textAlign = align)
@Composable private fun BriefIcon(details: UiAction, modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp)) {
        Icon(
            painter = painterResource(details.icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        if (details.haveError) Box(Modifier.align(Alignment.TopEnd).size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
    }
}
