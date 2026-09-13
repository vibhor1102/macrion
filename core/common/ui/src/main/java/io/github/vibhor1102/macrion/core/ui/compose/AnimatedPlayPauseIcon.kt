/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.ui.compose

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import io.github.vibhor1102.macrion.core.ui.R

import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedPlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier.size(40.dp)) {
    val vector = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_pause)
    Icon(
        painter = rememberAnimatedVectorPainter(vector, atEnd = isPlaying),
        contentDescription = null,
        modifier = modifier,
        tint = colorResource(R.color.overlayMenuButtons),
    )
}
