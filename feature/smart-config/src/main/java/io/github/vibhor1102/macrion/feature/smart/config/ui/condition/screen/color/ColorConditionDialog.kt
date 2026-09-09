/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialClickAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.capture.ColorCaptureMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.color.extensions.rgbToColorInt
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image.MAX_THRESHOLD
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ColorConditionDialog(private val listener: OnConditionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.COLOR_CONDITION.name
    private val viewModel: ColorConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { colorConditionViewModel() },
    )
    private var saveAnchor: View? = null

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ColorConditionDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect { if (!it) { Log.e(TAG, "Closing ColorConditionDialog because there is no condition edited"); finish() } }
        } }
    }
    override fun onStart() { super.onStart(); viewModel.monitorSaveButtonView(saveAnchor) }
    override fun onStop() { viewModel.detachMonitoredViews(); super.onStop() }

    @Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var name by rememberSaveable { mutableStateOf(ui.conditionName) }
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(ui.canBeSaved)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                        isError = ui.conditionNameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    ColorCard(ui)
                    ThresholdCard(ui.detectionThreshold)
                }
            }
        }
    }

    @Composable private fun TopBar(saveEnabled: Boolean) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back) { Icon(painterResource(R.drawable.ic_cancel), null) }
            Text(context.getString(R.string.dialog_title_condition_config), Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            FilledTonalIconButton(onClick = ::onDeleteClicked) { Icon(painterResource(R.drawable.ic_delete), null) }
            Spacer(Modifier.width(8.dp))
            Box {
                FilledIconButton(onClick = ::save, enabled = saveEnabled) { Icon(painterResource(R.drawable.ic_save_filled), null) }
                TutorialClickAnchor({ saveAnchor = it; viewModel.monitorSaveButtonView(it) }, ::save, saveEnabled)
            }
        }
    }

    @Composable private fun ColorCard(ui: ColorConditionUiState) {
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(72.dp).background(Color(ui.conditionColor), CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable(onClick = ::showPixelColorPickerOverlay))
                    Text(ui.conditionColorText, style = MaterialTheme.typography.titleSmall)
                }
                Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(152.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(Modifier.weight(1f)) {
                    RgbSlider(ui.redValue, Color(0, ui.greenValue, ui.blueValue), Color(255, ui.greenValue, ui.blueValue), Color(ui.redValue, 0, 0)) {
                        viewModel.setColor(rgbToColorInt(it, ui.greenValue, ui.blueValue))
                    }
                    RgbSlider(ui.greenValue, Color(ui.redValue, 0, ui.blueValue), Color(ui.redValue, 255, ui.blueValue), Color(0, ui.greenValue, 0)) {
                        viewModel.setColor(rgbToColorInt(ui.redValue, it, ui.blueValue))
                    }
                    RgbSlider(ui.blueValue, Color(ui.redValue, ui.greenValue, 0), Color(ui.redValue, ui.greenValue, 255), Color(0, 0, ui.blueValue)) {
                        viewModel.setColor(rgbToColorInt(ui.redValue, ui.greenValue, it))
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            VisibilityField(ui.shouldBeDetectedChecked)
        } }
    }

    @Composable private fun RgbSlider(value: Int, start: Color, end: Color, thumb: Color, onValueChanged: (Int) -> Unit) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(Brush.horizontalGradient(listOf(start, end))))
            Slider(value.toFloat(), { onValueChanged(it.roundToInt()) }, valueRange = 0f..255f, steps = 254,
                colors = SliderDefaults.colors(thumbColor = thumb, activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
        }
    }

    @Composable private fun VisibilityField(checked: Boolean) {
        Row(Modifier.fillMaxWidth().clickable { viewModel.toggleShouldBeDetected() }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.getString(R.string.field_condition_visibility_title), style = MaterialTheme.typography.titleSmall)
                Text(context.getString(if (checked) R.string.field_condition_visibility_desc_present else R.string.field_condition_visibility_desc_absent),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange = { viewModel.toggleShouldBeDetected() })
        }
    }

    @Composable private fun ThresholdCard(value: Int) {
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(context.getString(R.string.generic_condition_threshold_title), style = MaterialTheme.typography.titleSmall)
                Text("$value%", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(value.toFloat(), { viewModel.setThreshold(it.roundToInt()) }, valueRange = 0f..MAX_THRESHOLD,
                steps = MAX_THRESHOLD.roundToInt() - 1)
        } }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) { context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }; return }
        listener.onDismissClicked(); super.back()
    }
    private fun save() { listener.onConfirmClicked(); super.back() }
    private fun onDeleteClicked() {
        if (viewModel.isConditionRelatedToClick()) context.showDeleteConditionsWithAssociatedActionsDialog(::onConfirmDelete)
        else onConfirmDelete()
    }
    private fun onConfirmDelete() { listener.onDeleteClicked(); super.back() }
    private fun showPixelColorPickerOverlay() {
        overlayManager.navigateTo(context, ColorCaptureMenu(viewModel.uiState.value?.conditionPosition) { position, color ->
            viewModel.setColor(color); viewModel.setPosition(position)
        }, true)
    }
}

private const val TAG = "ColorConditionDialog"
