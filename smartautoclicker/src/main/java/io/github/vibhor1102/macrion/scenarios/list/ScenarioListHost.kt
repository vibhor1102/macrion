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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import io.github.vibhor1102.macrion.R
import io.github.vibhor1102.macrion.core.common.navigation.TutorialNavigator
import io.github.vibhor1102.macrion.core.common.navigation.getTutorialNavigator
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.ui.PermissionsHost
import io.github.vibhor1102.macrion.core.common.quality.ui.AccessibilityTroubleshootingDialog
import io.github.vibhor1102.macrion.core.common.quality.ui.BackgroundLaunchTroubleshootingDialog
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.crash.CrashReportDialog
import io.github.vibhor1102.macrion.feature.backup.ui.BackupDialog
import io.github.vibhor1102.macrion.feature.backup.ui.BackupViewModel
import io.github.vibhor1102.macrion.scenarios.ScenarioActivity
import io.github.vibhor1102.macrion.scenarios.creation.ScenarioCreationSheet
import io.github.vibhor1102.macrion.scenarios.creation.ScenarioCreationViewModel
import io.github.vibhor1102.macrion.scenarios.list.copy.ScenarioCopyDialog
import io.github.vibhor1102.macrion.scenarios.list.copy.ScenarioCopyViewModel
import io.github.vibhor1102.macrion.scenarios.list.model.ScenarioListUiState
import io.github.vibhor1102.macrion.scenarios.migration.ConditionsMigrationDialog
import io.github.vibhor1102.macrion.scenarios.migration.ConditionsMigrationViewModel
import io.github.vibhor1102.macrion.settings.SettingsActivity

import kotlinx.coroutines.launch

sealed interface ActiveScenarioDialog {
    data object Create : ActiveScenarioDialog
    data class Copy(val item: ScenarioListUiState.Item.ScenarioItem.Valid) : ActiveScenarioDialog
    data class Delete(val item: ScenarioListUiState.Item.ScenarioItem) : ActiveScenarioDialog
    data object ImportExport : ActiveScenarioDialog
    data class Backup(
        val isImport: Boolean,
        val smartScenariosToBackup: Collection<Long>? = null,
        val dumbScenariosToBackup: Collection<Long>? = null,
    ) : ActiveScenarioDialog
    data object ConditionsMigration : ActiveScenarioDialog
    data class CrashReport(val reportId: String) : ActiveScenarioDialog
    data object BackgroundLaunchHelp : ActiveScenarioDialog
    data class AccessibilityTroubleshooting(val onDismissed: () -> Unit) : ActiveScenarioDialog
}

/**
 * Activity-owned host for the Compose scenario list and its dialog/navigation callbacks.
 */
class ScenarioListHost(
    private val activity: ScenarioActivity,
    private val scenarioListViewModel: ScenarioListViewModel,
    private val scenarioCreationViewModel: ScenarioCreationViewModel,
    private val scenarioCopyViewModel: ScenarioCopyViewModel,
    private val backupViewModel: BackupViewModel,
    private val conditionsMigrationViewModel: ConditionsMigrationViewModel,
    private val permissionsController: PermissionsController,
    private val onLaunchScenario: (ScenarioListUiState.Item.ScenarioItem) -> Unit,
    private val onDialogDismissed: () -> Unit = {},
) {

    private val tutorialNavigator: TutorialNavigator by lazy {
        activity.getTutorialNavigator()
    }

    private var uiState by mutableStateOf<ScenarioListUiState?>(null)
    private var searchQuery by mutableStateOf("")
    private var activeDialog by mutableStateOf<ActiveScenarioDialog?>(null)

    fun hasActiveDialog(): Boolean = activeDialog != null

    fun isShowingBackgroundLaunchHelp(): Boolean = activeDialog == ActiveScenarioDialog.BackgroundLaunchHelp

    fun showCrashReportDialog(reportId: String) {
        activeDialog = ActiveScenarioDialog.CrashReport(reportId)
    }

    fun showLocalePluginBackgroundLaunchHelp() {
        activeDialog = ActiveScenarioDialog.BackgroundLaunchHelp
    }

    fun showAccessibilityTroubleshootingDialog(onDismissed: () -> Unit) {
        activeDialog = ActiveScenarioDialog.AccessibilityTroubleshooting(onDismissed)
    }

    private fun dismissActiveDialog() {
        activeDialog = null
        onDialogDismissed()
    }

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

                    when (val currentDialog = activeDialog) {
                        ActiveScenarioDialog.Create -> {
                            ScenarioCreationSheet(
                                viewModel = scenarioCreationViewModel,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        is ActiveScenarioDialog.Copy -> {
                            ScenarioCopyDialog(
                                scenarioId = currentDialog.item.getScenarioId(),
                                isSmart = currentDialog.item is ScenarioListUiState.Item.ScenarioItem.Valid.Smart,
                                defaultName = currentDialog.item.displayName,
                                viewModel = scenarioCopyViewModel,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        is ActiveScenarioDialog.Delete -> {
                            AlertDialog(
                                onDismissRequest = ::dismissActiveDialog,
                                title = {
                                    Text(
                                        stringResource(R.string.dialog_title_delete_scenario),
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.message_delete_scenario, currentDialog.item.displayName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            scenarioListViewModel.deleteScenario(currentDialog.item)
                                            dismissActiveDialog()
                                        },
                                    ) {
                                        Text(stringResource(android.R.string.ok))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = ::dismissActiveDialog) {
                                        Text(stringResource(android.R.string.cancel))
                                    }
                                },
                            )
                        }
                        ActiveScenarioDialog.ImportExport -> {
                            Dialog(onDismissRequest = ::dismissActiveDialog) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 6.dp,
                                ) {
                                    MacrionDialogSurface {
                                        ImportExportContent(
                                            onImport = {
                                                showBackupDialog(isImport = true)
                                            },
                                            onExport = {
                                                dismissActiveDialog()
                                                scenarioListViewModel.setUiState(ScenarioListUiState.Type.EXPORT)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        is ActiveScenarioDialog.Backup -> {
                            BackupDialog(
                                isImport = currentDialog.isImport,
                                exportSmartScenarios = currentDialog.smartScenariosToBackup?.toList().orEmpty(),
                                exportDumbScenarios = currentDialog.dumbScenariosToBackup?.toList().orEmpty(),
                                viewModel = backupViewModel,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        ActiveScenarioDialog.ConditionsMigration -> {
                            ConditionsMigrationDialog(
                                viewModel = conditionsMigrationViewModel,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        is ActiveScenarioDialog.CrashReport -> {
                            CrashReportDialog(
                                reportId = currentDialog.reportId,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        ActiveScenarioDialog.BackgroundLaunchHelp -> {
                            BackgroundLaunchTroubleshootingDialog(
                                title = activity.getString(R.string.dialog_title_locale_plugin_background_launch),
                                message = activity.getString(R.string.message_locale_plugin_background_launch),
                                helpUrl = DONT_KILL_MY_APP_URL,
                                onDismiss = ::dismissActiveDialog,
                            )
                        }
                        is ActiveScenarioDialog.AccessibilityTroubleshooting -> {
                            AccessibilityTroubleshootingDialog(
                                onDismiss = {
                                    val onDismissed = currentDialog.onDismissed
                                    dismissActiveDialog()
                                    onDismissed()
                                },
                            )
                        }
                        null -> Unit
                    }

                    PermissionsHost(permissionsController = permissionsController)
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
        activeDialog = null
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
        if (activeDialog == ActiveScenarioDialog.ConditionsMigration) return
        activeDialog = ActiveScenarioDialog.ConditionsMigration
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
     */
    private fun onCreateClicked() {
        activeDialog = ActiveScenarioDialog.Create
    }

    /**
     * Called when the user clicks on the delete button of a scenario.
     *
     * @param item the scenario to delete.
     */
    private fun onDeleteClicked(item: ScenarioListUiState.Item.ScenarioItem) {
        activeDialog = ActiveScenarioDialog.Delete(item)
    }

    private fun showImportExportDialog() {
        activeDialog = ActiveScenarioDialog.ImportExport
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
     * Shows the backup dialog.
     *
     * @param isImport true to display in import mode, false for export.
     * @param smartScenariosToBackup the list of identifiers for the smart scenarios to export. Null if isImport = true.
     * @param dumbScenariosToBackup the list of identifiers for the dumb scenarios to export. Null if isImport = true.
     */
    private fun showBackupDialog(
        isImport: Boolean,
        smartScenariosToBackup: Collection<Long>? = null,
        dumbScenariosToBackup: Collection<Long>? = null,
    ) {
        activeDialog = ActiveScenarioDialog.Backup(
            isImport = isImport,
            smartScenariosToBackup = smartScenariosToBackup,
            dumbScenariosToBackup = dumbScenariosToBackup,
        )
        scenarioListViewModel.setUiState(ScenarioListUiState.Type.SELECTION)
    }

    private fun showCopyScenarioDialog(scenarioItem: ScenarioListUiState.Item.ScenarioItem.Valid) {
        activeDialog = ActiveScenarioDialog.Copy(scenarioItem)
    }

    private fun startSettingsActivity() {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
    }
}

private const val DONT_KILL_MY_APP_URL = "https://dontkillmyapp.com/?app=Macrion"
