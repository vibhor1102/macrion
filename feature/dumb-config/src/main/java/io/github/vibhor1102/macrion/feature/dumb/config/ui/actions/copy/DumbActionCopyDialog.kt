/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.CopyDialog
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.CopySearchTopBar
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.DumbActionListItem
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog


/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param onActionSelected the listener called when the user select an Action.
 */
class DumbActionCopyDialog(
    private val onActionSelected: (DumbAction) -> Unit,
) : CopyDialog(R.style.AppTheme) {

    /** View model for this content. */
    private val viewModel: DumbActionCopyModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbActionCopyModel() },
    )

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_dumb_action_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { CopyContent() } }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    override fun onCopyClicked() = Unit

    @Composable
    private fun CopyContent() {
        val items = viewModel.dumbActionList.collectAsStateWithLifecycle(null).value
        Column(
            Modifier.fillMaxSize()
                .heightIn(min = dimensionResource(io.github.vibhor1102.macrion.core.common.overlays.R.dimen.bottom_sheet_min_height))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            CopySearchTopBar(
                titleRes = titleRes,
                searchHintRes = searchHintRes,
                modifier = Modifier.fillMaxWidth().height(dimensionResource(UiR.dimen.dialog_top_bar_height)),
                onDismiss = { debounceUserInteraction { back() } },
                onQueryChanged = viewModel::updateSearchQuery,
                onCopy = ::onCopyClicked,
            )
            when {
                items == null -> androidx.compose.material3.CircularProgressIndicator(Modifier.weight(1f).padding(24.dp))
                items.isEmpty() -> androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(stringResource(emptyRes), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.headlineSmall)
                }
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(bottom = dimensionResource(io.github.vibhor1102.macrion.core.common.overlays.R.dimen.margin_vertical_default))) {
                    items(items, key = { item ->
                        when (item) {
                            is DumbActionCopyItem.HeaderItem -> "header-${item.title}"
                            is DumbActionCopyItem.DumbActionItem -> item.dumbActionDetails.action.id.databaseId.takeIf { it != 0L } ?: -requireNotNull(item.dumbActionDetails.action.id.tempId)
                        }
                    }) { item ->
                        when (item) {
                            is DumbActionCopyItem.HeaderItem -> CopyHeader(item)
                            is DumbActionCopyItem.DumbActionItem -> {
                                DumbActionListItem(item.dumbActionDetails, showHandle = false) { select(item) }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CopyHeader(item: DumbActionCopyItem.HeaderItem) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(item.title), Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }

    private fun select(item: DumbActionCopyItem.DumbActionItem) = debounceUserInteraction {
        back()
        onActionSelected(item.dumbActionDetails.action)
    }
}
