package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.base.gesture.SwipePath
import io.github.vibhor1102.macrion.core.base.gesture.SwipePoint
import io.github.vibhor1102.macrion.core.base.gesture.SwipeNode
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity
import io.github.vibhor1102.macrion.core.database.entity.ConditionType
import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventToggleEntity
import io.github.vibhor1102.macrion.core.database.entity.EventToggleType
import io.github.vibhor1102.macrion.core.database.entity.EventType
import io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionWithSubActions
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbSplitActionItemEntity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KlickrCompatibilityProjectorTests {

    @Test
    fun currentProfileIsExplicitAndLosslessForAnEmptySelection() {
        val plan = KlickrCompatibilityProjector.createPlan(emptyList(), emptyList())

        assertEquals(BackupArchiveFormat.KLICKR_COMPATIBLE, plan.format)
        assertEquals("klickr-d466f1620c54", plan.profile?.id)
        assertEquals(23, plan.profile?.portableDatabaseVersion)
        assertEquals(false, plan.profile?.crossReaderVerified)
        assertEquals(0, plan.omittedComponentCount)
        assertEquals(0, plan.excludedScenarioCount)
        assertTrue(plan.losses.isEmpty())
    }

    @Test
    fun structuredLossesAreCounted() {
        val plan = BackupExportPlan(
            dumbScenarios = emptyList(),
            smartScenarios = emptyList(),
            format = BackupArchiveFormat.KLICKR_COMPATIBLE,
            losses = listOf(
                KlickrCompatibilityLoss(
                    reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                    componentCount = 2,
                    scenarioId = 42,
                ),
            ),
        )

        assertEquals(2, plan.omittedComponentCount)
    }

    @Test
    fun projectionIsDetachedFromOriginalEntities() {
        val original = CompleteScenario(
            scenario = ScenarioEntity(
                id = 42,
                name = "Test",
                detectionQuality = 600,
            ),
            events = listOf(completeEvent(ActionType.PAUSE)),
            counters = emptyList(),
        )

        val projected = KlickrCompatibilityProjector.projectSmartScenario(original).value!!

        assertEquals(original, projected)
        assertNotSame(original, projected)
        assertNotSame(original.scenario, projected.scenario)
        assertNotSame(original.events.single(), projected.events.single())
    }

    @Test
    fun externalActionsAreRemovedWithoutMutatingTheOriginalScenario() {
        val original = completeScenario(
            completeEvent(ActionType.PAUSE, ActionType.EXTERNAL_ACTION),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(original)

        assertEquals(listOf(ActionType.PAUSE), projection.value!!.events.single().actions.map { it.action.type })
        assertEquals(
            listOf(ActionType.PAUSE, ActionType.EXTERNAL_ACTION),
            original.events.single().actions.map { it.action.type },
        )
        assertEquals(1, projection.losses.single().componentCount)
        assertEquals(KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT, projection.losses.single().reason)
    }

    @Test
    fun curvedSwipeIsExcludedFromKlickrProjectionButRetainedInOriginal() {
        val curvedPath = SwipePath(listOf(
            SwipeNode(SwipePoint(0f, 0f)),
            SwipeNode(SwipePoint(40f, 30f)),
            SwipeNode(SwipePoint(80f, 0f)),
        ))
        val event = completeEvent(ActionType.PAUSE, ActionType.SWIPE)
        val swipe = event.actions[1]
        val original = completeScenario(event.copy(actions = listOf(
            event.actions[0], swipe.copy(action = swipe.action.copy(swipePath = curvedPath)),
        )))

        val projection = KlickrCompatibilityProjector.projectSmartScenario(original)

        assertEquals(listOf(ActionType.PAUSE), projection.value!!.events.single().actions.map { it.action.type })
        assertEquals(curvedPath, original.events.single().actions[1].action.swipePath)
        assertEquals(KlickrCompatibilityLossReason.CURVED_SWIPE, projection.losses.single().reason)
        assertEquals(1, projection.losses.single().componentCount)
    }

    @Test
    fun dumbProjectionRemovesMultiTouchAndItsNativeOnlyChildrenWithoutMutatingSource() {
        val parent = DumbActionEntity(1, 1, name = "Multi-touch", type = DumbActionType.SPLIT_ACTION)
        val pause = DumbActionEntity(2, 1, name = "Pause", type = DumbActionType.PAUSE, pauseDuration = 100)
        val original = DumbScenarioWithActions(
            scenario = DumbScenarioEntity(1, "Scenario", 1, false, 1, true, false),
            dumbActions = listOf(parent, pause),
            stats = null,
            dumbActionsWithSubActions = listOf(DumbActionWithSubActions(
                parent, listOf(DumbSplitActionItemEntity(id = 3, actionId = 1)),
            )),
        )

        val projected = KlickrCompatibilityProjector.projectDumbScenario(original).value!!

        assertEquals(listOf(DumbActionType.PAUSE), projected.dumbActions.map { it.type })
        assertTrue(projected.dumbActionsWithSubActions.isEmpty())
        assertEquals(1, original.dumbActionsWithSubActions.single().splitItems.size)
    }

    @Test
    fun dumbCurvedSwipeIsOmittedAndReported() {
        val path = SwipePath(listOf(
            SwipeNode(SwipePoint(0f, 0f)),
            SwipeNode(SwipePoint(40f, 30f)),
            SwipeNode(SwipePoint(80f, 0f)),
        ))
        val swipe = DumbActionEntity(
            1, 1, name = "Swipe", type = DumbActionType.SWIPE,
            fromX = 0, fromY = 0, toX = 80, toY = 0, swipeDuration = 500, swipePath = path,
        )
        val pause = DumbActionEntity(2, 1, name = "Pause", type = DumbActionType.PAUSE, pauseDuration = 100)
        val original = DumbScenarioWithActions(
            scenario = DumbScenarioEntity(1, "Scenario", 1, false, 1, true, false),
            dumbActions = listOf(swipe, pause),
            stats = null,
        )

        val projection = KlickrCompatibilityProjector.projectDumbScenario(original)

        assertEquals(listOf(DumbActionType.PAUSE), projection.value!!.dumbActions.map { it.type })
        assertEquals(path, original.dumbActions.first().swipePath)
        assertEquals(KlickrCompatibilityLossReason.CURVED_SWIPE, projection.losses.single().reason)
    }

    @Test
    fun scenarioWithOnlyExternalActionsIsExcludedAsMeaningless() {
        val projection = KlickrCompatibilityProjector.projectSmartScenario(
            completeScenario(completeEvent(ActionType.EXTERNAL_ACTION)),
        )

        assertEquals(null, projection.value)
        assertEquals(
            listOf(
                KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                KlickrCompatibilityLossReason.MEANINGLESS_EVENT,
                KlickrCompatibilityLossReason.MEANINGLESS_SCENARIO,
            ),
            projection.losses.map { it.reason },
        )
    }

    @Test
    fun playSoundActionsAreRemovedWithoutMutatingTheOriginalScenario() {
        val original = completeScenario(
            completeEvent(ActionType.PAUSE, ActionType.PLAY_SOUND),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(original)

        assertEquals(listOf(ActionType.PAUSE), projection.value!!.events.single().actions.map { it.action.type })
        assertEquals(
            listOf(ActionType.PAUSE, ActionType.PLAY_SOUND),
            original.events.single().actions.map { it.action.type },
        )
        assertEquals(1, projection.losses.single().componentCount)
        assertEquals(KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT, projection.losses.single().reason)
    }

    @Test
    fun captureScreenshotActionsAreRemovedWithoutMutatingTheOriginalScenario() {
        val original = completeScenario(
            completeEvent(ActionType.PAUSE, ActionType.CAPTURE_SCREENSHOT),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(original)

        assertEquals(listOf(ActionType.PAUSE), projection.value!!.events.single().actions.map { it.action.type })
        assertEquals(
            listOf(ActionType.PAUSE, ActionType.CAPTURE_SCREENSHOT),
            original.events.single().actions.map { it.action.type },
        )
        assertEquals(1, projection.losses.single().componentCount)
        assertEquals(KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT, projection.losses.single().reason)
    }

    @Test
    fun danglingEventToggleIsPrunedWhenTargetEventIsOmitted() {
        val targetEvent = CompleteEventEntity(
            event = EventEntity(id = 20, scenarioId = 42, name = "Target", conditionOperator = 0, priority = 1, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 200, eventId = 20, name = "Capture", type = ActionType.CAPTURE_SCREENSHOT),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 2000, eventId = 20, name = "Cond", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val sourceEvent = CompleteEventEntity(
            event = EventEntity(id = 10, scenarioId = 42, name = "Source", conditionOperator = 0, priority = 0, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 100, eventId = 10, name = "Wait", type = ActionType.PAUSE, pauseDuration = 100),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
                CompleteActionEntity(
                    action = ActionEntity(id = 101, eventId = 10, priority = 1, name = "Toggle", type = ActionType.TOGGLE_EVENT, toggleAll = false),
                    intentExtras = emptyList(),
                    eventsToggle = listOf(
                        EventToggleEntity(id = 500, actionId = 101, type = EventToggleType.ENABLE, toggleEventId = 20),
                        EventToggleEntity(id = 501, actionId = 101, type = EventToggleType.DISABLE, toggleEventId = 10),
                    ),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1000, eventId = 10, name = "Cond", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val scenario = CompleteScenario(
            scenario = ScenarioEntity(id = 42, name = "Test", detectionQuality = 600),
            events = listOf(sourceEvent, targetEvent),
            counters = emptyList(),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario)

        assertTrue(projection.isExportable)
        val projected = projection.value!!
        assertEquals(listOf(10L), projected.events.map { it.event.id })
        val toggleAction = projected.events.single().actions.first { it.action.type == ActionType.TOGGLE_EVENT }
        assertEquals(listOf(10L), toggleAction.eventsToggle.map { it.toggleEventId })
        verifyKlickrImportCompatibility(projected)

        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT })
        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.MEANINGLESS_EVENT })
        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.BROKEN_REFERENCE })
    }

    @Test
    fun cascadingPruningExcludesScenarioWhenAllEventsBecomeMeaningless() {
        val targetEvent = CompleteEventEntity(
            event = EventEntity(id = 20, scenarioId = 42, name = "Target", conditionOperator = 0, priority = 1, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 200, eventId = 20, name = "Play Sound", type = ActionType.PLAY_SOUND),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 2000, eventId = 20, name = "Cond", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val sourceEvent = CompleteEventEntity(
            event = EventEntity(id = 10, scenarioId = 42, name = "Source", conditionOperator = 0, priority = 0, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 100, eventId = 10, name = "Toggle", type = ActionType.TOGGLE_EVENT, toggleAll = false),
                    intentExtras = emptyList(),
                    eventsToggle = listOf(
                        EventToggleEntity(id = 500, actionId = 100, type = EventToggleType.ENABLE, toggleEventId = 20),
                    ),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1000, eventId = 10, name = "Cond", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val scenario = CompleteScenario(
            scenario = ScenarioEntity(id = 42, name = "Test", detectionQuality = 600),
            events = listOf(sourceEvent, targetEvent),
            counters = emptyList(),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario)

        assertEquals(null, projection.value)
        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.MEANINGLESS_SCENARIO })
    }

    @Test
    fun danglingClickOnConditionIsPrunedWhenTargetConditionIsOmitted() {
        val targetEvent = CompleteEventEntity(
            event = EventEntity(id = 20, scenarioId = 42, name = "Target", conditionOperator = 0, priority = 1, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 200, eventId = 20, name = "Screenshot", type = ActionType.CAPTURE_SCREENSHOT),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 2000, eventId = 20, name = "Cond Target", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val sourceEvent = CompleteEventEntity(
            event = EventEntity(id = 10, scenarioId = 42, name = "Source", conditionOperator = 0, priority = 0, type = EventType.TRIGGER_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 100, eventId = 10, name = "Wait", type = ActionType.PAUSE, pauseDuration = 50),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
                CompleteActionEntity(
                    action = ActionEntity(
                        id = 101,
                        eventId = 10,
                        priority = 1,
                        name = "Click condition",
                        type = ActionType.CLICK,
                        clickPositionType = ClickPositionType.ON_DETECTED_CONDITION,
                        clickOnConditionId = 2000,
                    ),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1000, eventId = 10, name = "Cond Source", type = ConditionType.ON_TIMER_REACHED, priority = 0),
            ),
        )

        val scenario = CompleteScenario(
            scenario = ScenarioEntity(id = 42, name = "Test", detectionQuality = 600),
            events = listOf(sourceEvent, targetEvent),
            counters = emptyList(),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(scenario)

        assertTrue(projection.isExportable)
        val projected = projection.value!!
        assertEquals(listOf(10L), projected.events.map { it.event.id })
        assertEquals(listOf(ActionType.PAUSE), projected.events.single().actions.map { it.action.type })
        verifyKlickrImportCompatibility(projected)
        assertTrue(projection.losses.any { it.reason == KlickrCompatibilityLossReason.BROKEN_REFERENCE })
    }

    @Test
    fun realWorldFloodClanChatScenarioRegression() {
        val event405 = CompleteEventEntity(
            event = EventEntity(id = 405, scenarioId = 4, name = "Screenshot event", conditionOperator = 1, priority = 0, type = EventType.IMAGE_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 1912, eventId = 405, name = "Capture", type = ActionType.CAPTURE_SCREENSHOT),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1014, eventId = 405, name = "Cond", type = ConditionType.ON_IMAGE_DETECTED, priority = 0, path = "c1.png"),
            ),
        )

        val event406 = CompleteEventEntity(
            event = EventEntity(id = 406, scenarioId = 4, name = "Default event", conditionOperator = 1, priority = 1, type = EventType.IMAGE_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 1913, eventId = 406, priority = 0, name = "Wait", type = ActionType.PAUSE, pauseDuration = 200),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
                CompleteActionEntity(
                    action = ActionEntity(id = 1914, eventId = 406, priority = 1, name = "Write text", type = ActionType.TEXT, textValue = "hi"),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
                CompleteActionEntity(
                    action = ActionEntity(id = 1915, eventId = 406, priority = 2, name = "Toggle event", type = ActionType.TOGGLE_EVENT, toggleAll = false),
                    intentExtras = emptyList(),
                    eventsToggle = listOf(
                        EventToggleEntity(id = 1259, actionId = 1915, type = EventToggleType.ENABLE, toggleEventId = 405),
                        EventToggleEntity(id = 1260, actionId = 1915, type = EventToggleType.DISABLE, toggleEventId = 406),
                    ),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1015, eventId = 406, name = "Cond", type = ConditionType.ON_IMAGE_DETECTED, priority = 0, path = "c2.png"),
            ),
        )

        val event407 = CompleteEventEntity(
            event = EventEntity(id = 407, scenarioId = 4, name = "2", conditionOperator = 2, priority = 2, type = EventType.IMAGE_EVENT),
            actions = listOf(
                CompleteActionEntity(
                    action = ActionEntity(id = 1916, eventId = 407, priority = 0, name = "Write text", type = ActionType.TEXT, textValue = "hello"),
                    intentExtras = emptyList(),
                    eventsToggle = emptyList(),
                ),
            ),
            conditions = listOf(
                ConditionEntity(id = 1016, eventId = 407, name = "Cond", type = ConditionType.ON_IMAGE_DETECTED, priority = 0, path = "c3.png"),
            ),
        )

        val original = CompleteScenario(
            scenario = ScenarioEntity(id = 4, name = "Flood Clan Chat", detectionQuality = 1170),
            events = listOf(event405, event406, event407),
            counters = emptyList(),
        )

        val projection = KlickrCompatibilityProjector.projectSmartScenario(original)

        assertTrue(projection.isExportable)
        val projected = projection.value!!
        assertEquals(listOf(406L, 407L), projected.events.map { it.event.id })

        val toggleAction = projected.events.first { it.event.id == 406L }.actions.first { it.action.type == ActionType.TOGGLE_EVENT }
        assertEquals(listOf(406L), toggleAction.eventsToggle.map { it.toggleEventId })

        verifyKlickrImportCompatibility(projected)
    }

    private fun verifyKlickrImportCompatibility(completeScenario: CompleteScenario) {
        val violations = KlickrCompatibilityProjector.validateReferentialIntegrity(completeScenario)
        assertTrue("Referential integrity violations: $violations", violations.isEmpty())

        val eventIds = completeScenario.events.map { it.event.id }.toSet()
        val conditionIds = completeScenario.events.flatMap { it.conditions }.map { it.id }.toSet()

        for (event in completeScenario.events) {
            assertTrue("Event must have actions", event.actions.isNotEmpty())
            assertTrue("Event must have conditions", event.conditions.isNotEmpty())

            for (action in event.actions) {
                if (action.action.type == ActionType.TOGGLE_EVENT && action.action.toggleAll != true) {
                    for (toggle in action.eventsToggle) {
                        assertTrue(
                            "Toggle ${toggle.id} references non-existent event ${toggle.toggleEventId}. Surviving events: $eventIds",
                            toggle.toggleEventId in eventIds,
                        )
                    }
                }
                if (action.action.type == ActionType.CLICK && action.action.clickPositionType == ClickPositionType.ON_DETECTED_CONDITION) {
                    val condId = action.action.clickOnConditionId
                    assertTrue(
                        "Click action ${action.action.id} references non-existent condition $condId. Surviving conditions: $conditionIds",
                        condId != null && condId in conditionIds,
                    )
                }
            }
        }
    }

    private fun completeScenario(vararg events: CompleteEventEntity) = CompleteScenario(
        scenario = ScenarioEntity(id = 42, name = "Test", detectionQuality = 600),
        events = events.toList(),
        counters = emptyList(),
    )

    private fun completeEvent(vararg actionTypes: ActionType) = CompleteEventEntity(
        event = EventEntity(
            id = 10,
            scenarioId = 42,
            name = "Event",
            conditionOperator = 0,
            priority = 0,
            type = EventType.TRIGGER_EVENT,
        ),
        actions = actionTypes.mapIndexed { index, type ->
            CompleteActionEntity(
                action = ActionEntity(
                    id = 100L + index,
                    eventId = 10,
                    name = type.name,
                    type = type,
                    pauseDuration = if (type == ActionType.PAUSE) 100 else null,
                    externalActionName = if (type == ActionType.EXTERNAL_ACTION) "event" else null,
                ),
                intentExtras = emptyList(),
                eventsToggle = emptyList(),
            )
        },
        conditions = listOf(
            ConditionEntity(
                id = 200,
                eventId = 10,
                name = "Timer",
                type = ConditionType.ON_TIMER_REACHED,
                priority = 0,
                timerValueMs = 100,
                restartWhenReached = false,
            )
        ),
    )
}
