/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.sort

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.debugging.R

/** Compact sort chooser which keeps the report visible behind it. */
internal class DebugReportSortPopup<T>(
    private val anchor: View,
    private val options: List<DebugReportSortOption<T>>,
    private val onSelected: (T) -> Unit,
) {
    private val density = anchor.resources.displayMetrics.density
    private val width = minOf(
        (280 * density).toInt(),
        anchor.resources.displayMetrics.widthPixels - (32 * density).toInt(),
    )
    private val height = ((SORT_MENU_VERTICAL_PADDING_DP * 2 + SORT_OPTION_HEIGHT_DP * options.size) * density).toInt()

    private val popup = PopupWindow(anchor.context).apply {
        contentView = ComposeView(anchor.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MacrionTheme {
                    SortMenu(options) { value ->
                        dismiss()
                        onSelected(value)
                    }
                }
            }
        }
        this.width = this@DebugReportSortPopup.width
        this.height = this@DebugReportSortPopup.height
        isFocusable = true
        isOutsideTouchable = true
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        elevation = 8 * density
    }

    fun show() {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val displayHeight = anchor.resources.displayMetrics.heightPixels
        val gap = (8 * density).toInt()
        val x = (location[0] + anchor.width - width).coerceAtLeast((16 * density).toInt())
        val y = if (location[1] + anchor.height + gap + height <= displayHeight) {
            location[1] + anchor.height + gap
        } else {
            (location[1] - height - gap).coerceAtLeast((16 * density).toInt())
        }
        popup.showAtLocation(anchor.rootView, Gravity.TOP or Gravity.START, x, y)
    }

    fun dismiss() = popup.dismiss()
}

internal data class DebugReportSortOption<T>(
    val value: T,
    @field:StringRes val titleRes: Int,
    val selected: Boolean,
)

@Composable
private fun <T> SortMenu(options: List<DebugReportSortOption<T>>, onSelected: (T) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(vertical = SORT_MENU_VERTICAL_PADDING_DP.dp)) {
            options.forEach { option -> SortOption(option, onSelected) }
        }
    }
}

@Composable
private fun <T> SortOption(option: DebugReportSortOption<T>, onSelected: (T) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(SORT_OPTION_HEIGHT_DP.dp)
            .selectable(
                selected = option.selected,
                onClick = { onSelected(option.value) },
                role = Role.RadioButton,
            ),
        color = if (option.selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(option.titleRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (option.selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            if (option.selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_debug_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

private const val SORT_OPTION_HEIGHT_DP = 56
private const val SORT_MENU_VERTICAL_PADDING_DP = 8
