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
import android.graphics.PathMeasure
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
    showTouchLocationsInPreview: Boolean = true,
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
    val labelAngles = remember(carouselDescription) {
        carouselDescription?.let(::calculateLabelAngles).orEmpty()
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
    val animateTouchLocations = animate && (carouselDescription == null || showTouchLocationsInPreview)
    val clickScale = remember { Animatable(1f) }
    LaunchedEffect(animationDescription, animateTouchLocations) {
        val clickDescList = when (animationDescription) {
            is ClickDescription -> listOf(animationDescription)
            else -> emptyList()
        }
        if (!animateTouchLocations || clickDescList.isEmpty()) {
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
    LaunchedEffect(animationDescription, animateTouchLocations) {
        val swipeDescList = when (animationDescription) {
            is SwipeDescription -> listOf(animationDescription)
            else -> emptyList()
        }.filter { it.from != null && it.to != null }

        if (!animateTouchLocations || swipeDescList.isEmpty()) {
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
    LaunchedEffect(animationDescription, animateTouchLocations) {
        val children = (animationDescription as? SplitDescription)?.subDescriptions.orEmpty()
        val endTime = children.maxOfOrNull { child ->
            when (child) {
                is SwipeDescription -> child.startOffsetMs + child.swipeDurationMs
                is ClickDescription -> child.startOffsetMs + child.pressDurationMs
                else -> 0L
            }
        } ?: 0L
        if (!animateTouchLocations || endTime <= 0L) {
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
                val badges = if (showTouchLocationsInPreview) {
                    buildList {
                        description.focusedFallback?.let { fallback ->
                            addAll(actionNumberBadges(fallback, description.focusedOrder, true,
                                animate, clickScale.value, simultaneousTime.value, outerRadiusPx, labelAngles))
                        }
                        description.previews.forEach { preview ->
                            addAll(actionNumberBadges(preview.description, preview.order,
                                preview.order == description.focusedOrder, animate && preview.order == description.focusedOrder,
                                clickScale.value, simultaneousTime.value, outerRadiusPx, labelAngles))
                        }
                    }
                } else emptyList()
                val distinctBadges = badges.distinctBy { it.order to it.center }
                // End crossing geometry at the badge stroke's centerline. The stroke covers
                // that join, while its inner half keeps paths out of the transparent center.
                val badgeCutouts = Path().apply {
                    distinctBadges.forEach { badge ->
                        val cutoutRadius = actionNumberBadgeRadius.toPx()
                        addOval(Rect(
                            badge.center.x - cutoutRadius, badge.center.y - cutoutRadius,
                            badge.center.x + cutoutRadius, badge.center.y + cutoutRadius,
                        ))
                    }
                }
                val drawCarouselActions: DrawScope.(IndicatorPass) -> Unit = { pass ->
                    when (val fallback = description.focusedFallback) {
                        is ClickDescription -> drawClickIndicator(
                            description = fallback,
                            showTouchLocation = showTouchLocationsInPreview,
                            scale = if (animate) clickScale.value else 1f,
                            outerRadiusPx = outerRadiusPx,
                            innerRadiusPx = innerRadiusPx,
                            thicknessPx = thicknessPx,
                            primaryColor = primaryColor,
                            innerColor = innerColor,
                            backgroundColor = backgroundColor,
                            pass = pass,
                        )
                        is PauseDescription -> if (pass != IndicatorPass.BACKGROUND) drawPauseIndicator(
                            rotationDegrees = if (animate) pauseRotation.value else 0f,
                            outerRadiusPx = outerRadiusPx,
                            thicknessPx = thicknessPx,
                            primaryColor = primaryColor,
                            innerColor = innerColor,
                            backgroundColor = backgroundColor,
                        )
                        is DefaultDescription -> if (pass != IndicatorPass.BACKGROUND) drawDefaultIndicator(
                            description = fallback,
                            outerRadiusPx = outerRadiusPx,
                            backgroundColor = backgroundColor,
                        )
                        is SplitDescription -> fallback.subDescriptions.forEach { child ->
                            if (child is ClickDescription) drawClickIndicator(
                                description = child,
                                showTouchLocation = showTouchLocationsInPreview,
                                scale = if (animate && childProgress(simultaneousTime.value, child.startOffsetMs, child.pressDurationMs) != null) 0.75f else 1f,
                                outerRadiusPx = outerRadiusPx,
                                innerRadiusPx = innerRadiusPx,
                                thicknessPx = thicknessPx,
                                primaryColor = primaryColor,
                                innerColor = innerColor,
                                backgroundColor = backgroundColor,
                                pass = pass,
                            )
                        }
                        else -> Unit
                    }
                    description.previews.filter { it.order != description.focusedOrder }.forEach { preview ->
                        drawNumberedActionPreview(
                            preview, false, false, clickScale.value, swipeProgress.value, simultaneousTime.value,
                            outerRadiusPx, innerRadiusPx, thicknessPx, primaryColor, secondaryColor, innerColor, backgroundColor,
                            showTouchLocations = showTouchLocationsInPreview,
                            pass = pass,
                        )
                    }
                    description.previews.firstOrNull { it.order == description.focusedOrder }?.let { preview ->
                        drawNumberedActionPreview(
                            preview, true, animate, clickScale.value, swipeProgress.value, simultaneousTime.value,
                            outerRadiusPx, innerRadiusPx, thicknessPx, primaryColor, secondaryColor, innerColor, backgroundColor,
                            showTouchLocations = showTouchLocationsInPreview,
                            pass = pass,
                        )
                    }
                }
                // The badge cuts only the visible geometry. Its transparent center still
                // receives the same background haze as the rest of the target.
                drawCarouselActions(IndicatorPass.BACKGROUND)
                clipPath(badgeCutouts, clipOp = ClipOp.Difference) {
                    drawCarouselActions(IndicatorPass.FOREGROUND)
                }
                distinctBadges.filterNot { it.isFocused }.forEach { drawActionNumber(it, innerColor.copy(alpha = 0.7f)) }
                distinctBadges.filter { it.isFocused }.forEach { drawActionNumber(it, innerColor) }
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

/** Spread only numbers at identical targets; the rings and precision dots stay on their exact coordinates. */
private fun calculateLabelAngles(description: ActionCarouselDescription): Map<Pair<Int, Offset>, Float> {
    val markers = description.previews.flatMap { preview ->
        preview.description.markerPositions().map { preview.order to it }
    }.distinct()
    return markers.groupBy { it.second }.values.flatMap { atPosition ->
        val spacing = min(47f, 340f / (atPosition.size - 1).coerceAtLeast(1))
        atPosition.mapIndexed { index, marker ->
            marker to (index - (atPosition.size - 1) / 2f) * spacing
        }
    }.toMap()
}

private fun ItemBriefDescription.markerPositions(): List<Offset> = when (this) {
    is ClickDescription -> position?.let { listOf(Offset(it.x, it.y)) }.orEmpty()
    is SwipeDescription -> numberBadgeTargets().map { Offset(it.x, it.y) }
    is SplitDescription -> subDescriptions.flatMap { it.markerPositions() }
    else -> emptyList()
}

private const val INACTIVE_PREVIEW_STROKE_ALPHA = 0.6f
private fun SwipeDescription.numberBadgeTargets(): List<PointF> =
    if (path?.isCurved == true) listOfNotNull(from) else listOfNotNull(from, to)

private enum class IndicatorPass { ALL, BACKGROUND, FOREGROUND }

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
    showTouchLocations: Boolean = true,
    pass: IndicatorPass = IndicatorPass.ALL,
) {
    val ringColor = if (isFocused) primaryColor else primaryColor.copy(alpha = INACTIVE_PREVIEW_STROKE_ALPHA)
    val markerColor = if (isFocused) innerColor else innerColor.copy(alpha = INACTIVE_PREVIEW_STROKE_ALPHA)
    val ringThickness = if (isFocused) thicknessPx else thicknessPx * 0.65f
    val hazeColor = if (isFocused) backgroundColor else backgroundColor.copy(alpha = backgroundColor.alpha * 0.6f)
    when (val action = preview.description) {
        is ClickDescription -> drawClickIndicator(
            description = action,
            showTouchLocation = showTouchLocations,
            scale = if (animate) clickScale else 1f,
            outerRadiusPx = outerRadiusPx,
            innerRadiusPx = innerRadiusPx,
            thicknessPx = ringThickness,
            primaryColor = ringColor,
            innerColor = markerColor,
            backgroundColor = hazeColor,
            pass = pass,
        )
        is SwipeDescription -> if (showTouchLocations) drawSwipeIndicator(
            description = action,
            progress = if (animate) swipeProgress else null,
            outerRadiusPx = outerRadiusPx,
            innerRadiusPx = innerRadiusPx,
            thicknessPx = ringThickness,
            primaryColor = ringColor,
            secondaryColor = if (isFocused) secondaryColor else secondaryColor.copy(alpha = INACTIVE_PREVIEW_STROKE_ALPHA),
            innerColor = markerColor,
            backgroundColor = hazeColor,
            isFocused = isFocused,
            pass = pass,
        )
        is SplitDescription -> action.subDescriptions.forEach { child ->
            when (child) {
                is ClickDescription -> drawClickIndicator(
                    description = child,
                    showTouchLocation = showTouchLocations,
                    scale = if (animate && childProgress(simultaneousTime, child.startOffsetMs, child.pressDurationMs) != null) 0.75f else 1f,
                    outerRadiusPx = outerRadiusPx,
                    innerRadiusPx = innerRadiusPx,
                    thicknessPx = ringThickness,
                    primaryColor = ringColor,
                    innerColor = markerColor,
                    backgroundColor = hazeColor,
                    pass = pass,
                )
                is SwipeDescription -> if (showTouchLocations) drawSwipeIndicator(
                    description = child,
                    progress = if (animate) childProgress(simultaneousTime, child.startOffsetMs, child.swipeDurationMs) else null,
                    outerRadiusPx = outerRadiusPx,
                    innerRadiusPx = innerRadiusPx,
                    thicknessPx = ringThickness,
                    primaryColor = ringColor,
                    secondaryColor = if (isFocused) secondaryColor else secondaryColor.copy(alpha = INACTIVE_PREVIEW_STROKE_ALPHA),
                    innerColor = markerColor,
                    backgroundColor = hazeColor,
                    isFocused = isFocused,
                    pass = pass,
                )
                else -> Unit
            }
        }
        else -> Unit
    }
}

private fun DrawScope.drawClickIndicator(
    description: ClickDescription,
    showTouchLocation: Boolean = true,
    scale: Float,
    outerRadiusPx: Float,
    innerRadiusPx: Float,
    thicknessPx: Float,
    primaryColor: Color,
    innerColor: Color,
    backgroundColor: Color,
    pass: IndicatorPass = IndicatorPass.ALL,
) {
    val bitmap = description.imageConditionBitmap
    val pos = if (bitmap != null) {
        val left = ((size.width - bitmap.width) / 2).toInt()
        val top = ((size.height - bitmap.height) / 2).toInt()
        if (pass != IndicatorPass.FOREGROUND) {
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(bitmap.width, bitmap.height),
            )
        }
        Offset(size.width / 2f, size.height / 2f)
    } else {
        description.position?.let { Offset(it.x, it.y) }
    } ?: return

    if (!showTouchLocation) return

    val animatedRadius = outerRadiusPx * scale

    // Keep the glow's drawing area fixed so the shrinking ring cannot clip its gradient.
    if (pass != IndicatorPass.FOREGROUND) drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(backgroundColor, Color.Transparent),
            center = pos,
            radius = outerRadiusPx * 1.75f,
        ),
        radius = outerRadiusPx * 2f,
        center = pos,
    )

    if (pass == IndicatorPass.BACKGROUND) return

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
    isFocused: Boolean = true,
    pass: IndicatorPass = IndicatorPass.ALL,
) {
    val from = description.from?.let { Offset(it.x, it.y) }
    val to = description.to?.let { Offset(it.x, it.y) }
    val curved = description.path?.isCurved == true
    val endRingRadius = if (curved) outerRadiusPx * 0.55f else outerRadiusPx
    val endDotRadius = if (curved) innerRadiusPx * 0.72f else innerRadiusPx

    if (from == null && to == null) return

    from?.let { p ->
        if (pass != IndicatorPass.FOREGROUND) drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                center = p,
                radius = outerRadiusPx * 1.75f,
            ),
            radius = outerRadiusPx * 2f,
            center = p,
        )
    }

    to?.let { p ->
        if (pass != IndicatorPass.FOREGROUND) drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                center = p,
                radius = if (curved) outerRadiusPx * 1.25f else outerRadiusPx * 1.75f,
            ),
            radius = outerRadiusPx * 2f,
            center = p,
        )
    }

    if (pass == IndicatorPass.BACKGROUND) return

    if (from != null && to != null) {
        val androidPath = description.previewTrace?.takeIf { it.size >= 2 }?.let { trace ->
            android.graphics.Path().apply {
                moveTo(trace.first().x, trace.first().y)
                for (index in 1 until trace.size) {
                    lineTo(trace[index].x, trace[index].y)
                }
            }
        } ?: description.path?.toAndroidPath() ?: android.graphics.Path().apply {
            moveTo(from.x, from.y)
            lineTo(to.x, to.y)
        }
        drawPath(
            path = androidPath.asComposePath(),
            color = if (isFocused) innerColor else innerColor.copy(alpha = INACTIVE_PREVIEW_STROKE_ALPHA),
            style = Stroke(width = innerRadiusPx / 2f),
        )

        // Draw handles in path order so an overlapping later node is visibly on top.
        from.let { center ->
            drawCircle(primaryColor, outerRadiusPx, center, style = Stroke(width = thicknessPx))
            drawCircle(innerColor, innerRadiusPx, center)
        }

        if (isFocused) description.path?.nodes?.drop(1)?.dropLast(1)?.forEach { node ->
            val center = Offset(node.position.x, node.position.y)
            drawCircle(primaryColor.copy(alpha = 0.8f), outerRadiusPx * 0.38f, center,
                style = Stroke(width = thicknessPx * 0.6f))
            drawCircle(innerColor, innerRadiusPx * 0.72f, center)
        }

        to.let { center ->
            drawCircle(secondaryColor, endRingRadius, center,
                style = Stroke(width = if (curved) thicknessPx * 0.7f else thicknessPx))
            drawCircle(innerColor, endDotRadius, center)
        }

        if (progress != null) {
            val measure = PathMeasure(androidPath, false)
            val position = FloatArray(2)
            if (measure.length > 0f && measure.getPosTan(measure.length * progress.coerceIn(0f, 1f), position, null)) {
                val animPos = Offset(position[0], position[1])

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
    } else {
        from?.let { center ->
            drawCircle(primaryColor, outerRadiusPx, center, style = Stroke(width = thicknessPx))
            drawCircle(innerColor, innerRadiusPx, center)
        }
        to?.let { center ->
            drawCircle(secondaryColor, endRingRadius, center,
                style = Stroke(width = if (curved) thicknessPx * 0.7f else thicknessPx))
            drawCircle(innerColor, endDotRadius, center)
        }
    }
}

private data class ActionNumberBadge(val order: Int, val center: Offset, val isFocused: Boolean)

private val actionNumberBadgeRadius = 11.dp

private fun DrawScope.numberBadgeStrokeWidth(isFocused: Boolean): Float =
    (if (isFocused) 2.dp else 1.5.dp).toPx()

private fun DrawScope.actionNumberBadges(
    description: ItemBriefDescription,
    order: Int,
    isFocused: Boolean,
    animate: Boolean,
    clickScale: Float,
    simultaneousTime: Float,
    outerRadiusPx: Float,
    labelAngles: Map<Pair<Int, Offset>, Float>,
): List<ActionNumberBadge> {
    fun badgeAt(target: Offset, radius: Float): ActionNumberBadge {
        val badgeRadius = actionNumberBadgeRadius.toPx()
        val preferredDegrees = labelAngles[order to target] ?: 0f
        val preferredAngle = Math.toRadians(preferredDegrees.toDouble())
        val anglesAtTarget = labelAngles.filterKeys { it.second == target }.values
        val spacing = Math.toRadians(min(47f, 340f / (anglesAtTarget.size - 1).coerceAtLeast(1)).toDouble())
        fun sideFor(degrees: Float) = when {
            degrees < 0f -> -1.0
            degrees > 0f -> 1.0
            target.x > size.width / 2f -> -1.0
            else -> 1.0
        }
        val side = sideFor(preferredDegrees)
        val rankOnSide = anglesAtTarget.count { sideFor(it) == side && abs(it) < abs(preferredDegrees) }
        // Rotate labels away from the clipped top edge while keeping their angular spacing.
        // This also moves continuously with an animated click ring instead of jumping below it.
        val topClearance = if (radius > 0f && target.y - radius < badgeRadius) {
            acos(((target.y - badgeRadius) / radius).coerceIn(-1f, 1f).toDouble())
        } else 0.0
        val angle = (side * max(abs(preferredAngle), topClearance + rankOnSide * spacing))
            .coerceIn(-Math.PI, Math.PI)
        val ringX = target.x + radius * sin(angle).toFloat()
        val ringY = target.y - radius * cos(angle).toFloat()
        return ActionNumberBadge(order, Offset(
            x = ringX.coerceIn(badgeRadius, (size.width - badgeRadius).coerceAtLeast(badgeRadius)),
            y = ringY.coerceIn(badgeRadius, (size.height - badgeRadius).coerceAtLeast(badgeRadius)),
        ), isFocused)
    }
    return when (description) {
        is ClickDescription -> {
            val target = if (description.imageConditionBitmap != null) Offset(size.width / 2f, size.height / 2f)
                else description.position?.let { Offset(it.x, it.y) }
            listOfNotNull(target?.let { badgeAt(it, outerRadiusPx * if (animate) clickScale else 1f) })
        }
        is SwipeDescription -> description.numberBadgeTargets()
            .map { badgeAt(Offset(it.x, it.y), outerRadiusPx) }
        is SplitDescription -> description.subDescriptions.flatMap { child ->
            val childScale = if (animate && child is ClickDescription &&
                childProgress(simultaneousTime, child.startOffsetMs, child.pressDurationMs) != null) 0.75f else 1f
            actionNumberBadges(child, order, isFocused, animate, childScale, simultaneousTime,
                outerRadiusPx, labelAngles)
        }
        else -> emptyList()
    }
}

private fun DrawScope.drawActionNumber(badge: ActionNumberBadge, badgeColor: Color) {
    val badgeRadius = actionNumberBadgeRadius.toPx()
    drawCircle(
        color = badgeColor,
        radius = badgeRadius,
        center = badge.center,
        style = Stroke(width = numberBadgeStrokeWidth(badge.isFocused)),
    )

    val text = badge.order.toString()
    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        textAlign = AndroidPaint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", if (badge.isFocused) Typeface.BOLD else Typeface.NORMAL)
        textSize = 16.sp.toPx().coerceAtMost(badgeRadius * 1.5f)
        val availableWidth = badgeRadius * 2f - 5.dp.toPx()
        val measuredWidth = measureText(text)
        if (measuredWidth > availableWidth) textSize *= availableWidth / measuredWidth
        style = AndroidPaint.Style.FILL
        color = badgeColor.toArgb()
    }
    drawIntoCanvas { canvas ->
        val baseline = badge.center.y - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.nativeCanvas.drawText(text, badge.center.x, baseline, textPaint)
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
