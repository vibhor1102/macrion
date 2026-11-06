/*
 * Copyright (C) 2024 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.backup.domain

import android.content.Context
import android.graphics.Point
import android.net.Uri
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import io.github.vibhor1102.macrion.feature.backup.data.BackupImportPlan
import io.github.vibhor1102.macrion.feature.backup.data.BackupImportPreparer

import io.github.vibhor1102.macrion.core.database.ClickDatabase
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbDatabase
import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.feature.backup.data.BackupEngine
import io.github.vibhor1102.macrion.feature.backup.data.BackupExportPlan
import io.github.vibhor1102.macrion.feature.backup.data.BackupProgress
import io.github.vibhor1102.macrion.feature.backup.data.BackupError
import io.github.vibhor1102.macrion.feature.backup.data.KlickrCompatibilityProjector
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dumbDatabase: DumbDatabase,
    private val dumbRepository: IDumbRepository,
    private val smartDatabase: ClickDatabase,
    private val smartRepository: IRepository,
) {

    private val backupEngine: BackupEngine = BackupEngine(
        appDataDir = context.filesDir,
        contentResolver = context.contentResolver,
    )

    private val importPreparer = BackupImportPreparer(context.cacheDir, context.contentResolver)

    internal suspend fun prepareScenarioImport(uri: Uri): BackupImportPlan = importPreparer.prepare(uri)

    /**
     * Create a backup of the provided scenario into the provided file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param dumbScenarios the dumb scenarios to backup.
     * @param smartScenarios the smart scenarios to backup.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup creation progress.
     */
    fun createNativeScenarioBackup(
        zipFileUri: Uri,
        dumbScenarios: List<Long>,
        smartScenarios: List<Long>,
        screenSize: Point,
    ) = channelFlow {
        launch {
            val plan = prepareScenarioBackup(dumbScenarios, smartScenarios, klickrCompatible = false)
            backupEngine.createBackup(
                zipFileUri = zipFileUri,
                plan = plan,
                screenSize = screenSize,
                progress = BackupProgress(
                    onError = { send(Backup.Error()) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onCompleted = { dumbs, smarts, failureCount, compatWarning, omittedComponentCount ->
                        send(Backup.Completed(
                            successCount = dumbs.size + smarts.size,
                            failureCount = failureCount,
                            compatWarning = compatWarning,
                            klickrCompatibleExport = false,
                            omittedComponentCount = omittedComponentCount,
                        ))
                    }
                )
            )
        }
    }

    internal suspend fun prepareScenarioBackup(
        dumbScenarioIds: List<Long>,
        smartScenarioIds: List<Long>,
        klickrCompatible: Boolean,
    ): BackupExportPlan {
        val dumbScenarios = dumbScenarioIds.mapNotNull {
            dumbDatabase.dumbScenarioDao().getDumbScenariosWithAction(it)
        }
        val smartScenarios = smartScenarioIds.mapNotNull {
            smartDatabase.scenarioDao().getCompleteScenario(it)
        }

        return if (klickrCompatible) {
            KlickrCompatibilityProjector.createPlan(dumbScenarios, smartScenarios)
        } else {
            BackupExportPlan(
                dumbScenarios = dumbScenarios,
                smartScenarios = smartScenarios,
                format = BackupArchiveFormat.MACRION_NATIVE,
            )
        }
    }

    internal fun createPreparedScenarioBackup(
        zipFileUri: Uri,
        plan: BackupExportPlan,
        screenSize: Point,
    ) = channelFlow {
        launch {
            require(
                plan.format != BackupArchiveFormat.KLICKR_COMPATIBLE || plan.profile != null
            ) { "A Klick'r-compatible export requires an explicit compatibility profile." }
            backupEngine.createBackup(
                zipFileUri = zipFileUri,
                plan = plan,
                screenSize = screenSize,
                progress = BackupProgress(
                    onError = { send(Backup.Error()) },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onCompleted = { dumbs, smarts, failureCount, compatWarning, omittedComponentCount ->
                        send(Backup.Completed(
                            successCount = dumbs.size + smarts.size,
                            failureCount = failureCount,
                            compatWarning = compatWarning,
                            klickrCompatibleExport = plan.format == BackupArchiveFormat.KLICKR_COMPATIBLE,
                            omittedComponentCount = omittedComponentCount,
                        ))
                    },
                ),
            )
        }
    }

    /**
     * Restore a backup of scenarios from the provided file.
     *
     * @param zipFileUri the uri of the file to read the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     *
     * @return a flow on the backup import progress.
     */
    fun restoreScenarioBackup(zipFileUri: Uri, screenSize: Point) = flow<Backup> {
        val plan = prepareScenarioImport(zipFileUri)
        try {
            if (plan.requiresReview) emit(Backup.Error(unsupportedSchema = true))
            else emitAll(restorePreparedScenarioBackup(plan, screenSize))
        } finally { plan.close() }
    }

    internal fun restorePreparedScenarioBackup(plan: BackupImportPlan, screenSize: Point, allowPartial: Boolean = false) = channelFlow {
        launch {
            backupEngine.loadBackup(
                Uri.fromFile(plan.source),
                screenSize,
                BackupProgress(
                    onError = { error ->
                        send(Backup.Error(
                            malformedArchive = error == BackupError.MALFORMED_ARCHIVE,
                            unsupportedSchema = error == BackupError.UNSUPPORTED_SCHEMA,
                        ))
                    },
                    onProgressChanged = { current, max -> send(Backup.Loading(current, max)) },
                    onVerification = { send(Backup.Verification) },
                    onCompleted = { dumbs, smarts, failureCount, compatWarning, _ ->
                        var totalFailures = failureCount

                        val dumbsSnapshot = dumbs.toList()
                        val dumbsSuccess = dumbsSnapshot.toMutableList()
                        dumbsSnapshot.forEach { completeScenario ->
                            if (!insertScenarioCopy(dumbDatabase) { dumbRepository.addDumbScenarioCopy(completeScenario) }) {
                                dumbsSuccess.remove(completeScenario)
                                totalFailures++
                            }
                        }

                        val smartsSnapshot = smarts.toList()
                        val smartsSuccess = smartsSnapshot.toMutableList()
                        smartsSnapshot.forEach { completeScenario ->
                            if (!insertScenarioCopy(smartDatabase) { smartRepository.addScenarioCopy(completeScenario) }) {
                                smartsSuccess.remove(completeScenario)
                                totalFailures++
                            }
                        }

                        send(Backup.Completed(
                            successCount = dumbsSuccess.size + smartsSuccess.size,
                            failureCount = totalFailures,
                            compatWarning = compatWarning,
                        ))
                    }
                ),
                prepared = plan,
                allowPartial = allowPartial,
            )
        }
    }
}

/** A repository may return null after a partial insert; force rollback of that entire scenario. */
private suspend fun insertScenarioCopy(database: RoomDatabase, insert: suspend () -> Long?): Boolean = try {
    database.withTransaction { checkNotNull(insert()) }
    true
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    false
}
