/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.core.ui.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.github.vibhor1102.macrion.core.ui.R


fun Context.createColorIndicatorDrawable(@ColorInt color: Int? = null): Drawable? {
    val indicatorDrawable = ContextCompat.getDrawable(
        this,
        R.drawable.ic_color_indicator,
    )?.mutate() as? LayerDrawable ?: return null

    if (color != null) indicatorDrawable.setColorIndicatorDrawableColor(color)

    return indicatorDrawable
}

private fun Drawable.setColorIndicatorDrawableColor(@ColorInt color: Int) {
    ((this as? LayerDrawable)?.findDrawableByLayerId(R.id.background_circle) as? GradientDrawable)
        ?.setColor(color)
}