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
 * A Compose navigation surface with non-rendering, correctly positioned item anchors.
 *
 * Tutorials still use Android [View] bounds and [View.performClick]. Keeping those tiny contracts here lets the
 * actual navigation UI be Compose without teaching the tutorial engine about Compose semantics.
 */
internal class NavigationHostView(
    context: Context,
    private val isPortrait: Boolean,
) : FrameLayout(context) {

    private val navigationSizePx = (80 * resources.displayMetrics.density).toInt()

    private val composeView = ComposeView(context).also { view ->
        addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
    private val anchors = mutableMapOf<Int, View>()

    fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeView.setContent(content)
    }

    fun setItemAnchors(items: List<DialogNavigationItem>, onClick: (Int) -> Unit) {
        items.forEach { item ->
            View(context).apply {
                id = item.id
                visibility = INVISIBLE
                setOnClickListener { onClick(item.id) }
                anchors[item.id] = this
                this@NavigationHostView.addView(this)
            }
        }
    }

    fun itemAnchor(itemId: Int): View =
        requireNotNull(anchors[itemId]) { "Unknown navigation item $itemId" }

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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val itemCount = anchors.size
        if (itemCount == 0) return

        anchors.values.forEachIndexed { index, anchor ->
            if (isPortrait) {
                val itemLeft = width * index / itemCount
                val itemRight = width * (index + 1) / itemCount
                anchor.layout(itemLeft, 0, itemRight, height)
            } else {
                val itemTop = height * index / itemCount
                val itemBottom = height * (index + 1) / itemCount
                anchor.layout(0, itemTop, width, itemBottom)
            }
        }
    }
}
