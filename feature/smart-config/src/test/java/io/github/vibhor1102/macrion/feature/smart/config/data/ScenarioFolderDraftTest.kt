/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.data

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.action.toggleevent.EventToggle
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.Scenario
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder
import org.junit.Assert.*
import org.junit.Test

class ScenarioFolderDraftTest {
    private val scenario = Scenario(Identifier(databaseId = 100), "Scenario", 600)
    private fun event(id: Long, folder: String? = null) = ScreenEvent(
        id = Identifier(databaseId = id), scenarioId = scenario.id, name = "Event $id",
        conditionOperator = OR, priority = id.toInt(), keepDetecting = false, cooldownMs = 0, folder = folder,
    )
    private fun editor(events: List<ScreenEvent>, folders: List<ScenarioFolder>) = ScenarioEditor().apply {
        startEdition(scenario.copy(folders = folders), events, emptyList(), emptyList())
    }

    @Test
    fun emptyFolderIsPartOfDraftAndCancellationDiscardsIt() {
        val editor = editor(listOf(event(1)), emptyList())
        editor.updateEditedScenario(scenario.copy(folders = listOf(ScenarioFolder("Empty", 0))))
        assertEquals(listOf(ScenarioFolder("Empty", 0)), editor.editedScenario.value?.folders)
        editor.stopEdition()
        editor.startEdition(scenario, listOf(event(1)), emptyList(), emptyList())
        assertTrue(editor.editedScenario.value!!.folders.isEmpty())
    }

    @Test
    fun addingToEmptyFolderInsertsAtItsPositionAndShiftsFollowingFolders() {
        val editor = editor(listOf(event(1), event(2)), listOf(ScenarioFolder("Empty", 1), ScenarioFolder("After", 1)))
        editor.startEventEdition(event(3, "Empty"))
        editor.upsertEditedEvent()
        assertEquals(listOf(1L, 3L, 2L), editor.getScreenEvents().map { it.id.databaseId })
        assertEquals(listOf(ScenarioFolder("Empty", 1), ScenarioFolder("After", 2)), editor.editedScenario.value?.folders)
        assertEquals(listOf(0, 1, 2), editor.getScreenEvents().map { it.priority })
    }

    @Test
    fun deletingLastMemberRetainsFolderAndShiftsLaterEmptyFolders() {
        val editor = editor(listOf(event(1), event(2, "A"), event(3)),
            listOf(ScenarioFolder("A", 1), ScenarioFolder("After", 3)))
        editor.startEventEdition(event(2, "A"))
        editor.deleteEditedEvent()
        assertEquals(listOf(ScenarioFolder("A", 1), ScenarioFolder("After", 2)), editor.editedScenario.value?.folders)
        assertEquals(listOf(1L, 3L), editor.getScreenEvents().map { it.id.databaseId })
    }

    @Test
    fun reassignmentMovesMemberToDestinationWithoutSplittingEitherFolder() {
        val source = listOf(event(1, "A"), event(2, "A"), event(3), event(4, "B"))
        val editor = editor(source, listOf(ScenarioFolder("A", 0), ScenarioFolder("Empty", 2), ScenarioFolder("B", 3)))
        editor.startEventEdition(source[0])
        editor.updateEditedEvent(source[0].copy(folder = "B"))
        editor.upsertEditedEvent()
        assertEquals(listOf(2L, 3L, 4L, 1L), editor.getScreenEvents().map { it.id.databaseId })
        assertEquals(listOf(ScenarioFolder("A", 0), ScenarioFolder("Empty", 1), ScenarioFolder("B", 2)), editor.editedScenario.value?.folders)
    }

    @Test
    fun bulkDeletionRemovesReferencesAndKeepsOtherEvents() {
        val target = event(1, "Delete")
        val action = ToggleEvent(Identifier(databaseId = 10), Identifier(databaseId = 2), priority = 0,
            eventToggles = listOf(EventToggle(Identifier(databaseId = 11), Identifier(databaseId = 10), target.id, ToggleEvent.ToggleType.ENABLE)))
        val survivor = event(2).copy(actions = listOf(action))
        val editor = editor(listOf(target, survivor), listOf(ScenarioFolder("Delete", 0)))
        editor.deleteScreenEvents(listOf(target))
        assertEquals(listOf(survivor.id), editor.getScreenEvents().map { it.id })
        assertTrue((editor.getScreenEvents().single().actions.single() as ToggleEvent).eventToggles.isEmpty())
    }

    @Test
    fun membershipOnlyBackupsAcquireFolderPositionsOnEdit() {
        val source = listOf(event(1), event(2, "A"), event(3))
        val editor = editor(source, emptyList())
        editor.startEventEdition(source[1])
        editor.deleteEditedEvent()
        assertEquals(listOf(ScenarioFolder("A", 1)), editor.editedScenario.value?.folders)
    }
}
