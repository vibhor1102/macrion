/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.bindings.dialogs

import android.content.Context
import android.view.View
import android.widget.FrameLayout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

/**
 * A single Compose FAB cluster. Invisible Android anchors retain the popup/tutorial integration contract without
 * participating in normal touch dispatch.
 */
class FloatingActionButtonsView(context: Context) : FrameLayout(context) {
    val root: View get() = this

    private val primaryDescription = mutableStateOf<CharSequence?>(null)
    val primary = object : View(context) {
        override fun setContentDescription(contentDescription: CharSequence?) {
            super.setContentDescription(contentDescription)
            primaryDescription.value = contentDescription
        }
    }.apply { visibility = INVISIBLE }
    val secondary = View(context).apply { visibility = INVISIBLE }

    private val primaryIcon = mutableIntStateOf(R.drawable.ic_add)
    private val secondaryIcon = mutableIntStateOf(R.drawable.ic_copy)
    private val badgeText = mutableStateOf<String?>(null)
    private val secondaryVisible = mutableStateOf(false)
    var primaryModifier by mutableStateOf<@Composable () -> Modifier>({ Modifier })
    private var onPrimary: () -> Unit = {}
    private var onSecondary: () -> Unit = {}

    init {
        clipChildren = false
        clipToPadding = false
        translationZ = 100 * resources.displayMetrics.density
        addView(ComposeView(context).apply { setContent { MacrionTheme { FabClusterContent() } } })
        addView(primary)
        addView(secondary)
    }

    fun configure(
        @DrawableRes primaryIcon: Int,
        @DrawableRes secondaryIcon: Int,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit,
    ) {
        this.primaryIcon.intValue = primaryIcon
        this.secondaryIcon.intValue = secondaryIcon
        this.onPrimary = onPrimary
        this.onSecondary = onSecondary
    }

    fun performPrimaryClick() { onPrimary() }
    fun performSecondaryClick() { onSecondary() }

    fun setSecondaryVisible(visible: Boolean) {
        if (secondaryVisible.value == visible) return
        secondaryVisible.value = visible
        requestLayout()
    }

    fun setBadge(text: String?, description: CharSequence? = null) {
        badgeText.value = text
        primary.contentDescription = description
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val clusterHeight = if (secondaryVisible.value) CLUSTER_HEIGHT_DP else PRIMARY_CONTAINER_SIZE_DP
        val width = resolveSize(PRIMARY_CONTAINER_SIZE_DP.dpPx, widthMeasureSpec)
        val height = resolveSize(clusterHeight.dpPx, heightMeasureSpec)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        repeat(childCount) { getChildAt(it).measure(childWidthSpec, childHeightSpec) }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        getChildAt(0).layout(0, 0, width, height)
        val primaryTop = height - PRIMARY_CONTAINER_SIZE_DP.dpPx
        primary.layout(0, primaryTop, width, height)
        secondary.layout(0, 0, width, SECONDARY_CONTAINER_SIZE_DP.dpPx)
    }

    @androidx.compose.runtime.Composable
    private fun FabClusterContent() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (secondaryVisible.value) {
                    SmallFloatingActionButton(onClick = onSecondary) {
                        Icon(painterResource(secondaryIcon.intValue), contentDescription = null)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Box(Modifier.size(PRIMARY_CONTAINER_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = onPrimary,
                        modifier = Modifier.size(56.dp).then(primaryModifier()),
                    ) {
                        Icon(
                            painterResource(primaryIcon.intValue),
                            contentDescription = primaryDescription.value?.toString(),
                        )
                    }
                    badgeText.value?.let { Badge(Modifier.align(Alignment.TopEnd).size(18.dp)) { Text(it) } }
                }
            }
        }
    }

    private val Int.dpPx get() = (this * resources.displayMetrics.density).toInt()

    private companion object {
        const val SECONDARY_CONTAINER_SIZE_DP = 40
        const val PRIMARY_CONTAINER_SIZE_DP = 64
        const val CLUSTER_HEIGHT_DP = SECONDARY_CONTAINER_SIZE_DP + 16 + PRIMARY_CONTAINER_SIZE_DP
    }
}
