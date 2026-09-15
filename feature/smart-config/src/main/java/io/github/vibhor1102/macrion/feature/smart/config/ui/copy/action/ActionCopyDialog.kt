/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.copy.action

import android.view.ViewGroup
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.CopyListItem
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.CopyPickerContent
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.CopySectionHeader
import io.github.vibhor1102.macrion.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

class ActionCopyDialog(
    private val onActionsCopied: (List<Action>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.ACTION_COPY.name
    private val viewModel: ActionCopyViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { actionCopyViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ActionCopyDialog.Content() } }
    }
@Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        var query by rememberSaveable { mutableStateOf("") }
        val list = state?.items.orEmpty()
        CopyPickerContent(
            title = context.getString(R.string.dialog_overlay_title_copy_from),
            searchHint = context.getString(R.string.search_view_hint_action_copy),
            emptyMessage = context.getString(R.string.message_empty_copy),
            query = query,
            loading = state == null,
            empty = state != null && list.isEmpty(),
            copyEnabled = list.any { it is ActionCopyItem.ActionItem },
            onQueryChanged = { query = it; viewModel.updateSearchQuery(it) },
            onDismiss = ::back,
            onCopy = ::onCopyClicked,
        ) {
            itemsIndexed(list, key = { index, item ->
                when (item) {
                    is ActionCopyItem.HeaderItem -> "header:${item.title}:$index"
                    is ActionCopyItem.ActionItem -> item.uiAction.action.id.let { "action:${it.databaseId}:${it.tempId ?: ""}" }
                }
            }) { index, item ->
                when (item) {
                    is ActionCopyItem.HeaderItem -> CopySectionHeader(context.getString(item.title))
                    is ActionCopyItem.ActionItem -> CopyListItem(
                        icon = item.uiAction.icon,
                        title = item.uiAction.name,
                        description = item.uiAction.description,
                        checked = item.checked,
                        error = item.uiAction.haveError,
                        onClick = { viewModel.toggleCheckedForCopy(item.uiAction.action, index) },
                    )
                }
            }
        }
    }

    private fun onCopyClicked() {
        val actions = viewModel.getActionsCopy()
        if (viewModel.actionCopyShouldWarnUser(actions)) {
            val args = viewModel.getFixEventDialogArgument(actions) ?: return
            overlayManager.navigateTo(context, FixEventChildrenCopyDialog(args) { notifySelectionAndDestroy(it.actions) }, false)
        } else notifySelectionAndDestroy(actions)
    }

    private fun notifySelectionAndDestroy(actions: List<Action>) {
        viewModel.saveCopyActions(actions)
        back()
        onActionsCopied(actions)
    }
}
