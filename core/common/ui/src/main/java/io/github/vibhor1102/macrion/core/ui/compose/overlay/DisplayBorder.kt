/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.compose.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

import io.github.vibhor1102.macrion.core.display.config.Corner
import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.display.config.DisplayRoundedCorner
import io.github.vibhor1102.macrion.core.display.config.haveRoundedCorner

/**
 * Draws the high-contrast display perimeter border respecting physical screen dimensions and
 * device rounded corners.
 */
fun DrawScope.drawDisplayBorder(
    displayConfig: DisplayConfig,
    color: Color,
    thicknessPx: Float,
) {
    val width = displayConfig.sizePx.x.toFloat()
    val height = displayConfig.sizePx.y.toFloat()

    if (!displayConfig.haveRoundedCorner()) {
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(width, height),
            style = Stroke(width = thicknessPx * 2f, cap = StrokeCap.Round),
        )
        return
    }

    val topLeftCorner = displayConfig.roundedCorners[Corner.TOP_LEFT]
    val topRightCorner = displayConfig.roundedCorners[Corner.TOP_RIGHT]
    val bottomRightCorner = displayConfig.roundedCorners[Corner.BOTTOM_RIGHT]
    val bottomLeftCorner = displayConfig.roundedCorners[Corner.BOTTOM_LEFT]

    val leftTop = topLeftCorner?.centerPx?.y?.toFloat() ?: 0f
    val leftBottom = bottomLeftCorner?.centerPx?.y?.toFloat() ?: height

    val topStartX = topLeftCorner?.centerPx?.x?.toFloat() ?: 0f
    val topEndX = topRightCorner?.centerPx?.x?.toFloat() ?: width

    val rightTop = topRightCorner?.centerPx?.y?.toFloat() ?: 0f
    val rightBottom = bottomRightCorner?.centerPx?.y?.toFloat() ?: height

    val bottomStartX = bottomLeftCorner?.centerPx?.x?.toFloat() ?: 0f
    val bottomEndX = bottomRightCorner?.centerPx?.x?.toFloat() ?: width

    // Left border line
    drawRect(
        color = color,
        topLeft = Offset(0f, leftTop),
        size = Size(thicknessPx, leftBottom - leftTop),
        style = Fill,
    )
    // Top border line
    drawRect(
        color = color,
        topLeft = Offset(topStartX, 0f),
        size = Size(topEndX - topStartX, thicknessPx),
        style = Fill,
    )
    // Right border line
    drawRect(
        color = color,
        topLeft = Offset(width - thicknessPx, rightTop),
        size = Size(thicknessPx, rightBottom - rightTop),
        style = Fill,
    )
    // Bottom border line
    drawRect(
        color = color,
        topLeft = Offset(bottomStartX, height - thicknessPx),
        size = Size(bottomEndX - bottomStartX, thicknessPx),
        style = Fill,
    )

    // Corner arcs
    val cornerStroke = Stroke(width = thicknessPx * 2f, cap = StrokeCap.Round)

    topLeftCorner?.let { corner ->
        val w = corner.centerPx.x * 2f + thicknessPx * 4f
        val h = corner.centerPx.y * 2f + thicknessPx * 4f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            style = cornerStroke,
        )
    }

    topRightCorner?.let { corner ->
        val offsetRight = (width - corner.centerPx.x) * 2f + thicknessPx * 4f
        val h = corner.centerPx.y * 2f + thicknessPx * 4f
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - offsetRight, 0f),
            size = Size(offsetRight, h),
            style = cornerStroke,
        )
    }

    bottomRightCorner?.let { corner ->
        val offsetRight = (width - corner.centerPx.x) * 2f + thicknessPx * 4f
        val offsetBottom = (height - corner.centerPx.y) * 2f + thicknessPx * 4f
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(width - offsetRight, height - offsetBottom),
            size = Size(offsetRight, offsetBottom),
            style = cornerStroke,
        )
    }

    bottomLeftCorner?.let { corner ->
        val w = corner.centerPx.x * 2f + thicknessPx * 4f
        val offsetBottom = (height - corner.centerPx.y) * 2f + thicknessPx * 4f
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, height - offsetBottom),
            size = Size(w, offsetBottom),
            style = cornerStroke,
        )
    }
}
