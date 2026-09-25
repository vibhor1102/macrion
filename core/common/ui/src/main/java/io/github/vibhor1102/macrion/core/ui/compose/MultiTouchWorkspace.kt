/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R
import kotlinx.coroutines.launch

data class MultiTouchWorkspaceItem(
    val key: String,
    val name: String,
    val typeLabel: String,
    @DrawableRes val icon: Int,
    val isComplete: Boolean,
)

/** A bottom sheet in portrait and an editor that uses the whole available height in landscape. */
@Composable
fun MultiTouchEditorFrame(
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val editorHeight = if (landscape || maxHeight < 560.dp) maxHeight
        else minOf(700.dp, maxHeight * 0.92f)
        Surface(
            modifier = if (expanded) Modifier.fillMaxWidth().height(editorHeight)
                else Modifier.fillMaxWidth().heightIn(max = maxHeight),
            shape = if (landscape && expanded) RectangleShape else OverlayDialogShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            MacrionDialogSurface {
                Column(if (expanded) Modifier.fillMaxSize() else Modifier.fillMaxWidth(), content = content)
            }
        }
    }
}

/** Child navigation stays fixed while all visible child forms share one scrolling workspace. */
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
    childContent: @Composable (index: Int, renaming: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var firstVisibleIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingNewIndex by rememberSaveable { mutableIntStateOf(-1) }
    var renamingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val stateHolder = rememberSaveableStateHolder()
    val scrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val widthBasedCount = ((maxWidth.value - 48f) / (320f * fontScale)).toInt()
            .coerceIn(1, 3).coerceAtMost(items.size)
        // The keyboard leaves too little height for a useful two-column form on a phone.
        val paneCount = if (maxHeight < 300.dp) 1 else widthBasedCount
        val allVisible = paneCount == items.size && paneCount > 1
        val nameMaxWidth = minOf(200.dp, maxOf(96.dp,
            (if (allVisible) (maxWidth.value - 48f) / paneCount - 112f
                else maxWidth.value - 156f).dp))
        val currentSelection = (pendingNewIndex.takeIf { it in items.indices } ?: selectedIndex)
            .coerceIn(items.indices)
        val lastStart = (items.size - paneCount).coerceAtLeast(0)
        val savedStart = firstVisibleIndex.coerceIn(0, lastStart)
        val visibleStart = when {
            currentSelection < savedStart -> currentSelection
            currentSelection >= savedStart + paneCount -> currentSelection - paneCount + 1
            else -> savedStart
        }.coerceIn(0, lastStart)
        val visibleIndices = visibleStart until visibleStart + paneCount
        val names = items.map { item ->
            val numberedDefault = item.name.removePrefix("${item.typeLabel} ")
                .takeIf { item.name.startsWith("${item.typeLabel} ") }
                ?.toIntOrNull()?.let { it in 1..10 } == true
            item.name.takeIf { it.isNotBlank() && !numberedDefault } ?: item.typeLabel
        }

        LaunchedEffect(items.map { it.key }, paneCount, selectedIndex, pendingNewIndex) {
            if (selectedIndex != currentSelection) selectedIndex = currentSelection
            if (pendingNewIndex in items.indices) pendingNewIndex = -1
            if (firstVisibleIndex != visibleStart) firstVisibleIndex = visibleStart
            if (renamingKey != null && items.none { it.key == renamingKey }) renamingKey = null
        }
        LaunchedEffect(currentSelection, allVisible) {
            if (!allVisible && listState.layoutInfo.visibleItemsInfo.none { it.index == currentSelection }) {
                listState.animateScrollToItem(currentSelection)
            }
        }

        fun select(index: Int) {
            pendingNewIndex = -1
            if (index != currentSelection) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            selectedIndex = index
            if (index !in visibleIndices) scope.launch { scrollState.scrollTo(0) }
        }

        fun add(onAdd: () -> Int?) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onAdd()?.let { newIndex ->
                pendingNewIndex = newIndex
                selectedIndex = newIndex
                scope.launch { scrollState.scrollTo(0) }
            }
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (allVisible) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items.forEachIndexed { index, item ->
                            TouchHeading(
                                item = item, name = names[index], index = index, count = items.size,
                                duplicated = names.count { it == names[index] } > 1,
                                selected = index == currentSelection, showActions = true,
                                nameMaxWidth = nameMaxWidth,
                                canDelete = canDeleteChild, onSelect = { select(index) },
                                onRename = {
                                    select(index)
                                    if (renamingKey == item.key) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                    renamingKey = item.key.takeUnless { renamingKey == item.key }
                                    scope.launch { scrollState.scrollTo(0) }
                                },
                                onDelete = {
                                    renamingKey = null
                                    selectedIndex = when {
                                        currentSelection > index -> currentSelection - 1
                                        currentSelection == index -> index.coerceAtMost(items.lastIndex - 1)
                                        else -> currentSelection
                                    }
                                    onDeleteChild(item.key)
                                },
                                renaming = renamingKey == item.key,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                            TouchHeading(
                                item = item, name = names[index], index = index, count = items.size,
                                duplicated = names.count { it == names[index] } > 1,
                                selected = index == currentSelection,
                                showActions = index == currentSelection,
                                nameMaxWidth = nameMaxWidth,
                                canDelete = canDeleteChild, onSelect = { select(index) },
                                onRename = {
                                    select(index)
                                    if (renamingKey == item.key) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                    renamingKey = item.key.takeUnless { renamingKey == item.key }
                                    scope.launch { scrollState.scrollTo(0) }
                                },
                                onDelete = {
                                    renamingKey = null
                                    selectedIndex = when {
                                        currentSelection > index -> currentSelection - 1
                                        currentSelection == index -> index.coerceAtMost(items.lastIndex - 1)
                                        else -> currentSelection
                                    }
                                    onDeleteChild(item.key)
                                },
                                renaming = renamingKey == item.key,
                            )
                        }
                    }
                }
                AddTouchMenu(
                    enabled = items.size < 10,
                    addClickLabel = addClickLabel,
                    addSwipeLabel = addSwipeLabel,
                    onAddClick = { add(onAddClick) },
                    onAddSwipe = { add(onAddSwipe) },
                )
            }

            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)
                    .padding(start = 12.dp, end = if (allVisible) 60.dp else 12.dp, bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    visibleIndices.forEach { index ->
                        val item = items[index]
                        Column(
                            Modifier.weight(1f).pointerInput(item.key) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    pendingNewIndex = -1
                                    selectedIndex = index
                                }
                            },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (paneCount > 1 && !allVisible) {
                                Text(names[index], style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            stateHolder.SaveableStateProvider(item.key) {
                                childContent(index, renamingKey == item.key)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchHeading(
    item: MultiTouchWorkspaceItem,
    name: String,
    index: Int,
    count: Int,
    duplicated: Boolean,
    selected: Boolean,
    showActions: Boolean,
    nameMaxWidth: Dp,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    renaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.multi_touch_touch_description, index + 1, count, name)
    val incomplete = stringResource(R.string.multi_touch_brief_needs_setup)
    Row(modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.widthIn(max = nameMaxWidth).heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .selectable(selected = selected, role = Role.Button, onClick = onSelect)
                .semantics { contentDescription = if (item.isComplete) description else "$description, $incomplete" }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(item.icon), null, Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (duplicated) {
                Surface(shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (!item.isComplete) {
                Icon(painterResource(R.drawable.ic_warning), null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
        if (showActions) {
            IconButton(onClick = onRename, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(if (renaming) R.drawable.ic_check else R.drawable.ic_edit),
                    stringResource(if (renaming) R.string.multi_touch_done_renaming
                        else R.string.multi_touch_rename_touch, index + 1),
                    Modifier.size(20.dp))
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_delete),
                        stringResource(R.string.action_editor_delete_touch, index + 1),
                        Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AddTouchMenu(
    enabled: Boolean,
    addClickLabel: String,
    addSwipeLabel: String,
    onAddClick: () -> Unit,
    onAddSwipe: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.multi_touch_add_touch))
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            DropdownMenuItem(text = { Text(addClickLabel) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_click), null) },
                onClick = { expanded = false; onAddClick() })
            DropdownMenuItem(text = { Text(addSwipeLabel) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_swipe), null) },
                onClick = { expanded = false; onAddSwipe() })
        }
    }
}
