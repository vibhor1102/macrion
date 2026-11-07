package io.github.vibhor1102.macrion.feature.backup.data

import android.os.Build
import io.github.vibhor1102.macrion.core.database.entity.*
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioBackup
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioSerializer
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Opt-in local evidence: private user backups never become repository fixtures. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SuppliedBackupRegressionTests {
    @Test fun suppliedArchivesRetainAllRepresentableActionsAndAssets() {
        val directory = System.getenv("MACRION_BACKUP_REGRESSION_DIR")?.let(::File)
        assumeTrue("Set MACRION_BACKUP_REGRESSION_DIR to inspect local archives", directory?.isDirectory == true)
        val archives = directory!!.listFiles()!!.filter { it.extension == "zip" }
        assertEquals(2, archives.size)
        val before = archives.associateWith { digest(it.readBytes()) }
        val native = archives.single { file -> ZipFile(file).use { it.getEntry("macrion-manifest.macrion.json") != null } }
        val compatible = archives.single { it != native }
        val output = System.getenv("MACRION_BACKUP_REGRESSION_OUTPUT")?.let(::File)
        require(output == null || output.canonicalFile !in archives.map { it.canonicalFile })
        output?.parentFile?.mkdirs()
        val evidence = mutableListOf<String>()
        ZipFile(native).use { zip ->
            val originals = zip.entries().asSequence().filter { it.name.endsWith(".macrion.json") && !it.name.startsWith("macrion-manifest") }
                .map { entry -> Json.decodeFromString<ScenarioBackup>(zip.getInputStream(entry).reader().readText()) }.toList()
            assertEquals(3, originals.size)
            val projections = originals.map { backup ->
                val source = backup.scenario.let { s -> s.copy(events = s.events.map { e ->
                    e.copy(conditions = e.conditions.map { c -> c.copy(path = c.path?.replace(".macrion.png", ".png")) })
                }) }
                val projection = KlickrCompatibilityProjector.projectSmartScenario(source)
                val projected = projection.value!!
                val originalClicks = source.events.flatMap { it.actions }.filter { it.action.type == ActionType.CLICK }
                val projectedActions = projected.events.flatMap { it.actions }
                assertEquals(originalClicks.map { it.action.id }.toSet(), projectedActions.filter { it.action.type == ActionType.CLICK }.map { it.action.id }.toSet())
                assertEquals(source.events.map { it.event.id }, projected.events.map { it.event.id })
                assertEquals(source.events.flatMap { it.conditions }, projected.events.flatMap { it.conditions })
                assertEquals(source.counters, projected.counters)
                // The only valid loss in these fixtures is the original empty targeted toggle.
                val noOps = source.events.flatMap { it.actions }.filter {
                    it.action.type == ActionType.TOGGLE_EVENT && it.action.toggleAll != true && it.eventsToggle.isEmpty()
                }
                assertEquals(source.events.sumOf { it.actions.size } - noOps.size, projectedActions.size)
                assertEquals(noOps.size, projection.losses.sumOf { it.componentCount })
                source.events.forEach { event ->
                    val result = projected.events.single { it.event.id == event.event.id }
                    event.actions.filterNot { it in noOps }.forEach { action ->
                        val retained = result.actions.single { it.action.id == action.action.id }
                        val expectedTarget = action.action.clickOnConditionId.takeUnless {
                            action.action.type == ActionType.CLICK && (event.event.conditionOperator == 2 || action.action.clickPositionType != ClickPositionType.ON_DETECTED_CONDITION)
                        }
                        val expectedToggles = if (action.action.type == ActionType.TOGGLE_EVENT && action.action.toggleAll == true)
                            emptyList() else action.eventsToggle
                        assertEquals(action.copy(
                            action = action.action.copy(priority = retained.action.priority, clickOnConditionId = expectedTarget),
                            eventsToggle = expectedToggles,
                        ), retained)
                    }
                }
                projected.events.forEach { event -> event.actions.filter { it.action.type == ActionType.CLICK }.forEach { complete ->
                    assertNotNull("Klick'r rejected ${event.event.name} / ${complete.action.id}",
                        io.github.vibhor1102.macrion.feature.backup.data.klickr412.LegacySmartClickReader().deserializeActionClick(
                            Json.encodeToJsonElement(complete.action).jsonObject, event.conditions, event.event.conditionOperator,
                        ))
                } }
                val buffer = ByteArrayOutputStream()
                ScenarioSerializer().serialize(backup.copy(format = null, version = 23, scenario = projected), buffer)
                val wire = buffer.toByteArray()
                val strictConsumer = Json.decodeFromString<io.github.vibhor1102.macrion.feature.backup.data.klickr400.ScenarioBackup>(wire.decodeToString())
                assertEquals(projected.events.size, strictConsumer.scenario.events.size)
                assertEquals(projectedActions.size, strictConsumer.scenario.events.sumOf { it.actions.size })
                val checked = BackupImportAnalyzer().analyze(Json.parseToJsonElement(wire.decodeToString()).jsonObject,
                    BackupArchiveFormat.KLICKR_COMPATIBLE, false)
                assertTrue(checked.issues.toString(), checked.issues.isEmpty())
                val roundTrip = Json.decodeFromJsonElement<ScenarioBackup>(checked.json!!).scenario
                assertEquals(projectedActions.map { it.action.id }.toSet(), roundTrip.events.flatMap { it.actions }.map { it.action.id }.toSet())
                assertEquals(projected.events.flatMap { it.conditions }.map { it.id }.toSet(), roundTrip.events.flatMap { it.conditions }.map { it.id }.toSet())
                evidence += "${source.scenario.name}: ${source.events.size} events; ${originalClicks.size} clicks retained; ${projectedActions.size} actions; ${noOps.size} empty toggles omitted"
                Triple(projected, wire, backup)
            }
            if (output != null) ZipOutputStream(output.outputStream()).use { target ->
                projections.forEach { (scenario, wire, _) ->
                    val folder = scenario.scenario.id.toString()
                    target.putNextEntry(ZipEntry("$folder/$folder.json")); target.write(wire); target.closeEntry()
                    val paths = scenario.events.flatMap { it.conditions }.mapNotNull { it.path }.toSet()
                    paths.forEach { path ->
                        val originalEntry = zip.getEntry("$folder/${path.replace(".png", ".macrion.png")}")!!
                        val bytes = zip.getInputStream(originalEntry).readBytes()
                        target.putNextEntry(ZipEntry("$folder/$path")); target.write(bytes); target.closeEntry()
                    }
                }
            }
            ZipFile(compatible).use { old ->
                val oldActions = old.entries().asSequence().filter { it.name.endsWith(".json") }.sumOf { entry ->
                    Json.decodeFromString<ScenarioBackup>(old.getInputStream(entry).reader().readText()).scenario.events.sumOf { it.actions.size }
                }
                val newActions = projections.sumOf { it.first.events.sumOf { e -> e.actions.size } }
                assertEquals(30, newActions - oldActions)
            }
        }
        archives.forEach { assertEquals("Original archive changed", before[it], digest(it.readBytes())) }
        if (output != null) {
            ZipFile(output).use { corrected ->
                assertNull(corrected.testEntries())
                assertEquals(64, corrected.entries().asSequence().count { it.name.endsWith(".png") })
            }
            File(output.parentFile, "verification.txt").writeText(evidence.joinToString("\n") + "\nOriginal archive SHA-256 values unchanged.\n")
        }
    }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun ZipFile.testEntries(): String? {
        entries().asSequence().forEach { entry ->
            val crc = java.util.zip.CRC32().apply { update(getInputStream(entry).readBytes()) }.value
            if (crc != entry.crc) return entry.name
        }
        return null
    }
}
