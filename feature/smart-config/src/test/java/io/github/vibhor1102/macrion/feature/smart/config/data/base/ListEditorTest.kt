/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.data.base

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.base.interfaces.Completable
import io.github.vibhor1102.macrion.core.base.interfaces.Identifiable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

private data class TestItem(
    override val id: Identifier,
    val value: String,
) : Identifiable, Completable {
    override fun isComplete(): Boolean = value.isNotEmpty()
}

class ListEditorTest {

    @Test
    fun `item edition cannot start before the list is opened or after it is closed`() = runTest {
        val editor = ListEditor<TestItem, String>(parentItem = MutableStateFlow("parent"))
        val item = TestItem(Identifier(databaseId = 1L), "original")

        editor.startItemEdition(item)
        assertNull(editor.editedItem.value)
        assertFalse(editor.isItemEditionStarted())
        assertEquals(emptyList<TestItem>(), editor.getAllEditedItems())

        editor.startEdition(listOf(item))
        editor.startItemEdition(item)
        editor.stopEdition()
        editor.startItemEdition(item)
        assertNull(editor.editedItem.value)
        assertFalse(editor.isItemEditionStarted())
        assertEquals(emptyList<TestItem>(), editor.getAllEditedItems())
    }

    @Test
    fun `an open empty list accepts a new item`() = runTest {
        val editor = ListEditor<TestItem, String>(parentItem = MutableStateFlow("parent"))
        val item = TestItem(Identifier(databaseId = 1L), "new")

        editor.startEdition(emptyList())
        editor.startItemEdition(item)
        assertEquals(item, editor.editedItem.value)
        editor.upsertEditedItem()
        assertEquals(listOf(item), editor.editedList.value)
    }

    @Test
    fun `allEditedItems immediately reflects in-place updates to an existing edited item`() = runTest {
        val parentFlow = MutableStateFlow<String?>("parent")
        val editor = ListEditor<TestItem, String>(parentItem = parentFlow)

        val id1 = Identifier(databaseId = 1L)
        val id2 = Identifier(databaseId = 2L)

        val originalItem1 = TestItem(id1, "initial_1")
        val originalItem2 = TestItem(id2, "initial_2")

        editor.startEdition(listOf(originalItem1, originalItem2))

        // Start editing item 1
        editor.startItemEdition(originalItem1)
        val updatedItem1 = originalItem1.copy(value = "updated_1")
        editor.updateEditedItem(updatedItem1)

        val allItems = editor.allEditedItems.first()

        // allEditedItems must contain the modified version of item 1, not the stale initial_1
        assertEquals("updated_1", allItems.first { it.id == id1 }.value)
        assertEquals("initial_2", allItems.first { it.id == id2 }.value)
    }

    @Test
    fun `startItemEdition resolves to latest item from editedList when existing id is passed with stale data`() = runTest {
        val parentFlow = MutableStateFlow<String?>("parent")
        val editor = ListEditor<TestItem, String>(parentItem = parentFlow)

        val id1 = Identifier(databaseId = 1L)
        val initialItem = TestItem(id1, "initial")
        editor.startEdition(listOf(initialItem))

        // Edit and upsert updated item
        editor.startItemEdition(initialItem)
        val updatedItem = initialItem.copy(value = "updated")
        editor.updateEditedItem(updatedItem)
        editor.upsertEditedItem()

        // Re-start edition passing the stale initialItem
        editor.startItemEdition(initialItem)

        // The edited item must be the updated item from editedList, not the stale initialItem
        assertEquals("updated", editor.editedItem.value?.value)
    }
}
