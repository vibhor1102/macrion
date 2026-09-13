/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.tutorial.ui.overlay

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.other.FullscreenOverlay
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.tutorial.R
import io.github.vibhor1102.macrion.feature.tutorial.di.TutorialViewModelsEntryPoint

class TutorialFullscreenOverlay : FullscreenOverlay(theme = R.style.AppTheme) {

    private val viewModel: TutorialOverlayViewModel by viewModels(
        entryPoint = TutorialViewModelsEntryPoint::class.java,
        creator = { tutorialOverlayViewModel() },
    )

    override fun onCreateView(layoutInflater: LayoutInflater): View = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@TutorialFullscreenOverlay.Content() } }
    }

    override fun onViewCreated() = Unit

    private fun onSkipAllClicked() {
        overlayManager.restoreVisibility()
        viewModel.toLastTutorialStep()
        finish()
    }

    @Composable
    private fun Content() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Box(Modifier.fillMaxSize()) {
            TutorialSpotlight(
                monitoredPosition =
                    (uiState?.exitButton as? TutorialExitButtonUiState.MonitoredView)?.position,
                onMonitoredViewClicked = viewModel::performClickOnMonitoredView,
            )

            IconButton(
                onClick = ::onSkipAllClicked,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp).size(48.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_cancel),
                    contentDescription = null,
                    tint = Color.White,
                )
            }

            uiState?.let { state ->
                InstructionPosition(state)
                if (state.exitButton == TutorialExitButtonUiState.Next) {
                    Button(
                        onClick = viewModel::toNextTutorialStep,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Text(stringResource(R.string.button_text_tutorial_next))
                    }
                }
            }
        }
    }

    @Composable
    private fun TutorialSpotlight(
        monitoredPosition: android.graphics.Rect?,
        onMonitoredViewClicked: () -> Unit,
    ) {
        val background = colorResource(R.color.tutorial_overlay_background)
        val borderWidth = dimensionResource(R.dimen.tutorial_hole_border_width)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .pointerInput(monitoredPosition) {
                    detectTapGestures { position ->
                        monitoredPosition?.takeIf { it.contains(position.x.toInt(), position.y.toInt()) }
                            ?.let { onMonitoredViewClicked() }
                    }
                },
        ) {
            drawRect(background)
            monitoredPosition ?: return@Canvas
            val isCircular = monitoredPosition.width().toDouble() in
                (0.5 * monitoredPosition.height())..(1.5 * monitoredPosition.height())
            if (isCircular) {
                val center = Offset(monitoredPosition.exactCenterX(), monitoredPosition.exactCenterY())
                val radius = monitoredPosition.height() * 0.8f
                drawCircle(Color.Transparent, radius, center, blendMode = BlendMode.Clear)
                drawCircle(Color.White, radius, center, style = Stroke(borderWidth.toPx()))
            } else {
                val source = Rect(
                    monitoredPosition.left.toFloat(),
                    monitoredPosition.top.toFloat(),
                    monitoredPosition.right.toFloat(),
                    monitoredPosition.bottom.toFloat(),
                )
                val center = source.center
                val expanded = Rect(
                    center.x - source.width * 0.525f,
                    center.y - source.height * 0.525f,
                    center.x + source.width * 0.525f,
                    center.y + source.height * 0.525f,
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = expanded.topLeft,
                    size = expanded.size,
                    cornerRadius = CornerRadius(25f, 25f),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = expanded.topLeft,
                    size = expanded.size,
                    cornerRadius = CornerRadius(25f, 25f),
                    style = Stroke(borderWidth.toPx()),
                )
            }
        }
    }

    @Composable
    private fun InstructionPosition(state: TutorialFullscreenUiState) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isDisplayedInTopHalf) InstructionCard(state)
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (!state.isDisplayedInTopHalf) InstructionCard(state)
            }
        }
    }

    @Composable
    private fun InstructionCard(state: TutorialFullscreenUiState) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            elevation = CardDefaults.elevatedCardElevation(),
            border = BorderStroke(2.dp, colorResource(R.color.tutorial_header_card_border)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(state.instructionsResId),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                state.image?.let { image ->
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp))
                    Image(
                        painter = painterResource(image.imageResId),
                        contentDescription = stringResource(image.imageDescResId),
                        modifier = Modifier.padding(vertical = 8.dp)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 144.dp, maxHeight = 82.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        stringResource(image.imageDescResId),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
