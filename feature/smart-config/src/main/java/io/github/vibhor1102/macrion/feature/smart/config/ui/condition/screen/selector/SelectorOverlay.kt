/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.selector

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

import io.github.vibhor1102.macrion.feature.smart.config.R

/**
 * The state and coordinate conversion shared by the image and existing-area selectors.
 * Coordinates deliberately remain physical screen pixels: that is the contract used by the
 * accessibility capture API and by the pre-Compose selector implementation.
 */
class SelectorController(
    val hasCapture: Boolean,
    private val onValidityChanged: ((Boolean) -> Unit)? = null,
) {
    var selectedArea by mutableStateOf(RectF())
        private set
    var captureArea by mutableStateOf(RectF())
        private set
    var zoomLevel by mutableFloatStateOf(1f)
        private set
    var screenshot by mutableStateOf<Bitmap?>(null)
        private set
    var visible by mutableStateOf(!hasCapture)
        private set
    var initialised by mutableStateOf(false)
        private set
    var interactionVersion by mutableIntStateOf(0)
        private set
    var revealVersion by mutableIntStateOf(0)
        private set
    var hintGesture by mutableStateOf<SelectorGesture?>(null)
        private set
    var captureInputEnabled by mutableStateOf(!hasCapture)
        private set

    private var minimumWidth = 0f
    private var minimumHeight = 0f
    private var screenSize = Size.Zero
    private var isValid = false

    fun setShown(value: Boolean) {
        visible = value
        hintGesture = null
        if (!value && hasCapture) {
            screenshot = null
            captureArea = RectF()
            initialised = false
            if (isValid) {
                isValid = false
                onValidityChanged?.invoke(false)
            }
        }
    }

    fun setAreaSelection(initialArea: Rect, minimalArea: Rect) {
        selectedArea = RectF(initialArea)
        minimumWidth = minimalArea.width().toFloat()
        minimumHeight = minimalArea.height().toFloat()
        initialised = true
        interactionVersion++
        hintGesture = null
        validate()
        revealVersion++
    }

    fun showCapture(bitmap: Bitmap) {
        screenshot = bitmap
        visible = true
        initialised = false
        zoomLevel = 1f
        captureInputEnabled = false
        captureArea = RectF(0f, 0f, screenSize.width, screenSize.height)
        hintGesture = null
        initializeDefaultSelection()
        validate()
        interactionVersion++
        revealVersion++
    }

    fun enableCaptureInput() { captureInputEnabled = true }

    /** Mirrors CaptureComponent.setZoomLevel() during the selector reveal animation. */
    fun setRevealZoom(level: Float) {
        if (!hasCapture || screenSize == Size.Zero || level == zoomLevel) return
        val nextZoom = level.coerceIn(.8f, 3f)
        val pivot = Offset(screenSize.width / 2f, screenSize.height / 2f)
        val ratio = nextZoom / zoomLevel
        captureArea = RectF(captureArea).also { area ->
            area.left = pivot.x + (area.left - pivot.x) * ratio
            area.top = pivot.y + (area.top - pivot.y) * ratio
            area.right = pivot.x + (area.right - pivot.x) * ratio
            area.bottom = pivot.y + (area.bottom - pivot.y) * ratio
        }
        zoomLevel = nextZoom
        validate()
    }

    fun onCanvasSizeChanged(size: Size) {
        if (size == screenSize || size == Size.Zero) return
        screenSize = size
        if (captureArea.isEmpty) captureArea = RectF(0f, 0f, size.width, size.height)
        initializeDefaultSelection()
        selectedArea = constrain(selectedArea)
        validate()
    }

    /** Initialize independently of layout changes: a new screenshot normally keeps the same canvas size. */
    private fun initializeDefaultSelection() {
        if (initialised || screenSize == Size.Zero) return
        // SelectorComponent received 230dp × 130dp for its *outer* border. Its exported
        // selection was inset by half the 4dp stroke, which is important for both the visible
        // crop and its persisted coordinates.
        val inset = selectorBorderInsetPx
        val width = 230.dpPx - inset * 2f
        val height = 130.dpPx - inset * 2f
        selectedArea = RectF(
            screenSize.width / 2f - width / 2f,
            screenSize.height / 2f - height / 2f,
            screenSize.width / 2f + width / 2f,
            screenSize.height / 2f + height / 2f,
        )
        minimumWidth = screenSize.width * .10f
        minimumHeight = screenSize.height * .05f
        initialised = true
    }

    internal fun moveOrResize(kind: SelectorGesture, delta: Offset) {
        val value = RectF(selectedArea)
        when (kind) {
            SelectorGesture.Left -> value.left = min(value.left + delta.x, value.right - minimumWidth)
            SelectorGesture.Top -> value.top = min(value.top + delta.y, value.bottom - minimumHeight)
            SelectorGesture.Right -> value.right = max(value.right + delta.x, value.left + minimumWidth)
            SelectorGesture.Bottom -> value.bottom = max(value.bottom + delta.y, value.top + minimumHeight)
            SelectorGesture.Move -> value.offset(delta.x, delta.y)
            SelectorGesture.Capture -> return
        }
        selectedArea = constrain(value)
        interactionVersion++
        hintGesture = kind
        validate()
    }

    fun transformCapture(pan: Offset, zoomChange: Float, pivot: Offset) {
        if (!hasCapture || screenSize == Size.Zero) return
        val nextZoom = (zoomLevel * zoomChange).coerceIn(.8f, 3f)
        val ratio = nextZoom / zoomLevel
        val area = RectF(captureArea)
        area.left = pivot.x + (area.left - pivot.x) * ratio + pan.x
        area.top = pivot.y + (area.top - pivot.y) * ratio + pan.y
        area.right = pivot.x + (area.right - pivot.x) * ratio + pan.x
        area.bottom = pivot.y + (area.bottom - pivot.y) * ratio + pan.y
        val horizontalMargin = screenSize.width * .2f
        val verticalMargin = screenSize.height * .2f
        val dx = when {
            area.left > screenSize.width - horizontalMargin -> screenSize.width - horizontalMargin - area.left
            area.right < horizontalMargin -> horizontalMargin - area.right
            else -> 0f
        }
        val dy = when {
            area.top > screenSize.height - verticalMargin -> screenSize.height - verticalMargin - area.top
            area.bottom < verticalMargin -> verticalMargin - area.bottom
            else -> 0f
        }
        area.offset(dx, dy)
        captureArea = area
        zoomLevel = nextZoom
        interactionVersion++
        hintGesture = SelectorGesture.Capture
        validate()
    }

    fun getCaptureSelection(): Pair<Rect, Bitmap> {
        val bitmap = checkNotNull(screenshot) { "There is no screen capture." }
        check(isValid) { "Can't get a selection, selector is invalid." }
        val result = RectF(selectedArea)
        check(result.intersect(captureArea)) { "The selection is outside the capture." }
        val inverseZoom = 1f / zoomLevel
        result.set(
            (result.left - captureArea.left) * inverseZoom,
            (result.top - captureArea.top) * inverseZoom,
            (result.right - captureArea.left) * inverseZoom,
            (result.bottom - captureArea.top) * inverseZoom,
        )
        result.intersect(0f, 0f, screenSize.width, screenSize.height)
        val area = result.toRect()
        val bounds = Rect(0, 0, bitmap.width, bitmap.height)
        check(bounds.intersect(area)) { "The selection is outside the screenshot." }
        return bounds to Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
    }

    fun getAreaSelection(): Rect = selectedArea.toRect()

    private fun constrain(value: RectF): RectF {
        if (screenSize == Size.Zero) return value
        if (value.width() > screenSize.width) { value.left = 0f; value.right = screenSize.width }
        if (value.height() > screenSize.height) { value.top = 0f; value.bottom = screenSize.height }
        if (value.left < 0f) value.offset(-value.left, 0f)
        if (value.top < 0f) value.offset(0f, -value.top)
        if (value.right > screenSize.width) value.offset(screenSize.width - value.right, 0f)
        if (value.bottom > screenSize.height) value.offset(0f, screenSize.height - value.bottom)
        return value
    }

    private fun validate() {
        if (!hasCapture) return
        val intersection = RectF(selectedArea)
        val newValidity = intersection.intersect(captureArea) &&
            intersection.width() >= 50f && intersection.height() >= 50f
        if (newValidity != isValid) {
            isValid = newValidity
            onValidityChanged?.invoke(newValidity)
        }
    }

    private fun RectF.toRect() = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    private val Int.dpPx: Float get() = this * android.content.res.Resources.getSystem().displayMetrics.density
    private val selectorBorderInsetPx: Float get() = ceil(4.dpPx / 2f)
}

enum class SelectorGesture { Left, Top, Right, Bottom, Move, Capture }

@Composable
fun SelectorOverlay(controller: SelectorController, modifier: Modifier = Modifier) {
    val outline = Color.White
    // These are the exact legacy resource dimensions, converted to the physical coordinate
    // space used by screen captures. Do not replace them with literal pixels.
    val borderWidth = 4.dpPx
    val borderInset = ceil(borderWidth / 2f)
    val cornerRadius = 2.dpPx
    val selectorAlpha = remember { Animatable(0f) }
    val backgroundAlpha = remember { Animatable(0f) }
    val hintsAlpha = remember { Animatable(0f) }
    val captureZoom = remember { Animatable(1f) }

    LaunchedEffect(controller.revealVersion, controller.visible) {
        selectorAlpha.snapTo(0f)
        backgroundAlpha.snapTo(0f)
        hintsAlpha.snapTo(0f)
        if (!controller.visible) return@LaunchedEffect
        coroutineScope {
            launch { selectorAlpha.animateTo(1f, tween(500, easing = LinearEasing)) }
            launch { backgroundAlpha.animateTo(119f / 255f, tween(500, easing = LinearEasing)) }
            launch { hintsAlpha.animateTo(1f, tween(500, easing = LinearEasing)) }
            // ImageSelectorAnimations used DecelerateInterpolator(2f): 1 - (1 - t)^4.
            if (controller.hasCapture) launch {
                captureZoom.snapTo(1f)
                captureZoom.animateTo(.8f, tween(750, easing = Easing { fraction ->
                    1f - (1f - fraction) * (1f - fraction) * (1f - fraction) * (1f - fraction)
                })) {
                    controller.setRevealZoom(value)
                }
            }
        }
        if (controller.hasCapture) controller.enableCaptureInput()
        // The original animator started this after its complete reveal set finished (750ms for
        // an image selector, 500ms for an area selector), then held it for one second.
        delay(1_000)
        hintsAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
    }
    // Legacy HintsComponent made the relevant hints fully visible while a gesture was active.
    LaunchedEffect(controller.interactionVersion) {
        if (controller.hintGesture != null && controller.visible) {
            hintsAlpha.snapTo(1f)
            // Each pointer update replaces this effect. Therefore the countdown begins only
            // once movement has stopped, rather than leaving a stale arrow permanently visible.
            delay(1_000)
            hintsAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier.fillMaxSize().onSizeChanged {
        controller.onCanvasSizeChanged(Size(it.width.toFloat(), it.height.toFloat()))
    }) {
        // Keep the captured frame and selector in distinct RenderNodes. A selector must remain a
        // foreground layer even while the image is moved or transformed beneath it.
        Canvas(Modifier.fillMaxSize()) {
            if (controller.visible && controller.hasCapture) drawRect(Color.Black)
            controller.screenshot?.let { image ->
                val area = controller.captureArea
                drawImage(
                    image = image.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(area.left.roundToInt(), area.top.roundToInt()),
                    dstSize = IntSize(area.width().roundToInt(), area.height().roundToInt()),
                    filterQuality = FilterQuality.None,
                )
            }
        }
        Canvas(
            Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(controller, controller.visible) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!controller.visible) return@awaitEachGesture
                    val startArea = controller.selectedArea
                    val kind = gestureAt(startArea, down.position)
                    if (kind == SelectorGesture.Capture && !controller.captureInputEnabled) return@awaitEachGesture
                    var previous = down.position
                    var pointerId: PointerId = down.id
                    var twoFingerStartDistance = 0f
                    var twoFingerCenter = Offset.Zero
                    var wasPinching = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.filter { it.pressed }
                        if (active.size >= 2 && kind == SelectorGesture.Capture) {
                            val first = active[0].position
                            val second = active[1].position
                            val center = (first + second) / 2f
                            val distance = (first - second).getDistance()
                            if (twoFingerStartDistance != 0f) controller.transformCapture(
                                center - twoFingerCenter, distance / twoFingerStartDistance, center,
                            )
                            twoFingerStartDistance = distance
                            twoFingerCenter = center
                            wasPinching = true
                            active.forEach { it.consume() }
                        } else if (active.size == 1 && wasPinching && kind == SelectorGesture.Capture) {
                            // A pointer-up event still carries the removed finger's old position.
                            // Treating that position as a pan delta teleported the capture as soon
                            // as a pinch ended. Rebase on the surviving finger instead.
                            pointerId = active.single().id
                            previous = active.single().position
                            twoFingerStartDistance = 0f
                            twoFingerCenter = Offset.Zero
                            wasPinching = false
                        } else if (active.size == 1) {
                            val changed = active.firstOrNull { it.id == pointerId } ?: active.single().also {
                                pointerId = it.id
                                previous = it.position
                            }
                            if (changed.position == previous) continue
                            val delta = changed.position - previous
                            if (kind == SelectorGesture.Capture) controller.transformCapture(delta, 1f, changed.position)
                            else controller.moveOrResize(kind, delta)
                            previous = changed.position
                            changed.consume()
                        }
                        if (event.changes.all { it.changedToUpIgnoreConsumed() }) break
                    }
                }
            },
        ) {
            if (controller.visible && controller.initialised) {
                val area = controller.selectedArea
                val selectorArea = RectF(area).also { it.inset(-borderInset, -borderInset) }
                val background = Color.Black.copy(alpha = backgroundAlpha.value)
                // Four bands rather than BlendMode.Clear: the overlay is hosted by WindowManager and
                // must remain transparent inside the selection on every Android GPU path.
                drawRect(background, Offset.Zero, Size(size.width, area.top))
                drawRect(background, Offset(0f, area.top), Size(area.left, area.height()))
                drawRect(background, Offset(area.right, area.top), Size(size.width - area.right, area.height()))
                drawRect(background, Offset(0f, area.bottom), Size(size.width, size.height - area.bottom))
                drawRoundRect(
                    color = outline.copy(alpha = selectorAlpha.value),
                    topLeft = Offset(selectorArea.left, selectorArea.top),
                    size = Size(selectorArea.width(), selectorArea.height()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                    style = Stroke(borderWidth),
                )
            }
        }
        if (controller.visible && controller.initialised) SelectorHints(
            area = controller.selectedArea,
            alpha = hintsAlpha.value,
            gesture = controller.hintGesture,
            hasCapture = controller.hasCapture,
        )
    }
}

private fun gestureAt(area: RectF, position: Offset): SelectorGesture {
    val handle = 50f * android.content.res.Resources.getSystem().displayMetrics.density
    val inner = handle / 3f
    fun inRect(left: Float, top: Float, right: Float, bottom: Float) =
        position.x in left..right && position.y in top..bottom
    val enough = area.width() > handle && area.height() > handle
    return when {
        inRect(area.left - handle, area.top, if (enough) area.left + inner else area.left, area.bottom) -> SelectorGesture.Left
        inRect(area.left, area.top - handle, area.right, if (enough) area.top + inner else area.top) -> SelectorGesture.Top
        inRect(if (enough) area.right - inner else area.right, area.top, area.right + handle, area.bottom) -> SelectorGesture.Right
        inRect(area.left, if (enough) area.bottom - inner else area.bottom, area.right, area.bottom + handle) -> SelectorGesture.Bottom
        inRect(area.left, area.top, area.right, area.bottom) -> SelectorGesture.Move
        else -> SelectorGesture.Capture
    }
}

@Composable
private fun SelectorHints(area: RectF, alpha: Float, gesture: SelectorGesture?, hasCapture: Boolean) {
    val size = 30.dp
    val halfSize = 15.dpPx
    val margin = 5.dpPx
    @Composable fun icon(@DrawableRes res: Int, x: Float, y: Float, insideSelection: Boolean = false) {
        // HintsComponent hid any icon whose complete bounds were outside the screen, and hid
        // inner arrows that no longer fitted inside a small selector.
        val left = x - halfSize
        val top = y - halfSize
        val right = x + halfSize
        val bottom = y + halfSize
        val screenWidth = android.content.res.Resources.getSystem().displayMetrics.widthPixels.toFloat()
        val screenHeight = android.content.res.Resources.getSystem().displayMetrics.heightPixels.toFloat()
        if (left < 0f || top < 0f || right > screenWidth || bottom > screenHeight) return
        if (insideSelection && (left < area.left || top < area.top || right > area.right || bottom > area.bottom)) return
        Image(
            painterResource(res), null,
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }.size(size).alpha(alpha),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
    val edgeOffset = margin + halfSize
    val showingAll = gesture == null
    if (showingAll || gesture == SelectorGesture.Move) icon(R.drawable.ic_hint_move, area.centerX(), area.centerY(), insideSelection = true)
    if (showingAll || gesture == SelectorGesture.Top) {
        icon(R.drawable.ic_hint_resize_up, area.centerX(), area.top - edgeOffset)
        if (!showingAll) icon(R.drawable.ic_hint_resize_down, area.centerX(), area.top + edgeOffset, insideSelection = true)
    }
    if (showingAll || gesture == SelectorGesture.Bottom) {
        icon(R.drawable.ic_hint_resize_down, area.centerX(), area.bottom + edgeOffset)
        if (!showingAll) icon(R.drawable.ic_hint_resize_up, area.centerX(), area.bottom - edgeOffset, insideSelection = true)
    }
    if (showingAll || gesture == SelectorGesture.Left) {
        icon(R.drawable.ic_hint_resize_left, area.left - edgeOffset, area.centerY())
        if (!showingAll) icon(R.drawable.ic_hint_resize_right, area.left + edgeOffset, area.centerY(), insideSelection = true)
    }
    if (showingAll || gesture == SelectorGesture.Right) {
        icon(R.drawable.ic_hint_resize_right, area.right + edgeOffset, area.centerY())
        if (!showingAll) icon(R.drawable.ic_hint_resize_left, area.right - edgeOffset, area.centerY(), insideSelection = true)
    }
    if (hasCapture && (showingAll || gesture == SelectorGesture.Capture)) icon(R.drawable.ic_hint_pinch, area.centerX(), area.top - 100.dpPx)
}

private val Int.dpPx: Float get() = this * android.content.res.Resources.getSystem().displayMetrics.density
