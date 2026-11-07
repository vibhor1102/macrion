package io.github.vibhor1102.macrion.feature.backup.data

import android.os.Build
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.*
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioBackup
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class KlickrReferenceRegressionTests {
    @Test fun orClicksPreserveOffsetsWithoutDemandingASpecificConditionId() {
        for (target in listOf(null, 100L, 200L, 999L)) {
            val click = click(1, target).copy(clickOffsetX = -199, clickOffsetY = -20)
            val source = scenario(event(10, 2, listOf(CompleteActionEntity(click))), event(20, 1, listOf(pause(2, 20))))
            val projection = KlickrCompatibilityProjector.projectSmartScenario(source)
            assertTrue(projection.losses.toString(), projection.losses.isEmpty())
            val retained = projection.value!!.events.first().actions.single().action
            assertEquals(1L, retained.id)
            assertNull(retained.clickOnConditionId)
            val consumer = io.github.vibhor1102.macrion.feature.backup.data.klickr412.LegacySmartClickReader()
                .deserializeActionClick(Json.encodeToJsonElement(retained).jsonObject, source.events.first().conditions, 2)
            assertNotNull(consumer)
            assertNull(consumer!!.clickOnConditionId)
            assertEquals(-199, consumer.clickOffsetX)
            assertEquals(-199, retained.clickOffsetX)
            assertEquals(-20, retained.clickOffsetY)
            assertEquals(target, source.events.first().actions.single().action.clickOnConditionId)
            assertTrue(KlickrCompatibilityProjector.validateReferentialIntegrity(projection.value!!).isEmpty())
        }
    }

    @Test fun andClicksRequireAPositiveScreenConditionInTheirOwnEvent() {
        val owner = event(10, 1, listOf(100L, 101L, 200L, 999L).mapIndexed { index, target ->
            CompleteActionEntity(click(index + 1L, target))
        } + pause(9, 10)).let { it.copy(conditions = it.conditions + image(101, 10).copy(shouldBeDetected = false)) }
        val source = scenario(owner, event(20, 1, listOf(pause(10, 20))))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(source)
        assertEquals(listOf(1L, 9L), projection.value!!.events.first().actions.map { it.action.id })
        assertEquals(3, projection.losses.sumOf { it.componentCount })
        // The validator must also reject sibling-event references before projection repairs them.
        assertTrue(KlickrCompatibilityProjector.validateReferentialIntegrity(source).any { "3" in it })
    }

    @Test fun omittedDetectionPolarityUsesTheExistingPositiveDefault() {
        val owner = event(10, 1, listOf(CompleteActionEntity(click(1, 100))))
            .let { e -> e.copy(conditions = e.conditions.map { it.copy(shouldBeDetected = null) }) }
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario(owner))
        assertTrue(projection.losses.toString(), projection.losses.isEmpty())
        val result = analyze(scenario(owner))
        assertTrue(result.issues.toString(), result.issues.isEmpty())
    }

    @Test fun staleOrTargetsDoNotTriggerPartialImportOrSurviveAsDatabaseReferences() {
        val source = scenario(event(10, 2, listOf(CompleteActionEntity(click(1, 999)))))
        val result = analyze(source)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        assertNull(restored(result).events.single().actions.single().action.clickOnConditionId)
        assertEquals(999L, source.events.single().actions.single().action.clickOnConditionId)
    }

    @Test fun ignoredTargetsAreClearedInFixedClicksAndCombinedOrChildren() {
        val fixed = click(1, 999).copy(clickPositionType = ClickPositionType.USER_SELECTED, x = 10, y = 20)
        val parent = CompleteActionEntity(
            ActionEntity(2, 10, name = "Combined", type = ActionType.SPLIT_ACTION),
            splitItems = listOf(3L, 4L).map { id -> SplitActionItemEntity(
                id = id, actionId = 2, type = ActionType.CLICK, duration = 80,
                clickPositionType = ClickPositionType.ON_DETECTED_CONDITION, clickOnConditionId = 999,
            ) },
        )
        val result = analyze(scenario(event(10, 2, listOf(CompleteActionEntity(fixed), parent))))
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val actions = restored(result).events.single().actions
        assertNull(actions.first().action.clickOnConditionId)
        assertTrue(actions.last().splitItems.all { it.clickOnConditionId == null })
    }

    @Test fun importRejectsAndTargetsOutsideTheEventOrNegativeConditions() {
        val owner = event(10, 1, listOf(CompleteActionEntity(click(1, 200)), CompleteActionEntity(click(2, 101)), pause(3, 10)))
            .let { it.copy(conditions = it.conditions + image(101, 10).copy(shouldBeDetected = false)) }
        val result = analyze(scenario(owner, event(20, 1, listOf(pause(4, 20)))))
        assertEquals(listOf(3L), restored(result).events.first().actions.map { it.action.id })
        assertEquals(2, result.issues.count { it.kind == ImportIssueKind.REFERENCE })
    }

    @Test fun irrelevantDynamicFieldsDoNotExcludeSupportedActionsOrDemandUnusedCounters() {
        val unused = PositionCountersEntity(x = "Unused")
        val source = scenario(event(10, 1, listOf(CompleteActionEntity(ActionEntity(
            1, 10, name = "Wait", type = ActionType.PAUSE, pauseDuration = 100,
            positionCounters = unused, fromCounters = unused, toCounters = unused,
            counterOperationValueType = CounterOperationValueType.CONDITION, counterOperationConditionId = 999,
        )), CompleteActionEntity(click(2, 100).copy(fromCounters = unused, toCounters = unused)))))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(source)
        assertTrue(projection.losses.toString(), projection.losses.isEmpty())
        assertEquals(2, projection.value!!.events.single().actions.size)
        assertTrue(projection.value!!.events.single().actions.all { it.action.fromCounters == null && it.action.positionCounters == null })
        val result = analyze(source)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        assertEquals(2, restored(result).events.single().actions.size)
    }

    @Test fun emptyCounterSelectionsRemainLiteralButActiveDynamicAxesAreReported() {
        val fixed = click(1, 100).copy(positionCounters = PositionCountersEntity())
        val dynamic = click(2, 100).copy(positionCounters = PositionCountersEntity(x = "X"))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario(event(10, 1,
            listOf(CompleteActionEntity(fixed), CompleteActionEntity(dynamic)))))
        assertEquals(listOf(1L), projection.value!!.events.single().actions.map { it.action.id })
        assertEquals(1, projection.losses.sumOf { it.componentCount })
    }

    @Test fun toggleAllDoesNotMisclassifyIgnoredPerEventTargetsAsLosses() {
        val toggle = CompleteActionEntity(ActionEntity(1, 10, name = "Disable all", type = ActionType.TOGGLE_EVENT,
            toggleAll = true, toggleAllType = EventToggleType.DISABLE),
            eventsToggle = listOf(EventToggleEntity(5, 1, EventToggleType.ENABLE, 999)))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario(event(10, 1, listOf(toggle))))
        assertTrue(projection.losses.toString(), projection.losses.isEmpty())
        assertTrue(projection.value!!.events.single().actions.single().eventsToggle.isEmpty())
        assertEquals(EventToggleType.DISABLE, projection.value!!.events.single().actions.single().action.toggleAllType)
        val imported = analyze(scenario(event(10, 1, listOf(toggle))))
        assertTrue(imported.issues.toString(), imported.issues.isEmpty())
        assertTrue(restored(imported).events.single().actions.single().eventsToggle.isEmpty())
    }

    @Test fun lossNamesDoNotIncludeStraightSwipesThatWereOnlyNormalized() {
        val swipe = CompleteActionEntity(ActionEntity(1, 10, name = "Retained swipe", type = ActionType.SWIPE,
            fromX = 10, fromY = 20, toX = 30, toY = 40, swipeDuration = 200,
            swipePath = SwipePath(listOf(SwipeNode(SwipePoint(10f, 20f)), SwipeNode(SwipePoint(30f, 40f))))))
        val sound = CompleteActionEntity(ActionEntity(2, 10, name = "Omitted sound", type = ActionType.PLAY_SOUND))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario(event(10, 1, listOf(swipe, sound))))
        assertEquals(1, projection.losses.single().componentCount)
        assertEquals(listOf("Event 10 → Omitted sound"), projection.losses.single().componentNames)
        assertEquals(1L, projection.value!!.events.single().actions.single().action.id)
    }

    @Test fun unsupportedConditionRatesOmitTheWholeEventRatherThanChangingItsTriggerTiming() {
        val limited = event(10, 1, listOf(pause(1, 10))).let { e -> e.copy(conditions = e.conditions.map { it.copy(computeRate = 0.5) }) }
        val other = event(20, 1, listOf(pause(2, 20)))
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario(limited, other))
        assertEquals(listOf(20L), projection.value!!.events.map { it.event.id })
        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT && it.componentNames == listOf("Event 10 → Image 100") })
        val result = analyze(scenario(limited, other))
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        assertEquals(0.5, restored(result).events.first().conditions.single().computeRate, 0.0)
    }

    private fun click(id: Long, target: Long?) = ActionEntity(id, 10, name = "Click $id", type = ActionType.CLICK,
        clickPositionType = ClickPositionType.ON_DETECTED_CONDITION, clickOnConditionId = target, pressDuration = 80)
    private fun pause(id: Long, owner: Long) = CompleteActionEntity(ActionEntity(id, owner, name = "Wait", type = ActionType.PAUSE, pauseDuration = 100))
    private fun image(id: Long, owner: Long) = ConditionEntity(id, owner, "Image $id", ConditionType.ON_IMAGE_DETECTED, 0,
        shouldBeDetected = true, path = "Condition_$id.png", areaLeft = 0, areaTop = 0, areaRight = 20, areaBottom = 20,
        threshold = 4, detectionType = 1)
    private fun event(id: Long, operator: Int, actions: List<CompleteActionEntity>) = CompleteEventEntity(
        EventEntity(id, 42, "Event $id", operator, id.toInt(), type = EventType.IMAGE_EVENT,
            keepDetecting = false, detectionCooldownMs = 0), actions, listOf(image(id * 10, id)),
    )
    private fun scenario(vararg events: CompleteEventEntity) = CompleteScenario(ScenarioEntity(42, "Regression", 1200), events.toList(), emptyList())
    private fun analyze(scenario: CompleteScenario) = BackupImportAnalyzer().analyze(
        Json.encodeToJsonElement(ScenarioBackup("macrion", DATABASE_VERSION, 1080, 2400, scenario)).jsonObject,
        BackupArchiveFormat.MACRION_NATIVE, false,
    )
    private fun restored(result: NormalizedScenario) = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario
}
