/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.scenarios.list

import android.content.Intent
import android.util.Log
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.common.navigation.TutorialNavigator
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.createMacrionMessageDialog
import io.github.vibhor1102.macrion.feature.backup.ui.BackupDialogFragment
import io.github.vibhor1102.macrion.feature.backup.ui.BackupDialogFragment.Companion.FRAGMENT_TAG_BACKUP_DIALOG
import io.github.vibhor1102.macrion.scenarios.migration.ConditionsMigrationFragment
import io.github.vibhor1102.macrion.scenarios.creation.ScenarioCreationDialog
import io.github.vibhor1102.macrion.scenarios.list.copy.ScenarioCopyDialog
import io.github.vibhor1102.macrion.scenarios.list.copy.ScenarioCopyDialog.Companion.FRAGMENT_TAG_COPY_DIALOG
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState
import io.github.vibhor1102.macrion.scenarios.migration.ConditionsMigrationFragment.Companion.FRAGMENT_RESULT_KEY_COMPLETED
import io.github.vibhor1102.macrion.scenarios.migration.ConditionsMigrationFragment.Companion.FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG
import io.github.vibhor1102.macrion.scenarios.ScenarioActivity
import io.github.vibhor1102.macrion.settings.SettingsActivity

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import kotlinx.coroutines.launch

/**
 * Activity-owned host for the Compose scenario list and its dialog/navigation callbacks.
 */
class ScenarioListHost(
    private val activity: ScenarioActivity,
    private val scenarioListViewModel: ScenarioListViewModel,
    private val onLaunchScenario: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
) {

    private val tutorialNavigator: TutorialNavigator by lazy {
        activity.getTutorialNavigator()
    }

    private var uiState by mutableStateOf<ScenarioListUiState?>(null)
    private var searchQuery by mutableStateOf("")


    /** The current dialog being displayed. Null if not displayed. */
    private var dialog: AlertDialog? = null

    fun createView(): ComposeView {
        return ComposeView(activity).apply {
            setContent {
                MacrionTheme {
                    ComposeScenarioList(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        bitmapProvider = scenarioListViewModel::getConditionBitmap,
                        onSearchQueryChanged = { query ->
                            searchQuery = query
                            scenarioListViewModel.updateSearchQuery(query)
                        },
                        onSearchRequested = {
                            searchQuery = ""
                            scenarioListViewModel.setUiState(ScenarioListUiState.Type.SEARCH)
                            scenarioListViewModel.updateSearchQuery("")
                        },
                        onCancel = {
                            searchQuery = ""
                            scenarioListViewModel.updateSearchQuery(null)
                            scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
                        },
                        onSelectAll = scenarioListViewModel::toggleAllScenarioSelectionForBackup,
                        onImportExport = ::onImportExportClicked,
                        onTutorials = { tutorialNavigator.startTutorialActivity(activity) },
                        onSettings = ::startSettingsActivity,
                        onCreate = ::onCreateClicked,
                        onLaunch = ::onStartClicked,
                        onExpand = scenarioListViewModel::expandCollapseItem,
                        onExport = ::onExportClicked,
                        onCopy = ::showCopyScenarioDialog,
                        onDelete = ::onDeleteClicked,
                        onSortTypeClicked = scenarioListViewModel::updateSortType,
                        onSmartChipClicked = scenarioListViewModel::updateSmartVisible,
                        onDumbChipClicked = scenarioListViewModel::updateDumbVisible,
                        onSortOrderClicked = scenarioListViewModel::updateSortOrder,
                    )
                }
            }
        }
    }

    fun start() {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { scenarioListViewModel.uiState.collect(::updateUiState) }
                launch { scenarioListViewModel.needsConditionMigration.collect(::onConditionMigrationRequired) }
            }
        }
    }

    fun destroy() {
        dialog?.dismiss()
        dialog = null
    }

    private fun onImportExportClicked() {
        val uiState = scenarioListViewModel.uiState.value ?: return
        if (uiState.type == ScenarioListUiState.Type.EXPORT) {
            showBackupDialog(
                isImport = false,
                smartScenariosToBackup = scenarioListViewModel.getSmartScenariosSelectedForBackup(),
                dumbScenariosToBackup = scenarioListViewModel.getDumbScenariosSelectedForBackup(),
            )
        }
        else if (scenarioListViewModel.getScenarioValidForBackupCount() == 0) showBackupDialog(isImport = true)
        else showImportExportDialog()
    }

    private fun updateUiState(uiState: ScenarioListUiState?) {
        uiState ?: return

        this.uiState = uiState
    }

    private fun onConditionMigrationRequired(isRequired: Boolean) {
        if (!isRequired) return
        if (activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG) != null)
            return

        val fragmentManager = activity.supportFragmentManager
        fragmentManager.setFragmentResultListener(FRAGMENT_RESULT_KEY_COMPLETED, activity) { _, _ ->
            // Nothing to do
        }
        ConditionsMigrationFragment
            .newInstance()
            .show(fragmentManager, FRAGMENT_TAG_CONDITION_MIGRATION_DIALOG)
    }

    /**
     * Show an AlertDialog from this fragment.
     * This method will ensure that only one dialog is shown at the same time.
     *
     * @param newDialog the new dialog to be shown.
     */
    private fun showDialog(newDialog: AlertDialog) {
        dialog.let {
            Log.w(TAG, "Requesting show dialog while another one is one screen.")
            it?.dismiss()
        }

        dialog = newDialog
        newDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        newDialog.setOnDismissListener { dialog = null }
        newDialog.show()
    }

    /**
     * Called when the user clicks on a scenario.
     * @param scenario the scenario clicked.
     */
    private fun onStartClicked(scenario: ScenarioListUiState.Item.ScenarioItem) {
        onLaunchScenario(scenario)
    }

    /**
     * Called when the user clicks on the export button of a scenario.
     *
     * @param item the scenario clicked.
     */
    private fun onExportClicked(item: ScenarioListUiState.Item) {
        scenarioListViewModel.toggleScenarioSelectionForBackup(item)
    }

    /**
     * Called when the user clicks on the add scenario button.
     * Create and show the [dialog]. Upon Ok press, creates the scenario.
     */
    private fun onCreateClicked() {
        ScenarioCreationDialog()
            .show(activity.supportFragmentManager, ScenarioCreationDialog.FRAGMENT_TAG)
    }

    /**
     * Called when the user clicks on the delete button of a scenario.
     * Create and show the [dialog]. Upon Ok press, delete the scenario.
     *
     * @param item the scenario to delete.
     */
    private fun onDeleteClicked(item: ScenarioListUiState.Item.ScenarioItem) {
        showDialog(activity.createMacrionMessageDialog(
            title = activity.getString(R.string.dialog_title_delete_scenario),
            message = activity.getString(R.string.message_delete_scenario, item.displayName),
            cancelLabel = android.R.string.cancel,
            onConfirm = { scenarioListViewModel.deleteScenario(item) },
        ))
    }

    private fun showImportExportDialog() {
        val dialogContext = activity
        val composeView = ComposeView(dialogContext)
        val dialog = MaterialAlertDialogBuilder(dialogContext)
            .setView(composeView)
            .create()

        composeView.setContent {
            MacrionTheme {
                MacrionDialogSurface {
                    ImportExportContent(
                        onImport = {
                            dialog.dismiss()
                            showBackupDialog(isImport = true)
                        },
                        onExport = {
                            dialog.dismiss()
                            scenarioListViewModel.setUiState(ScenarioListUiState.Type.EXPORT)
                        },
                    )
                }
            }
        }
        dialog.show()
    }

    @Composable
    private fun ImportExportContent(onImport: () -> Unit, onExport: () -> Unit) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.dialog_title_backup), style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            Text(stringResource(R.string.message_backup), textAlign = TextAlign.Center)
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_backup_load), contentDescription = null)
                Text(stringResource(R.string.button_import), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_menu_save), contentDescription = null)
                Text(stringResource(R.string.button_export), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    /**
     * Shows the backup dialog fragment.
     *
     * @param isImport true to display in import mode, false for export.
     * @param smartScenariosToBackup the list of identifiers for the smart scenarios to export. Null if isImport = true.
     * @param dumbScenariosToBackup the list of identifiers for the dumb scenarios to export. Null if isImport = true.
     *
     */
    private fun showBackupDialog(
        isImport: Boolean,
        smartScenariosToBackup: Collection<Long>? = null,
        dumbScenariosToBackup: Collection<Long>? = null,
    ) {
        BackupDialogFragment
            .newInstance(isImport, smartScenariosToBackup, dumbScenariosToBackup)
            .show(activity.supportFragmentManager, FRAGMENT_TAG_BACKUP_DIALOG)
        scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
    }

    private fun showCopyScenarioDialog(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Valid) {
        ScenarioCopyDialog
            .newInstance(
                scenarioId = scenarioItem.getScenarioId(),
                isSmart = scenarioItem is ScenarioListUiState.Item.ScenarioItem.Valid.Smart,
                defaultName = scenarioItem.displayName,
            )
            .show(activity.supportFragmentManager, FRAGMENT_TAG_COPY_DIALOG)
    }

    private fun startSettingsActivity() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }
}

/** Tag for logs. */
private const val TAG = "ScenarioListHost"
