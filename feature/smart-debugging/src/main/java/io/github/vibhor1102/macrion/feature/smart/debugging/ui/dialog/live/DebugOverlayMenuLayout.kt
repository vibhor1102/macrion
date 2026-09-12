/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.live

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R

internal fun createDebugOverlayMenu(
    context: Context,
    contentWidthDp: Int,
    contentHeightDp: Int,
    content: @Composable () -> Unit,
): ViewGroup {
    return createOverlayMenuLayout(
        context = context,
        buttons = listOf(
            OverlayMenuButton(R.id.btn_back, R.drawable.ic_back, R.string.content_desc_go_back),
            OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on, R.string.content_desc_go_back),
            OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
        ),
        content = { MacrionTheme { content() } },
        contentWidthDp = contentWidthDp,
        contentHeightDp = contentHeightDp,
    )
}
