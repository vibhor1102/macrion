/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.compose.ActionEditorMenuItem
import io.github.vibhor1102.macrion.core.ui.compose.ActionEditorOverflowMenu
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionOption
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails

class DumbCombinationOptions(
    val existingActions: List<DumbActionDetails>,
    val onNewClick: (DumbAction) -> Unit,
    val onNewSwipe: (DumbAction) -> Unit,
    val onExisting: (DumbAction, DumbAction) -> Unit,
)

@Composable
internal fun DumbActionHeaderMenu(
    showNewOptions: Boolean,
    canAdd: Boolean,
    canCombineExisting: Boolean,
    canUnsplit: Boolean = false,
    onNewClick: () -> Unit = {},
    onNewSwipe: () -> Unit = {},
    onExisting: () -> Unit = {},
    onUnsplit: () -> Unit = {},
) {
    val items = buildList {
        if (showNewOptions && canAdd) {
            add(ActionEditorMenuItem(stringResource(R.string.action_combine_with_new_click), onNewClick))
            add(ActionEditorMenuItem(stringResource(R.string.action_combine_with_new_swipe), onNewSwipe))
        }
        if (canCombineExisting) {
            add(ActionEditorMenuItem(stringResource(R.string.action_combine_with_existing), onExisting))
        }
        if (canUnsplit) {
            add(ActionEditorMenuItem(stringResource(R.string.action_unsplit), onUnsplit))
        }
    }
    ActionEditorOverflowMenu(items)
}

internal fun List<DumbActionDetails>.toPickerOptions(): List<ExistingActionOption<DumbAction>> = map { details ->
    ExistingActionOption(
        value = details.action,
        title = details.name,
        subtitle = details.detailsText,
        icon = details.icon,
    )
}
