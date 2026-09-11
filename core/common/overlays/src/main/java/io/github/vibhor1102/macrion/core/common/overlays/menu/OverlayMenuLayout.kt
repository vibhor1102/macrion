/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.common.overlays.menu

import android.animation.LayoutTransition
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import io.github.vibhor1102.macrion.core.common.overlays.R

data class OverlayMenuButton(
    @IdRes val id: Int,
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int? = null,
)

fun createOverlayMenuLayout(
    context: Context,
    buttons: List<OverlayMenuButton>,
    content: View? = null,
    contentLayoutParams: LinearLayout.LayoutParams? = null,
    buttonViewFactory: ((OverlayMenuButton) -> View)? = null,
): ViewGroup {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    val buttonsContainer = LinearLayout(context).apply {
        id = R.id.menu_items
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        layoutTransition = LayoutTransition()
        buttons.forEach { button ->
            val buttonView = buttonViewFactory?.invoke(button) ?: ImageButton(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                scaleType = ImageView.ScaleType.FIT_CENTER
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.overlayMenuButtons),
                )
                setImageResource(button.icon)
            }
            addView(buttonView.apply {
                id = button.id
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                button.contentDescription?.let { contentDescription = context.getString(it) }
            })
        }
    }
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutTransition = LayoutTransition()
        addView(buttonsContainer)
        if (content != null) {
            addView(
                content,
                contentLayoutParams ?: LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }
    val card = CardView(context).apply {
        id = R.id.menu_background
        radius = dp(10).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.overlayMenuBackground))
        layoutTransition = LayoutTransition()
        addView(row)
    }
    return FrameLayout(context).apply {
        addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }
}
