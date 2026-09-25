/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R

data class MultiTouchWorkspaceItem(
    val key: String,
    val typeLabel: String,
    @DrawableRes val icon: Int,
    val isComplete: Boolean,
)

/** A touch selector and one or more full child editors in the space actually available. */
@Composable
fun MultiTouchWorkspace(
    items: List<MultiTouchWorkspaceItem>,
    canDeleteChild: Boolean,
    addClickLabel: String,
    addSwipeLabel: String,
    onAddClick: () -> Int?,
    onAddSwipe: () -> Int?,
    onDeleteChild: (String) -> Unit,
    modifier: Modifier = Modifier,
    childContent: @Composable (Int) -> Unit,
) {
    if (items.isEmpty()) return
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var firstVisibleIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingNewIndex by rememberSaveable { mutableIntStateOf(-1) }
    val stateHolder = rememberSaveableStateHolder()
    val addEnabled = items.size < 10

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        // The pane minimum is driven by the width of the existing name and duration fields.
        // A narrow landscape window may still need a single pane.
        val paneCount = (maxWidth.value / (340f * fontScale)).toInt()
            .coerceIn(1, 3).coerceAtMost(items.size)
        val actionsBesideTabs = maxWidth.value >= 720f * fontScale
        val stackAddButtons = maxWidth.value < 328f * fontScale
        val currentSelection = (pendingNewIndex.takeIf { it in items.indices } ?: selectedIndex)
            .coerceIn(items.indices)
        val lastStart = (items.size - paneCount).coerceAtLeast(0)
        val savedStart = firstVisibleIndex.coerceIn(0, lastStart)
        val visibleStart = when {
            currentSelection < savedStart -> currentSelection
            currentSelection >= savedStart + paneCount -> currentSelection - paneCount + 1
            else -> savedStart
        }.coerceIn(0, lastStart)

        LaunchedEffect(items.size, paneCount, selectedIndex, pendingNewIndex) {
            if (selectedIndex != currentSelection) selectedIndex = currentSelection
            if (pendingNewIndex in items.indices) pendingNewIndex = -1
            if (firstVisibleIndex != visibleStart) firstVisibleIndex = visibleStart
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (actionsBesideTabs) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TouchTabs(items, currentSelection, {
                        pendingNewIndex = -1
                        selectedIndex = it
                    }, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    AddTouchButtons(addClickLabel, addSwipeLabel, addEnabled,
                        onAddClick = { onAddClick()?.let { pendingNewIndex = it } },
                        onAddSwipe = { onAddSwipe()?.let { pendingNewIndex = it } })
                }
            } else {
                TouchTabs(items, currentSelection, {
                    pendingNewIndex = -1
                    selectedIndex = it
                }, Modifier.fillMaxWidth())
                AddTouchButtons(addClickLabel, addSwipeLabel, addEnabled,
                    onAddClick = { onAddClick()?.let { pendingNewIndex = it } },
                    onAddSwipe = { onAddSwipe()?.let { pendingNewIndex = it } },
                    modifier = Modifier.fillMaxWidth(), fillWidth = true,
                    stacked = stackAddButtons)
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(max = 490.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                (visibleStart until visibleStart + paneCount).forEach { index ->
                    val item = items[index]
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(index) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                pendingNewIndex = -1
                                selectedIndex = index
                            }
                        },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            if (index == currentSelection) 2.dp else 1.dp,
                            if (index == currentSelection) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        stateHolder.SaveableStateProvider(item.key) {
                            Column(
                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(item.icon), null, Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${index + 1}  ${item.typeLabel}",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    if (canDeleteChild) {
                                        IconButton(onClick = {
                                            pendingNewIndex = -1
                                            selectedIndex = index.coerceAtMost(items.lastIndex - 1)
                                            onDeleteChild(item.key)
                                        }) {
                                            Icon(painterResource(R.drawable.ic_delete),
                                                stringResource(R.string.action_editor_delete_touch, index + 1),
                                                Modifier.size(20.dp))
                                        }
                                    }
                                }
                                childContent(index)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchTabs(
    items: List<MultiTouchWorkspaceItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEachIndexed { index, item ->
            val label = "${index + 1} ${item.typeLabel}"
            val description = if (item.isComplete) label
            else "$label, ${stringResource(R.string.multi_touch_brief_needs_setup)}"
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.semantics { contentDescription = description },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, maxLines = 1)
                        if (!item.isComplete) {
                            Spacer(Modifier.width(4.dp))
                            Icon(painterResource(R.drawable.ic_warning), null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AddTouchButtons(
    addClickLabel: String,
    addSwipeLabel: String,
    enabled: Boolean,
    onAddClick: () -> Unit,
    onAddSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    stacked: Boolean = false,
) {
    if (stacked) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AddTouchButton(addClickLabel, enabled, onAddClick, Modifier.fillMaxWidth())
            AddTouchButton(addSwipeLabel, enabled, onAddSwipe, Modifier.fillMaxWidth())
        }
        return
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AddTouchButton(addClickLabel, enabled, onAddClick,
            if (fillWidth) Modifier.weight(1f) else Modifier)
        AddTouchButton(addSwipeLabel, enabled, onAddSwipe,
            if (fillWidth) Modifier.weight(1f) else Modifier)
    }
}

@Composable
private fun AddTouchButton(label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp)) {
        Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, maxLines = 1)
    }
}
