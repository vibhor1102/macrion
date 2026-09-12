/* Copyright (C) 2026 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.Tip
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTextField
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialClickAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.compose.TutorialViewAnchor
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showDeleteConditionsWithAssociatedActionsDialog
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.OnConditionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.areaselector.ConditionAreaSelectorMenu
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.image.MAX_THRESHOLD
import io.github.vibhor1102.macrion.feature.smart.config.ui.condition.screen.text.alphabet.AlphabetActivity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class TextConditionDialog(private val listener: OnConditionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.TEXT_CONDITION.name
    private val viewModel: TextConditionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { textConditionViewModel() },
    )
    private var saveAnchor: View? = null
    private var textAnchor: View? = null
    private var areaAnchor: View? = null

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@TextConditionDialog.Content() } }
    }
    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.isEditingCondition.collect { if (!it) { Log.e(TAG, "Closing TextConditionDialog because there is no condition edited"); finish() } }
        } }
    }
    override fun onStart() {
        super.onStart()
        viewModel.monitorSaveButtonView(saveAnchor)
        viewModel.monitorTextToDetectField(textAnchor)
        viewModel.monitorDetectionAreaSelectorView(areaAnchor)
    }
    override fun onStop() { viewModel.detachMonitoredViews(); super.onStop() }

    @Composable private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return
        var name by rememberSaveable { mutableStateOf(ui.name) }
        var textToDetect by rememberSaveable { mutableStateOf(ui.textToSearch) }
        Surface(Modifier.fillMaxWidth().heightIn(max = 600.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column {
                TopBar(ui.canBeSaved)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacrionTextField(name, { name = it; viewModel.setName(it) }, context.getString(R.string.generic_name),
                        isError = ui.nameError, maxLength = context.resources.getInteger(R.integer.name_max_length))
                    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 8.dp)) {
                        TextToDetectField(textToDetect) { textToDetect = it; viewModel.setTextToDetect(it) }
                        SelectorField(context.getString(R.string.field_text_detection_alphabet_title), ui.alphabetDesc, false, ::showAlphabetSelectionDialog)
                        HorizontalDivider()
                        VisibilityField(ui.shouldBeDetectedChecked)
                    } }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Box {
                            SelectorField(context.getString(R.string.generic_detection_area_title), ui.detectionAreaDescription,
                                ui.detectionAreaError, ::showDetectionAreaSelector)
                            TutorialClickAnchor({ areaAnchor = it; viewModel.monitorDetectionAreaSelectorView(it) }, ::showDetectionAreaSelector)
                        }
                    }
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

    @Composable private fun TextToDetectField(value: String, onValueChange: (String) -> Unit) {
        val focusRequester = remember { FocusRequester() }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            TutorialViewAnchor({ textAnchor = it; viewModel.monitorTextToDetectField(it) },
                { focusRequester.requestFocus() }, Modifier.matchParentSize())
            MacrionTextField(value, onValueChange, context.getString(R.string.field_text_to_detect_label),
                Modifier.focusRequester(focusRequester), maxLength = context.resources.getInteger(R.integer.text_condition_max_length),
                trimWhitespace = false)
        }
    }

    @Composable private fun SelectorField(title: String, description: String, error: Boolean, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 62.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null)
        }
    }

    @Composable private fun VisibilityField(checked: Boolean) {
        Row(Modifier.fillMaxWidth().clickable { viewModel.toggleShouldBeDetected() }
            .padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.getString(R.string.field_condition_visibility_title), style = MaterialTheme.typography.titleSmall)
                Text(context.getString(if (checked) R.string.field_condition_visibility_desc_present
                    else R.string.field_condition_visibility_desc_absent), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    private fun showDetectionAreaSelector() = overlayManager.navigateTo(context,
        ConditionAreaSelectorMenu(onHelpClicked = { context.getTutorialNavigator().showTipDialog(context, Tip.TEXT_DETECTION_AREA) },
            onAreaSelected = viewModel::setDetectionArea), true)
    private fun showAlphabetSelectionDialog() = context.startActivity(
        AlphabetActivity.getStartIntent(context, AlphabetActivity.MODE_SELECTION))
}

private const val TAG = "TextConditionDialog"
