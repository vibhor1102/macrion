/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.external

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.IRepository
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.EditedElementState
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.IEditionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalActionViewModelTest {

    @Test
    fun `knownExternalActionNames combines live edited scenario actions and other database actions without needing save`() = runTest {
        val eventId1 = Identifier(databaseId = 101L)
        val eventId2 = Identifier(databaseId = 202L)

        // Live scenario events currently in memory (not yet saved to DB)
        val liveAction1 = ExternalAction(
            id = Identifier(1L, asTemporary = true),
            eventId = eventId1,
            name = "Action 1",
            priority = 0,
            externalActionName = "live_event_alpha",
        )
        val liveEvent1 = mockk<ScreenEvent> {
            every { id } returns eventId1
            every { actions } returns listOf(liveAction1)
        }

        val eventsFlow = MutableStateFlow<List<Event>>(listOf(liveEvent1))
        val actionStateFlow = MutableStateFlow(EditedElementState<Action>(value = null, hasChanged = false, canBeSaved = false))
        val editingActionFlow = MutableStateFlow(false)

        val mockEditionState: IEditionState = mockk {
            every { allEditedEventsFlow } returns eventsFlow
            every { editedActionState } returns actionStateFlow
        }
        val mockEditionRepository: EditionRepository = mockk {
            every { editionState } returns mockEditionState
            every { isEditingAction } returns editingActionFlow
        }

        // Database actions: one from another scenario (eventId2) and one stale from eventId1
        val dbActionOtherScenario = ExternalAction(
            id = Identifier(databaseId = 2L),
            eventId = eventId2,
            name = "DB Action Other",
            priority = 0,
            externalActionName = "db_event_beta",
        )
        val dbActionStaleSameScenario = ExternalAction(
            id = Identifier(databaseId = 3L),
            eventId = eventId1,
            name = "DB Action Stale",
            priority = 1,
            externalActionName = "stale_event_should_be_ignored",
        )

        val allActionsFlow = MutableStateFlow<List<Action>>(listOf(dbActionOtherScenario, dbActionStaleSameScenario))
        val mockSmartRepository: IRepository = mockk {
            every { allActions } returns allActionsFlow
        }

        val viewModel = ExternalActionViewModel(
            editionRepository = mockEditionRepository,
            smartRepository = mockSmartRepository,
        )

        val names = viewModel.knownExternalActionNames.first()

        // Should include live_event_alpha and db_event_beta, but NOT the stale event from the currently edited scenario
        assertEquals(listOf("db_event_beta", "live_event_alpha"), names)
    }
}
