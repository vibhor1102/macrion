/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.core.ui.compose

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
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

data class MultiTouchWorkspaceItem(
    val key: String,
    val name: String,
    val typeLabel: String,
    val isComplete: Boolean,
    @StringRes val deleteLabelRes: Int,
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

/** Child navigation stays fixed while the selected child uses the editor area. */
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
    childContent: @Composable (index: Int) -> Unit,
) {
    if (items.isEmpty()) return
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingNewIndex by rememberSaveable { mutableIntStateOf(-1) }
    val stateHolder = rememberSaveableStateHolder()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val nameMaxWidth = minOf(200.dp, maxOf(96.dp, maxWidth - 156.dp))
        val currentSelection = (pendingNewIndex.takeIf { it in items.indices } ?: selectedIndex)
            .coerceIn(items.indices)
        val names = items.map { item ->
            val numberedDefault = item.name.removePrefix("${item.typeLabel} ")
                .takeIf { item.name.startsWith("${item.typeLabel} ") }
                ?.toIntOrNull()?.let { it in 1..10 } == true
            item.name.takeIf { it.isNotBlank() && !numberedDefault } ?: item.typeLabel
        }

        LaunchedEffect(items.map { it.key }, selectedIndex, pendingNewIndex) {
            if (selectedIndex != currentSelection) selectedIndex = currentSelection
            if (pendingNewIndex in items.indices) pendingNewIndex = -1
        }
        LaunchedEffect(currentSelection) {
            listState.animateScrollToItem(currentSelection)
        }

        fun select(index: Int) {
            pendingNewIndex = -1
            if (index != currentSelection) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            selectedIndex = index
        }

        fun add(onAdd: () -> Int?) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onAdd()?.let { newIndex ->
                pendingNewIndex = newIndex
                selectedIndex = newIndex
            }
        }

        fun delete(index: Int) {
            focusManager.clearFocus()
            keyboardController?.hide()
            pendingNewIndex = -1
            selectedIndex = when {
                currentSelection > index -> currentSelection - 1
                currentSelection == index -> index.coerceAtMost(items.lastIndex - 1)
                else -> currentSelection
            }
            onDeleteChild(items[index].key)
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                            TouchHeading(
                                item = item, name = names[index], index = index, count = items.size,
                                selected = index == currentSelection,
                                nameMaxWidth = nameMaxWidth,
                                onSelect = { select(index) },
                            )
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

            Box(Modifier.fillMaxWidth().weight(1f).imePadding().padding(horizontal = 12.dp),
                contentAlignment = Alignment.TopCenter) {
                val item = items[currentSelection]
                stateHolder.SaveableStateProvider(item.key) {
                    Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight()
                        .verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                        childContent(currentSelection)
                        if (canDeleteChild) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { delete(currentSelection) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error)) {
                                    Icon(painterResource(R.drawable.ic_delete), null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(stringResource(item.deleteLabelRes))
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
private fun TouchHeading(
    item: MultiTouchWorkspaceItem,
    name: String,
    index: Int,
    count: Int,
    selected: Boolean,
    nameMaxWidth: Dp,
    onSelect: () -> Unit,
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
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    Text((index + 1).toString(), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (!item.isComplete) {
                Icon(painterResource(R.drawable.ic_warning), null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error)
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
