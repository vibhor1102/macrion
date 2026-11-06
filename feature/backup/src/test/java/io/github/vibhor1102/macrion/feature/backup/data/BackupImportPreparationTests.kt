package io.github.vibhor1102.macrion.feature.backup.data

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Build
import androidx.room.Room
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbDatabase
import io.github.vibhor1102.macrion.core.dumb.domain.IDumbRepository
import io.github.vibhor1102.macrion.feature.backup.domain.BackupRepository
import io.github.vibhor1102.macrion.feature.backup.domain.Backup
import io.mockk.*
import kotlinx.coroutines.flow.toList
import androidx.test.core.app.ApplicationProvider
import io.github.vibhor1102.macrion.feature.backup.data.base.MalformedBackupArchiveException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BackupImportPreparationTests {
    @Test fun futureRecoveryUsesTheReviewedSnapshotAndNeedsExplicitConsent() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = Files.createTempDirectory("backup-recovery-test").toFile()
        val source = directory.resolve("original.zip")
        zip(source, "dumb-42/42.json" to simple(999))
        val plan = BackupImportPreparer(directory, context.contentResolver).prepare(Uri.fromFile(source))
        try {
            assertTrue(plan.requiresReview)
            assertEquals(1, plan.recoverableCount)
            val bytes = plan.source.readBytes()
            source.writeText("provider changed the original file")
            assertArrayEquals(bytes, plan.source.readBytes())
            val appFiles = directory.resolve("app-files").apply { mkdirs() }
            val engine = BackupEngine(appFiles, context.contentResolver)
            var restoredCount = 0
            val progress = BackupProgress(
                onProgressChanged = { _, _ -> },
                onCompleted = { dumb, _, _, _, _ -> restoredCount += dumb.size },
                onError = { fail("Unexpected import error: $it") },
            )
            try {
                engine.loadBackup(Uri.fromFile(plan.source), Point(1080, 1920), progress, prepared = plan)
                fail("Unreviewed future formats must not import")
            } catch (_: IllegalArgumentException) { }
            assertEquals(0, restoredCount)
            assertTrue(appFiles.listFiles().orEmpty().isEmpty())
            engine.loadBackup(Uri.fromFile(plan.source), Point(1080, 1920), progress, prepared = plan, allowPartial = true)
            assertEquals(1, restoredCount)
        } finally { plan.close(); directory.deleteRecursively() }
    }

    @Test fun futureNativeContainersAndSchemasRequireReview() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = Files.createTempDirectory("backup-future-native").toFile()
        try {
            val source = directory.resolve("source.zip")
            zip(source,
                "macrion-manifest.macrion.json" to """{"format":"macrion","containerVersion":2,"databaseVersion":999}""",
                "dumb-42/42.macrion.json" to simple(999).replace("\"version\":999", "\"format\":\"macrion\",\"version\":999"),
            )
            val plan = BackupImportPreparer(directory, context.contentResolver).prepare(Uri.fromFile(source))
            try {
                assertTrue(plan.requiresReview)
                assertEquals(1, plan.recoverableCount)
                assertTrue(plan.issues.count { it.kind == ImportIssueKind.VERSION } >= 3)
            } finally { plan.close() }
        } finally { directory.deleteRecursively() }
    }

    @Test fun malformedAndMixedArchivesDoNotGetARecoveryBypass() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = Files.createTempDirectory("backup-mixed").toFile()
        try {
            val source = directory.resolve("source.zip")
            zip(source, "dumb-42/42.json" to simple(999), "43/43.macrion.json" to """{"format":"macrion","version":34}""")
            try {
                BackupImportPreparer(directory, context.contentResolver).prepare(Uri.fromFile(source))
                fail("Mixed archive must be rejected")
            } catch (_: MalformedBackupArchiveException) { }
            assertEquals(listOf("source.zip"), directory.listFiles().orEmpty().map { it.name })
        } finally { directory.deleteRecursively() }
    }

    @Test fun failedScenarioCopyRollsBackEvenIfRepositoryAlreadyInsertedItsParent() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = Files.createTempDirectory("backup-rollback").toFile()
        val database = Room.inMemoryDatabaseBuilder(context, DumbDatabase::class.java).allowMainThreadQueries().build()
        val dumbRepository = mockk<IDumbRepository>()
        coEvery { dumbRepository.addDumbScenarioCopy(any()) } coAnswers {
            val scenario = firstArg<io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions>()
            database.dumbScenarioDao().addDumbScenario(scenario.scenario)
            null
        }
        try {
            val source = directory.resolve("source.zip")
            zip(source, "dumb-42/42.json" to simple(1))
            val repository = BackupRepository(context, database, dumbRepository, mockk(), mockk())
            val plan = repository.prepareScenarioImport(Uri.fromFile(source))
            try {
                val completed = repository.restorePreparedScenarioBackup(plan, Point(1080, 1920)).toList().filterIsInstance<Backup.Completed>().single()
                assertEquals(0, completed.successCount)
                assertEquals(1, completed.failureCount)
                database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM dumb_scenario_table").use { cursor ->
                    cursor.moveToFirst(); assertEquals(0, cursor.getInt(0))
                }
            } finally { plan.close() }
        } finally { database.close(); directory.deleteRecursively() }
    }

    private fun simple(version: Int) = """{"version":$version,"screenWidth":1080,"screenHeight":1920,"dumbScenario":{"scenario":{"id":42,"name":"Fixture","repeatCount":1,"isRepeatInfinite":false,"maxDurationMin":1,"isDurationInfinite":true,"randomize":false},"dumbActions":[{"id":1,"dumbScenarioId":42,"name":"Pause","type":"PAUSE","pauseDuration":100}],"stats":null}}"""
    private fun zip(file: File, vararg entries: Pair<String, String>) = ZipOutputStream(file.outputStream()).use { zip ->
        entries.forEach { (name, contents) -> zip.putNextEntry(ZipEntry(name)); zip.write(contents.toByteArray()); zip.closeEntry() }
    }
}
