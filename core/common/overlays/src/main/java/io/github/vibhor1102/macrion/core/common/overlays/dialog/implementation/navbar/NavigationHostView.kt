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
 * A Compose navigation surface hosted with fixed M3 dimension constraints.
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
        // Supply the M3 fixed dimension explicitly for portrait bottom bar and landscape rail.
        val constrainedWidthSpec = if (isPortrait) widthMeasureSpec else {
            MeasureSpec.makeMeasureSpec(navigationSizePx, MeasureSpec.EXACTLY)
        }
        val constrainedHeightSpec = if (isPortrait) {
            MeasureSpec.makeMeasureSpec(navigationSizePx, MeasureSpec.EXACTLY)
        } else heightMeasureSpec
        super.onMeasure(constrainedWidthSpec, constrainedHeightSpec)
    }
}
