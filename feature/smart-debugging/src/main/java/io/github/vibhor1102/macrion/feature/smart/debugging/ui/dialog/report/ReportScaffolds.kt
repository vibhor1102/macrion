/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import kotlinx.coroutines.delay
import kotlin.math.max

private data class FastScrollerThumbBounds(
    val top: Float,
    val height: Float,
    val minimumTouchHeight: Float,
)

@Composable
internal fun ReportDialogTopBar(
    title: String,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(painterResource(R.drawable.ic_cancel), contentDescription = null)
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        if (onSave != null) {
            FilledIconButton(onClick = onSave) {
                Icon(painterResource(R.drawable.ic_save_filled), contentDescription = null)
            }
        }
    }
}

@Composable
internal fun ReportLoading(color: Color = MaterialTheme.colorScheme.primary) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(48.dp), color = color)
    }
}

@Composable
internal fun ReportEmptyMessage(
    title: String,
    secondary: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        if (secondary != null) {
            HorizontalDivider(Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp))
            Text(
                secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) action()
    }
}

/** Shared fast scroller for report LazyColumns. */
@Composable
internal fun ReportFastScroller(
    state: LazyListState,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var heightPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var thumbVisible by remember { mutableStateOf(true) }
    val layoutInfo = state.layoutInfo
    val itemCount = layoutInfo.totalItemsCount
    val visibleCount = layoutInfo.visibleItemsInfo.size
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val thumbColor = MaterialTheme.colorScheme.primary
    if (itemCount == 0 || visibleCount >= itemCount) return

    LaunchedEffect(state.isScrollInProgress, dragging) {
        if (state.isScrollInProgress || dragging) {
            thumbVisible = true
        } else {
            delay(1_200)
            thumbVisible = false
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val marginPx = with(density) { 4.dp.toPx() }
    val minimumThumbHeightPx = with(density) { 48.dp.toPx() }
    val averageItemHeight = (layoutInfo.visibleItemsInfo
        .map { it.size }
        .average()
        .takeIf { !it.isNaN() } ?: 1.0)
        .toFloat()
        .coerceAtLeast(1f)
    val viewportHeight = heightPx.toFloat().coerceAtLeast(1f)
    val availableHeight = (viewportHeight - marginPx * 2).coerceAtLeast(1f)
    val estimatedRange = (averageItemHeight * itemCount).coerceAtLeast(viewportHeight)
    val scrollableRange = (estimatedRange - viewportHeight).coerceAtLeast(1f)
    val thumbHeight = max(
        minimumThumbHeightPx,
        availableHeight * viewportHeight / estimatedRange,
    ).coerceAtMost(availableHeight)
    val thumbTravel = (availableHeight - thumbHeight).coerceAtLeast(1f)
    val currentOffset = (state.firstVisibleItemIndex * averageItemHeight + state.firstVisibleItemScrollOffset)
        .coerceIn(0f, scrollableRange)
    val thumbTop = marginPx + thumbTravel * currentOffset / scrollableRange

    val latestScrollMultiplier = rememberUpdatedState(scrollableRange / thumbTravel)
    val latestThumbBounds = rememberUpdatedState(
        FastScrollerThumbBounds(
            top = thumbTop,
            height = thumbHeight,
            minimumTouchHeight = minimumThumbHeightPx,
        ),
    )
    val dragVisualProgress by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "report-fast-scroller-drag",
    )
    val visibilityProgress by animateFloatAsState(
        targetValue = if (thumbVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "report-fast-scroller-visibility",
    )

    Canvas(
        modifier = modifier
            .width(32.dp)
            .fillMaxHeight()
            .alpha(visibilityProgress)
            .onSizeChanged { heightPx = it.height }
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val bounds = latestThumbBounds.value
                    val touchPadding = (max(bounds.height, bounds.minimumTouchHeight) - bounds.height) / 2
                    val touchTop = bounds.top - touchPadding
                    val touchBottom = bounds.top + bounds.height + touchPadding
                    if (down.position.y !in touchTop..touchBottom) return@awaitEachGesture

                    try {
                        thumbVisible = true
                        dragging = true
                        drag(down.id) { change ->
                            val dragAmount = change.positionChange().y
                            if (dragAmount != 0f) {
                                change.consume()
                                state.dispatchRawDelta(dragAmount * latestScrollMultiplier.value)
                            }
                        }
                    } finally {
                        dragging = false
                    }
                }
            },
    ) {
        val trackWidth = 3.dp.toPx() + 5.dp.toPx() * dragVisualProgress
        val visualThumbWidth = 8.dp.toPx() + 6.dp.toPx() * dragVisualProgress
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(size.width - trackWidth, marginPx),
            size = Size(trackWidth, availableHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2),
        )
        drawRoundRect(
            color = thumbColor.copy(alpha = 0.8f + 0.2f * dragVisualProgress),
            topLeft = Offset(size.width - visualThumbWidth, thumbTop),
            size = Size(visualThumbWidth, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(visualThumbWidth / 2),
        )
    }
}
