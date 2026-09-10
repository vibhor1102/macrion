/* Copyright (C) 2025 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.scenarios.list.adapter

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.settings.domain.model.ScenarioSortType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState

class SortViewHolder(
    parent: ViewGroup,
    private val onSortTypeClicked: (ScenarioSortType) -> Unit,
    private val onSmartChipClicked: (Boolean) -> Unit,
    private val onDumbChipClicked: (Boolean) -> Unit,
    private val onSortOrderClicked: (Boolean) -> Unit,
) : RecyclerView.ViewHolder(ComposeView(parent.context)) {
    private var config by mutableStateOf<ScenarioListUiState.Item.SortItem?>(null)

    init {
        (itemView as ComposeView).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent { MacrionTheme { config?.let { SortControls(it) } } }
        }
    }

    fun onBind(value: ScenarioListUiState.Item.SortItem) { config = value }
    fun onUnbind() = Unit

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SortControls(state: ScenarioListUiState.Item.SortItem) {
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (landscape) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderingButtons(state, Modifier.weight(1f).padding(end = 8.dp))
                FilterControls(state)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OrderingButtons(state, Modifier.fillMaxWidth().padding(horizontal = 32.dp))
                FilterControls(state, Modifier.padding(top = 8.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OrderingButtons(state: ScenarioListUiState.Item.SortItem, modifier: Modifier) {
        Box(modifier, contentAlignment = Alignment.Center) {
            SingleChoiceSegmentedButtonRow {
                OrderingButton(
                    R.drawable.ic_sort_name,
                    R.string.item_scenario_filter_by_name,
                    state.sortType == ScenarioSortType.NAME,
                    0,
                    { onSortTypeClicked(ScenarioSortType.NAME) },
                )
                OrderingButton(
                    R.drawable.ic_sort_recent,
                    R.string.item_scenario_filter_by_recent,
                    state.sortType == ScenarioSortType.RECENT,
                    1,
                    { onSortTypeClicked(ScenarioSortType.RECENT) },
                )
                OrderingButton(
                    R.drawable.ic_most_used,
                    R.string.item_scenario_filter_by_most_used,
                    state.sortType == ScenarioSortType.MOST_USED,
                    2,
                    { onSortTypeClicked(ScenarioSortType.MOST_USED) },
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SingleChoiceSegmentedButtonRowScope.OrderingButton(
        icon: Int,
        label: Int,
        selected: Boolean,
        index: Int,
        onClick: () -> Unit,
    ) {
        SegmentedButton(
            selected = selected,
            onClick = onClick,
            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
            icon = {},
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, Modifier.size(18.dp))
                Text(stringResource(label), Modifier.padding(start = 8.dp), maxLines = 1)
            }
        }
    }

    @Composable
    private fun FilterControls(state: ScenarioListUiState.Item.SortItem, modifier: Modifier = Modifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.smartVisible,
                onClick = { onSmartChipClicked(!state.smartVisible) },
                label = { Text(stringResource(R.string.item_title_smart_scenario)) },
                leadingIcon = {
                    Icon(
                        painterResource(if (state.smartVisible) R.drawable.ic_confirm else R.drawable.ic_smart),
                        null,
                        Modifier.size(18.dp),
                    )
                },
            )
            FilterChip(
                selected = state.dumbVisible,
                onClick = { onDumbChipClicked(!state.dumbVisible) },
                label = { Text(stringResource(R.string.item_title_dumb_scenario)) },
                leadingIcon = {
                    Icon(
                        painterResource(if (state.dumbVisible) R.drawable.ic_confirm else R.drawable.ic_dumb),
                        null,
                        Modifier.size(18.dp),
                    )
                },
            )
            OutlinedIconToggleButton(
                checked = state.changeOrderChecked,
                onCheckedChange = onSortOrderClicked,
                colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(
                    painterResource(R.drawable.ic_sort_order),
                    null,
                    tint = if (state.changeOrderChecked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

/** Compose-native counterpart of [SortViewHolder], used by the scenario screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScenarioSortControls(
    state: ScenarioListUiState.Item.SortItem,
    onSortTypeClicked: (ScenarioSortType) -> Unit,
    onSmartChipClicked: (Boolean) -> Unit,
    onDumbChipClicked: (Boolean) -> Unit,
    onSortOrderClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val ordering: @Composable (Modifier) -> Unit = { orderingModifier ->
        Box(orderingModifier, contentAlignment = Alignment.Center) {
            SingleChoiceSegmentedButtonRow {
                listOf(
                    Triple(R.drawable.ic_sort_name, R.string.item_scenario_filter_by_name, ScenarioSortType.NAME),
                    Triple(R.drawable.ic_sort_recent, R.string.item_scenario_filter_by_recent, ScenarioSortType.RECENT),
                    Triple(R.drawable.ic_most_used, R.string.item_scenario_filter_by_most_used, ScenarioSortType.MOST_USED),
                ).forEachIndexed { index, (icon, label, type) ->
                    SegmentedButton(
                        selected = state.sortType == type,
                        onClick = { onSortTypeClicked(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        icon = {},
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(icon), null, Modifier.size(18.dp))
                            Text(stringResource(label), Modifier.padding(start = 8.dp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    val filters: @Composable (Modifier) -> Unit = { filterModifier ->
        Row(filterModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.smartVisible,
                onClick = { onSmartChipClicked(!state.smartVisible) },
                label = { Text(stringResource(R.string.item_title_smart_scenario)) },
                leadingIcon = { Icon(painterResource(if (state.smartVisible) R.drawable.ic_confirm else R.drawable.ic_smart), null, Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = state.dumbVisible,
                onClick = { onDumbChipClicked(!state.dumbVisible) },
                label = { Text(stringResource(R.string.item_title_dumb_scenario)) },
                leadingIcon = { Icon(painterResource(if (state.dumbVisible) R.drawable.ic_confirm else R.drawable.ic_dumb), null, Modifier.size(18.dp)) },
            )
            OutlinedIconToggleButton(
                checked = state.changeOrderChecked,
                onCheckedChange = onSortOrderClicked,
                colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(
                    painterResource(R.drawable.ic_sort_order),
                    contentDescription = null,
                    tint = if (state.changeOrderChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    if (landscape) {
        Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ordering(Modifier.weight(1f).padding(end = 8.dp))
            filters(Modifier)
        }
    } else {
        Column(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ordering(Modifier.fillMaxWidth().padding(horizontal = 32.dp))
            filters(Modifier.padding(top = 8.dp))
        }
    }
}
