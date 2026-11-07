package io.github.vibhor1102.macrion.feature.backup.data

import android.os.Build
import io.github.vibhor1102.macrion.core.database.DATABASE_VERSION
import io.github.vibhor1102.macrion.core.database.entity.*
import io.github.vibhor1102.macrion.core.dumb.data.database.*
import io.github.vibhor1102.macrion.feature.backup.data.base.*
import io.github.vibhor1102.macrion.feature.backup.data.smart.ScenarioBackup
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BackupImportAnalyzerTests {
    @Test fun klickr41SmartClickAndSwipeKeepContactTimingAndOffsets() {
        val click = gesture(1, "CLICK", listOf(finger(100, 1, 0, 0, moving = false)))
        val swipe = gesture(2, "GESTURE", listOf(finger(200, 2, 0, 15, moving = true)))
        val result = analyze(smart(listOf(event(10, listOf(click, swipe)))))
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val scenario = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario
        val actions = scenario.events.single().actions.map { it.action }
        assertEquals(listOf(ActionType.CLICK, ActionType.SWIPE), actions.map { it.type })
        assertEquals(80L, actions[0].pressDuration)
        assertEquals(10, actions[0].x)
        assertEquals(20, actions[0].y)
        assertEquals(15L, actions[1].waitBeforeMs)
        assertEquals(400L, actions[1].swipeDuration)
        assertEquals(100, actions[1].toX)
        assertEquals(200, actions[1].toY)
    }

    @Test fun klickr41SmartMultiTouchBecomesOneSynchronizedGroup() {
        val group = gesture(1, "GESTURE", listOf(finger(100, 1, 0, 0, true), finger(200, 1, 1, 50, false)))
        val result = analyze(smart(listOf(event(10, listOf(group)))))
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val action = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.single()
        assertEquals(ActionType.SPLIT_ACTION, action.action.type)
        assertEquals(listOf(ActionType.SWIPE, ActionType.CLICK), action.splitItems.map { it.type })
        assertEquals(listOf(0L, 50L), action.splitItems.map { it.startOffset })
        assertEquals(listOf(400L, 80L), action.splitItems.map { it.duration })
    }

    @Test fun klickr41SimpleGroupKeepsParentRepeatSettings() {
        val action = gesture(1, "GESTURE", listOf(finger(100, 1, 0, 0, true, dumb = true), finger(200, 1, 1, 50, false, dumb = true)), dumb = true)
        val result = BackupImportAnalyzer().analyze(simple(action), BackupArchiveFormat.KLICKR_COMPATIBLE, true)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val restored = Json.decodeFromJsonElement<io.github.vibhor1102.macrion.feature.backup.data.dumb.DumbScenarioBackup>(result.json!!).dumbScenario
        assertEquals(DumbActionType.SPLIT_ACTION, restored.dumbActions.single().type)
        assertEquals(3, restored.dumbActions.single().repeatCount)
        assertEquals(90L, restored.dumbActions.single().repeatDelay)
        assertEquals(listOf(0L, 50L), restored.dumbActionsWithSubActions.single().splitItems.map { it.startOffset })
    }

    @Test fun futureVersionsRequireReviewEvenWhenTheirShapeIsFullyRecognizable() {
        for (format in BackupArchiveFormat.entries) {
            val source = JsonObject(smart(listOf(event(10, listOf(pause(1))))) + mapOf(
                "version" to JsonPrimitive(999), "format" to if (format == BackupArchiveFormat.MACRION_NATIVE) JsonPrimitive("macrion") else JsonNull,
            ))
            val result = BackupImportAnalyzer().analyze(source, format, false)
            assertNotNull(result.json)
            assertTrue(result.issues.any { it.kind == ImportIssueKind.VERSION })
        }
    }

    @Test fun unknownActionsAndCounterOperandsAreOmittedWithoutNumericDefaults() {
        val unknown = JsonObject(pause(1) + ("action" to JsonObject(pause(1).obj("action") + ("type" to JsonPrimitive("FUTURE_ACTION")))))
        val counter = buildJsonObject {
            put("action", buildJsonObject {
                put("id", 2); put("eventId", 10); put("name", "Position to counter"); put("type", "CHANGE_COUNTER")
                put("counterName", "X"); put("counterOperation", "SET"); put("counterOperationValueType", "CONDITION")
            }); put("gestureFingers", JsonArray(emptyList()))
        }
        val result = analyze(smart(listOf(event(10, listOf(unknown, counter, pause(3))))))
        val actions = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions
        assertEquals(listOf(3L), actions.map { it.action.id })
        assertTrue(result.issues.count { it.kind == ImportIssueKind.ACTION } >= 2)
    }

    @Test fun unknownConditionsRemoveTheEntireEventAndDependentToggleActions() {
        val first = event(10, listOf(pause(1)))
        val unknownCondition = JsonObject(first.array("conditions").single().jsonObject + ("futureGuard" to JsonPrimitive(true)))
        val guarded = JsonObject(first + ("conditions" to JsonArray(listOf(first.array("conditions").single(), unknownCondition))))
        val toggle = Json.encodeToJsonElement(CompleteActionEntity(
            ActionEntity(2, 20, name = "Toggle removed event", type = ActionType.TOGGLE_EVENT, toggleAll = false),
            eventsToggle = listOf(EventToggleEntity(id = 100, actionId = 2, toggleEventId = 10, type = EventToggleType.ENABLE)),
        )).jsonObject
        val result = analyze(smart(listOf(guarded, event(20, listOf(toggle, pause(3, 20))))))
        val events = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events
        assertEquals(listOf(20L), events.map { it.event.id })
        assertEquals(listOf(3L), events.single().actions.map { it.action.id })
        assertTrue(result.issues.any { it.kind == ImportIssueKind.EVENT })
        assertTrue(result.issues.any { it.kind == ImportIssueKind.REFERENCE })
    }

    @Test fun holdsAndDynamicCoordinatesNeverBecomeOrdinaryTouches() {
        val holdFinger = finger(100, 1, 0, 0, false)
        val holdMotions = holdFinger.array("motions").toMutableList()
        holdMotions[2] = JsonObject(holdMotions[2].jsonObject + ("releaseHoldType" to JsonPrimitive("UNTIL_UNFULFILLED")))
        val held = gesture(1, "CLICK", listOf(JsonObject(holdFinger + ("motions" to JsonArray(holdMotions)))))
        val dynamicFinger = finger(200, 2, 0, 0, true)
        val dynamicMotions = dynamicFinger.array("motions").toMutableList()
        dynamicMotions[0] = JsonObject(dynamicMotions[0].jsonObject + ("xCoordinateType" to JsonPrimitive("COUNTER")))
        val dynamic = gesture(2, "GESTURE", listOf(JsonObject(dynamicFinger + ("motions" to JsonArray(dynamicMotions)))))
        val result = analyze(smart(listOf(event(10, listOf(held, dynamic, pause(3))))))
        assertEquals(listOf(3L), Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.map { it.action.id })
        assertEquals(2, result.issues.count { it.kind == ImportIssueKind.ACTION })
    }

    @Test fun executionOrderUsesPrioritiesRatherThanSerializedArrayOrder() {
        val first = JsonObject(pause(1) + ("action" to JsonObject(pause(1).obj("action") + ("priority" to JsonPrimitive(1)))))
        val result = analyze(smart(listOf(event(10, listOf(first, pause(2))))))
        assertEquals(listOf(2L, 1L), Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.map { it.action.id })
    }

    @Test fun legacyReadersCannotSilentlyWeakenConditionsOrLoseCounters() {
        val first = event(10, listOf(pause(1)))
        val broken = JsonObject(first.array("conditions").single().jsonObject - "timerValueMs" + ("id" to JsonPrimitive(101)))
        val unsafe = JsonObject(first + ("conditions" to JsonArray(first.array("conditions") + broken)))
        val legacy = JsonObject(smart(listOf(unsafe, event(20, listOf(pause(2, 20))))) + ("version" to JsonPrimitive(23)))
        val result = analyze(legacy)
        assertNotNull(result.issues.toString(), result.json)
        assertEquals(listOf(20L), Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.map { it.event.id })
        assertTrue(result.issues.any { it.kind == ImportIssueKind.EVENT })
        val badCounter = buildJsonObject { put("name", "X"); put("scenarioId", 42) }
        val counterSource = JsonObject(legacy + ("scenario" to JsonObject(legacy.obj("scenario") + ("counters" to JsonArray(listOf(badCounter))))))
        assertNull(analyze(counterSource).json)
    }

    @Test fun knownNativeCurvesAndTenStrokeGroupsArePreserved() {
        val path = io.github.vibhor1102.macrion.core.base.gesture.SwipePath(listOf(
            io.github.vibhor1102.macrion.core.base.gesture.SwipeNode(io.github.vibhor1102.macrion.core.base.gesture.SwipePoint(10f, 20f), controlOut = io.github.vibhor1102.macrion.core.base.gesture.SwipePoint(20f, 20f)),
            io.github.vibhor1102.macrion.core.base.gesture.SwipeNode(io.github.vibhor1102.macrion.core.base.gesture.SwipePoint(100f, 200f), controlIn = io.github.vibhor1102.macrion.core.base.gesture.SwipePoint(80f, 200f)),
        ))
        val group = CompleteActionEntity(ActionEntity(1, 10, name = "Native group", type = ActionType.SPLIT_ACTION),
            splitItems = (1..10).map { index -> SplitActionItemEntity(id = index.toLong(), actionId = 1, name = "Stroke $index", priority = index,
                type = ActionType.SWIPE, fromX = 10, fromY = 20, toX = 100, toY = 200, duration = 400, swipePath = path) })
        val native = JsonObject(smart(listOf(event(10, listOf(Json.encodeToJsonElement(group).jsonObject)))) + mapOf(
            "format" to JsonPrimitive("macrion"), "version" to JsonPrimitive(DATABASE_VERSION)))
        val result = BackupImportAnalyzer().analyze(native, BackupArchiveFormat.MACRION_NATIVE, false)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val restored = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.single()
        assertEquals(group.splitItems, restored.splitItems)
    }

    @Test fun historicalNotificationCountersAreMigratedBeforeNativeInspection() {
        val notification = Json.encodeToJsonElement(CompleteActionEntity(ActionEntity(
            1, 10, name = "Old notification", type = ActionType.NOTIFICATION, notificationImportance = 3,
        ))).jsonObject
        val legacyAction = JsonObject(notification + ("action" to JsonObject(notification.obj("action") + mapOf(
            "notificationMessageType" to JsonPrimitive("COUNTER_VALUE"), "notificationMessageCounterName" to JsonPrimitive("Score")))))
        val source = JsonObject(smart(listOf(event(10, listOf(legacyAction)))) + ("version" to JsonPrimitive(19)))
        val result = analyze(source)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val scenario = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario
        assertEquals("Score", scenario.counters.single().name)
        assertTrue(scenario.events.single().actions.single().action.notificationMessageText!!.contains("{Score}"))
    }

    @Test fun historicalEndConditionsKeepTheirGeneratedStopEvent() {
        val condition = ConditionEntity(100, 10, "Image", ConditionType.ON_IMAGE_DETECTED, 0,
            shouldBeDetected = true, path = "Condition_100.png", areaLeft = 0, areaTop = 0, areaRight = 20, areaBottom = 20,
            threshold = 4, detectionType = 1, detectionAreaLeft = 0, detectionAreaTop = 0, detectionAreaRight = 1080, detectionAreaBottom = 1920)
        val original = event(10, listOf(pause(1)))
        val imageEvent = JsonObject(original + mapOf(
            "event" to JsonObject(original.obj("event") + ("type" to JsonPrimitive("IMAGE_EVENT"))),
            "conditions" to JsonArray(listOf(Json.encodeToJsonElement(condition)))))
        val source = JsonObject(smart(listOf(imageEvent)) + ("version" to JsonPrimitive(12)))
        val withEnd = JsonObject(source + ("scenario" to JsonObject(source.obj("scenario") + ("endConditions" to JsonArray(listOf(
            buildJsonObject { put("id", 1); put("scenarioId", 42); put("eventId", 10); put("executions", 3) }
        ))))))
        val result = analyze(withEnd)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val events = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events
        assertEquals(2, events.size)
        assertTrue(events.any { event -> event.actions.any { it.action.toggleAll == true && it.action.toggleAllType == EventToggleType.DISABLE } })
        assertTrue(events.single { it.event.id == 10L }.actions.any { it.action.type == ActionType.CHANGE_COUNTER })
    }

    @Test fun conditionTargetedClicksRetainAndOrSelectionAndLiteralOffsets() {
        val image = ConditionEntity(100, 10, "Image", ConditionType.ON_IMAGE_DETECTED, 0,
            shouldBeDetected = true, path = "Condition_100.png", areaLeft = 0, areaTop = 0, areaRight = 20, areaBottom = 20,
            threshold = 4, detectionType = 1, detectionAreaLeft = 0, detectionAreaTop = 0, detectionAreaRight = 1080, detectionAreaBottom = 1920)
        for (operator in 1..2) {
            val base = finger(100, 1, 0, 0, false)
            val motions = base.array("motions").toMutableList()
            motions[0] = buildJsonObject {
                motions[0].jsonObject.filterKeys { it !in setOf("xLiteral", "yLiteral") }.forEach { (key, value) -> put(key, value) }
                put("xCoordinateType", "CONDITION"); put("yCoordinateType", "CONDITION")
                if (operator == 1) put("conditionId", 100)
                put("xOffsetCoordinateType", "LITERAL"); put("yOffsetCoordinateType", "LITERAL")
                put("xOffsetLiteral", 5); put("yOffsetLiteral", -3)
            }
            val click = gesture(1, "CLICK", listOf(JsonObject(base + ("motions" to JsonArray(motions)))))
            val original = event(10, listOf(click))
            val onImage = JsonObject(original + mapOf(
                "event" to JsonObject(original.obj("event") + mapOf("type" to JsonPrimitive("IMAGE_EVENT"), "conditionOperator" to JsonPrimitive(operator))),
                "conditions" to JsonArray(listOf(Json.encodeToJsonElement(image)))))
            val result = analyze(smart(listOf(onImage)))
            assertTrue(result.issues.toString(), result.issues.isEmpty())
            val action = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.single().action
            assertEquals(ClickPositionType.ON_DETECTED_CONDITION, action.clickPositionType)
            assertEquals(if (operator == 1) 100L else null, action.clickOnConditionId)
            assertEquals(5, action.clickOffsetX); assertEquals(-3, action.clickOffsetY)
            // A timer cannot supply a detected screen position. Keep only the other, independent pause.
            val trigger = analyze(smart(listOf(event(10, listOf(click, pause(2))))))
            assertEquals(listOf(2L), Json.decodeFromJsonElement<ScenarioBackup>(trigger.json!!).scenario.events.single().actions.map { it.action.id })
            assertTrue(trigger.issues.any { it.kind == ImportIssueKind.REFERENCE })
        }
    }

    @Test fun unknownLegacyValueShapesNeverBecomeZeroOrFalseDefaults() {
        val action = Json.encodeToJsonElement(CompleteActionEntity(ActionEntity(
            1, 10, name = "Change counter", type = ActionType.CHANGE_COUNTER, counterName = "Score",
            counterOperation = ChangeCounterOperationType.ADD, counterOperationValueType = CounterOperationValueType.NUMBER,
            counterOperationValue = 7.0,
        ))).jsonObject
        val unknown = JsonObject(action + ("action" to JsonObject(action.obj("action") + ("counterOperationValue" to buildJsonObject { put("futureExpression", "X + Y") }))))
        val source = JsonObject(smart(listOf(event(10, listOf(unknown, pause(2))))) + ("version" to JsonPrimitive(23)))
        val result = analyze(source)
        assertNotNull(result.issues.toString(), result.json)
        assertEquals(listOf(2L), Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.map { it.action.id })
        assertTrue(result.issues.any { it.kind == ImportIssueKind.ACTION && it.detail.contains("counterOperationValue") })
    }

    @Test fun klickrCounterCoordinatesAreImportedOnlyWithExistingCounters() {
        val base = finger(100, 1, 0, 0, true)
        val motions = base.array("motions").toMutableList()
        motions[0] = JsonObject(motions[0].jsonObject - "xLiteral" + mapOf(
            "xCoordinateType" to JsonPrimitive("COUNTER"), "xCounter" to JsonPrimitive("Saved X"),
        ))
        val swipe = gesture(1, "GESTURE", listOf(JsonObject(base + ("motions" to JsonArray(motions)))))
        val source = smart(listOf(event(10, listOf(swipe, pause(2)))))
        val withCounter = JsonObject(source + ("scenario" to JsonObject(source.obj("scenario") +
            ("counters" to JsonArray(listOf(Json.encodeToJsonElement(CountersEntity("Saved X", 42, 50.0))))))))
        val result = analyze(withCounter)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val action = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario.events.single().actions.first().action
        assertEquals(ActionType.SWIPE, action.type)
        assertEquals("Saved X", action.fromCounters?.x)
        assertEquals(20, action.fromY)
        assertEquals(100, action.toX)
        val missing = analyze(source)
        assertEquals(listOf(2L), Json.decodeFromJsonElement<ScenarioBackup>(missing.json!!).scenario.events.single().actions.map { it.action.id })
        assertTrue(missing.issues.any { it.kind == ImportIssueKind.REFERENCE })
    }

    @Test fun conditionCoordinateOperandsAreRecognizedAndLegacyExportReportsTheirLoss() {
        val image = ConditionEntity(100, 10, "Image", ConditionType.ON_IMAGE_DETECTED, 0,
            shouldBeDetected = true, path = "Condition_100.png", areaLeft = 0, areaTop = 0, areaRight = 20, areaBottom = 20,
            threshold = 4, detectionType = 1, detectionAreaLeft = 0, detectionAreaTop = 0, detectionAreaRight = 1080, detectionAreaBottom = 1920)
        val change = CompleteActionEntity(ActionEntity(1, 10, name = "Remember X", type = ActionType.CHANGE_COUNTER,
            counterName = "X", counterOperation = ChangeCounterOperationType.SET,
            counterOperationValueType = CounterOperationValueType.CONDITION, counterOperationConditionId = 100,
            counterOperationConditionPositionType = CounterOperationConditionPositionType.CONDITION_CENTER_X))
        val original = event(10, listOf(Json.encodeToJsonElement(change).jsonObject, pause(2)))
        val screenEvent = JsonObject(original + mapOf(
            "event" to JsonObject(original.obj("event") + ("type" to JsonPrimitive("IMAGE_EVENT"))),
            "conditions" to JsonArray(listOf(Json.encodeToJsonElement(image)))))
        val source = smart(listOf(screenEvent))
        val withCounter = JsonObject(source + ("scenario" to JsonObject(source.obj("scenario") +
            ("counters" to JsonArray(listOf(Json.encodeToJsonElement(CountersEntity("X", 42, 0.0))))))))
        val result = analyze(withCounter)
        assertTrue(result.issues.toString(), result.issues.isEmpty())
        val scenario = Json.decodeFromJsonElement<ScenarioBackup>(result.json!!).scenario
        assertEquals(CounterOperationValueType.CONDITION, scenario.events.single().actions.first().action.counterOperationValueType)
        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario)
        assertEquals(listOf(2L), projection.value!!.events.single().actions.map { it.action.id })
        assertTrue(projection.losses.any { it.componentNames.any { name -> name.endsWith("Remember X") } })
    }

    private fun analyze(source: JsonObject) = BackupImportAnalyzer().analyze(source, BackupArchiveFormat.KLICKR_COMPATIBLE, false)
    private fun smart(events: List<JsonObject>) = buildJsonObject {
        put("version", 25); put("screenWidth", 1080); put("screenHeight", 1920)
        put("scenario", buildJsonObject {
            put("scenario", Json.encodeToJsonElement(ScenarioEntity(42, "Smart fixture", 1200)))
            put("events", JsonArray(events)); put("counters", JsonArray(emptyList()))
        })
    }
    private fun event(id: Long, actions: List<JsonObject>) = buildJsonObject {
        put("event", Json.encodeToJsonElement(EventEntity(id, 42, "Event $id", 1, id.toInt(), type = EventType.TRIGGER_EVENT)))
        put("actions", JsonArray(actions))
        put("conditions", JsonArray(listOf(Json.encodeToJsonElement(ConditionEntity(id * 10, id, "Timer", ConditionType.ON_TIMER_REACHED, 0,
            timerValueMs = 100, restartWhenReached = false)))))
    }
    private fun pause(id: Long, eventId: Long = 10) = Json.encodeToJsonElement(CompleteActionEntity(
        ActionEntity(id, eventId, name = "Pause", type = ActionType.PAUSE, pauseDuration = 100),
    )).jsonObject
    private fun gesture(id: Long, type: String, fingers: List<JsonObject>, dumb: Boolean = false) = buildJsonObject {
        put("action", buildJsonObject {
            put("id", id); put(if (dumb) "dumbScenarioId" else "eventId", if (dumb) 42 else 10)
            put("name", "Gesture $id"); put("type", type)
            if (dumb) { put("repeatCount", 3); put("isRepeatInfinite", false); put("repeatDelay", 90) }
        })
        put("gestureFingers", JsonArray(fingers))
    }
    private fun finger(id: Long, actionId: Long, index: Int, start: Long, moving: Boolean, dumb: Boolean = false) = buildJsonObject {
        put("finger", buildJsonObject { put("id", id); put(if (dumb) "dumbActionId" else "actionId", actionId); put("index", index); put("startTimeMs", start) })
        fun motion(i: Int, type: String) = buildJsonObject {
            put("id", id * 10 + i); put("fingerId", id); put("index", i); put("motionType", type)
            if (type in setOf("PRESS", "MOVE")) {
                if (dumb) { put("x", if (type == "PRESS") 10 else 100); put("y", if (type == "PRESS") 20 else 200) }
                else { put("xCoordinateType", "LITERAL"); put("yCoordinateType", "LITERAL"); put("xLiteral", if (type == "PRESS") 10 else 100); put("yLiteral", if (type == "PRESS") 20 else 200) }
            }
            if (type == "MOVE" || type == "WAIT") put("durationMs", if (moving) 400 else 80)
        }
        put("motions", JsonArray(listOf(motion(0, "PRESS"), motion(1, if (moving) "MOVE" else "WAIT"), motion(2, "RELEASE"))))
    }
    private fun simple(action: JsonObject) = buildJsonObject {
        put("version", 3); put("screenWidth", 1080); put("screenHeight", 1920)
        put("dumbScenario", buildJsonObject {
            put("scenario", Json.encodeToJsonElement(DumbScenarioEntity(42, "Simple fixture", 1, false, 1, true, false)))
            put("dumbActions", JsonArray(listOf(action))); put("stats", JsonNull)
        })
    }
}
