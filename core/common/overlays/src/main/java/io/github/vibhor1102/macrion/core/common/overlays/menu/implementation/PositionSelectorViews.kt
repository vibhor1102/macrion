/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.menu.implementation

import android.content.Context
import android.graphics.PointF
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.display.config.DisplayConfig
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.overlay.ItemBriefCanvas
import io.github.vibhor1102.macrion.core.ui.compose.overlay.GestureRecordOverlay
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.core.ui.views.gesturerecord.RecordedGesture

/** The nearest handle wins; a later node sits above an earlier one at equal distance. */
internal fun findSwipeNodeAt(points: List<SwipePoint>, touch: SwipePoint, hitRadiusPx: Float): Int? =
    points.withIndex()
        .filter { it.value.distanceTo(touch) <= hitRadiusPx }
        .minWithOrNull(compareBy<IndexedValue<SwipePoint>> { it.value.distanceTo(touch) }.thenByDescending { it.index })
        ?.index

internal fun SwipeDescription.dragInstruction(): Int =
    if ((path?.nodes?.size ?: 0) > 2) R.string.swipe_position_drag_node_instruction
    else R.string.swipe_position_drag_instruction

private fun Offset.toClampedPoint(width: Float, height: Float): PointF =
    PointF(x.coerceIn(0f, width), y.coerceIn(0f, height))

internal class PositionSelectorViews(
    context: Context,
    private val displayConfig: DisplayConfig,
) {
    val root: ComposeView = ComposeView(context)

    var onTouchListener: ((position: PointF) -> Unit)? = null
    var onSwipeNodeDragged: ((index: Int, position: PointF) -> Unit)? = null
    var onSwipeDragCancelled: ((original: SwipeDescription) -> Unit)? = null
    var onSwipeDragStateChanged: ((dragging: Boolean) -> Unit)? = null
    var onSwipeMultiTouch: (() -> Unit)? = null
    var onGestureRecorded: ((gesture: RecordedGesture?, isFinished: Boolean) -> Unit)? = null

    private var currentDescription by mutableStateOf<ItemBriefDescription?>(null)
    private var isSwipeEditing by mutableStateOf(false)
    private var isRecordingSwipe by mutableStateOf(false)
    private var isDraggingSwipe by mutableStateOf(false)
    private var instructionText by mutableIntStateOf(R.string.toast_configure_single_click)
    private var isInstructionsVisible by mutableStateOf(true)
    private var instructionsTimerTrigger by mutableIntStateOf(0)

    companion object {
        private const val AUTO_HIDE_DELAY_MS = 3_000L
    }

    init {
        val safeInsetTopDp = (displayConfig.safeInsetTopPx / context.resources.displayMetrics.density).dp

        root.setContent {
            val density = LocalDensity.current
            val handleHitRadiusPx = with(density) { 32.dp.toPx() }
            val coordinateStepPx = with(density) { 24.dp.toPx() }
            LaunchedEffect(instructionsTimerTrigger) {
                if (instructionsTimerTrigger > 0) {
                    isInstructionsVisible = true
                    delay(AUTO_HIDE_DELAY_MS)
                    isInstructionsVisible = false
                }
            }
            MacrionTheme {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isSwipeEditing, isRecordingSwipe, handleHitRadiusPx) {
                            if (isRecordingSwipe) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pointerId = down.id
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                if (!isSwipeEditing) {
                                    onTouchListener?.invoke(down.position.toClampedPoint(width, height))
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (!change.pressed) break
                                        onTouchListener?.invoke(change.position.toClampedPoint(width, height))
                                        change.consume()
                                    }
                                } else {
                                    val swipe = currentDescription as? SwipeDescription ?: return@awaitEachGesture
                                    val from = swipe.from ?: return@awaitEachGesture
                                    val to = swipe.to ?: return@awaitEachGesture
                                    val points = swipe.path?.nodes?.map { it.position }
                                        ?: listOf(SwipePoint(from), SwipePoint(to))
                                    val nodeIndex = findSwipeNodeAt(
                                        points, SwipePoint(down.position.x, down.position.y), handleHitRadiusPx,
                                    )
                                    if (nodeIndex == null) {
                                        var multiTouch = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.count { it.pressed } > 1) {
                                                multiTouch = true
                                                onSwipeMultiTouch?.invoke()
                                                break
                                            }
                                            if (event.changes.none { it.pressed }) break
                                        }
                                        if (!multiTouch) showInstruction(swipe.dragInstruction())
                                        return@awaitEachGesture
                                    }
                                    val original = points[nodeIndex]
                                    var dragging = false
                                    down.consume()
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.any { it.id != pointerId && it.pressed }) {
                                                onSwipeDragCancelled?.invoke(swipe)
                                                break
                                            }
                                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                            if (!change.pressed) break
                                            val delta = change.position - down.position
                                            if (!dragging && delta.getDistance() >= viewConfiguration.touchSlop) {
                                                dragging = true
                                                isDraggingSwipe = true
                                                onSwipeDragStateChanged?.invoke(true)
                                            }
                                            if (dragging) {
                                                onSwipeNodeDragged?.invoke(
                                                    nodeIndex,
                                                    PointF(
                                                        (original.x + delta.x).coerceIn(0f, width),
                                                        (original.y + delta.y).coerceIn(0f, height),
                                                    ),
                                                )
                                            }
                                            change.consume()
                                        }
                                    } finally {
                                        isDraggingSwipe = false
                                        if (dragging) onSwipeDragStateChanged?.invoke(false)
                                    }
                                }
                            }
                        },
                ) {
                    ItemBriefCanvas(
                        description = currentDescription,
                        displayConfig = displayConfig,
                        modifier = Modifier.fillMaxSize(),
                        animate = !isDraggingSwipe && !isRecordingSwipe,
                    )

                    if (isSwipeEditing && !isRecordingSwipe) {
                        (currentDescription as? SwipeDescription)?.let { swipe ->
                            swipe.from?.let { position ->
                                SwipeHandleLabel(
                                    label = stringResource(R.string.swipe_position_start),
                                    position = position,
                                    above = true,
                                    maxWidthPx = constraints.maxWidth,
                                    maxHeightPx = constraints.maxHeight,
                                    safeInsetTopPx = displayConfig.safeInsetTopPx,
                                    coordinateStepPx = coordinateStepPx,
                                    onMove = { point -> onSwipeNodeDragged?.invoke(0, point) },
                                )
                            }
                            swipe.path?.nodes?.drop(1)?.dropLast(1)?.forEachIndexed { index, node ->
                                SwipeNodeAccessibilityTarget(
                                    label = stringResource(R.string.swipe_position_node, index + 2),
                                    position = PointF(node.position.x, node.position.y),
                                    maxWidthPx = constraints.maxWidth,
                                    maxHeightPx = constraints.maxHeight,
                                    coordinateStepPx = coordinateStepPx,
                                    onMove = { point -> onSwipeNodeDragged?.invoke(index + 1, point) },
                                )
                            }
                            swipe.to?.let { position ->
                                SwipeHandleLabel(
                                    label = stringResource(R.string.swipe_position_end),
                                    position = position,
                                    above = false,
                                    maxWidthPx = constraints.maxWidth,
                                    maxHeightPx = constraints.maxHeight,
                                    safeInsetTopPx = displayConfig.safeInsetTopPx,
                                    coordinateStepPx = coordinateStepPx,
                                    onMove = { point -> onSwipeNodeDragged?.invoke(swipe.path?.nodes?.lastIndex ?: 1, point) },
                                )
                            }
                        }
                    }

                    if (isRecordingSwipe) {
                        GestureRecordOverlay(
                            displayConfig = displayConfig,
                            isRecording = true,
                            swipeMinDistancePx = with(density) { 8.dp.toPx() },
                            onGestureCaptured = { gesture, finished -> onGestureRecorded?.invoke(gesture, finished) },
                        )
                    }

                    AnimatedVisibility(
                        visible = isInstructionsVisible,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        InstructionsBanner(
                            safeInsetTopDp = safeInsetTopDp,
                            instructionText = instructionText,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SwipeNodeAccessibilityTarget(
        label: String,
        position: PointF,
        maxWidthPx: Int,
        maxHeightPx: Int,
        coordinateStepPx: Float,
        onMove: (PointF) -> Unit,
    ) {
        val targetSizePx = with(LocalDensity.current) { 48.dp.toPx() }
        val x = (position.x - targetSizePx / 2f).coerceIn(0f, (maxWidthPx - targetSizePx).coerceAtLeast(0f))
        val y = (position.y - targetSizePx / 2f).coerceIn(0f, (maxHeightPx - targetSizePx).coerceAtLeast(0f))
        val moveLeftLabel = stringResource(R.string.swipe_position_move_left, label)
        val moveRightLabel = stringResource(R.string.swipe_position_move_right, label)
        val moveUpLabel = stringResource(R.string.swipe_position_move_up, label)
        val moveDownLabel = stringResource(R.string.swipe_position_move_down, label)
        fun move(dx: Float, dy: Float): Boolean {
            onMove(PointF(
                (position.x + dx).coerceIn(0f, maxWidthPx.toFloat()),
                (position.y + dy).coerceIn(0f, maxHeightPx.toFloat()),
            ))
            return true
        }
        Box(Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(48.dp)
            .semantics {
                contentDescription = "$label, ${position.x.roundToInt()}, ${position.y.roundToInt()}"
                customActions = listOf(
                    CustomAccessibilityAction(moveLeftLabel) { move(-coordinateStepPx, 0f) },
                    CustomAccessibilityAction(moveRightLabel) { move(coordinateStepPx, 0f) },
                    CustomAccessibilityAction(moveUpLabel) { move(0f, -coordinateStepPx) },
                    CustomAccessibilityAction(moveDownLabel) { move(0f, coordinateStepPx) },
                )
            },
        )
    }

    @Composable
    private fun SwipeHandleLabel(
        label: String,
        position: PointF,
        above: Boolean,
        maxWidthPx: Int,
        maxHeightPx: Int,
        safeInsetTopPx: Int,
        coordinateStepPx: Float,
        onMove: (PointF) -> Unit,
    ) {
        val density = LocalDensity.current
        val moveLeftLabel = stringResource(R.string.swipe_position_move_left, label)
        val moveRightLabel = stringResource(R.string.swipe_position_move_right, label)
        val moveUpLabel = stringResource(R.string.swipe_position_move_up, label)
        val moveDownLabel = stringResource(R.string.swipe_position_move_down, label)
        val x = (position.x + with(density) { 32.dp.toPx() })
            .coerceIn(0f, (maxWidthPx - with(density) { 72.dp.toPx() }).coerceAtLeast(0f))
        val yOffset = with(density) { (if (above) -56 else 32).dp.toPx() }
        val minY = safeInsetTopPx.toFloat() + with(density) { 8.dp.toPx() }
        val maxY = (maxHeightPx - with(density) { 72.dp.toPx() }).coerceAtLeast(minY)
        val y = (position.y + yOffset).coerceIn(minY, maxY)
        fun move(dx: Float, dy: Float): Boolean {
            onMove(PointF(
                (position.x + dx).coerceIn(0f, maxWidthPx.toFloat()),
                (position.y + dy).coerceIn(0f, maxHeightPx.toFloat()),
            ))
            return true
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics {
                    contentDescription = "$label, ${position.x.roundToInt()}, ${position.y.roundToInt()}"
                    customActions = listOf(
                        CustomAccessibilityAction(moveLeftLabel) { move(-coordinateStepPx, 0f) },
                        CustomAccessibilityAction(moveRightLabel) { move(coordinateStepPx, 0f) },
                        CustomAccessibilityAction(moveUpLabel) { move(0f, -coordinateStepPx) },
                        CustomAccessibilityAction(moveDownLabel) { move(0f, coordinateStepPx) },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }

    @Composable
    private fun InstructionsBanner(safeInsetTopDp: androidx.compose.ui.unit.Dp, instructionText: Int) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black,
                        0.7f to Color.Black.copy(alpha = 0.53f),
                        1f to Color.Transparent,
                    ),
                )
                .padding(
                    start = 32.dp,
                    top = safeInsetTopDp + 4.dp,
                    end = 32.dp,
                    bottom = 32.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(instructionText),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }

    fun setDescription(description: ItemBriefDescription?) {
        currentDescription = description
    }

    fun showSwipeEditor(enabled: Boolean) { isSwipeEditing = enabled }

    fun showSwipeRecording(enabled: Boolean) { isRecordingSwipe = enabled }

    fun setInstruction(@StringRes text: Int) {
        instructionText = text
    }

    fun showOrResetInstructionsTimer() {
        isInstructionsVisible = true
        instructionsTimerTrigger++
    }

    fun showInstruction(@StringRes text: Int) {
        setInstruction(text)
        showOrResetInstructionsTimer()
    }

    fun hideInstructions() {
        instructionsTimerTrigger = 0
        isInstructionsVisible = false
    }

    fun dispose() = Unit
}
