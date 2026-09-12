/*
 * Copyright (C) 2023 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.backup.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.feature.backup.R
import io.github.vibhor1102.macrion.feature.backup.data.BackupExportPlan
import io.github.vibhor1102.macrion.feature.backup.domain.Backup
import io.github.vibhor1102.macrion.feature.backup.domain.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * View model for the [BackupDialog].
 * Handle the state of a backup, import or export.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    private var isImportMode: Boolean = false
    private var klickrCompatibleExport: Boolean = false
    private var preparedKlickrExport: BackupExportPlan? = null

    /** The state of the backup. Null if not started yet. */
    private val _backupState = MutableStateFlow<BackupDialogUiState?>(null)
    /** The current UI state of the backup. Null if not started yet. */
    val backupState: StateFlow<BackupDialogUiState?> = _backupState

    /**
     * Setup this view model by specifying if we want to import or export.
     * @param isImport true for import, false for export.
     */
    fun initialize(context: Context, isImport: Boolean) {
        if (_backupState.value != null) return

        isImportMode = isImport
        _backupState.value = getInitialState(context, isImport)
    }

    fun reset() {
        _backupState.value = null
        preparedKlickrExport = null
        klickrCompatibleExport = false
    }

    fun setKlickrCompatibleExport(context: Context, enabled: Boolean) {
        if (isImportMode) return
        klickrCompatibleExport = enabled
        preparedKlickrExport = null
        _backupState.value = getInitialState(context, isImport = false)
    }

    fun prepareKlickrCompatibleExport(
        context: Context,
        dumbScenarios: List<Long>,
        smartScenarios: List<Long>,
    ) {
        if (isImportMode || !klickrCompatibleExport) return

        _backupState.value = getCompatibilityAnalysisState(context)
        viewModelScope.launch {
            try {
                val plan = withContext(Dispatchers.IO) {
                    repository.prepareScenarioBackup(dumbScenarios, smartScenarios, klickrCompatible = true)
                }
                preparedKlickrExport = plan
                _backupState.value = getCompatibilityReviewState(context, plan)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                preparedKlickrExport = null
                _backupState.value = getErrorState(context, isImport = false, malformedArchive = false)
            }
        }
    }

    /** @return the intent for selecting the file for the new exported backup. */
    fun createBackupFileCreationIntent() =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE_ZIP
            putExtra(
                Intent.EXTRA_TITLE,
                if (klickrCompatibleExport) "Klickr-Compatible-Backup.zip" else "Macrion-Backup.macrion.zip",
            )
        }

    /** @return the intent for selecting the file containing the imported backup. */
    fun createBackupRestorationFileSelectionIntent() =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MIME_TYPE_ZIP
        }

    /**
     * Start the backup.
     *
     * @param uri the uri of the file provided by [createBackupFileCreationIntent] or
     * [createBackupRestorationFileSelectionIntent] intent data result.
     * @param isImport true for an import, false for an export.
     * @param smartScenarios the list of scenario to be exported. Ignored for an import.
     */
    fun startBackup(
        context: Context,
        uri: Uri,
        isImport: Boolean,
        dumbScenarios: List<Long> = emptyList(),
        smartScenarios: List<Long> = emptyList(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isImport) {
                repository.restoreScenarioBackup(uri, displayConfigManager.displayConfig.sizePx).collect { backup ->
                    updateBackupState(context, backup, true)
                }
            } else {
                val backupFlow = preparedKlickrExport?.let { plan ->
                    repository.createPreparedScenarioBackup(
                        uri,
                        plan,
                        displayConfigManager.displayConfig.sizePx,
                    )
                } ?: repository.createNativeScenarioBackup(
                    uri,
                    dumbScenarios,
                    smartScenarios,
                    displayConfigManager.displayConfig.sizePx,
                )
                backupFlow.collect { backup ->
                    updateBackupState(context, backup, false)
                }
            }
        }
    }

    /**
     * Update the backup with the last results.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     */
    private fun updateBackupState(context: Context, backup: Backup?, isImport: Boolean) {
        _backupState.value = when (backup) {
            is Backup.Loading -> getLoadingState(context, backup, isImport)
            Backup.Verification -> getVerificationState(context)
            is Backup.Error -> getErrorState(context, isImport, backup.malformedArchive)
            is Backup.Completed -> getCompletedState(context, backup, isImport)
            null -> getInitialState(context, isImport)
        }
    }

    /**
     * Get the initial UI state.
     * @param isImport true for an import, false for an export.
     * @return the initial state.
     */
    private fun getInitialState(context: Context, isImport: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.VISIBLE,
        loadingVisibility = View.GONE,
        textStatusVisibility = View.GONE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = if (isImport) View.GONE else View.VISIBLE,
        klickrExportWarningVisibility = if (!isImport && klickrCompatibleExport) View.VISIBLE else View.GONE,
        klickrCompatibleChecked = klickrCompatibleExport,
        iconStatusVisibility = View.VISIBLE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = true,
        requiresCompatibilityPreparation = !isImport && klickrCompatibleExport,
        fileSelectionText = if (isImport) context.getString(R.string.item_title_backup_import_select_file)
                            else context.getString(R.string.item_title_backup_create_select_file),
        iconStatus = if (isImport) R.drawable.img_load else R.drawable.img_save,
    )

    private fun getCompatibilityAnalysisState(context: Context) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.VISIBLE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = View.GONE,
        klickrExportWarningVisibility = View.VISIBLE,
        klickrCompatibleChecked = true,
        iconStatusVisibility = View.GONE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = true,
        textStatusText = context.getString(R.string.message_backup_klickr_compatibility_analysis),
    )

    private fun getCompatibilityReviewState(
        context: Context,
        plan: BackupExportPlan,
    ) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.GONE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = View.GONE,
        klickrExportWarningVisibility = View.VISIBLE,
        klickrCompatibleChecked = true,
        iconStatusVisibility = View.VISIBLE,
        iconStatus = R.drawable.ic_warning,
        iconTint = Color.YELLOW,
        dialogOkButtonEnabled = true,
        dialogCancelButtonEnabled = true,
        compatibilityReviewReady = true,
        textStatusText = if (plan.omittedComponentCount == 0 && plan.excludedScenarioCount == 0) {
            context.getString(
                R.string.message_backup_klickr_compatibility_review_without_loss,
                plan.profile?.displayName.orEmpty(),
            )
        } else {
            context.getString(
                R.string.message_backup_klickr_compatibility_review,
                plan.profile?.displayName.orEmpty(),
                plan.omittedComponentCount,
                plan.excludedScenarioCount,
            )
        },
    )

    /**
     * Get the loading backup UI state.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     * @return the loading state.
     */
    private fun getLoadingState(context: Context, backup: Backup.Loading, isImport: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.VISIBLE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = View.GONE,
        klickrExportWarningVisibility = if (!isImport && klickrCompatibleExport) View.VISIBLE else View.GONE,
        klickrCompatibleChecked = klickrCompatibleExport,
        iconStatusVisibility = View.GONE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = false,
        textStatusText = if (isImport) context.getString(R.string.message_backup_import_progress, backup.progress ?: 0)
                         else context.getString(R.string.message_backup_create_progress, backup.progress ?: 0, backup.maxProgress ?: 0),
    )

    /** @return Get the verification backup UI state. */
    private fun getVerificationState(context: Context) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.VISIBLE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = View.GONE,
        klickrExportWarningVisibility = View.GONE,
        klickrCompatibleChecked = false,
        iconStatusVisibility = View.GONE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = false,
        textStatusText = context.getString(R.string.message_backup_import_verification)
    )

    /**
     * Get the error UI state.
     * @param isImport true for an import, false for an export.
     * @return the error state.
     */
    private fun getErrorState(context: Context, isImport: Boolean, malformedArchive: Boolean) = BackupDialogUiState(
        fileSelectionVisibility = View.GONE,
        loadingVisibility = View.GONE,
        textStatusVisibility = View.VISIBLE,
        compatWarningVisibility = View.GONE,
        klickrCheckboxVisibility = View.GONE,
        klickrExportWarningVisibility = View.GONE,
        klickrCompatibleChecked = false,
        iconStatusVisibility = View.VISIBLE,
        dialogOkButtonEnabled = false,
        dialogCancelButtonEnabled = true,
        textStatusText = if (isImport && malformedArchive) context.getString(R.string.message_backup_import_malformed)
                         else if (isImport) context.getString(R.string.message_backup_import_error)
                         else context.getString(R.string.message_backup_create_error),
        iconStatus = R.drawable.img_error,
        iconTint = Color.RED,
    )

    /**
     * Get the completed backup UI state.
     * @param backup the last backup results.
     * @param isImport true for an import, false for an export.
     * @return the completed state.
     */
    private fun getCompletedState(context: Context, backup: Backup.Completed, isImport: Boolean): BackupDialogUiState {
        var iconStatus = R.drawable.img_success
        val textStatus = when {
            !isImport && backup.klickrCompatibleExport ->
                if (backup.omittedComponentCount == 0) {
                    context.getString(R.string.message_backup_create_klickr_compatible_completed_without_loss)
                } else {
                    context.getString(
                        R.string.message_backup_create_klickr_compatible_completed,
                        backup.omittedComponentCount,
                    )
                }
            !isImport -> context.getString(R.string.message_backup_create_completed)
            backup.failureCount == 0 ->
                context.getString(R.string.message_backup_import_completed, backup.successCount)
            else -> {
                iconStatus = R.drawable.ic_warning
                context.getString(
                    R.string.message_backup_import_completed_with_error,
                    backup.successCount,
                    backup.failureCount
                )
            }
        }

        val compatVisibility = if (backup.compatWarning) {
            iconStatus = R.drawable.ic_warning
            View.VISIBLE
        } else {
            View.GONE
        }

        return BackupDialogUiState(
            fileSelectionVisibility = View.GONE,
            loadingVisibility = View.GONE,
            iconStatusVisibility = View.VISIBLE,
            iconStatus = iconStatus,
            iconTint = if (iconStatus == R.drawable.ic_warning) Color.YELLOW else Color.GREEN,
            textStatusVisibility = View.VISIBLE,
            textStatusText = textStatus,
            compatWarningVisibility = compatVisibility,
            klickrCheckboxVisibility = View.GONE,
            klickrExportWarningVisibility = if (backup.klickrCompatibleExport) View.VISIBLE else View.GONE,
            klickrCompatibleChecked = backup.klickrCompatibleExport,
            dialogOkButtonEnabled = true,
            dialogCancelButtonEnabled = false,
        )
    }
}

/** Ui state for the backup dialog. */
data class BackupDialogUiState(
    val fileSelectionVisibility: Int,
    val loadingVisibility: Int,
    val textStatusVisibility: Int,
    val compatWarningVisibility: Int,
    val klickrCheckboxVisibility: Int,
    val klickrExportWarningVisibility: Int,
    val klickrCompatibleChecked: Boolean,
    val iconStatusVisibility: Int,

    val dialogOkButtonEnabled: Boolean,
    val dialogCancelButtonEnabled: Boolean,
    val requiresCompatibilityPreparation: Boolean = false,
    val compatibilityReviewReady: Boolean = false,

    val fileSelectionText: String? = null,
    val textStatusText: String? = null,
    @field:DrawableRes val iconStatus: Int? = null,
    @field:ColorInt val iconTint: Int? = null,
)

/** Zip mime type. */
private const val MIME_TYPE_ZIP = "application/zip"
