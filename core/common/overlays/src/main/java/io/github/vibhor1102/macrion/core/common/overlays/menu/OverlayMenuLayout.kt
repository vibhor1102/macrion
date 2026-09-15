/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.common.overlays.menu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.EntryPoints
import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.di.OverlaysEntryPoint
import kotlinx.coroutines.flow.flowOf

data class OverlayMenuButton(
    @IdRes val id: Int,
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int? = null,
)

/** Stable native interaction/tutorial anchor; Compose owns the visual icon and placement. */
@SuppressLint("ViewConstructor")
class OverlayMenuButtonView(context: Context, icon: Int) : FrameLayout(context) {
    private var iconResource by mutableIntStateOf(icon)
    internal var composeVisibility by mutableIntStateOf(View.VISIBLE)
        private set
    internal var composeAlpha by mutableFloatStateOf(1f)
        private set

    fun setImageResource(@DrawableRes resource: Int) { iconResource = resource }

    internal val currentIconResource: Int get() = iconResource

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        composeVisibility = visibility
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        composeAlpha = alpha
    }

    // Keep one owner for the whole pointer sequence, including native tutorial monitoring.
    override fun onInterceptTouchEvent(event: MotionEvent) = true
}

/**
 * Non-rendering compatibility anchor for a Compose content panel.
 */
@SuppressLint("ViewConstructor")
class OverlayMenuContentAnchor(
    context: Context,
    initiallyVisible: Boolean = true,
) : View(context) {

    var composeVisibility by mutableIntStateOf(
        if (initiallyVisible) View.VISIBLE else View.GONE,
    )
        private set

    init {
        super.setVisibility(if (initiallyVisible) View.VISIBLE else View.GONE)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        composeVisibility = visibility
    }
}

/** WindowManager needs a View root. Lookups must work before composition attaches. */
class ComposeOverlayMenuHost(context: Context) : FrameLayout(context) {
    val anchors = mutableMapOf<Int, View>()
    val buttons = mutableListOf<OverlayMenuButtonView>()
    var isTucked by mutableStateOf(false)
    var isDockedOnLeft by mutableStateOf(true)
    var onUntuckRequested: (() -> Unit)? = null
    var onTuckedTouch: ((MotionEvent) -> Boolean)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isTucked) return true
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTucked) {
            return onTuckedTouch?.invoke(event) ?: false
        }
        return super.onTouchEvent(event)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.findOverlayView(@IdRes id: Int): T =
    ((this as? ComposeOverlayMenuHost)?.anchors?.get(id) ?: findViewById<View>(id)
        ?: error("Missing overlay anchor $id")) as T

fun createOverlayMenuLayout(
    context: Context,
    buttons: List<OverlayMenuButton>,
    content: (@Composable () -> Unit)? = null,
    contentWidthDp: Int = 0,
    contentHeightDp: Int = 0,
    contentInitiallyVisible: Boolean = true,
    @IdRes contentId: Int? = null,
    buttonModifier: (@Composable (OverlayMenuButton, performClick: () -> Unit) -> Modifier)? = null,
    buttonContent: (@Composable (OverlayMenuButton) -> Unit)? = null,
): ViewGroup {
    val root = ComposeOverlayMenuHost(context)
    buttons.forEach { button ->
        val anchor = OverlayMenuButtonView(context, button.icon).apply {
            id = button.id
            button.contentDescription?.let { contentDescription = context.getString(it) }
        }
        root.buttons += anchor
        root.anchors[button.id] = anchor
    }

    val itemsAnchor = FrameLayout(context).apply {
        id = R.id.menu_items
    }
    root.anchors[R.id.menu_items] = itemsAnchor

    val contentAnchor = content?.let {
        OverlayMenuContentAnchor(context, contentInitiallyVisible).apply {
            id = contentId ?: View.generateViewId()
        }
    }
    contentAnchor?.let { root.anchors[it.id] = it }

    val scaleProvider = try {
        EntryPoints.get(context.applicationContext, OverlaysEntryPoint::class.java).overlayScaleProvider()
    } catch (_: Exception) {
        null
    }

    var cornerRadius = 10 * context.resources.displayMetrics.density
    val background = ComposeView(context).apply {
        id = R.id.menu_background
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        clipToOutline = true
        addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                view.invalidateOutline()
            }
        }
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val scale by (scaleProvider?.scaleFlow ?: flowOf(1f))
                .collectAsState(initial = scaleProvider?.getScale() ?: 1f)
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, scale) {
                Density(
                    density = baseDensity.density * scale,
                    fontScale = baseDensity.fontScale * scale,
                )
            }

            LaunchedEffect(scaledDensity.density) {
                cornerRadius = 10 * scaledDensity.density
                invalidateOutline()
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                val density = LocalDensity.current
                val isTucked = root.isTucked
                val isDockedOnLeft = root.isDockedOnLeft
                val panelVisible = !isTucked && contentAnchor != null && contentAnchor.composeVisibility != View.GONE
                val panelWidthPx = if (panelVisible) with(density) { contentWidthDp.dp.roundToPx() } else 0
                val panelHeightPx = if (panelVisible) with(density) { contentHeightDp.dp.roundToPx() } else 0
                val buttonHeightPx = with(density) { (8 + 48 * root.buttons.count { it.composeVisibility != View.GONE }).dp.roundToPx() }
                val buttonWidthPx = with(density) { 56.dp.roundToPx() }
                val tuckedWidthPx = with(density) { 24.dp.roundToPx() }
                val tuckedHeightPx = with(density) { 56.dp.roundToPx() }
                val targetSize = if (isTucked) {
                    IntSize(tuckedWidthPx, tuckedHeightPx)
                } else {
                    IntSize(buttonWidthPx + panelWidthPx, maxOf(buttonHeightPx, panelHeightPx))
                }
                val animatedSize by animateIntSizeAsState(
                    targetSize,
                    tween(300, easing = OverlayMenuResizeEasing),
                    label = "overlayMenuSize",
                )

                val cornerRadiusShape = if (isTucked) {
                    if (isDockedOnLeft) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, bottomStart = 0.dp)
                    else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                } else {
                    RoundedCornerShape(10.dp)
                }
                val bgColor = if (isTucked) {
                    colorResource(R.color.overlayMenuBackground).copy(alpha = 0.6f)
                } else {
                    colorResource(R.color.overlayMenuBackground)
                }

                Box(
                    Modifier.requiredSize(
                        with(density) { animatedSize.width.toDp() },
                        with(density) { animatedSize.height.toDp() },
                    ).clip(cornerRadiusShape)
                        .background(bgColor),
                ) {
                    if (isTucked) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isDockedOnLeft) io.github.vibhor1102.macrion.core.ui.R.drawable.ic_chevron_right
                                    else io.github.vibhor1102.macrion.core.ui.R.drawable.ic_chevron_left
                                ),
                                contentDescription = stringResource(R.string.content_desc_expand_toolbar),
                                tint = colorResource(R.color.overlayMenuButtons),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.wrapContentSize()) {
                            AndroidView(factory = { itemsAnchor }, modifier = Modifier.matchParentSize())
                            Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                root.buttons.forEachIndexed { index, button ->
                                    key(button.id) {
                                        val visible = button.composeVisibility != View.GONE
                                        AnimatedVisibility(
                                            visible = visible,
                                            enter = expandVertically(animationSpec = tween(300, easing = OverlayMenuResizeEasing)) +
                                                fadeIn(animationSpec = tween(300)),
                                            exit = shrinkVertically(animationSpec = tween(300, easing = OverlayMenuResizeEasing)) +
                                                fadeOut(animationSpec = tween(300)),
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(48.dp)
                                                    .then(buttonModifier?.invoke(buttons[index]) { button.performClick() } ?: Modifier),
                                            ) {
                                                if (visible) {
                                                    Box(Modifier.fillMaxSize().alpha(button.composeAlpha), contentAlignment = Alignment.Center) {
                                                        if (buttonContent != null) {
                                                            buttonContent(buttons[index])
                                                        } else {
                                                            Icon(
                                                                painterResource(button.currentIconResource),
                                                                null,
                                                                Modifier.size(40.dp),
                                                                tint = colorResource(R.color.overlayMenuButtons),
                                                            )
                                                        }
                                                    }
                                                    AndroidView(factory = { button }, modifier = Modifier.fillMaxSize())
                                                } else if (buttonContent != null) {
                                                    Box(Modifier.fillMaxSize().alpha(button.composeAlpha), contentAlignment = Alignment.Center) {
                                                        buttonContent(buttons[index])
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize().alpha(button.composeAlpha), contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            painterResource(button.currentIconResource),
                                                            null,
                                                            Modifier.size(40.dp),
                                                            tint = colorResource(R.color.overlayMenuButtons),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (panelVisible) {
                            Box(
                                Modifier.size(
                                    with(density) { panelWidthPx.toDp() },
                                    with(density) { panelHeightPx.toDp() },
                                ),
                            ) {
                                content.invoke()
                            }
                        }
                    }
                }
            }
        }
    }
    }
    root.anchors[R.id.menu_background] = background
    root.addView(background, FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ))
    return root
}

private val OverlayMenuResizeEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}
