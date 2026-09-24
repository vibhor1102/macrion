/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.core.ui.R as UiR

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
internal fun EventListRow(
    name: String,
    conditionsCount: String,
    actionsCount: String,
    @StringRes enabledTextRes: Int,
    @DrawableRes enabledIconRes: Int,
    conditionIconRes: Int,
    actionsInError: Boolean,
    showReorderHandle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reorderHandleModifier: Modifier = Modifier,
    handleInteractionSource: MutableInteractionSource? = null,
    isBeingDragged: Boolean = false,
    dragFolderFeedback: String? = null,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    isInFolder: Boolean = false,
    alignDetailsToEnd: Boolean = false,
) {
    val rowBackground by animateColorAsState(
        if (isBeingDragged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else if (isInFolder) MaterialTheme.colorScheme.surfaceContainerLow
        else Color.Transparent,
        label = "event_row_drag_bg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(rowBackground)
            .then(
                if (accessibilityActions.isEmpty()) Modifier
                else Modifier.semantics { customActions = accessibilityActions },
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showReorderHandle) {
            DragHandle(
                modifier = reorderHandleModifier,
                isBeingDragged = isBeingDragged,
                interactionSource = handleInteractionSource ?: remember { MutableInteractionSource() },
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 4.dp),
        ) {
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isBeingDragged) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Surface(
                        shape = CircleShape,
                        color = if (dragFolderFeedback != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (dragFolderFeedback != null) UiR.drawable.ic_folder else UiR.drawable.ic_move
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (dragFolderFeedback != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = dragFolderFeedback ?: androidx.compose.ui.res.stringResource(R.string.folder_ungrouped_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dragFolderFeedback != null) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    EventDetail(
                        iconRes = enabledIconRes,
                        text = androidx.compose.ui.res.stringResource(enabledTextRes),
                        tint = null,
                        modifier = Modifier.weight(1f),
                    )
                    if (alignDetailsToEnd) Spacer(Modifier.width(16.dp))
                    EventDetail(
                        iconRes = R.drawable.ic_click,
                        text = actionsCount,
                        tint = if (actionsInError) MaterialTheme.colorScheme.error else null,
                        modifier = if (alignDetailsToEnd) Modifier else Modifier.weight(1f),
                    )
                    if (alignDetailsToEnd) Spacer(Modifier.width(16.dp))
                    EventDetail(
                        iconRes = conditionIconRes,
                        text = conditionsCount,
                        tint = null,
                        modifier = if (alignDetailsToEnd) Modifier else Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
    }
}

/** Gives the active drag a visible state without reducing its 48dp touch target. */
@Composable
private fun DragHandle(
    modifier: Modifier,
    isBeingDragged: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isPressed || isBeingDragged

    val handleTint by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 100),
        label = "handle_tint",
    )
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "handle_container",
    )

    IconButton(
        onClick = {},
        modifier = modifier.size(48.dp),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = handleTint,
        ),
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_drag_indicator),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EventDetail(
    @DrawableRes iconRes: Int,
    text: String,
    tint: Color?,
    modifier: Modifier = Modifier,
) {
    val resolvedTint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = resolvedTint,
        )
        Text(text = text, color = resolvedTint, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
