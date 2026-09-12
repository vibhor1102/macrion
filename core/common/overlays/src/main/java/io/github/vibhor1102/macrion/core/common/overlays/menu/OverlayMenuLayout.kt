/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.common.overlays.menu

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import io.github.vibhor1102.macrion.core.common.overlays.R

data class OverlayMenuButton(
    @IdRes val id: Int,
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int? = null,
)

/** Stable native interaction/tutorial anchor; Compose owns its icon and placement. */
class OverlayMenuButtonView(context: Context, icon: Int, content: (@Composable () -> Unit)? = null) : FrameLayout(context) {
    private var iconResource by mutableIntStateOf(icon)
    internal var composeVisibility by mutableIntStateOf(View.VISIBLE)
        private set

    init {
        addView(object : AbstractComposeView(context) {
            @Composable override fun Content() {
                if (content != null) content() else Icon(painterResource(iconResource), null, Modifier.fillMaxSize(),
                    tint = colorResource(R.color.overlayMenuButtons))
            }
            override fun dispatchTouchEvent(event: MotionEvent) = false
        }.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setImageResource(@DrawableRes resource: Int) { iconResource = resource }

    internal val currentIconResource: Int get() = iconResource

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        composeVisibility = visibility
    }

    // Keep one owner for the whole pointer sequence, including native tutorial monitoring.
    override fun onInterceptTouchEvent(event: MotionEvent) = true

}

private class OverlayMenuContentView(context: Context, content: View) : FrameLayout(context) {
    var composeVisibility by mutableIntStateOf(content.visibility)
        private set
    init {
        id = content.id
        visibility = content.visibility
        content.visibility = View.VISIBLE
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        composeVisibility = visibility
    }
}

/** WindowManager needs a View root. Lookups must work before composition attaches. */
internal class ComposeOverlayMenuHost(context: Context) : FrameLayout(context) {
    val anchors = mutableMapOf<Int, View>()
    val buttons = mutableListOf<OverlayMenuButtonView>()
}

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.findOverlayView(@IdRes id: Int): T =
    ((this as? ComposeOverlayMenuHost)?.anchors?.get(id) ?: findViewById<View>(id)
        ?: error("Missing overlay anchor $id")) as T

fun createOverlayMenuLayout(
    context: Context,
    buttons: List<OverlayMenuButton>,
    content: View? = null,
    contentLayoutParams: ViewGroup.LayoutParams? = null,
    buttonContent: (@Composable (OverlayMenuButton) -> Unit)? = null,
): ViewGroup {
    val root = ComposeOverlayMenuHost(context)
    buttons.forEach { button ->
        val iconContent: (@Composable () -> Unit)? = buttonContent?.let { render -> { render(button) } }
        val anchor = OverlayMenuButtonView(context, button.icon, iconContent).apply {
            id = button.id
            button.contentDescription?.let { contentDescription = context.getString(it) }
        }
        root.buttons += anchor
        root.anchors[button.id] = anchor
    }
    val items = ComposeView(context).apply {
        id = R.id.menu_items
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
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
                            Box(Modifier.size(48.dp)) {
                                if (visible) {
                                    AndroidView(factory = { button }, modifier = Modifier.fillMaxSize())
                                } else if (buttonContent != null) {
                                    buttonContent(buttons[index])
                                } else {
                                    Icon(painterResource(button.currentIconResource), null, Modifier.fillMaxSize(),
                                        tint = colorResource(R.color.overlayMenuButtons))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    root.anchors[R.id.menu_items] = items
    val contentAnchor = content?.let { OverlayMenuContentView(context, it) }
    contentAnchor?.let { root.anchors[it.id] = it }
    val background = ComposeView(context).apply {
        id = R.id.menu_background
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val density = LocalDensity.current
            val panelVisible = contentAnchor != null && contentAnchor.composeVisibility != View.GONE
            val panelWidthPx = if (panelVisible) contentLayoutParams?.width ?: 0 else 0
            val panelHeightPx = if (panelVisible) contentLayoutParams?.height ?: 0 else 0
            val buttonHeightPx = with(density) { (8 + 48 * root.buttons.count { it.composeVisibility != View.GONE }).dp.roundToPx() }
            val buttonWidthPx = with(density) { 56.dp.roundToPx() }
            val targetSize = IntSize(buttonWidthPx + panelWidthPx, maxOf(buttonHeightPx, panelHeightPx))
            val animatedSize by animateIntSizeAsState(
                targetSize,
                tween(300, easing = OverlayMenuResizeEasing),
                label = "overlayMenuSize",
            )

            Box(
                Modifier.requiredSize(
                    with(density) { animatedSize.width.toDp() },
                    with(density) { animatedSize.height.toDp() },
                ).clip(RoundedCornerShape(10.dp))
                    .background(colorResource(R.color.overlayMenuBackground)),
            ) {
                Row(Modifier.wrapContentSize(unbounded = true, align = Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically) {
                    AndroidView(factory = { items })
                    if (panelVisible && contentAnchor != null) {
                        AndroidView(factory = { contentAnchor }, modifier = Modifier.size(
                            with(density) { panelWidthPx.toDp() },
                            with(density) { panelHeightPx.toDp() },
                        ))
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
