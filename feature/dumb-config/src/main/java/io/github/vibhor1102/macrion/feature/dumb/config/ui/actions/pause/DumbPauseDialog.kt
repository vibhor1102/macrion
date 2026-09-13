/* Copyright (C) 2023 Kevin Buzeau; Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.pause

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.bindings.dropdown.TimeUnitDropDownItem
import io.github.vibhor1102.macrion.core.ui.compose.MacrionPauseEditor
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

class DumbPauseDialog(
    private val dumbPause: DumbAction.DumbPause,
    private val onConfirmClicked: (DumbAction.DumbPause) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbPause) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    private val viewModel: DumbPauseViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbPauseViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbPause(dumbPause)
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MacrionTheme { this@DumbPauseDialog.Content() } }
        }
    }

@Composable private fun Content() {
        val initialName by viewModel.name.collectAsStateWithLifecycle(initialValue = null)
        val displayedDuration by viewModel.pauseDuration.collectAsStateWithLifecycle(initialValue = null)
        val unit by viewModel.selectedUnitItem.collectAsStateWithLifecycle(TimeUnitDropDownItem.Milliseconds)
        val nameError by viewModel.nameError.collectAsStateWithLifecycle(false)
        val durationError by viewModel.pauseDurationError.collectAsStateWithLifecycle(false)
        val saveEnabled by viewModel.isValidDumbPause.collectAsStateWithLifecycle(false)
        var name by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("") }
        LaunchedEffect(initialName) { initialName?.let { name = it } }
        LaunchedEffect(displayedDuration) { displayedDuration?.let { duration = it } }
        MacrionPauseEditor(
            title = context.getString(R.string.item_title_dumb_pause), name = name, duration = duration,
            selectedUnit = unit, nameLabel = context.getString(R.string.input_field_label_name),
            durationLabel = context.getString(R.string.input_field_label_pause_duration),
            unitLabel = context.getString(R.string.dropdown_label_time_unit), nameError = nameError,
            durationError = durationError, saveEnabled = saveEnabled,
            maxNameLength = context.resources.getInteger(R.integer.name_max_length),
            onNameChanged = { name = it; viewModel.setName(it) },
            onDurationChanged = { duration = it; viewModel.setPauseDurationMs(it.toLongOrNull() ?: 0) },
            onUnitChanged = viewModel::setTimeUnit,
            onDismiss = { onDismissClicked(); back() },
            onDelete = { viewModel.getEditedDumbPause()?.let(onDeleteClicked); back() },
            onSave = {
                viewModel.getEditedDumbPause()?.let {
                    viewModel.saveLastConfig(context); onConfirmClicked(it); back()
                }
            },
        )
    }
}
