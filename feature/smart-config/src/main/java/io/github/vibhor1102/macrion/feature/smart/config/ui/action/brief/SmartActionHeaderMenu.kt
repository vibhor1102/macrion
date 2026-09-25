/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.ui.compose.ActionEditorMenuItem
import io.github.vibhor1102.macrion.core.ui.compose.ActionEditorOverflowMenu
import io.github.vibhor1102.macrion.core.ui.compose.ExistingActionOption
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.getIconRes

@Composable
internal fun SmartActionHeaderMenu(
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

internal fun List<Action>.toPickerOptions(context: Context): List<ExistingActionOption<Action>> = map { action ->
    val type = when (action) {
        is Click -> context.getString(R.string.item_click_title)
        is Swipe -> context.getString(R.string.item_swipe_title)
        is SplitAction -> context.getString(R.string.combined_touch_count, action.subActions.size)
        else -> ""
    }
    ExistingActionOption(
        value = action,
        title = action.name?.takeIf { it.isNotBlank() } ?: type,
        subtitle = type,
        icon = action.getIconRes(),
    )
}
