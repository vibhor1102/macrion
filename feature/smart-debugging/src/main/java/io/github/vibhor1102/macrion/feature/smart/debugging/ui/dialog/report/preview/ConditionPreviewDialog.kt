/*
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.domain.model.condition.Condition
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.core.domain.model.counter.ComparisonOperation
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import kotlinx.coroutines.Job

internal data class ConditionPreviewData(
    val condition: Condition,
    val bitmap: Bitmap? = null,
    val bitmapFailed: Boolean = false,
)

@Composable
internal fun ConditionPreviewDialog(
    data: ConditionPreviewData,
    onDismiss: () -> Unit,
    bitmapProvider: ((ScreenCondition.Image, (Bitmap?) -> Unit) -> Job?)? = null,
) {
    var currentBitmap by remember(data.condition.id) { mutableStateOf(data.bitmap) }
    var currentBitmapFailed by remember(data.condition.id) { mutableStateOf(data.bitmapFailed) }

    DisposableEffect(data.condition.id) {
        if (currentBitmap == null && !currentBitmapFailed && data.condition is ScreenCondition.Image && bitmapProvider != null) {
            val job = bitmapProvider(data.condition) { loaded ->
                currentBitmap = loaded
                currentBitmapFailed = loaded == null
            }
            onDispose { job?.cancel() }
        } else {
            onDispose { }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val squareSize = minOf(maxWidth * 0.8f, maxHeight * 0.8f)

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(squareSize)
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ConditionPreviewBody(
                        condition = data.condition,
                        bitmap = currentBitmap,
                        bitmapFailed = currentBitmapFailed,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp),
                    onClick = onDismiss,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = stringResource(R.string.content_desc_close_condition_preview),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionPreviewBody(
    condition: Condition,
    bitmap: Bitmap?,
    bitmapFailed: Boolean,
) {
    when (condition) {
        is ScreenCondition.Image -> ImageConditionPreview(bitmap, bitmapFailed)
        is ScreenCondition.Color -> ColorConditionPreview(condition.color)
        is ScreenCondition.Text -> TextConditionPreview(condition)
        is ScreenCondition.Number -> NumberConditionPreview(condition)
        is TriggerCondition -> TriggerConditionPreview(condition)
    }
}

@Composable
private fun ImageConditionPreview(
    bitmap: Bitmap?,
    bitmapFailed: Boolean,
) {
    when {
        bitmap != null -> {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        bitmapFailed -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.title_condition_preview_image_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        else -> {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ColorConditionPreview(colorInt: Int) {
    val hexColor = remember(colorInt) {
        String.format("#%06X", 0xFFFFFF and colorInt)
    }
    val outlineColor = MaterialTheme.colorScheme.outline
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val radius = size.minDimension * 0.42f
            drawCircle(Color(colorInt), radius = radius)
            drawCircle(outlineColor, radius = radius * 1.05f, style = Stroke(size.minDimension * 0.05f))
        }
        Text(
            text = hexColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun TextConditionPreview(condition: ScreenCondition.Text) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_text_condition),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "“${condition.text}”",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp),
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                text = condition.alphabet.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun NumberConditionPreview(condition: ScreenCondition.Number) {
    val symbol = when (condition.comparisonOperation) {
        ComparisonOperation.EQUALS -> "=="
        ComparisonOperation.GREATER -> ">"
        ComparisonOperation.GREATER_OR_EQUALS -> ">="
        ComparisonOperation.LOWER -> "<"
        ComparisonOperation.LOWER_OR_EQUALS -> "<="
    }
    val operand = when (val value = condition.counterValue) {
        is CounterOperationValue.Counter -> value.value
        is CounterOperationValue.Number -> {
            if (value.value % 1.0 == 0.0) value.value.toLong().toString()
            else value.value.toString()
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_number_condition),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "$symbol $operand",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = condition.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun TriggerConditionPreview(condition: TriggerCondition) {
    val iconRes = when (condition) {
        is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
        is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
        is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = condition.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
