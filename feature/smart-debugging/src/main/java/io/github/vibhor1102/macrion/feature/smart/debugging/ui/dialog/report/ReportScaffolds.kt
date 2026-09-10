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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

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
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val layoutInfo = state.layoutInfo
    val itemCount = layoutInfo.totalItemsCount
    val visibleCount = layoutInfo.visibleItemsInfo.size
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val thumbColor = MaterialTheme.colorScheme.primary
    if (itemCount == 0 || visibleCount >= itemCount) return

    fun scrollTo(positionY: Float) {
        if (heightPx == 0) return
        val fraction = (positionY / heightPx).coerceIn(0f, 1f)
        val target = (fraction * (itemCount - 1)).roundToInt()
        scope.launch { state.scrollToItem(target) }
    }

    Canvas(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .onSizeChanged { heightPx = it.height }
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(itemCount) {
                detectVerticalDragGestures(
                    onDragStart = { position ->
                        dragging = true
                        scrollTo(position.y)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        scrollTo(change.position.y)
                    },
                )
            },
    ) {
        val trackWidth = if (dragging) 8.dp.toPx() else 4.dp.toPx()
        val margin = 8.dp.toPx()
        val availableHeight = size.height - margin * 2
        val thumbHeight = max(48.dp.toPx(), availableHeight * visibleCount / itemCount)
            .coerceAtMost(availableHeight)
        val scrollableItems = max(itemCount - visibleCount, 1)
        val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()
        val itemProgress = if (firstItem == null || firstItem.size == 0) 0f else {
            state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset.toFloat() / firstItem.size
        }
        val fraction = (itemProgress / scrollableItems).coerceIn(0f, 1f)
        val thumbTop = margin + (availableHeight - thumbHeight) * fraction
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(size.width - trackWidth, margin),
            size = Size(trackWidth, availableHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2),
        )
        drawRoundRect(
            color = thumbColor.copy(alpha = if (dragging) 1f else 0.72f),
            topLeft = Offset(size.width - trackWidth, thumbTop),
            size = Size(trackWidth, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2),
        )
    }
}
