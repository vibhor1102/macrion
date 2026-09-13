/*
 * Copyright (C) 2026 Vibhor Goel
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.vibhor1102.macrion.scenarios.list

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.scenarios.list.adapter.ScenarioListItem
import io.github.vibhor1102.macrion.scenarios.list.adapter.ScenarioSortControls
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState
import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeScenarioList(
    uiState: ScenarioListUiState?,
    searchQuery: String,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
    onSearchQueryChanged: (String) -> Unit,
    onSearchRequested: () -> Unit,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onImportExport: () -> Unit,
    onTutorials: () -> Unit,
    onSettings: () -> Unit,
    onCreate: () -> Unit,
    onLaunch: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    onExpand: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onExport: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onCopy: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onDelete: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    onSortTypeClicked: (io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortType) -> Unit,
    onSmartChipClicked: (Boolean) -> Unit,
    onDumbChipClicked: (Boolean) -> Unit,
    onSortOrderClicked: (Boolean) -> Unit,
) {
    val content = uiState?.listContent.orEmpty()
    val isSelection = uiState?.type == ScenarioListUiState.Type.SELECTION
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState?.type == ScenarioListUiState.Type.SEARCH) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.menu_item_title_search)) },
                            singleLine = true,
                        )
                    } else Text(stringResource(R.string.activity_scenario_title))
                },
                navigationIcon = {
                    if (uiState?.type == ScenarioListUiState.Type.SEARCH) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.menu_item_title_cancel),
                            )
                        }
                    }
                },
                actions = {
                    val menu = uiState?.menuUiState ?: return@TopAppBar
                    if (menu.cancelItemState.visible) ScenarioToolbarButton(R.drawable.ic_cancel, R.string.menu_item_title_cancel, menu.cancelItemState.enabled, onCancel)
                    if (menu.selectAllItemState.visible) ScenarioToolbarButton(R.drawable.ic_select_all, R.string.menu_item_title_select_all, menu.selectAllItemState.enabled, onSelectAll)
                    if (menu.importExportItemState.visible) ScenarioToolbarButton(R.drawable.ic_load, R.string.menu_item_title_import, menu.importExportItemState.enabled, onImportExport)
                    if (menu.searchItemState.visible) ScenarioToolbarButton(R.drawable.ic_search, R.string.menu_item_title_search, menu.searchItemState.enabled, onSearchRequested)
                    if (menu.tutorialsItemState.visible) ScenarioToolbarButton(R.drawable.ic_tutorials, R.string.menu_item_title_export, menu.tutorialsItemState.enabled, onTutorials)
                    if (menu.settingsItemState.visible) ScenarioToolbarButton(R.drawable.ic_settings_filled, R.string.menu_item_title_settings, menu.settingsItemState.enabled, onSettings)
                },
            )
        },
        floatingActionButton = {
            if (isSelection && content.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onCreate,
                ) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.content_desc_add_scenario)) }
            }
        },
    ) { padding ->
        when {
            uiState == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
            content.isEmpty() && isSelection -> EmptyScenarioList(Modifier.fillMaxSize().padding(padding), onCreate)
            isLandscape -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                content.forEach { item ->
                    item(key = item.scenarioKey(), span = { GridItemSpan(if (item is ScenarioListUiState.Item.SortItem || item is ScenarioListUiState.Item.ScenarioItem.Empty) maxLineSpan else 1) }) {
                        ScenarioListContentItem(item, bitmapProvider, onLaunch, onExpand, onExport, onCopy, onDelete, onSortTypeClicked, onSmartChipClicked, onDumbChipClicked, onSortOrderClicked, Modifier)
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                content.forEach { item ->
                    item(key = item.scenarioKey()) {
                        ScenarioListContentItem(item, bitmapProvider, onLaunch, onExpand, onExport, onCopy, onDelete, onSortTypeClicked, onSmartChipClicked, onDumbChipClicked, onSortOrderClicked, Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioToolbarButton(icon: Int, label: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(icon), stringResource(label), Modifier.size(24.dp))
    }
}

@Composable
private fun ScenarioListContentItem(
    item: ScenarioListUiState.Item,
    bitmapProvider: (ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?,
    onLaunch: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    onExpand: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onExport: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onCopy: (ScenarioListUiState.Item.ScenarioItem.Valid) -> Unit,
    onDelete: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    onSortTypeClicked: (io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortType) -> Unit,
    onSmartChipClicked: (Boolean) -> Unit,
    onDumbChipClicked: (Boolean) -> Unit,
    onSortOrderClicked: (Boolean) -> Unit,
    modifier: Modifier,
) = when (item) {
    is ScenarioListUiState.Item.SortItem -> ScenarioSortControls(item, onSortTypeClicked, onSmartChipClicked, onDumbChipClicked, onSortOrderClicked, modifier)
    is ScenarioListUiState.Item.ScenarioItem -> ScenarioListItem(item, bitmapProvider, onLaunch, onExpand, onExport, onCopy, onDelete, modifier)
}

@Composable
private fun EmptyScenarioList(modifier: Modifier, onCreate: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            stringResource(R.string.message_empty_scenario_list),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier.padding(top = 24.dp),
        ) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.content_desc_add_scenario)) }
    }
}

private fun ScenarioListUiState.Item.scenarioKey(): String = when (this) {
    is ScenarioListUiState.Item.SortItem -> "sort"
    is ScenarioListUiState.Item.ScenarioItem.Valid.Dumb -> "dumb-${getScenarioId()}"
    is ScenarioListUiState.Item.ScenarioItem.Valid.Smart -> "smart-${getScenarioId()}"
    is ScenarioListUiState.Item.ScenarioItem.Empty.Dumb -> "empty-dumb-${scenario.id.databaseId}"
    is ScenarioListUiState.Item.ScenarioItem.Empty.Smart -> "empty-smart-${scenario.id.databaseId}"
}
