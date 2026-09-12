/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription

private const val MINIMAL_CLICK_ANIMATION_DURATION_MS = 1L
private const val MINIMAL_SWIPE_ANIMATION_DURATION_MS = 250L
private const val MINIMAL_PAUSE_ANIMATION_DURATION_MS = 500L

data class ClickDescription(
    val pressDurationMs: Long = MINIMAL_CLICK_ANIMATION_DURATION_MS,
    val position: PointF? = null,
    val imageConditionBitmap: Bitmap? = null,
) : ItemBriefDescription

data class SwipeDescription(
    val swipeDurationMs: Long = MINIMAL_SWIPE_ANIMATION_DURATION_MS,
    val from: PointF? = null,
    val to: PointF? = null,
) : ItemBriefDescription

data class PauseDescription(
    val pauseDurationMs: Long = MINIMAL_PAUSE_ANIMATION_DURATION_MS,
) : ItemBriefDescription

data class ImageConditionDescription(
    val conditionBitmap: Bitmap?,
    val conditionDetectionType: ImageConditionBriefRenderingType,
    val conditionPosition: Rect,
    val conditionDetectionArea: Rect?,
) : ItemBriefDescription

enum class ImageConditionBriefRenderingType {
    AREA,
    EXACT,
    WHOLE_SCREEN,
}

data class ColorConditionDescription(
    @field:ColorInt val conditionColor: Int,
    val conditionPosition: PointF,
) : ItemBriefDescription

data class TextConditionDescription(
    val conditionText: String,
    val conditionDetectionArea: Rect,
) : ItemBriefDescription

data class DefaultDescription(
    val icon: Drawable? = null,
) : ItemBriefDescription
