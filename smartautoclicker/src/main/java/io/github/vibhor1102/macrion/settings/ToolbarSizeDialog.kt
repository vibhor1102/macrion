/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.R
import kotlin.math.roundToInt

private val SCALE_STOPS = listOf(50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150)

@Composable
internal fun ToolbarSizeDialog(
    currentPercent: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialIndex = remember(currentPercent) {
        val closest = SCALE_STOPS.minByOrNull { kotlin.math.abs(it - currentPercent) } ?: 100
        SCALE_STOPS.indexOf(closest).coerceIn(0, SCALE_STOPS.lastIndex).toFloat()
    }
    var sliderIndex by remember { mutableFloatStateOf(initialIndex) }
    val selectedIndex = sliderIndex.roundToInt().coerceIn(0, SCALE_STOPS.lastIndex)
    val selectedPercent = SCALE_STOPS[selectedIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_toolbar_size_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "$selectedPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_toolbar_size_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )

                // Dedicated vertical preview stage
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(290.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    val previewScale = selectedPercent / 100f
                    val baseDensity = LocalDensity.current
                    val previewDensity = remember(baseDensity, previewScale) {
                        Density(
                            density = baseDensity.density * previewScale,
                            fontScale = baseDensity.fontScale * previewScale,
                        )
                    }

                    CompositionLocalProvider(LocalDensity provides previewDensity) {
                        ToolbarLivePreview()
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Slider with center tick mark
                val tickColor = MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                    ) {
                        // Center is exactly 100% position
                        val centerX = size.width / 2f
                        drawLine(
                            color = tickColor,
                            start = Offset(centerX, 4.dp.toPx()),
                            end = Offset(centerX, size.height - 4.dp.toPx()),
                            strokeWidth = 2.5.dp.toPx(),
                        )
                    }
                    Slider(
                        value = sliderIndex,
                        onValueChange = { sliderIndex = it },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Labels below slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "50%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "100%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "150%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPercent) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ToolbarLivePreview() {
    val bg = colorResource(io.github.vibhor1102.macrion.core.common.overlays.R.color.overlayMenuBackground)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewButton(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_play_arrow)
            PreviewButton(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_stop)
            PreviewButton(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_settings_filled)
            PreviewButton(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_move)
        }
    }
}

@Composable
private fun PreviewButton(iconRes: Int) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = colorResource(io.github.vibhor1102.macrion.core.ui.R.color.overlayMenuButtons),
        )
    }
}
