package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.feature.backup.data.base.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupSchemaTests {
    @Test fun overlappingSmartSchemaIsAcceptedOnlyForNativeArchives() {
        val json = Json.parseToJsonElement("""{"version":25}""").jsonObject
        validateBackupSchema(json, BackupArchiveFormat.MACRION_NATIVE, isDumb = false)
        assertThrows(UnsupportedBackupSchemaException::class.java) {
            validateBackupSchema(json, BackupArchiveFormat.KLICKR_COMPATIBLE, isDumb = false)
        }
    }

    @Test fun klickr41SchemasAreRejectedBeforeAnyScenarioIsExtracted() {
        for ((name, version) in listOf("12/12.json" to 24, "12/12.json" to 25, "dumb-12/12.json" to 3)) {
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("11/11.json"))
                zip.write("""{"version":23}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(name))
                zip.write("""{"version":$version}""".toByteArray())
                zip.closeEntry()
            }
            assertThrows(UnsupportedBackupSchemaException::class.java) {
                detectArchiveFormat(ByteArrayInputStream(output.toByteArray()))
            }
        }
    }

    @Test fun legacyKlickrAndExistingPortableMacrionSchemasRemainReadable() {
        for (version in listOf(1, 2, 5)) {
            validateBackupSchema(Json.parseToJsonElement("""{"version":$version}""").jsonObject,
                BackupArchiveFormat.KLICKR_COMPATIBLE, isDumb = true)
        }
        validateBackupSchema(Json.parseToJsonElement("""{"version":23}""").jsonObject,
            BackupArchiveFormat.KLICKR_COMPATIBLE, isDumb = false)
    }

    @Test fun futureSchemasAreRejectedInBothFamilies() {
        for (format in BackupArchiveFormat.entries) for (dumb in listOf(false, true)) {
            assertThrows(UnsupportedBackupSchemaException::class.java) {
                validateBackupSchema(Json.parseToJsonElement("""{"version":999}""").jsonObject, format, dumb)
            }
        }
    }
}
