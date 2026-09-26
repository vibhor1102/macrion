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
package io.github.vibhor1102.macrion.feature.backup.data.dumb

import android.graphics.Point
import android.util.Log

import io.github.vibhor1102.macrion.core.dumb.data.database.DUMB_DATABASE_VERSION
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions
import io.github.vibhor1102.macrion.feature.backup.data.base.DUMB_SCENARIO_BACKUP_MATCH_REGEX
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_DUMB_SCENARIO_BACKUP_MATCH_REGEX
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_FORMAT_NAME
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupAdditionalFile
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.ScenarioBackupDataSource
import io.github.vibhor1102.macrion.feature.backup.data.base.ScenarioBackupSerializer
import io.github.vibhor1102.macrion.feature.backup.data.base.backupFolderName
import io.github.vibhor1102.macrion.feature.backup.data.base.scenarioBackupFileName

import java.io.File

internal class DumbBackupDataSource(
    appDataDir: File,
): ScenarioBackupDataSource<DumbScenarioBackup, DumbScenarioWithActions>(appDataDir) {

    /** Regex matching a condition file into its folder in a backup archive. */
    private val scenarioUnzipMatchRegex = DUMB_SCENARIO_BACKUP_MATCH_REGEX.toRegex()
    private val nativeScenarioUnzipMatchRegex = MACRION_DUMB_SCENARIO_BACKUP_MATCH_REGEX.toRegex()

    override val serializer: ScenarioBackupSerializer<DumbScenarioBackup> = DumbScenarioSerializer()

    override fun isScenarioBackupFileZipEntry(fileName: String, format: BackupArchiveFormat): Boolean =
        fileName.matches(if (format == BackupArchiveFormat.MACRION_NATIVE) nativeScenarioUnzipMatchRegex else scenarioUnzipMatchRegex)

    override fun isScenarioBackupAdditionalFileZipEntry(fileName: String, format: BackupArchiveFormat): Boolean =
        false

    override fun getBackupZipFolderName(scenario: DumbScenarioWithActions): String =
        scenario.backupFolderName()

    override fun getBackupFileName(scenario: DumbScenarioWithActions, format: BackupArchiveFormat): String =
        scenario.scenarioBackupFileName(format)

    override fun createBackupFromScenario(
        scenario: DumbScenarioWithActions,
        screenSize: Point,
        format: BackupArchiveFormat,
        portableDatabaseVersion: Int?,
    ): DumbScenarioBackup =
        DumbScenarioBackup(
            format = if (format == BackupArchiveFormat.MACRION_NATIVE) MACRION_FORMAT_NAME else null,
            dumbScenario = if (format == BackupArchiveFormat.MACRION_NATIVE) {
                scenario.copy(dumbActionsWithSubActions = scenario.dumbActionsWithSubActions.filter { it.splitItems.isNotEmpty() })
            } else {
                scenario.copy(dumbActionsWithSubActions = emptyList())
            },
            screenWidth = screenSize.x,
            screenHeight = screenSize.y,
            // Keep the previously emitted portable version; the new path column is native-only.
            version = if (format == BackupArchiveFormat.MACRION_NATIVE) DUMB_DATABASE_VERSION else 5,
        )

    override fun verifyExtractedBackup(backup: DumbScenarioBackup, screenSize: Point): DumbScenarioWithActions? {
        Log.i(TAG, "Verifying dumb scenario ${backup.dumbScenario.scenario.id}")

        if (backup.dumbScenario.dumbActions.isEmpty()) {
            Log.w(TAG, "Invalid dumb scenario, dumb action list is empty.")
            return null
        }

        return backup.dumbScenario
    }

    override fun getBackupAdditionalFilesPaths(
        scenario: DumbScenarioWithActions,
        format: BackupArchiveFormat,
    ): Set<BackupAdditionalFile> =
        emptySet()
}

/** Tag for logs. */
private const val TAG = "DumbBackupEngine"
