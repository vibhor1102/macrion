/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.common

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.vibhor1102.macrion.core.base.extensions.disableMoveAnimations
import io.github.vibhor1102.macrion.core.base.extensions.safeAddView
import io.github.vibhor1102.macrion.core.base.extensions.safeRemoveView
import io.github.vibhor1102.macrion.core.ui.R
import kotlin.math.roundToInt

internal class OverlayDismissTargetController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
) {
    private var composeView: ComposeView? = null
    private var isAttached = false
    private var isVisibleState by mutableStateOf(false)
    private var isHoveredState by mutableStateOf(false)

    fun show() {
        if (!isAttached) {
            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setContent {
                    DismissTargetContent(
                        isVisible = isVisibleState,
                        isHovered = isHoveredState,
                    )
                }
            }
            composeView = view

            val density = context.resources.displayMetrics.density
            val windowHeight = (140 * density).roundToInt()
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                windowHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                disableMoveAnimations()
            }

            if (windowManager.safeAddView(view, params)) {
                isAttached = true
            }
        }
        isVisibleState = true
        isHoveredState = false
    }

    fun updateHoverState(isHovered: Boolean) {
        if (isHoveredState != isHovered) {
            isHoveredState = isHovered
        }
    }

    fun hide() {
        isVisibleState = false
        isHoveredState = false
        composeView?.postDelayed({
            if (!isVisibleState) {
                destroy()
            }
        }, 250)
    }

    fun destroy() {
        isVisibleState = false
        isHoveredState = false
        composeView?.let { view ->
            if (isAttached) {
                windowManager.safeRemoveView(view)
                isAttached = false
            }
            view.disposeComposition()
        }
        composeView = null
    }

    companion object {
        const val TARGET_BOTTOM_MARGIN_DP = 36f
        const val TARGET_CONTAINER_SIZE_DP = 68f
        const val TARGET_IDLE_SIZE_DP = 56f
        const val TARGET_HOVER_SIZE_DP = 68f
        const val ATTRACTION_RADIUS_DP = 140f
        const val DISMISS_RADIUS_DP = 65f

        fun getTargetCenter(displaySize: Point, density: Float): PointF {
            val centerX = displaySize.x / 2f
            val centerY = displaySize.y - ((TARGET_BOTTOM_MARGIN_DP + TARGET_CONTAINER_SIZE_DP / 2f) * density)
            return PointF(centerX, centerY)
        }
    }
}

@Composable
private fun DismissTargetContent(
    isVisible: Boolean,
    isHovered: Boolean,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 2 },
        ),
        exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { it / 2 },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x55000000),
                        ),
                    ),
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = OverlayDismissTargetController.TARGET_BOTTOM_MARGIN_DP.dp)
                    .size(OverlayDismissTargetController.TARGET_CONTAINER_SIZE_DP.dp),
                contentAlignment = Alignment.Center,
            ) {
                val circleSize by animateDpAsState(
                    targetValue = if (isHovered) {
                        OverlayDismissTargetController.TARGET_HOVER_SIZE_DP.dp
                    } else {
                        OverlayDismissTargetController.TARGET_IDLE_SIZE_DP.dp
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "dismiss_target_size",
                )
                val backgroundColor by animateColorAsState(
                    targetValue = if (isHovered) {
                        Color(0xFFE53935)
                    } else {
                        Color(0xCC1F1F1F)
                    },
                    animationSpec = tween(150),
                    label = "dismiss_target_color",
                )
                val iconSize by animateDpAsState(
                    targetValue = if (isHovered) 34.dp else 28.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "dismiss_icon_size",
                )

                Box(
                    modifier = Modifier
                        .size(circleSize)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}
