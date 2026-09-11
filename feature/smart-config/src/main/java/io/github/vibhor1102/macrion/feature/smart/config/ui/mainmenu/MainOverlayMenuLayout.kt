/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButton
import io.github.vibhor1102.macrion.core.common.overlays.menu.createOverlayMenuLayout
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.mainmenu.debugging.LiveDebuggingUiState

internal class MainMenuViews(val root: ViewGroup) {
    val menuItems: ViewGroup = root.findViewById(R.id.menu_items)
    val btnPlay: View = root.findViewById(R.id.btn_play)
    val btnStop: View = root.findViewById(R.id.btn_stop)
    val btnClickList: View = root.findViewById(R.id.btn_click_list)
    val btnSwitchScenario: View = root.findViewById(R.id.btn_switch_scenario)
    val btnOpenHome: View = root.findViewById(R.id.btn_open_home)
    val layoutDebug: View = root.findViewById(R.id.layout_debug)
    val errorBadge: ImageView = root.findViewById(R.id.error_badge)
}

internal fun createMainOverlayMenu(
    context: Context,
    debugContent: @Composable () -> Unit,
    playPauseContent: @Composable () -> Unit,
): MainMenuViews {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    val debugContainer = FrameLayout(context).apply {
        id = R.id.layout_debug
        isVisible = false
        var contentInstalled = false
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                if (contentInstalled) return
                contentInstalled = true
                addView(
                    ComposeView(context).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent { MacrionTheme { debugContent() } }
                    },
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
    val root = createOverlayMenuLayout(
        context = context,
        buttons = listOf(
            OverlayMenuButton(R.id.btn_play, R.drawable.ic_play_arrow, R.string.content_desc_play_pause_scenario),
            OverlayMenuButton(R.id.btn_stop, R.drawable.ic_stop, R.string.content_desc_stop_clicker),
            OverlayMenuButton(R.id.btn_click_list, R.drawable.ic_settings_filled, R.string.content_desc_open_event_list),
            OverlayMenuButton(R.id.btn_switch_scenario, R.drawable.ic_swap_horiz, R.string.content_desc_switch_scenario),
            OverlayMenuButton(R.id.btn_open_home, R.drawable.ic_home, R.string.content_desc_open_home),
            OverlayMenuButton(R.id.btn_move, R.drawable.ic_move, R.string.content_desc_move_menu),
        ),
        content = debugContainer,
        contentLayoutParams = LinearLayout.LayoutParams(dp(200), dp(100)),
        buttonViewFactory = { button ->
            OverlayButtonFrameLayout(context).apply {
                addView(TouchTransparentComposeView(context) {
                    MacrionTheme {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (button.id == R.id.btn_play) playPauseContent()
                            else Icon(
                                painterResource(button.icon),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                tint = colorResource(io.github.vibhor1102.macrion.core.ui.R.color.overlayMenuButtons),
                            )
                        }
                    }
                }.apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                })
            }
        },
    )
    root.findViewById<View>(R.id.btn_switch_scenario).isVisible = false
    root.findViewById<View>(R.id.btn_open_home).isVisible = false
    root.addView(ImageView(context).apply {
        id = R.id.error_badge
        setImageResource(R.drawable.ic_badge_error)
        scaleType = ImageView.ScaleType.FIT_CENTER
        isVisible = false
    }, FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.TOP or Gravity.START,
    ).apply {
        leftMargin = dp(32)
        topMargin = dp(8)
    })
    return MainMenuViews(root)
}

private class TouchTransparentComposeView(
    context: Context,
    private val content: @Composable () -> Unit,
) : AbstractComposeView(context) {
    @Composable override fun Content() = content()
    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}

private class OverlayButtonFrameLayout(context: Context) : FrameLayout(context) {
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = true
}

@Composable
internal fun MainLiveDebugPanel(state: LiveDebuggingUiState?) {
    val primary = colorResource(R.color.overlayViewPrimary)
    val iconColor = colorResource(R.color.overlayMenuButtons)
    val divider = primary.copy(alpha = 19f / 255f)
    val normal = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
    val small = normal.copy(fontSize = 13.sp)

    Box(Modifier.width(200.dp).height(100.dp)) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(divider))
        Column(Modifier.fillMaxWidth().fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state?.let { Icon(painterResource(it.eventIcon), null, Modifier.size(24.dp), tint = iconColor) }
                Text(
                    state?.eventName.orEmpty(),
                    Modifier.weight(1f).padding(start = 8.dp).basicMarquee(),
                    color = primary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = normal,
                )
            }
            HorizontalDivider(thickness = 2.dp, color = divider)
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                DebugMetric(R.drawable.ic_confirm, state?.eventFulfilledCount, small, Modifier.weight(1f), iconColor, primary)
                Spacer(Modifier.width(2.dp).fillMaxHeight().background(divider))
                DebugMetric(R.drawable.ic_duration, state?.eventDuration, small, Modifier.weight(1f), iconColor, primary)
            }
            HorizontalDivider(thickness = 2.dp, color = divider)
            Row(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state?.actions?.forEach { action ->
                    Icon(
                        painterResource(action.icon),
                        null,
                        Modifier.width(32.dp).height(24.dp).padding(horizontal = 4.dp),
                        tint = primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugMetric(
    icon: Int,
    value: String?,
    style: TextStyle,
    modifier: Modifier,
    iconColor: Color,
    textColor: Color,
) {
    Row(modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (value != null) Icon(painterResource(icon), null, Modifier.size(20.dp), tint = iconColor)
        Text(value.orEmpty(), Modifier.padding(start = 4.dp), color = textColor, maxLines = 1, style = style)
    }
}
