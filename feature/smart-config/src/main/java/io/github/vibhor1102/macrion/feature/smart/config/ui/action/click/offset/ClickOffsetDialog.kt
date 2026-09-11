/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.offset

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.core.ui.R as CoreUiR
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

class ClickOffsetDialog : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CLICK_OFFSET.name
    private val viewModel: ClickOffsetViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickOffsetViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ClickOffsetDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    @Composable private fun Content() {
        val offsetState by viewModel.clickOffset.collectAsStateWithLifecycle(null)
        val image by viewModel.conditionImage.collectAsStateWithLifecycle(null)
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        var xText by rememberSaveable { mutableStateOf("") }
        var yText by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(offsetState) {
            offsetState?.takeIf { it.updateFrom != ClickOffsetUpdateType.TEXT_INPUT }?.let {
                xText = it.offset.x.toString(); yText = it.offset.y.toString()
            }
        }
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar()
                if (landscape) Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    OffsetFields(xText, yText, { xText = it }, { yText = it }, Modifier.width(166.dp).padding(8.dp))
                    VerticalDivider()
                    OffsetCanvas(offsetState, image, Modifier.weight(1f).fillMaxHeight())
                } else Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    OffsetCanvas(offsetState, image, Modifier.fillMaxWidth().height(360.dp))
                    HorizontalDivider()
                    OffsetFields(xText, yText, { xText = it }, { yText = it }, Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }

    @Composable private fun TopBar() {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.field_click_offset_title), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledIconButton(onClick = { viewModel.saveChanges(); back() }) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @Composable private fun OffsetFields(xText: String, yText: String, onXChanged: (String) -> Unit,
        onYChanged: (String) -> Unit, modifier: Modifier) {
        ElevatedCard(modifier) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OffsetField(xText, context.getString(R.string.field_click_offset_x), viewModel.getOffsetMaxBoundsX(), onXChanged) {
                viewModel.setClickOffsetX(it, ClickOffsetUpdateType.TEXT_INPUT)
            }
            OffsetField(yText, context.getString(R.string.field_click_offset_y), viewModel.getOffsetMaxBoundsY(), onYChanged) {
                viewModel.setClickOffsetY(it, ClickOffsetUpdateType.TEXT_INPUT)
            }
        } }
    }

    @Composable private fun OffsetField(value: String, label: String, bounds: IntRange, onTextChanged: (String) -> Unit,
        onValueChanged: (Int) -> Unit) {
        OutlinedTextField(value, { input ->
            val filtered = input.filterIndexed { index, char -> char.isDigit() || (char == '-' && index == 0) }
            val parsed = filtered.toIntOrNull()
            if (filtered.isEmpty() || filtered == "-" || parsed in bounds) {
                onTextChanged(filtered); parsed?.let(onValueChanged)
            }
        }, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
            keyboardOptions = macrionDoneKeyboardOptions(KeyboardType.Number),
            keyboardActions = macrionDoneKeyboardActions())
    }

    @Composable private fun OffsetCanvas(offsetState: ClickOffsetState?, image: Any?, modifier: Modifier) {
        val markerRadius = dimensionResource(CoreUiR.dimen.overlay_click_selector_radius)
        val innerMarkerRadius = dimensionResource(CoreUiR.dimen.overlay_click_selector_inner_radius)
        val markerThickness = dimensionResource(CoreUiR.dimen.overlay_click_selector_thickness)
        val markerColor = colorResource(CoreUiR.color.overlayViewPrimary)
        val markerBackground = colorResource(CoreUiR.color.overlayActionsBriefBackground)
        val imageBitmap = remember(image) { (image as? Drawable)?.toBitmap()?.asImageBitmap() }
        Box(
            modifier = modifier.pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun updateOffset(position: Offset) {
                        viewModel.setClickOffset(
                            android.graphics.Point(
                                (position.x - size.width / 2f).toInt(),
                                (position.y - size.height / 2f).toInt(),
                            ),
                            ClickOffsetUpdateType.VIEW,
                        )
                    }
                    updateOffset(down.position)
                    drag(down.id) { change ->
                        updateOffset(change.position)
                        change.consume()
                    }
                }
            },
        ) {
            when (image) {
                is Bitmap -> Image(image.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else -> imageBitmap?.let {
                    Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                } ?: Image(
                    painter = painterResource(R.drawable.ic_image_condition_big),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            offsetState?.let { state ->
                Canvas(Modifier.fillMaxSize()) {
                    val position = Offset(
                        size.width / 2f + state.offset.x,
                        size.height / 2f + state.offset.y,
                    )
                    drawCircle(
                        color = markerBackground,
                        radius = markerRadius.toPx() * 2f,
                        center = position,
                        alpha = 0.5f,
                    )
                    drawCircle(
                        color = markerColor,
                        radius = markerRadius.toPx(),
                        center = position,
                        style = Stroke(markerThickness.toPx()),
                    )
                    drawCircle(markerColor, innerMarkerRadius.toPx(), position)
                }
            }
        }
    }
}
