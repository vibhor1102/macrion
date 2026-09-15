/* Copyright (C) 2024 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.pause

import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.app.Dialog
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.TimeUnitDropDownItem
import io.github.vibhor1102.macrion.core.ui.compose.MacrionPauseEditor
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.OnActionConfigCompleteListener
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import kotlinx.coroutines.launch

class PauseDialog(private val listener: OnActionConfigCompleteListener) : OverlayDialog(R.style.ScenarioConfigTheme) {
    override fun tutorialMonitoringTag(): String = MonitoredOverlayType.PAUSE.name
    private val viewModel: PauseViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { pauseViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@PauseDialog.Content() } }
    }

    override fun onDialogCreated(dialog: Dialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.isEditingAction.collect {
                    if (!it) { Log.e(TAG, "Closing PauseDialog because there is no action edited"); finish() }
                }
            }
        }
    }

    @Composable private fun Content() {
        val initialName by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val displayedDuration by viewModel.pauseDuration.collectAsStateWithLifecycle(initialValue = null)
        val unit by viewModel.selectedUnitItem.collectAsStateWithLifecycle(TimeUnitDropDownItem.Milliseconds)
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
        val durationError by viewModel.pauseDurationError.collectAsStateWithLifecycle(false)
        val saveEnabled by viewModel.isValidAction.collectAsStateWithLifecycle(false)
        var name by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("") }
        LaunchedEffect(initialName) { initialName?.let { name = it } }
        LaunchedEffect(displayedDuration) { displayedDuration?.let { duration = it } }
        MacrionPauseEditor(
            title = context.getString(R.string.dialog_title_pause), name = name, duration = duration,
            selectedUnit = unit, nameLabel = context.getString(R.string.generic_name),
            durationLabel = context.getString(R.string.input_field_label_pause_duration),
            unitLabel = context.getString(R.string.dropdown_label_time_unit), nameError = nameError,
            durationError = durationError, saveEnabled = saveEnabled,
            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
            onNameChanged = { name = it; viewModel.setName(it) },
            onDurationChanged = { duration = it; viewModel.setPauseDuration(it.toLongOrNull()) },
            onUnitChanged = viewModel::setTimeUnit, onDismiss = ::back,
            onDelete = { listener.onDeleteClicked(); super.back() },
            onSave = { viewModel.saveLastConfig(); listener.onConfirmClicked(); super.back() },
        )
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog { listener.onDismissClicked(); super.back() }
            return
        }
        listener.onDismissClicked(); super.back()
    }
}
private const val TAG = "PauseDialog"
