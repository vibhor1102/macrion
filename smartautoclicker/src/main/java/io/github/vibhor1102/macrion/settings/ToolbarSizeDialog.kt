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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.R
import kotlin.math.roundToInt

private val SCALE_STOPS = listOf(50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200)
private const val DEFAULT_STOP_INDEX = 5 // 100%

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
            Text(
                text = stringResource(R.string.settings_toolbar_size_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_toolbar_size_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // Live Preview Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
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

                Spacer(Modifier.height(16.dp))

                // Current percentage display
                Text(
                    text = "$selectedPercent%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(8.dp))

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
                        text = "100% (${stringResource(R.string.settings_toolbar_size_default)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "200%",
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
            Row {
                TextButton(onClick = { sliderIndex = DEFAULT_STOP_INDEX.toFloat() }) {
                    Text(stringResource(R.string.settings_toolbar_size_reset))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
            SampleLiveDebugPanel()
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

@Composable
private fun SampleLiveDebugPanel() {
    val primary = colorResource(io.github.vibhor1102.macrion.core.ui.R.color.overlayViewPrimary)
    val iconColor = colorResource(io.github.vibhor1102.macrion.core.ui.R.color.overlayMenuButtons)
    val divider = primary.copy(alpha = 19f / 255f)

    Box(
        modifier = Modifier
            .width(170.dp)
            .height(90.dp),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(divider),
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_image_condition),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor,
                )
                Text(
                    text = "Event 1",
                    modifier = Modifier.padding(start = 6.dp),
                    color = primary,
                    maxLines = 1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            HorizontalDivider(thickness = 2.dp, color = divider)
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor,
                )
                Text("1", Modifier.padding(start = 4.dp), color = primary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(io.github.vibhor1102.macrion.core.ui.R.drawable.ic_duration),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor,
                )
                Text("0.5s", Modifier.padding(start = 4.dp), color = primary, fontSize = 12.sp)
            }
        }
    }
}
