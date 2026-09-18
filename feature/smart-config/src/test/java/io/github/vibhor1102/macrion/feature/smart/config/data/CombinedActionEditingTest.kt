package io.github.vibhor1102.macrion.feature.smart.config.data

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.action.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CombinedActionEditingTest {
    private val eventId = Identifier(databaseId = 1)
    private var nextId = 100L
    private fun newId() = Identifier(tempId = nextId++)
    private fun swipe(id: Long) = Swipe(Identifier(databaseId = id), eventId, "Touch $id", 0, 100)
    private fun split() = SplitAction(Identifier(databaseId = 1), eventId, "Zoom", 0,
        listOf(swipe(1), swipe(2)))
    private fun editor(actions: List<Action>) = ActionsEditor<Any>({}, MutableStateFlow(null)).apply { startEdition(actions) }

    @Test fun childSaveStaysInParentDraftAndCancelDiscardsEverything() = runBlocking {
        val original = split()
        val editor = editor(listOf(original))
        editor.startItemEdition(original)
        val draft = original.copy(name = "Renamed", subActions = original.subActions + swipe(3))
        editor.updateEditedItem(draft)
        editor.startSubActionEdition(draft, 2)
        editor.updateEditedItem(swipe(3).copy(name = "Edited finger"))
        editor.upsertEditedItem()
        val returned = editor.editedItem.value as SplitAction
        assertEquals("Renamed", returned.name)
        assertEquals("Edited finger", returned.subActions[2].name)
        assertTrue(editor.editedItemState.first().hasChanged)
        assertEquals(listOf(original), editor.editedList.value)
        editor.stopItemEdition()
        assertEquals(listOf(original), editor.editedList.value)
    }

    @Test fun childIdentifierMayMatchTopLevelActionWithoutEditingThatAction() {
        val original = split()
        val editor = editor(listOf(original))
        editor.startItemEdition(original)
        editor.startSubActionEdition(original, 0)
        assertTrue(editor.editedItem.value is Swipe)
        editor.stopItemEdition()
        assertEquals(original, editor.editedItem.value)
    }

    @Test fun deletingOneOfTwoChildrenDoesNotDeleteParent() {
        val original = split()
        val editor = editor(listOf(original))
        editor.startItemEdition(original)
        editor.startSubActionEdition(original, 0)
        editor.deleteEditedItem()
        assertEquals(original, editor.editedItem.value)
        assertEquals(listOf(original), editor.editedList.value)
    }

    @Test fun newParentIsNotInsertedBySavingChild() {
        val original = split()
        val editor = editor(emptyList())
        editor.startItemEdition(original)
        editor.startSubActionEdition(original, 0)
        editor.updateEditedItem(original.subActions[0].copyBase(name = "Changed"))
        editor.upsertEditedItem()
        assertTrue(editor.editedList.value!!.isEmpty())
        editor.upsertEditedItem()
        assertEquals(1, editor.editedList.value!!.size)
    }

    @Test fun combineCanBeCancelledAndAllocatesFreshChildIds() {
        val original = listOf(swipe(10), swipe(11))
        val editor = editor(original)
        val combined = editor.combineActions(original[0], original[1], newId(), ::newId)!!
        assertEquals(original, editor.editedList.value)
        assertTrue(combined.subActions.none { child -> original.any { it.id == child.id } })
        editor.startItemEdition(combined) // Opening the UI must preserve the transaction.
        editor.stopItemEdition()
        assertEquals(original, editor.editedList.value)
    }

    @Test fun mergeFlattensExistingGroupsAndCommitsAtOriginalPosition() {
        val group = split()
        val extra = swipe(10)
        val editor = editor(listOf(group, extra))
        val combined = editor.combineActions(group, extra, newId(), ::newId)!!
        assertEquals(3, combined.subActions.size)
        assertTrue(combined.subActions.all { it is Swipe })
        editor.upsertEditedItem()
        assertEquals(listOf(combined), editor.editedList.value)
    }

    @Test fun stoppingEntireEditionClearsNestedDraft() {
        val original = split()
        val editor = editor(listOf(original))
        editor.startItemEdition(original)
        editor.startSubActionEdition(original, 0)
        editor.stopEdition()
        assertNull(editor.editedItem.value)
        assertNull(editor.editedList.value)
    }

    @Test fun rejectSelfCombinationAndMoreThanTenTouches() {
        val group = split().copy(subActions = (1L..10L).map(::swipe))
        val extra = swipe(11)
        val editor = editor(listOf(group, extra))
        assertNull(editor.combineActions(group, group, newId(), ::newId))
        assertNull(editor.combineActions(group, extra, newId(), ::newId))
        assertEquals(listOf(group, extra), editor.editedList.value)
    }
}
