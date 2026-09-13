/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import io.github.vibhor1102.macrion.core.domain.model.EXACT
import io.github.vibhor1102.macrion.core.domain.model.IN_AREA
import io.github.vibhor1102.macrion.core.domain.model.WHOLE_SCREEN
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.tutorialAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.areaselector.ConditionAreaSelectorMenu
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ImageConditionDialog(private val listener: OnConditionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.IMAGE_CONDITION.name
    private val viewModel: ImageConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { imageConditionViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@ImageConditionDialog.Content() } }
    }
    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect { if (!it) { Log.e(TAG, "Closing ImageConditionDialog because there is no condition edited"); finish() } }
        } }
    }

    @Composable private fun Content() {
        val initialName by viewModel.name.collectAsStateWithLifecycle(null)
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
        val bitmap by viewModel.conditionBitmap.collectAsStateWithLifecycle(null)
        val visible by viewModel.shouldBeDetected.collectAsStateWithLifecycle(false)
        val detection by viewModel.detectionType.collectAsStateWithLifecycle(null)
        val threshold by viewModel.threshold.collectAsStateWithLifecycle(0)
        val saveEnabled by viewModel.conditionCanBeSaved.collectAsStateWithLifecycle(false)
        var name by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(initialName) { initialName?.let { name = it } }
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(saveEnabled)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                        isError = nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    PreviewCard(bitmap, visible)
                    detection?.let { DetectionCard(it) }
                    ThresholdCard(threshold)
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
            FilledIconButton(
                onClick = ::save,
                enabled = saveEnabled,
                modifier = Modifier.tutorialAnchor(
                    MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    onClick = ::save,
                    enabled = saveEnabled,
                ),
            ) { Icon(painterResource(R.drawable.ic_save_filled), null) }
        }
    }

    @Composable private fun PreviewCard(bitmap: android.graphics.Bitmap?, visible: Boolean) {
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap.asImageBitmap(), context.getString(R.string.content_desc_image_condition),
                    Modifier.widthIn(min = 50.dp, max = 400.dp).heightIn(min = 100.dp, max = 600.dp), contentScale = ContentScale.Fit)
                else Icon(painterResource(R.drawable.ic_cancel), null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider()
            VisibilityField(visible)
        } }
    }

    @Composable private fun VisibilityField(visible: Boolean) {
        Row(
            Modifier
                .fillMaxWidth()
                .tutorialAnchor(
                    MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_VISIBILITY,
                    onClick = viewModel::toggleShouldBeDetected,
                )
                .clickable(onClick = viewModel::toggleShouldBeDetected)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.getString(R.string.field_condition_visibility_title), style = MaterialTheme.typography.titleSmall)
                Text(context.getString(if (visible) R.string.field_condition_visibility_desc_present else R.string.field_condition_visibility_desc_absent),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(visible, { viewModel.toggleShouldBeDetected() })
        }
    }

    @Composable private fun DetectionCard(state: DetectionTypeState) {
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(context.getString(R.string.field_detection_type_title), style = MaterialTheme.typography.titleSmall)
                    Text(context.getString(when (state.type) { EXACT -> R.string.field_detection_type_desc_exact
                        WHOLE_SCREEN -> R.string.field_detection_type_desc_screen else -> R.string.field_select_detection_area_title }),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DetectionTypeButtons(state.type)
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            AreaSelector(state)
        } }
    }

    @Composable private fun DetectionTypeButtons(selected: Int) {
        val shape = RoundedCornerShape(20.dp)
        Row(Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
            listOf(EXACT to R.drawable.ic_detect_exact, WHOLE_SCREEN to R.drawable.ic_detect_whole_screen,
                IN_AREA to R.drawable.ic_detect_in_area).forEachIndexed { index, item ->
                if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline))
                Box(Modifier.width(40.dp).fillMaxHeight().background(if (selected == item.first)
                    MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .then(
                        if (item.first == IN_AREA) {
                            Modifier.tutorialAnchor(
                                MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_TYPE_ITEM_IN_AREA,
                                onClick = { viewModel.setDetectionType(IN_AREA) },
                            )
                        } else Modifier
                    )
                    .clickable { viewModel.setDetectionType(item.first) },
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(item.second), null, Modifier.size(18.dp))
                }
            }
        }
    }

    @Composable private fun AreaSelector(state: DetectionTypeState) {
        val enabled = state.type == IN_AREA
        val color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .then(
                    if (enabled) {
                        Modifier.tutorialAnchor(
                            MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_AREA_SELECTOR,
                            onClick = ::showDetectionAreaSelector,
                        )
                    } else Modifier
                )
                .clickable(enabled, onClick = ::showDetectionAreaSelector)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.getString(R.string.field_select_detection_area_title), style = MaterialTheme.typography.titleSmall, color = color)
                Text(state.areaText, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.75f))
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = color)
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
        if (viewModel.isConditionRelatedToClick()) context.showDeleteConditionsWithAssociatedActionsDialog(::confirmDelete)
        else confirmDelete()
    }
    private fun confirmDelete() { listener.onDeleteClicked(); super.back() }
    private fun showDetectionAreaSelector() = overlayManager.navigateTo(context, ConditionAreaSelectorMenu(
        onHelpClicked = { context.getTutorialNavigator().showTipDialog(context, Tip.IMAGE_DETECTION_AREA) },
        onAreaSelected = viewModel::setDetectionArea), true)
}

private const val TAG = "ImageConditionDialog"
