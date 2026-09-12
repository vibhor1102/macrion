/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar

import android.content.Context
import android.view.View
import android.widget.FrameLayout

import androidx.compose.ui.platform.ComposeView

/**
 * A Compose navigation surface hosted for BottomSheetDialog / CoordinatorLayout compatibility.
 */
internal class NavigationHostView(
    context: Context,
    private val isPortrait: Boolean,
) : FrameLayout(context) {

    private val navigationSizePx = (80 * resources.displayMetrics.density).toInt()

    private val composeView = ComposeView(context).also { view ->
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeView.setContent(content)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // A CoordinatorLayout measures its bottom child with an effectively unbounded height. Compose's navigation
        // primitives then accept that full space unless the Android host supplies their M3 fixed axis explicitly.
        val constrainedWidthSpec = if (isPortrait) widthMeasureSpec else {
            MeasureSpec.makeMeasureSpec(navigationSizePx, MeasureSpec.EXACTLY)
        }
        val constrainedHeightSpec = if (isPortrait) {
            MeasureSpec.makeMeasureSpec(navigationSizePx, MeasureSpec.EXACTLY)
        } else heightMeasureSpec
        super.onMeasure(constrainedWidthSpec, constrainedHeightSpec)
    }
}
