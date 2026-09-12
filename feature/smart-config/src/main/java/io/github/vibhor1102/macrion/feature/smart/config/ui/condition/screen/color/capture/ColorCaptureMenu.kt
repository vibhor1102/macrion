/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.capture
import androidx.compose.runtime.setValue

import io.github.vibhor1102.macrion.core.common.overlays.menu.findOverlayView

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenuButtonView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.menu.OverlayMenu
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.createColorCaptureOverlayToolbar
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import kotlinx.coroutines.launch
import kotlin.getValue
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType


class ColorCaptureMenu (
    private val initialPosition: PointF? = null,
    private val onColorSelected: (position: PointF, colorInt: Int) -> Unit,
) : OverlayMenu(theme = R.style.AppTheme, recreateOverlayViewOnRotation = true) {

    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COLOR_CAPTURE_MENU.name

    /** The view model for this menu. */
    private val viewModel: ColorCaptureViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { colorCaptureViewModel() },
    )

    private lateinit var menuView: ViewGroup
    private val confirmButton get() = menuView.findOverlayView<OverlayMenuButtonView>(R.id.btn_confirm)
    private var pixelSelectionState by mutableStateOf<PixelSelectionUiState?>(null)

    /** Orientation of the device. */
    private var orientation: Int = Configuration.ORIENTATION_PORTRAIT

    override fun animateOverlayView(): Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        menuView = createColorCaptureOverlayToolbar(context)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUiState)
            }
        }

        return menuView
    }

    override fun onCreateOverlayView(): View {
        orientation = displayConfigManager.displayConfig.orientation
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@ColorCaptureMenu.ColorCaptureOverlay() } }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        val captureStep = viewModel.uiState.value.captureStep

        when (viewId) {
            R.id.btn_confirm -> when (captureStep) {
                ColorCaptureMenuStep.SCREENSHOT_SELECTION -> viewModel.captureScreen(initialPosition)
                ColorCaptureMenuStep.PIXEL_SELECTION -> {
                    viewModel.getPixelSelection()?.let { (position, color) ->
                        back()
                        onColorSelected(position, color)
                    }
                }
                ColorCaptureMenuStep.CAPTURING -> return
            }

            R.id.btn_cancel -> when (captureStep) {
                ColorCaptureMenuStep.SCREENSHOT_SELECTION -> back()
                ColorCaptureMenuStep.PIXEL_SELECTION -> viewModel.cancelCapture()
                ColorCaptureMenuStep.CAPTURING -> return
            }
        }
    }

    private fun updateUiState(uiState: ColorCaptureUiState) {
       updateMenu(uiState)

        if (uiState.pixelSelectionUiState == null) {
            setOverlayViewVisibility(false)
            return
        }

        setOverlayViewVisibility(true)
        updateOverlay(uiState.pixelSelectionUiState)
    }

    private fun updateMenu(uiState: ColorCaptureUiState) {
        setMenuVisibility(if (uiState.menuVisibility) View.VISIBLE else View.GONE)

        confirmButton.setImageResource(uiState.topButtonIcon)
        setMenuItemViewEnabled(confirmButton, uiState.topButtonEnabled)
        setMenuItemViewEnabled(menuView.findOverlayView(R.id.btn_hide_overlay), uiState.showHideButtonEnabled)
    }

    private fun updateOverlay(uiState: PixelSelectionUiState) {
        pixelSelectionState = uiState
    }

    @Composable
    private fun ColorCaptureOverlay() {
        var overlaySize by androidx.compose.runtime.remember { mutableStateOf(Size.Zero) }
        val uiState = pixelSelectionState
        Box(Modifier.fillMaxSize().onSizeChanged { overlaySize = Size(it.width.toFloat(), it.height.toFloat()) }) {
            PixelSelector(uiState, Modifier.fillMaxSize())

            val position = uiState?.selectedPosition
            if (uiState != null && position != null && overlaySize != Size.Zero) {
                ZoomCard(
                    uiState = uiState,
                    modifier = Modifier.align(zoomCardAlignment(position, overlaySize)).padding(
                        horizontal = 16.dp,
                        vertical = if (orientation == Configuration.ORIENTATION_PORTRAIT) 48.dp else 0.dp,
                    ),
                )
            }
        }
    }

    private fun zoomCardAlignment(position: PointF, overlaySize: Size): Alignment =
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (position.x < overlaySize.width / 2f) Alignment.CenterEnd else Alignment.CenterStart
        } else {
            if (position.y < overlaySize.height / 2f) Alignment.BottomCenter else Alignment.TopCenter
        }

    @Composable
    private fun ZoomCard(uiState: PixelSelectionUiState, modifier: Modifier) {
        ElevatedCard(
            modifier = modifier,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        ) {
            Box(Modifier.width(266.dp).height(266.dp).padding(horizontal = 8.dp)) {
                ZoomedPixelPreview(
                    screenshot = uiState.screenshot,
                    selectedPosition = uiState.selectedPosition,
                    onPixelSelected = viewModel::updateSelectedPosition,
                    modifier = Modifier.align(Alignment.Center).size(250.dp),
                )
                OutlinedCard(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorIndicator(uiState.selectedColor ?: 0)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.selectedColorDisplayText.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    @Composable
    private fun PixelSelector(uiState: PixelSelectionUiState?, modifier: Modifier) {
        val screenshot = uiState?.screenshot
        val selectedPosition = uiState?.selectedPosition
        val selectorColor = colorResource(R.color.overlayViewPrimary)
        val selectorThickness = dimensionResource(R.dimen.overlay_pixel_selector_thickness)
        Canvas(
            modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun select(position: androidx.compose.ui.geometry.Offset) =
                        viewModel.updateSelectedPosition(PointF(position.x.toInt().toFloat(), position.y.toInt().toFloat()))
                    select(down.position)
                    drag(down.id) { change -> select(change.position); change.consume() }
                }
            },
        ) {
            screenshot?.let { bitmap ->
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None,
                )
            }
            selectedPosition?.let { position ->
                drawLine(selectorColor, androidx.compose.ui.geometry.Offset(0f, position.y), androidx.compose.ui.geometry.Offset(size.width, position.y), selectorThickness.toPx())
                drawLine(selectorColor, androidx.compose.ui.geometry.Offset(position.x, 0f), androidx.compose.ui.geometry.Offset(position.x, size.height), selectorThickness.toPx())
            }
        }
    }

    @Composable
    private fun ZoomedPixelPreview(
        screenshot: Bitmap,
        selectedPosition: PointF?,
        onPixelSelected: (PointF) -> Unit,
        modifier: Modifier,
    ) {
        val center = selectedPosition ?: return
        val bitmap = screenshot.asImageBitmap()
        Canvas(
            modifier.pointerInput(screenshot, center.x, center.y) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    drag(down.id) { change -> change.consume() }
                    val scale = size.width * 0.1f
                    onPixelSelected(PointF(
                        ((down.position.x - (size.width - scale) / 2f) / scale + center.x).toInt().coerceIn(0, screenshot.width - 1).toFloat(),
                        ((down.position.y - (size.height - scale) / 2f) / scale + center.y).toInt().coerceIn(0, screenshot.height - 1).toFloat(),
                    ))
                }
            },
        ) {
            val scale = size.width * 0.1f
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - scale) / 2f - (center.x * scale),
                (size.height - scale) / 2f - (center.y * scale),
            )
            drawImage(
                image = bitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize((screenshot.width * scale).toInt(), (screenshot.height * scale).toInt()),
                filterQuality = FilterQuality.None,
            )
            val cellLeft = (size.width - scale) / 2f
            val cellTop = (size.height - scale) / 2f
            clipRect(topLeft.x, topLeft.y, topLeft.x + screenshot.width * scale, topLeft.y + screenshot.height * scale) {
                var x = cellLeft - scale / 2f
                while (x > -scale) { drawLine(Color.White, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 3f); drawLine(Color.Black, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f); x -= scale }
                x = cellLeft + scale / 2f
                while (x < size.width + scale) { drawLine(Color.White, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 3f); drawLine(Color.Black, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f); x += scale }
                var y = cellTop - scale / 2f
                while (y > -scale) { drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 3f); drawLine(Color.Black, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f); y -= scale }
                y = cellTop + scale / 2f
                while (y < size.height + scale) { drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 3f); drawLine(Color.Black, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f); y += scale }
            }
            val rectTopLeft = androidx.compose.ui.geometry.Offset(cellLeft - 3.5f, cellTop - 3.5f)
            val rectSize = Size(scale + 7f, scale + 7f)
            drawRect(Color.Black, rectTopLeft, rectSize, style = Stroke(7f))
            drawRect(Color.White, rectTopLeft, rectSize, style = Stroke(5f))
        }
    }

    @Composable
    private fun ColorIndicator(color: Int) {
        val border = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(24.dp)) {
            drawCircle(Color(color), radius = 10.dp.toPx(), center = center)
            drawCircle(border, radius = 11.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
        }
    }
}
