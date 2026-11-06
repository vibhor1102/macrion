/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import kotlinx.serialization.json.JsonObject
import java.io.File

enum class ImportIssueKind { VERSION, FIELD, ACTION, EVENT, SCENARIO, REFERENCE }
data class ImportIssue(val scenario: String, val item: String, val kind: ImportIssueKind, val detail: String)
internal data class NormalizedScenario(val json: JsonObject?, val issues: List<ImportIssue>)

/** Owns a private snapshot: the bytes imported must be the bytes the user reviewed. */
internal data class BackupImportPlan(
    val directory: File,
    val source: File,
    val format: BackupArchiveFormat,
    val scenarios: Map<String, File?>,
    val issues: List<ImportIssue>,
) {
    val recoverableCount: Int get() = scenarios.values.count { it != null }
    val excludedCount: Int get() = scenarios.values.count { it == null }
    val requiresReview: Boolean get() = issues.isNotEmpty()
    fun close() { directory.deleteRecursively() }
}
