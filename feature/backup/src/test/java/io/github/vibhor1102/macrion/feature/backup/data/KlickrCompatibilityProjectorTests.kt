package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity
import io.github.vibhor1102.macrion.core.database.entity.ConditionType
import io.github.vibhor1102.macrion.core.database.entity.EventEntity
import io.github.vibhor1102.macrion.core.database.entity.EventType
import io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity

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
