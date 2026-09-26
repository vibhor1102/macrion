/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.offset

import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardActions
import io.github.vibhor1102.macrion.core.ui.compose.macrionDoneKeyboardOptions
import io.github.vibhor1102.macrion.core.ui.R as CoreUiR
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class ClickOffsetDialog(
    private val workspaceClick: Click? = null,
    private val onWorkspaceOffsetSelected: ((Point) -> Unit)? = null,
) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.CLICK_OFFSET.name
    private val viewModel: ClickOffsetViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { clickOffsetViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        workspaceClick?.let(viewModel::setWorkspaceClick)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ClickOffsetDialog.Content() } }
    }
@Composable private fun Content() {
        val offsetState by viewModel.clickOffset.collectAsStateWithLifecycle(null)
        val previewState by viewModel.conditionPreviews.collectAsStateWithLifecycle(ClickOffsetPreviewState())
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        var xText by rememberSaveable { mutableStateOf("") }
        var yText by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(offsetState) {
            offsetState?.takeIf { it.updateFrom != ClickOffsetUpdateType.TEXT_INPUT }?.let {
                xText = it.offset.x.toString(); yText = it.offset.y.toString()
            }
        }
        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar()
                if (landscape) Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    OffsetFields(xText, yText, { xText = it }, { yText = it }, Modifier.width(166.dp).padding(8.dp))
                    VerticalDivider()
                    OffsetCanvas(offsetState, previewState, Modifier.weight(1f).fillMaxHeight())
                } else Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    OffsetCanvas(offsetState, previewState, Modifier.fillMaxWidth().height(360.dp))
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
            FilledIconButton(onClick = {
                if (onWorkspaceOffsetSelected != null) {
                    viewModel.selectedOffset()?.let(onWorkspaceOffsetSelected)
                } else viewModel.saveChanges()
                back()
            }) { Icon(painterResource(R.drawable.ic_save_filled), null) }
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

    @Composable private fun OffsetCanvas(offsetState: ClickOffsetState?, previewState: ClickOffsetPreviewState, modifier: Modifier) {
        val markerRadius = dimensionResource(CoreUiR.dimen.overlay_click_selector_radius)
        val innerMarkerRadius = dimensionResource(CoreUiR.dimen.overlay_click_selector_inner_radius)
        val markerThickness = dimensionResource(CoreUiR.dimen.overlay_click_selector_thickness)
        val markerColor = MaterialTheme.colorScheme.primary
        val markerBackground = colorResource(CoreUiR.color.overlayActionsBriefBackground)
        val previewIds = previewState.previews.map { it.id }
        var previewIndex by remember(previewIds) { mutableIntStateOf(0) }
        var isAdjusting by remember { mutableStateOf(false) }
        val accessibilityManager = LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val canAutoAdvance = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()) &&
            accessibilityManager?.isTouchExplorationEnabled != true
        LaunchedEffect(previewIds, previewIndex, isAdjusting, canAutoAdvance) {
            if (previewState.isOrMode && previewState.previews.size > 1 && !isAdjusting && canAutoAdvance) {
                delay(3_000)
                previewIndex = (previewIndex + 1) % previewState.previews.size
            }
        }

        Column(modifier) {
            if (previewState.isOrMode && previewState.previews.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (previewState.previews.size > 1) {
                        IconButton(onClick = {
                            previewIndex = (previewIndex + previewState.previews.size - 1) % previewState.previews.size
                        }) {
                            Icon(painterResource(CoreUiR.drawable.ic_chevron_left),
                                stringResource(R.string.click_offset_previous_condition))
                        }
                    }
                    Text(
                        stringResource(R.string.click_offset_preview_label,
                            previewIndex + 1, previewState.previews.size,
                            previewState.previews[previewIndex].name),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (previewState.previews.size > 1) {
                        IconButton(onClick = { previewIndex = (previewIndex + 1) % previewState.previews.size }) {
                            Icon(painterResource(CoreUiR.drawable.ic_chevron_right),
                                stringResource(R.string.click_offset_next_condition))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isAdjusting = true
                    fun updateOffset(position: Offset) {
                        viewModel.setClickOffset(
                            Point(
                                (position.x - size.width / 2f).toInt(),
                                (position.y - size.height / 2f).toInt(),
                            ),
                            ClickOffsetUpdateType.VIEW,
                        )
                    }
                    try {
                        updateOffset(down.position)
                        drag(down.id) { change ->
                            updateOffset(change.position)
                            change.consume()
                        }
                    } finally {
                        isAdjusting = false
                    }
                }
            }) {
                Crossfade(targetState = previewIndex, animationSpec = tween(220), label = "Click offset condition") { index ->
                    val preview = previewState.previews.getOrNull(index)
                    val loadedBitmap by produceState<Bitmap?>(null, preview?.condition) {
                        value = preview?.condition?.let { viewModel.loadPreviewBitmap(it) }
                    }
                    val bitmap = remember(loadedBitmap) { loadedBitmap?.asImageBitmap() }
                    val imageCondition = preview?.condition as? ScreenCondition.Image
                    // Bitmap decoding may subsample a captured image; its recorded area is the
                    // actual number of screen pixels it covered when the condition was created.
                    val widthPx = imageCondition?.area?.width()?.takeIf { it > 0 } ?: bitmap?.width ?: 0
                    val heightPx = imageCondition?.area?.height()?.takeIf { it > 0 } ?: bitmap?.height ?: 0
                    if (bitmap != null && widthPx > 0 && heightPx > 0) {
                        Canvas(Modifier.fillMaxSize()) {
                            // One canvas pixel represents one device-screen pixel. The saved image
                            // area determines its actual footprint even if decoding sampled it.
                            clipRect {
                                drawImage(
                                    image = bitmap,
                                    dstOffset = IntOffset(
                                        ((size.width - widthPx) / 2f).roundToInt(),
                                        ((size.height - heightPx) / 2f).roundToInt(),
                                    ),
                                    dstSize = IntSize(widthPx, heightPx),
                                )
                            }
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_image_condition_big),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
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
}
