/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.adapter

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.condition.TriggerCondition
import io.github.vibhor1102.macrion.feature.smart.debugging.R
import io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.conditions.ConditionPerformanceEntry

internal data class ConditionPerformanceRowState(
    val entry: ConditionPerformanceEntry,
    val totalTime: String,
    val fulfilled: String,
    val average: String,
    val percentage: String,
    val bitmap: Bitmap? = null,
    val bitmapFailed: Boolean = false,
)

@Composable
internal fun ConditionPerformanceRow(state: ConditionPerformanceRowState) {
    val primary = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(72.dp).clipToBounds().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                ConditionPreview(state)
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    state.entry.condition.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                )
                Text(state.entry.eventName, style = MaterialTheme.typography.bodySmall, color = secondary)
                Text(state.totalTime, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = secondary)
                Text(state.fulfilled, style = MaterialTheme.typography.bodySmall, color = secondary)
                Text(state.average, style = MaterialTheme.typography.bodySmall, color = secondary)
            }
            Box(
                modifier = Modifier.padding(start = 8.dp).size(width = 76.dp, height = 72.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    state.percentage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ConditionPreview(state: ConditionPerformanceRowState) {
    when (val condition = state.entry.condition) {
        is ScreenCondition.Color -> ColorPreview(condition.color)
        is ScreenCondition.Image -> when {
            state.bitmap != null -> Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            state.bitmapFailed -> Icon(
                painter = painterResource(R.drawable.ic_cancel),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
        is ScreenCondition.Number -> ConditionIcon(R.drawable.ic_number_condition)
        is ScreenCondition.Text -> ConditionIcon(R.drawable.ic_text_condition)
        is TriggerCondition.OnBroadcastReceived -> ConditionIcon(R.drawable.ic_broadcast_received)
        is TriggerCondition.OnCounterCountReached -> ConditionIcon(R.drawable.ic_counter_reached)
        is TriggerCondition.OnTimerReached -> ConditionIcon(R.drawable.ic_timer_reached)
    }
}

@Composable
private fun ConditionIcon(icon: Int) = Icon(
    painter = painterResource(icon),
    contentDescription = null,
    modifier = Modifier.fillMaxSize().padding(16.dp),
    tint = Color.Unspecified,
)

@Composable
private fun ColorPreview(color: Int) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.3f
        drawCircle(Color(color), radius = radius)
        drawCircle(outline, radius = radius * 1.15f, style = Stroke(size.minDimension * 0.06f))
    }
}

@Composable
internal fun ConditionPerformanceFooter() {
    Text(
        text = stringResource(R.string.desc_condition_performance_footer),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
