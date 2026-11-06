/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.backup.data.base

import io.github.vibhor1102.macrion.core.base.extensions.getInt
import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.dumb.data.database.DUMB_DATABASE_VERSION
import kotlinx.serialization.json.JsonObject

/** A schema number belongs to its archive family, not to whichever Room database is newer. */
internal fun validateBackupSchema(json: JsonObject, format: BackupArchiveFormat, isDumb: Boolean) {
    val version = json.getInt("version") ?: throw MalformedBackupArchiveException()
    val supported = when {
        format == BackupArchiveFormat.MACRION_NATIVE ->
            if (isDumb) version in 1..DUMB_DATABASE_VERSION else version in 8..DATABASE_VERSION
        isDumb -> version in 1..2 || version == 5 // Existing flat Macrion compatible exports.
        else -> version in 8..23
    }
    if (!supported) throw UnsupportedBackupSchemaException()
}

internal class UnsupportedBackupSchemaException : IllegalArgumentException()
