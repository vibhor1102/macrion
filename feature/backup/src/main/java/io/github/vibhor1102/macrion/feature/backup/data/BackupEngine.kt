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
package io.github.vibhor1102.macrion.feature.backup.data

import android.content.ContentResolver
import android.graphics.Point
import android.net.Uri
import android.util.Log

import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions
import io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbBackupDataSource
import io.github.vibhor1102.macrion.feature.backup.data.smart.SmartBackupDataSource
import io.github.vibhor1102.macrion.feature.backup.data.base.UnsupportedBackupSchemaException
import io.github.vibhor1102.macrion.feature.backup.data.base.validateBackupSchema
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_MANIFEST_FILE_NAME
import io.github.vibhor1102.macrion.feature.backup.data.base.MACRION_FORMAT_NAME
import io.github.vibhor1102.macrion.feature.backup.data.base.detectBackupEntryFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.mergeDetectedFormat
import io.github.vibhor1102.macrion.feature.backup.data.base.MalformedBackupArchiveException
import io.github.vibhor1102.macrion.core.base.extensions.getInt
import io.github.vibhor1102.macrion.core.base.extensions.getString

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.UUID

/** [BackupEngine] internal implementation. */
internal class BackupEngine(private val appDataDir: File, private val contentResolver: ContentResolver) {

    private val dumbBackupDataSource: DumbBackupDataSource = DumbBackupDataSource(appDataDir)
    private val smartBackupDataSource: SmartBackupDataSource = SmartBackupDataSource(appDataDir)

    /**
     * Creates a new backup file.
     *
     * @param zipFileUri the uri of the file to write the backup into. Must be retrieved using the DocumentProvider.
     * @param plan the scenarios and compatibility decisions approved before writing.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup progress.
     */
    suspend fun createBackup(
        zipFileUri: Uri,
        plan: BackupExportPlan,
        screenSize: Point,
        progress: BackupProgress,
    ) {
        Log.d(TAG, "Create backup: $zipFileUri for plan: $plan")
        dumbBackupDataSource.reset()
        smartBackupDataSource.reset()

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, plan.dumbScenarios.size + plan.smartScenarios.size)

        // Create the zip file containing the scenarios and their events conditions.
        withContext(Dispatchers.IO) {
            try {
                ZipOutputStream(contentResolver.openOutputStream(zipFileUri, "wt")).use { zipStream ->
                    if (plan.format == BackupArchiveFormat.MACRION_NATIVE) {
                        zipStream.putNextEntry(ZipEntry(MACRION_MANIFEST_FILE_NAME))
                        zipStream.write(
                            """{"format":"macrion","containerVersion":1,"databaseVersion":${io.github.vibhor1102.macrion.core.database.DATABASE_VERSION}}"""
                                .toByteArray(),
                        )
                    }

                    plan.dumbScenarios.forEach { dumbScenario ->
                        Log.d(TAG, "Backup dumb scenario ${dumbScenario.scenario.id}")

                        dumbBackupDataSource.addScenarioToZipFile(
                            zipStream,
                            dumbScenario,
                            screenSize,
                            plan.format,
                        )

                        currentProgress++
                        progress.onProgressChanged(currentProgress, plan.dumbScenarios.size + plan.smartScenarios.size)
                    }

                    plan.smartScenarios.forEach { completeScenario ->
                        Log.d(TAG, "Backup smart scenario ${completeScenario.scenario.id}")

                        smartBackupDataSource.addScenarioToZipFile(
                            zipStream,
                            completeScenario,
                            screenSize,
                            plan.format,
                            plan.profile?.portableDatabaseVersion,
                        )

                        currentProgress++
                        progress.onProgressChanged(currentProgress, plan.dumbScenarios.size + plan.smartScenarios.size)
                    }
                }

                progress.onCompleted(
                    plan.dumbScenarios,
                    plan.smartScenarios,
                    0,
                    false,
                    plan.omittedComponentCount,
                )
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while creating backup archive.")
                progress.onError(BackupError.GENERIC)
            } catch (isEx: IllegalStateException) {
                Log.e(TAG, "Error while creating backup archive, target folder can't be written")
                progress.onError(BackupError.GENERIC)
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while creating backup archive, permission is denied")
                progress.onError(BackupError.GENERIC)
            }
        }
    }

    /**
     * Loads a backup file.
     *
     * @param zipFileUri the uri of the file to load the backup from. Must be retrieved using the DocumentProvider.
     * @param screenSize the size of this device screen.
     * @param progress the object notified about the backup import progress.
     */
    suspend fun loadBackup(
        zipFileUri: Uri, screenSize: Point, progress: BackupProgress,
        prepared: BackupImportPlan? = null, allowPartial: Boolean = false,
    ) {
        require(prepared == null || (zipFileUri == Uri.fromFile(prepared.source) && (!prepared.requiresReview || allowPartial)))
        Log.i(TAG, "Load backup: $zipFileUri")

        val importStagingDir = File(appDataDir, "backup-import-${UUID.randomUUID()}")
        if (!importStagingDir.mkdirs()) {
            progress.onError(BackupError.GENERIC)
            return
        }
        val importDumbDataSource = DumbBackupDataSource(importStagingDir)
        val importSmartDataSource = SmartBackupDataSource(importStagingDir)

        var currentProgress = 0
        progress.onProgressChanged(currentProgress, null)

        withContext(Dispatchers.IO) {
            try {
                val archiveFormat = prepared?.format ?: detectArchiveFormat(contentResolver, zipFileUri)
                ZipInputStream(contentResolver.openInputStream(zipFileUri)).use { zipStream ->
                    generateSequence { zipStream.nextEntry }
                        .forEach { zipEntry ->
                            if (zipEntry.isDirectory) return@forEach

                            val entryFormat = detectBackupEntryFormat(zipEntry.name)
                            if (zipEntry.name == MACRION_MANIFEST_FILE_NAME) return@forEach
                            if (entryFormat != archiveFormat) return@forEach

                            val scenarioEntry = zipEntry.name.endsWith(".json")
                            if (prepared != null && scenarioEntry && prepared.scenarios[zipEntry.name] == null) return@forEach
                            val normalized = prepared?.scenarios?.get(zipEntry.name)
                            Log.d(TAG, "Extracting file ${zipEntry.name}")
                            when {
                                importDumbDataSource.extractFromZip(zipStream, zipEntry.name, archiveFormat, normalized) -> {
                                    Log.d(TAG, "Dumb scenario file ${zipEntry.name} extracted.")

                                    currentProgress++
                                    progress.onProgressChanged(currentProgress, null)
                                }

                                importSmartDataSource.extractFromZip(zipStream, zipEntry.name, archiveFormat, normalized) -> {
                                    if (importSmartDataSource.isScenarioBackupFileZipEntry(zipEntry.name, archiveFormat)) {
                                        Log.d(TAG, "Smart scenario file ${zipEntry.name} extracted")

                                        currentProgress++
                                        progress.onProgressChanged(currentProgress, null)
                                    }
                                }

                                else -> Log.w(TAG, "Nothing found to handle zip entry ${zipEntry.name}")
                            }
                        }
                }

                progress.onVerification?.invoke()
                importDumbDataSource.verifyExtractedScenarios(screenSize)
                importSmartDataSource.verifyExtractedScenarios(screenSize)
                importSmartDataSource.commitValidAdditionalFiles(appDataDir)

                Log.i(TAG, "Backup loading completed: $zipFileUri")
                Log.i(TAG, "Inserting extracted scenarios into database")

                progress.onCompleted(
                    importDumbDataSource.validBackups,
                    importSmartDataSource.validBackups,
                    importDumbDataSource.failureCount + importSmartDataSource.failureCount,
                    importSmartDataSource.screenCompatWarning,
                    0,
                )
            } catch (unsupported: UnsupportedBackupSchemaException) {
                Log.w(TAG, "Backup uses an unsupported schema; nothing imported", unsupported)
                progress.onError(BackupError.UNSUPPORTED_SCHEMA)
            } catch (malformed: MalformedBackupArchiveException) {
                Log.e(TAG, "Backup archive is malformed or mixes formats", malformed)
                progress.onError(BackupError.MALFORMED_ARCHIVE)
            } catch (ioEx: IOException) {
                Log.e(TAG, "Error while loading backup archive", ioEx)
                progress.onError(BackupError.GENERIC)
            } catch (secEx: SecurityException) {
                Log.e(TAG, "Error while loading backup archive, permission is denied", secEx)
                progress.onError(BackupError.GENERIC)
            } catch (iaEx: IllegalArgumentException) {
                Log.e(TAG, "Error while loading backup archive, file is invalid", iaEx)
                progress.onError(BackupError.GENERIC)
            } catch (npEx: NullPointerException) {
                Log.e(TAG, "Error while loading backup archive, file path is null", npEx)
                progress.onError(BackupError.GENERIC)
            } finally {
                importStagingDir.deleteRecursively()
            }
        }
    }
}

private fun detectArchiveFormat(
    contentResolver: ContentResolver,
    zipFileUri: Uri,
): BackupArchiveFormat = contentResolver.openInputStream(zipFileUri).use { inputStream ->
    inputStream ?: throw MalformedBackupArchiveException()
    detectArchiveFormat(inputStream)
}

internal fun detectArchiveFormat(inputStream: InputStream, validateSchemas: Boolean = true): BackupArchiveFormat {
    var archiveFormat: BackupArchiveFormat? = null
    var nativeManifestFound = false
    var scenarioEntryFound = false

    ZipInputStream(inputStream).use { zipStream ->
        generateSequence { zipStream.nextEntry }
            .forEach { zipEntry ->
                if (zipEntry.isDirectory) return@forEach

                val entryFormat = detectBackupEntryFormat(zipEntry.name)
                archiveFormat = mergeDetectedFormat(archiveFormat, entryFormat)
                if (entryFormat != null && zipEntry.name.endsWith(".json") &&
                    zipEntry.name != MACRION_MANIFEST_FILE_NAME) {
                    scenarioEntryFound = true
                    validateScenarioEntryFormat(zipStream, entryFormat, isDumb = zipEntry.name.startsWith("dumb-"), validateSchema = validateSchemas)
                }

                if (zipEntry.name == MACRION_MANIFEST_FILE_NAME) {
                    validateNativeManifest(zipStream, allowFuture = !validateSchemas)
                    nativeManifestFound = true
                }
            }
    }

    val detectedFormat = archiveFormat ?: throw MalformedBackupArchiveException()
    if (!scenarioEntryFound || (
            detectedFormat == BackupArchiveFormat.MACRION_NATIVE && !nativeManifestFound
        )) {
        throw MalformedBackupArchiveException()
    }
    return detectedFormat
}

private fun validateScenarioEntryFormat(
    zipStream: ZipInputStream,
    format: BackupArchiveFormat,
    isDumb: Boolean,
    validateSchema: Boolean,
) {
    val json = try {
        Json.parseToJsonElement(zipStream.readBytes().toString(Charsets.UTF_8)).jsonObject
    } catch (error: RuntimeException) {
        throw MalformedBackupArchiveException()
    }
    val declaredFormat = json.getString("format")
    if (
        (format == BackupArchiveFormat.MACRION_NATIVE && declaredFormat != MACRION_FORMAT_NAME) ||
        (format == BackupArchiveFormat.KLICKR_COMPATIBLE && declaredFormat == MACRION_FORMAT_NAME)
    ) {
        throw MalformedBackupArchiveException()
    }
    if (validateSchema) validateBackupSchema(json, format, isDumb)
}

private fun validateNativeManifest(zipStream: ZipInputStream, allowFuture: Boolean = false) {
    val manifest = try {
        Json.parseToJsonElement(zipStream.readBytes().toString(Charsets.UTF_8)).jsonObject
    } catch (error: RuntimeException) {
        throw MalformedBackupArchiveException()
    }

    if (
        manifest.getString("format") != MACRION_FORMAT_NAME ||
        (if (allowFuture) (manifest.getInt("containerVersion") ?: 0) < 1 else manifest.getInt("containerVersion") != 1) ||
        (manifest.getInt("databaseVersion") ?: -1) < 0
    ) {
        throw MalformedBackupArchiveException()
    }
}

/** Tag for logs. */
private const val TAG = "BackupEngine"
