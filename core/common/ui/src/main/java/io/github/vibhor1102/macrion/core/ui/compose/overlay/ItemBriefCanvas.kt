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

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.PointF
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ActionCarouselDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ColorConditionDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.DefaultDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ImageConditionBriefRenderingType
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ImageConditionDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.NumberedActionPreview
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.PauseDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SplitDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.TextConditionDescription

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Compose canvas rendering all automation action and condition visual indicators with
 * high-performance hardware-accelerated drawing and 60fps animations.
 */
@Composable
fun ItemBriefCanvas(
    description: ItemBriefDescription?,
    displayConfig: DisplayConfig,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    backgroundColor: Color = colorResource(R.color.overlayActionsBriefBackground),
    primaryColor: Color = colorResource(R.color.overlayViewPrimary),
    secondaryColor: Color = colorResource(R.color.overlayViewPrimary),
    innerColor: Color = colorResource(R.color.overlayViewPrimary),
    thicknessPx: Float = with(LocalDensity.current) { dimensionResource(R.dimen.overlay_click_selector_thickness).toPx() },
    outerRadiusPx: Float = with(LocalDensity.current) { dimensionResource(R.dimen.overlay_click_selector_radius).toPx() },
    innerRadiusPx: Float = with(LocalDensity.current) { dimensionResource(R.dimen.overlay_click_selector_inner_radius).toPx() },
    cornerRadiusPx: Float = with(LocalDensity.current) { 2.dp.toPx() },
) {
    val carouselDescription = description as? ActionCarouselDescription
    val labelOffsetSteps = remember(carouselDescription) {
        carouselDescription?.let(::calculateLabelOffsetSteps).orEmpty()
    }
    val animationDescription = carouselDescription?.let { carousel ->
        val focusedPreview = carousel.previews.firstOrNull { it.order == carousel.focusedOrder }?.description
        val fallbackSplit = carousel.focusedFallback as? SplitDescription
        if (focusedPreview is SplitDescription && fallbackSplit != null) {
            SplitDescription(focusedPreview.subDescriptions + fallbackSplit.subDescriptions)
        } else {
            focusedPreview ?: carousel.focusedFallback
        }
    } ?: description
    val clickScale = remember { Animatable(1f) }
    LaunchedEffect(animationDescription, animate) {
        val clickDescList = when (animationDescription) {
            is ClickDescription -> listOf(animationDescription)
            else -> emptyList()
        }
        if (!animate || clickDescList.isEmpty()) {
            clickScale.snapTo(1f)
            return@LaunchedEffect
        }
        val maxPressDuration = clickDescList.maxOfOrNull { it.pressDurationMs } ?: 1L
        val pressDurationMs = max(maxPressDuration, 1L)
        while (isActive) {
            clickScale.snapTo(1f)
            delay(250)
            clickScale.animateTo(
                targetValue = 0.75f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing,
                ),
            )
            delay(pressDurationMs)
            clickScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = FastOutSlowInEasing,
                ),
            )
            delay(500)
        }
    }

    val swipeProgress = remember { Animatable(0f) }
    LaunchedEffect(animationDescription, animate) {
        val swipeDescList = when (animationDescription) {
            is SwipeDescription -> listOf(animationDescription)
            else -> emptyList()
        }.filter { it.from != null && it.to != null }

        if (!animate || swipeDescList.isEmpty()) {
            swipeProgress.snapTo(0f)
            return@LaunchedEffect
        }
        val maxDuration = swipeDescList.maxOfOrNull { it.swipeDurationMs } ?: 250L
        val animDurationMs = max(maxDuration, 250L).toInt()
        while (isActive) {
            swipeProgress.snapTo(0f)
            delay(250)
            swipeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animDurationMs,
                    easing = LinearEasing,
                ),
            )
            delay(500)
        }
    }

    val simultaneousTime = remember { Animatable(-1f) }
    LaunchedEffect(animationDescription, animate) {
        val children = (animationDescription as? SplitDescription)?.subDescriptions.orEmpty()
        val endTime = children.maxOfOrNull { child ->
            when (child) {
                is SwipeDescription -> child.startOffsetMs + child.swipeDurationMs
                is ClickDescription -> child.startOffsetMs + child.pressDurationMs
                else -> 0L
            }
        } ?: 0L
        if (!animate || endTime <= 0L) {
            simultaneousTime.snapTo(-1f)
            return@LaunchedEffect
        }
        while (isActive) {
            simultaneousTime.snapTo(-1f)
            delay(250)
            simultaneousTime.snapTo(0f)
            simultaneousTime.animateTo(endTime.toFloat(), tween(endTime.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), easing = LinearEasing))
            delay(500)
        }
    }

    val pauseRotation = remember { Animatable(0f) }
    LaunchedEffect(animationDescription, animate) {
        if (!animate || animationDescription !is PauseDescription) {
            pauseRotation.snapTo(0f)
            return@LaunchedEffect
        }
        val animDurationMs = max(animationDescription.pauseDurationMs, 500L).toInt()
        while (isActive) {
            pauseRotation.snapTo(0f)
            delay(250)
            pauseRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(
                    durationMillis = animDurationMs,
                    easing = LinearEasing,
                ),
            )
            delay(500)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        when (description) {
            is ActionCarouselDescription -> {
                when (val fallback = description.focusedFallback) {
                    is ClickDescription -> drawClickIndicator(
                        description = fallback,
                        scale = if (animate) clickScale.value else 1f,
                        outerRadiusPx = outerRadiusPx,
                        innerRadiusPx = innerRadiusPx,
                        thicknessPx = thicknessPx,
                        primaryColor = primaryColor,
                        innerColor = innerColor,
                        backgroundColor = backgroundColor,
                        number = description.focusedOrder,
                    )
                    is PauseDescription -> drawPauseIndicator(
                        rotationDegrees = if (animate) pauseRotation.value else 0f,
                        outerRadiusPx = outerRadiusPx,
                        thicknessPx = thicknessPx,
                        primaryColor = primaryColor,
                        innerColor = innerColor,
                        backgroundColor = backgroundColor,
                    )
                    is DefaultDescription -> drawDefaultIndicator(
                        description = fallback,
                        outerRadiusPx = outerRadiusPx,
                        backgroundColor = backgroundColor,
                    )
                    is SplitDescription -> fallback.subDescriptions.forEach { child ->
                        if (child is ClickDescription) drawClickIndicator(
                            description = child,
                            scale = if (animate && childProgress(simultaneousTime.value, child.startOffsetMs, child.pressDurationMs) != null) 0.75f else 1f,
                            outerRadiusPx = outerRadiusPx,
                            innerRadiusPx = innerRadiusPx,
                            thicknessPx = thicknessPx,
                            primaryColor = primaryColor,
                            innerColor = innerColor,
                            backgroundColor = backgroundColor,
                            number = description.focusedOrder,
                        )
                    }
                    else -> Unit
                }
                description.previews.filter { it.order != description.focusedOrder }.forEach { preview ->
                    drawNumberedActionPreview(
                        preview, false, false, clickScale.value, swipeProgress.value, simultaneousTime.value,
                        outerRadiusPx, innerRadiusPx, thicknessPx, primaryColor, secondaryColor, innerColor, backgroundColor,
                        labelOffsetFor = { position -> (labelOffsetSteps[preview.order to position] ?: 0f) * 11.dp.toPx() },
                    )
                }
                description.previews.firstOrNull { it.order == description.focusedOrder }?.let { preview ->
                    drawNumberedActionPreview(
                        preview, true, animate, clickScale.value, swipeProgress.value, simultaneousTime.value,
                        outerRadiusPx, innerRadiusPx, thicknessPx, primaryColor, secondaryColor, innerColor, backgroundColor,
                        labelOffsetFor = { position -> (labelOffsetSteps[preview.order to position] ?: 0f) * 11.dp.toPx() },
                    )
                }
            }

            is ClickDescription -> drawClickIndicator(
                description = description,
                scale = if (animate) clickScale.value else 1f,
                outerRadiusPx = outerRadiusPx,
                innerRadiusPx = innerRadiusPx,
                thicknessPx = thicknessPx,
                primaryColor = primaryColor,
                innerColor = innerColor,
                backgroundColor = backgroundColor,
            )

            is SwipeDescription -> drawSwipeIndicator(
                description = description,
                progress = if (animate) swipeProgress.value else null,
                outerRadiusPx = outerRadiusPx,
                innerRadiusPx = innerRadiusPx,
                thicknessPx = thicknessPx,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                innerColor = innerColor,
                backgroundColor = backgroundColor,
            )

            is SplitDescription -> {
                description.subDescriptions.forEach { subDesc ->
                    when (subDesc) {
                        is SwipeDescription -> drawSwipeIndicator(
                            description = subDesc,
                            progress = if (animate) childProgress(simultaneousTime.value, subDesc.startOffsetMs, subDesc.swipeDurationMs) else null,
                            outerRadiusPx = outerRadiusPx,
                            innerRadiusPx = innerRadiusPx,
                            thicknessPx = thicknessPx,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            innerColor = innerColor,
                            backgroundColor = backgroundColor,
                        )

                        is ClickDescription -> drawClickIndicator(
                            description = subDesc,
                            scale = if (animate && childProgress(simultaneousTime.value, subDesc.startOffsetMs, subDesc.pressDurationMs) != null) 0.75f else 1f,
                            outerRadiusPx = outerRadiusPx,
                            innerRadiusPx = innerRadiusPx,
                            thicknessPx = thicknessPx,
                            primaryColor = primaryColor,
                            innerColor = innerColor,
                            backgroundColor = backgroundColor,
                        )

                        else -> Unit
                    }
                }
            }

            is PauseDescription -> drawPauseIndicator(
                rotationDegrees = if (animate) pauseRotation.value else 0f,
                outerRadiusPx = outerRadiusPx,
                thicknessPx = thicknessPx,
                primaryColor = primaryColor,
                innerColor = innerColor,
                backgroundColor = backgroundColor,
            )

            is ImageConditionDescription -> drawImageConditionIndicator(
                description = description,
                displayConfig = displayConfig,
                thicknessPx = thicknessPx,
                cornerRadiusPx = cornerRadiusPx,
                primaryColor = primaryColor,
                backgroundColor = backgroundColor,
            )

            is ColorConditionDescription -> drawColorConditionIndicator(
                description = description,
                primaryColor = primaryColor,
            )

            is TextConditionDescription -> drawTextConditionIndicator(
                description = description,
                thicknessPx = thicknessPx,
                cornerRadiusPx = cornerRadiusPx,
                primaryColor = primaryColor,
                backgroundColor = backgroundColor,
            )

            is DefaultDescription -> drawDefaultIndicator(
                description = description,
                outerRadiusPx = outerRadiusPx,
                backgroundColor = backgroundColor,
            )

            null -> Unit
        }
    }
}

private fun childProgress(timeMs: Float, startMs: Long, durationMs: Long): Float? {
    if (durationMs <= 0L || timeMs < startMs || timeMs >= startMs + durationMs) return null
    return ((timeMs - startMs) / durationMs).coerceIn(0f, 1f)
}

/** Spread only labels at identical targets; the rings and precision dots stay on their exact coordinates. */
private fun calculateLabelOffsetSteps(description: ActionCarouselDescription): Map<Pair<Int, Offset>, Float> {
    val markers = description.previews.flatMap { preview ->
        preview.description.markerPositions().map { preview.order to it }
    }.distinct()
    return markers.groupBy { it.second }.values.flatMap { atPosition ->
        atPosition.mapIndexed { index, marker ->
            marker to (index - (atPosition.size - 1) / 2f)
        }
    }.toMap()
}

private fun ItemBriefDescription.markerPositions(): List<Offset> = when (this) {
    is ClickDescription -> position?.let { listOf(Offset(it.x, it.y)) }.orEmpty()
    is SwipeDescription -> listOfNotNull(from, to).map { Offset(it.x, it.y) }
    is SplitDescription -> subDescriptions.flatMap { it.markerPositions() }
    else -> emptyList()
}

private fun DrawScope.drawNumberedActionPreview(
    preview: NumberedActionPreview,
    isFocused: Boolean,
    animate: Boolean,
    clickScale: Float,
    swipeProgress: Float,
    simultaneousTime: Float,
    outerRadiusPx: Float,
    innerRadiusPx: Float,
    thicknessPx: Float,
    primaryColor: Color,
    secondaryColor: Color,
    innerColor: Color,
    backgroundColor: Color,
    labelOffsetFor: (Offset) -> Float,
) {
    val ringColor = if (isFocused) primaryColor else primaryColor.copy(alpha = 0.7f)
    val ringThickness = if (isFocused) thicknessPx else thicknessPx * 0.65f
    val hazeColor = if (isFocused) backgroundColor else backgroundColor.copy(alpha = backgroundColor.alpha * 0.6f)
    when (val action = preview.description) {
        is ClickDescription -> drawClickIndicator(
            description = action,
            scale = if (animate) clickScale else 1f,
            outerRadiusPx = outerRadiusPx,
            innerRadiusPx = innerRadiusPx,
            thicknessPx = ringThickness,
            primaryColor = ringColor,
            innerColor = innerColor,
            backgroundColor = hazeColor,
            number = preview.order,
            isFocused = isFocused,
            labelOffsetFor = labelOffsetFor,
        )
        is SwipeDescription -> drawSwipeIndicator(
            description = action,
            progress = if (animate) swipeProgress else null,
            outerRadiusPx = outerRadiusPx,
            innerRadiusPx = innerRadiusPx,
            thicknessPx = ringThickness,
            primaryColor = ringColor,
            secondaryColor = if (isFocused) secondaryColor else secondaryColor.copy(alpha = 0.7f),
            innerColor = innerColor,
            backgroundColor = hazeColor,
            number = preview.order,
            isFocused = isFocused,
            labelOffsetFor = labelOffsetFor,
        )
        is SplitDescription -> action.subDescriptions.forEach { child ->
            when (child) {
                is ClickDescription -> drawClickIndicator(
                    description = child,
                    scale = if (animate && childProgress(simultaneousTime, child.startOffsetMs, child.pressDurationMs) != null) 0.75f else 1f,
                    outerRadiusPx = outerRadiusPx,
                    innerRadiusPx = innerRadiusPx,
                    thicknessPx = ringThickness,
                    primaryColor = ringColor,
                    innerColor = innerColor,
                    backgroundColor = hazeColor,
                    number = preview.order,
                    isFocused = isFocused,
                    labelOffsetFor = labelOffsetFor,
                )
                is SwipeDescription -> drawSwipeIndicator(
                    description = child,
                    progress = if (animate) childProgress(simultaneousTime, child.startOffsetMs, child.swipeDurationMs) else null,
                    outerRadiusPx = outerRadiusPx,
                    innerRadiusPx = innerRadiusPx,
                    thicknessPx = ringThickness,
                    primaryColor = ringColor,
                    secondaryColor = if (isFocused) secondaryColor else secondaryColor.copy(alpha = 0.7f),
                    innerColor = innerColor,
                    backgroundColor = hazeColor,
                    number = preview.order,
                    isFocused = isFocused,
                    labelOffsetFor = labelOffsetFor,
                )
                else -> Unit
            }
        }
        else -> Unit
    }
}

private fun DrawScope.drawClickIndicator(
    description: ClickDescription,
    scale: Float,
    outerRadiusPx: Float,
    innerRadiusPx: Float,
    thicknessPx: Float,
    primaryColor: Color,
    innerColor: Color,
    backgroundColor: Color,
    number: Int? = null,
    isFocused: Boolean = true,
    labelOffsetFor: (Offset) -> Float = { 0f },
) {
    val bitmap = description.imageConditionBitmap
    val pos = if (bitmap != null) {
        val left = ((size.width - bitmap.width) / 2).toInt()
        val top = ((size.height - bitmap.height) / 2).toInt()
        val imageBitmap = bitmap.asImageBitmap()
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(bitmap.width, bitmap.height),
        )
        Offset(size.width / 2f, size.height / 2f)
    } else {
        description.position?.let { Offset(it.x, it.y) }
    } ?: return

    val animatedRadius = outerRadiusPx * scale

    // Radial gradient glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(backgroundColor, Color.Transparent),
            center = pos,
            radius = outerRadiusPx * 1.75f,
        ),
        radius = animatedRadius * 2f,
        center = pos,
    )

    // Outer stroke circle
    drawCircle(
        color = primaryColor,
        radius = animatedRadius,
        center = pos,
        style = Stroke(width = thicknessPx),
    )

    // Inner dot
    drawCircle(
        color = innerColor,
        radius = innerRadiusPx,
        center = pos,
        style = Fill,
    )
    number?.let { drawActionNumber(it, pos, animatedRadius, isFocused, labelOffsetFor(pos)) }
}

private fun DrawScope.drawSwipeIndicator(
    description: SwipeDescription,
    progress: Float?,
    outerRadiusPx: Float,
    innerRadiusPx: Float,
    thicknessPx: Float,
    primaryColor: Color,
    secondaryColor: Color,
    innerColor: Color,
    backgroundColor: Color,
    number: Int? = null,
    isFocused: Boolean = true,
    labelOffsetFor: (Offset) -> Float = { 0f },
) {
    val from = description.from?.let { Offset(it.x, it.y) }
    val to = description.to?.let { Offset(it.x, it.y) }

    if (from == null && to == null) return

    from?.let { p ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                center = p,
                radius = outerRadiusPx * 1.75f,
            ),
            radius = outerRadiusPx * 2f,
            center = p,
        )
        drawCircle(
            color = primaryColor,
            radius = outerRadiusPx,
            center = p,
            style = Stroke(width = thicknessPx),
        )
        drawCircle(
            color = innerColor,
            radius = innerRadiusPx,
            center = p,
            style = Fill,
        )
    }

    to?.let { p ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                center = p,
                radius = outerRadiusPx * 1.75f,
            ),
            radius = outerRadiusPx * 2f,
            center = p,
        )
        drawCircle(
            color = secondaryColor,
            radius = outerRadiusPx,
            center = p,
            style = Stroke(width = thicknessPx),
        )
        drawCircle(
            color = innerColor,
            radius = innerRadiusPx,
            center = p,
            style = Fill,
        )
    }

    if (from != null && to != null) {
        drawLine(
            color = if (isFocused) innerColor else innerColor.copy(alpha = 0.7f),
            start = from,
            end = to,
            strokeWidth = innerRadiusPx / 2f,
        )

        val dx = to.x - from.x
        val dy = to.y - from.y
        val mag = hypot(dx, dy)
        if (mag > 0f && progress != null) {
            val animX = from.x + (dx / mag) * (mag * progress)
            val animY = from.y + (dy / mag) * (mag * progress)
            val animPos = Offset(animX, animY)

            drawCircle(
                color = innerColor,
                radius = innerRadiusPx * 2f,
                center = animPos,
                style = Fill,
            )
            drawCircle(
                color = backgroundColor,
                radius = innerRadiusPx * 2f,
                center = animPos,
                style = Stroke(width = innerRadiusPx * 0.75f),
            )
        }
    }
    number?.let { order ->
        from?.let { drawActionNumber(order, it, outerRadiusPx, isFocused, labelOffsetFor(it)) }
        to?.let { drawActionNumber(order, it, outerRadiusPx, isFocused, labelOffsetFor(it)) }
    }
}

private fun DrawScope.drawActionNumber(order: Int, center: Offset, ringRadius: Float, isFocused: Boolean, offsetX: Float) {
    val text = order.toString()
    val labelY = center.y - ringRadius * 0.6f
    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textAlign = AndroidPaint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", if (isFocused) Typeface.BOLD else Typeface.NORMAL)
        textSize = 12.sp.toPx().coerceAtMost(ringRadius * 0.65f)
    }
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        textPaint.style = AndroidPaint.Style.STROKE
        textPaint.strokeWidth = 2.dp.toPx()
        textPaint.strokeJoin = AndroidPaint.Join.ROUND
        textPaint.color = AndroidColor.BLACK
        nativeCanvas.drawText(text, center.x + offsetX, labelY, textPaint)
        textPaint.style = AndroidPaint.Style.FILL
        textPaint.color = AndroidColor.WHITE
        nativeCanvas.drawText(text, center.x + offsetX, labelY, textPaint)
    }
}

private fun DrawScope.drawPauseIndicator(
    rotationDegrees: Float,
    outerRadiusPx: Float,
    thicknessPx: Float,
    primaryColor: Color,
    innerColor: Color,
    backgroundColor: Color,
) {
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(backgroundColor, Color.Transparent),
            center = center,
            radius = outerRadiusPx * 1.75f,
        ),
        radius = outerRadiusPx * 2f,
        center = center,
    )

    drawCircle(
        color = primaryColor,
        radius = outerRadiusPx,
        center = center,
        style = Stroke(width = thicknessPx),
    )

    rotate(rotationDegrees, pivot = center) {
        val handLeft = center.x - 5f
        val handTop = center.y - (outerRadiusPx - thicknessPx * 1.5f)
        val handRight = center.x + 5f
        val handBottom = center.y + 5f

        drawRoundRect(
            color = innerColor,
            topLeft = Offset(handLeft, handTop),
            size = Size(handRight - handLeft, handBottom - handTop),
            cornerRadius = CornerRadius(4f, 4f),
            style = Fill,
        )
    }
}

private fun DrawScope.drawImageConditionIndicator(
    description: ImageConditionDescription,
    displayConfig: DisplayConfig,
    thicknessPx: Float,
    cornerRadiusPx: Float,
    primaryColor: Color,
    backgroundColor: Color,
) {
    val detectionArea = description.conditionDetectionArea ?: return
    val pos = description.conditionPosition
    val bitmap = description.conditionBitmap

    when (description.conditionDetectionType) {
        ImageConditionBriefRenderingType.WHOLE_SCREEN -> {
            drawDisplayBorder(displayConfig, primaryColor, thicknessPx)
            if (bitmap != null) {
                val offsetX = (detectionArea.width() - pos.width()) / 2
                val offsetY = (detectionArea.height() - pos.height()) / 2
                val left = detectionArea.left + offsetX
                val top = detectionArea.top + offsetY
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(pos.width(), pos.height()),
                )
            }
        }

        ImageConditionBriefRenderingType.AREA, ImageConditionBriefRenderingType.EXACT -> {
            val holeRect = Rect(
                detectionArea.left.toFloat(),
                detectionArea.top.toFloat(),
                detectionArea.right.toFloat(),
                detectionArea.bottom.toFloat(),
            )
            val fullPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(holeRect)
            }
            drawPath(fullPath, color = backgroundColor)

            val borderLeft = max(0f, holeRect.left - thicknessPx)
            val borderTop = max(0f, holeRect.top - thicknessPx)
            val borderRight = min(size.width, holeRect.right + thicknessPx)
            val borderBottom = min(size.height, holeRect.bottom + thicknessPx)

            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(borderLeft, borderTop),
                size = Size(borderRight - borderLeft, borderBottom - borderTop),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = thicknessPx),
            )

            if (bitmap != null) {
                val imageOffset = if (description.conditionDetectionType == ImageConditionBriefRenderingType.AREA) {
                    val offsetX = (detectionArea.width() - pos.width()) / 2
                    val offsetY = (detectionArea.height() - pos.height()) / 2
                    IntOffset(detectionArea.left + offsetX, detectionArea.top + offsetY)
                } else {
                    IntOffset(pos.left, pos.top)
                }
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = imageOffset,
                    dstSize = IntSize(pos.width(), pos.height()),
                )
            }
        }
    }
}

private fun DrawScope.drawColorConditionIndicator(
    description: ColorConditionDescription,
    primaryColor: Color,
) {
    val pos = description.conditionPosition
    // Horizontal line
    drawLine(
        color = primaryColor,
        start = Offset(0f, pos.y),
        end = Offset(size.width, pos.y),
        strokeWidth = 5f,
    )
    // Vertical line
    drawLine(
        color = primaryColor,
        start = Offset(pos.x, 0f),
        end = Offset(pos.x, size.height),
        strokeWidth = 5f,
    )
}

private fun DrawScope.drawTextConditionIndicator(
    description: TextConditionDescription,
    thicknessPx: Float,
    cornerRadiusPx: Float,
    primaryColor: Color,
    backgroundColor: Color,
) {
    val detectionArea = description.conditionDetectionArea
    val holeRect = Rect(
        detectionArea.left.toFloat(),
        detectionArea.top.toFloat(),
        detectionArea.right.toFloat(),
        detectionArea.bottom.toFloat(),
    )
    val fullPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(0f, 0f, size.width, size.height))
        addRect(holeRect)
    }
    drawPath(fullPath, color = backgroundColor)

    val borderLeft = max(0f, holeRect.left - thicknessPx)
    val borderTop = max(0f, holeRect.top - thicknessPx)
    val borderRight = min(size.width, holeRect.right + thicknessPx)
    val borderBottom = min(size.height, holeRect.bottom + thicknessPx)
    val borderSize = Size(borderRight - borderLeft, borderBottom - borderTop)

    val text = description.conditionText
    if (text.isNotEmpty()) {
        // Vertical gradient behind text
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xA0, 0x00, 0x00, 0x00), Color.Transparent),
                startY = borderTop,
                endY = borderBottom,
            ),
            topLeft = Offset(borderLeft, borderTop),
            size = borderSize,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        )
    }

    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(borderLeft, borderTop),
        size = borderSize,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(width = thicknessPx),
    )

    if (text.isNotEmpty()) {
        val margin = 8f + thicknessPx / 2f
        val maxWidth = borderSize.width - 2 * margin
        val maxHeight = borderSize.height - 2 * margin
        if (maxWidth > 0f && maxHeight > 0f) {
            drawIntoCanvas { canvas ->
                val textPaint = AndroidPaint().apply {
                    isAntiAlias = true
                    color = AndroidColor.WHITE
                    textAlign = AndroidPaint.Align.CENTER
                    textSize = 48f
                }
                val bounds = AndroidRect()
                textPaint.getTextBounds(text, 0, text.length, bounds)

                val widthScale = maxWidth / bounds.width().coerceAtLeast(1)
                val heightScale = maxHeight / bounds.height().coerceAtLeast(1)
                val scale = min(1f, min(widthScale, heightScale))

                textPaint.textSize = 48f * scale
                val textX = borderLeft + borderSize.width / 2f
                val textY = (borderTop + borderSize.height / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.nativeCanvas.drawText(text, textX, textY, textPaint)
            }
        }
    }
}

private fun DrawScope.drawDefaultIndicator(
    description: DefaultDescription,
    outerRadiusPx: Float,
    backgroundColor: Color,
) {
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(backgroundColor, Color.Transparent),
            center = center,
            radius = outerRadiusPx * 1.75f,
        ),
        radius = outerRadiusPx * 2f,
        center = center,
    )

    description.icon?.let { icon ->
        drawIntoCanvas { canvas ->
            val iconSize = outerRadiusPx.toInt()
            icon.setBounds(
                center.x.toInt() - iconSize,
                center.y.toInt() - iconSize,
                center.x.toInt() + iconSize,
                center.y.toInt() + iconSize,
            )
            icon.draw(canvas.nativeCanvas)
        }
    }
}
