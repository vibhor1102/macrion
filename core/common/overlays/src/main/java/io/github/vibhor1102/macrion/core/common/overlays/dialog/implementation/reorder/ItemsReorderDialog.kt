/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.reorder

import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.core.ui.R as UiR
import kotlinx.coroutines.flow.Flow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class ReorderItemDescriptor(
    val title: String,
    val subtitle: String? = null,
    val trailingContent: @Composable () -> Unit = {},
)

class ItemsReorderDialog(
    @StyleRes theme: Int,
    @StringRes private val titleRes: Int,
    private val itemsFlow: Flow<List<ItemBrief>>,
    private val itemDescriptor: (ItemBrief) -> ReorderItemDescriptor,
    private val onReorder: (from: Int, to: Int) -> Unit,
) : OverlayDialog(theme) {

    override fun onCreateView(): ViewGroup {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MacrionTheme {
                    this@ItemsReorderDialog.ReorderDialogContent()
                }
            }
        }
    }

    @Composable
    private fun ReorderDialogContent() {
        val sourceItems by itemsFlow.collectAsState(initial = null)
        var displayedItems by remember { mutableStateOf<List<ItemBrief>>(emptyList()) }
        var isReordering by remember { mutableStateOf(false) }
        var dragStartIndex by remember { mutableIntStateOf(-1) }
        val haptic = LocalHapticFeedback.current

        LaunchedEffect(sourceItems) {
            if (!isReordering && sourceItems != null) {
                displayedItems = sourceItems.orEmpty()
            }
        }

        val currentItems = if (displayedItems.isNotEmpty() || isReordering) displayedItems else sourceItems.orEmpty()

        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            displayedItems = currentItems.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(UiR.dimen.dialog_top_bar_height)),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = ::back) {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_cancel),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = stringResource(titleRes),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        sourceItems == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        currentItems.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.dialog_reorder_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        else -> {
                            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                                itemsIndexed(
                                    items = currentItems,
                                    key = { _, item -> item.id.toString() },
                                ) { index, item ->
                                    val descriptor = itemDescriptor(item)
                                    ReorderableItem(reorderState, key = item.id.toString()) { isBeingDragged ->
                                        Column {
                                            ReorderItemRow(
                                                title = descriptor.title,
                                                subtitle = descriptor.subtitle,
                                                trailingContent = descriptor.trailingContent,
                                                isBeingDragged = isBeingDragged,
                                                reorderHandleModifier = Modifier.draggableHandle(
                                                    onDragStarted = {
                                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                        dragStartIndex = index
                                                        isReordering = true
                                                    },
                                                    onDragStopped = {
                                                        val start = dragStartIndex
                                                        val end = currentItems.indexOfFirst { it.id == item.id }
                                                        if (start >= 0 && end >= 0 && start != end) {
                                                            onReorder(start, end)
                                                        }
                                                        dragStartIndex = -1
                                                        isReordering = false
                                                    },
                                                    dragGestureDetector = DualDragGestureDetector,
                                                ),
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderItemRow(
    title: String,
    subtitle: String?,
    trailingContent: @Composable () -> Unit,
    isBeingDragged: Boolean,
    reorderHandleModifier: Modifier,
) {
    val rowBackground by animateColorAsState(
        if (isBeingDragged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        label = "reorder_row_drag_bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(rowBackground)
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragHandle(reorderHandleModifier, isBeingDragged)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailingContent()
    }
}

@Composable
private fun DragHandle(
    reorderHandleModifier: Modifier,
    isBeingDragged: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
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

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(containerColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = MaterialTheme.colorScheme.primary,
                    bounded = true,
                    radius = 24.dp,
                ),
                onClick = {},
            )
            .then(reorderHandleModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ic_drag_indicator),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = handleTint,
        )
    }
}
