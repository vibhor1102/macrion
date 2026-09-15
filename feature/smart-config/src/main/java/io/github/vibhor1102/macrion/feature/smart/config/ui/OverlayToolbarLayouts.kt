/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui

import android.content.Context
import android.view.ViewGroup
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.feature.smart.config.R

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun createValidationOverlayToolbar(context: Context): ViewGroup = createOverlayMenuLayout(
    context,
    listOf(
        OverlayMenuButton(R.id.btn_confirm, R.drawable.ic_confirm, R.string.content_desc_confirm),
        OverlayMenuButton(R.id.btn_cancel, R.drawable.ic_cancel, R.string.content_desc_go_back),
        OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on),
        OverlayMenuButton(R.id.btn_help, R.drawable.ic_help),
        OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
    ),
)

fun createScreenConditionsOverlayToolbar(
    context: Context,
    buttonModifier: (@Composable (OverlayMenuButton, performClick: () -> Unit) -> Modifier)? = null,
): ViewGroup = createOverlayMenuLayout(
    context = context,
    buttons = listOf(
        OverlayMenuButton(R.id.btn_save, R.drawable.ic_back),
        OverlayMenuButton(R.id.btn_add, R.drawable.ic_add),
        OverlayMenuButton(R.id.btn_copy, R.drawable.ic_copy_small),
        OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on),
        OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
    ),
    buttonModifier = buttonModifier,
)

fun createActionsOverlayToolbar(
    context: Context,
    buttonModifier: (@Composable (OverlayMenuButton, performClick: () -> Unit) -> Modifier)? = null,
): ViewGroup = createOverlayMenuLayout(
    context = context,
    buttons = listOf(
        OverlayMenuButton(R.id.btn_back, R.drawable.ic_back),
        OverlayMenuButton(R.id.btn_record, R.drawable.ic_gesture_record),
        OverlayMenuButton(R.id.btn_add_other, R.drawable.ic_add),
        OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on),
        OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
    ),
    buttonModifier = buttonModifier,
)

fun createColorCaptureOverlayToolbar(context: Context): ViewGroup = createOverlayMenuLayout(
    context,
    listOf(
        OverlayMenuButton(R.id.btn_confirm, R.drawable.ic_color_validate, R.string.content_desc_confirm),
        OverlayMenuButton(R.id.btn_cancel, R.drawable.ic_cancel, R.string.content_desc_go_back),
        OverlayMenuButton(R.id.btn_hide_overlay, R.drawable.ic_visible_on),
        OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
    ),
)
