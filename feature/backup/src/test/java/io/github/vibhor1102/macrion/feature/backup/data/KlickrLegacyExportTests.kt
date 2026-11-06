package io.github.vibhor1102.macrion.feature.backup.data

import android.os.Build
import io.github.vibhor1102.macrion.core.dumb.data.database.*
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbScenarioBackup
import io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbScenarioSerializer
import io.github.vibhor1102.macrion.feature.backup.data.klickr412.DumbScenarioSerializer as Klickr412Reader
import io.github.vibhor1102.macrion.feature.backup.data.klickr412.DumbActionType as Klickr412ActionType
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class KlickrLegacyExportTests {
    @Test fun clickSwipeAndDelaysSurviveTheKlickr412Consumer() {
        val source = scenario(listOf(
            DumbActionEntity(-1, 42, name = "Tap", type = DumbActionType.CLICK, x = 30, y = 40,
                pressDuration = 80, repeatCount = 3, isRepeatInfinite = false, repeatDelay = 120,
                waitBeforeMs = 250, waitAfterMs = 350),
            DumbActionEntity(2, 42, name = "Swipe", type = DumbActionType.SWIPE,
                fromX = 10, fromY = 20, toX = 100, toY = 200, swipeDuration = 500,
                repeatCount = 2, isRepeatInfinite = false, repeatDelay = 40),
        ))
        val projected = KlickrCompatibilityProjector.projectDumbScenario(source).value!!
        val output = ByteArrayOutputStream()
        DumbScenarioSerializer().serialize(DumbScenarioBackup(version = 1, screenWidth = 1080,
            screenHeight = 1920, dumbScenario = projected), output)

        val consumer = Klickr412Reader().deserialize(output.toByteArray().inputStream())!!
        val actions = consumer.dumbScenario.dumbActions
        assertEquals(listOf(Klickr412ActionType.PAUSE, Klickr412ActionType.CLICK,
            Klickr412ActionType.PAUSE, Klickr412ActionType.GESTURE), actions.map { it.action.type })
        assertEquals(250L, actions[0].action.pauseDuration)
        assertEquals(350L, actions[2].action.pauseDuration)
        assertEquals(30, actions[1].action.x)
        assertEquals(40, actions[1].action.y)
        assertEquals(80L, actions[1].action.pressDuration)
        assertEquals(3, actions[1].action.repeatCount)
        assertEquals(120L, actions[1].action.repeatDelay)
        val swipe = actions[3]
        assertEquals(2, swipe.action.repeatCount)
        assertEquals(40L, swipe.action.repeatDelay)
        val motions = swipe.gestureFingers.single().motions
        assertEquals(10, motions.first().x)
        assertEquals(20, motions.first().y)
        assertEquals(100, motions[1].x)
        assertEquals(200, motions[1].y)
        assertEquals(500L, motions[1].durationMs)
        assertEquals(4, actions.map { it.action.id }.distinct().size)
        assertEquals(listOf(0, 1, 2, 3), actions.map { it.action.priority })
        assertEquals(250L, source.dumbActions.first().waitBeforeMs)

        val wire = Json.parseToJsonElement(output.toString(Charsets.UTF_8)).jsonObject
        assertEquals(1, wire.getValue("version").jsonPrimitive.int)
        val click = wire.getValue("dumbScenario").jsonObject.getValue("dumbActions").jsonArray[1].jsonObject
        assertEquals(42L, click.getValue("dumb_scenario_id").jsonPrimitive.long)
        assertFalse(click.containsKey("dumbScenarioId"))
        assertFalse(click.containsKey("waitBeforeMs"))
        val restored = DumbScenarioSerializer().deserialize(output.toByteArray().inputStream(), BackupArchiveFormat.KLICKR_COMPATIBLE)!!
        assertEquals(projected.dumbActions, restored.dumbScenario.dumbActions)
    }

    @Test fun legacyCamelCaseScenarioReferencesAreAccepted() {
        val json = """{"version":2,"dumbScenario":{"scenario":{"id":42,"name":"Legacy"},"dumbActions":[{"id":1,"dumbScenarioId":42,"name":"Pause","type":"PAUSE","pauseDuration":90}]}}"""
        val backup = DumbScenarioSerializer().deserialize(json.byteInputStream(), BackupArchiveFormat.KLICKR_COMPATIBLE)!!
        assertEquals(42L, backup.dumbScenario.dumbActions.single().dumbScenarioId)
        assertEquals(90L, backup.dumbScenario.dumbActions.single().pauseDuration)
    }

    private fun scenario(actions: List<DumbActionEntity>) = DumbScenarioWithActions(
        scenario = DumbScenarioEntity(42, "Export", 1, false, 1, true, false),
        dumbActions = actions, stats = null,
    )
}
