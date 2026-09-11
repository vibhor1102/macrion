/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui

import android.content.Context
import android.view.ViewGroup
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.feature.dumb.config.R

fun createDumbBriefOverlayToolbar(context: Context): ViewGroup = createOverlayMenuLayout(
    context,
    listOf(
        OverlayMenuButton(R.id.btn_back, R.drawable.ic_back, R.string.content_desc_play_pause_scenario),
        OverlayMenuButton(R.id.btn_record, R.drawable.ic_gesture_record),
        OverlayMenuButton(R.id.btn_add, R.drawable.ic_add, R.string.content_desc_add_action),
        OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on, R.string.content_desc_go_back),
        OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
    ),
)
