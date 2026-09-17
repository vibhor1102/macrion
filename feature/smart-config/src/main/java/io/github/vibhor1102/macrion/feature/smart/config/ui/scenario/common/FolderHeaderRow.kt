/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.feature.smart.config.R

@Composable
internal fun FolderHeaderRow(
    name: String,
    eventCount: Int,
    enabledCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddEventClick: () -> Unit,
    modifier: Modifier = Modifier,
    reorderHandleModifier: Modifier = Modifier,
    handleInteractionSource: MutableInteractionSource? = null,
    isBeingDragged: Boolean = false,
    showReorderHandle: Boolean = !isExpanded,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation",
    )
    val rowBackground by animateColorAsState(
        if (isBeingDragged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "folder_row_bg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(rowBackground)
            .clickable(onClick = onToggleExpand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showReorderHandle) {
            FolderDragHandle(
                modifier = reorderHandleModifier,
                isBeingDragged = isBeingDragged,
                interactionSource = handleInteractionSource ?: remember { MutableInteractionSource() },
            )
        } else {
            Spacer(Modifier.width(44.dp))
        }

        Icon(
            painter = painterResource(UiR.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(chevronRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(UiR.drawable.ic_folder),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.width(8.dp))

        // Badge showing event counts
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text(
                text = if (enabledCount == eventCount) "$eventCount" else "$enabledCount/$eventCount",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_more),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_action_rename)) },
                    onClick = {
                        menuExpanded = false
                        onRenameClick()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_write),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_action_add_event)) },
                    onClick = {
                        menuExpanded = false
                        onAddEventClick()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_action_delete)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(UiR.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun FolderDragHandle(
    modifier: Modifier,
    isBeingDragged: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isPressed || isBeingDragged

    val handleTint by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 100),
        label = "folder_handle_tint",
    )
    val containerColor by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "folder_handle_container",
    )

    IconButton(
        onClick = {},
        modifier = modifier.size(44.dp),
        interactionSource = interactionSource,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = handleTint,
        ),
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_drag_indicator),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun FolderEndBoundaryRow(
    folderName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
        Text(
            text = folderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}
