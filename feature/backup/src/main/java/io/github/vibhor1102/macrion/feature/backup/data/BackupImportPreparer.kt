/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data

import android.content.ContentResolver
import android.net.Uri
import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.feature.backup.data.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

internal class BackupImportPreparer(private val cacheDir: File, private val resolver: ContentResolver) {
    suspend fun prepare(uri: Uri): BackupImportPlan {
        val directory = File(cacheDir, "backup-review-${UUID.randomUUID()}")
        try {
            return withContext(Dispatchers.IO) {
                check(directory.mkdirs())
                val snapshot = File(directory, "source.zip")
                resolver.openInputStream(uri)?.use { input -> snapshot.outputStream().use { input.copyTo(it) } }
                    ?: throw MalformedBackupArchiveException()
                val format = detectArchiveFormat(snapshot.inputStream(), validateSchemas = false)
                val scenarios = linkedMapOf<String, File?>()
                val issues = mutableListOf<ImportIssue>()
                val seen = mutableSetOf<String>()
                ZipInputStream(snapshot.inputStream()).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        coroutineContext.ensureActive()
                        if (!seen.add(entry.name)) throw MalformedBackupArchiveException()
                        if (entry.isDirectory) return@forEach
                        if (detectBackupEntryFormat(entry.name) == null) {
                            issues += ImportIssue("", "", ImportIssueKind.FIELD, entry.name)
                            return@forEach
                        }
                        if (!entry.name.endsWith(".json")) return@forEach
                        if (entry.name == MACRION_MANIFEST_FILE_NAME) {
                            val manifest = Json.parseToJsonElement(zip.readBytes().toString(Charsets.UTF_8)).jsonObject
                            if (manifest["containerVersion"]?.jsonPrimitive?.int != 1) issues += ImportIssue("Macrion", "", ImportIssueKind.VERSION, "container ${manifest["containerVersion"]}")
                            if ((manifest["databaseVersion"]?.jsonPrimitive?.int ?: -1) > DATABASE_VERSION) issues += ImportIssue("Macrion", "", ImportIssueKind.VERSION, "schema ${manifest["databaseVersion"]}")
                            (manifest.keys - setOf("format", "containerVersion", "databaseVersion")).forEach {
                                issues += ImportIssue("Macrion", "", ImportIssueKind.FIELD, it)
                            }
                            return@forEach
                        }
                        if (detectBackupEntryFormat(entry.name) != format) return@forEach
                        val source = Json.parseToJsonElement(zip.readBytes().toString(Charsets.UTF_8)).jsonObject
                        val result = BackupImportAnalyzer().analyze(source, format, entry.name.startsWith("dumb-"))
                        issues += result.issues
                        scenarios[entry.name] = result.json?.let { json ->
                            File(directory, "scenario-${scenarios.size}.json").apply { writeText(json.toString()) }
                        }
                    }
                }
                BackupImportPlan(directory, snapshot, format, scenarios, issues)
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { directory.deleteRecursively() }
            throw error
        }
    }
}
