/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.toUiImageEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioFolderReorderHelperTest {

    private fun createTestEvent(id: Long, name: String, folder: String? = null): ScreenEvent {
        return ScreenEvent(
            id = Identifier(databaseId = id),
            scenarioId = Identifier(databaseId = 100L),
            name = name,
            conditionOperator = OR,
            priority = id.toInt(),
            keepDetecting = false,
            cooldownMs = 0L,
            folder = folder,
        )
    }

    @Test
    fun `buildVisibleItems correctly structures ungrouped and expanded folder items`() {
        val events = listOf(
            createTestEvent(1, "Event 1", null),
            createTestEvent(2, "Event 2", "Combat"),
            createTestEvent(3, "Event 3", "Combat"),
            createTestEvent(4, "Event 4", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(
            events = events,
            folders = emptyList(),
            collapsedFolders = emptySet(),
        )

        // Expected:
        // 0: EventItem(1, null)
        // 1: FolderHeader("Combat", isExpanded=true)
        // 2: EventItem(2, "Combat")
        // 3: EventItem(3, "Combat")
        // 4: FolderEndBoundary("Combat")
        // 5: EventItem(4, null)
        assertEquals(6, visible.size)
        assertTrue(visible[0] is ScenarioListItem.EventItem && (visible[0] as ScenarioListItem.EventItem).item.name == "Event 1")
        assertTrue(visible[1] is ScenarioListItem.FolderHeader && (visible[1] as ScenarioListItem.FolderHeader).name == "Combat")
        assertTrue(visible[2] is ScenarioListItem.EventItem && (visible[2] as ScenarioListItem.EventItem).item.name == "Event 2")
        assertTrue(visible[3] is ScenarioListItem.EventItem && (visible[3] as ScenarioListItem.EventItem).item.name == "Event 3")
        assertTrue(visible[4] is ScenarioListItem.FolderEndBoundary && (visible[4] as ScenarioListItem.FolderEndBoundary).folderName == "Combat")
        assertTrue(visible[5] is ScenarioListItem.EventItem && (visible[5] as ScenarioListItem.EventItem).item.name == "Event 4")
    }

    @Test
    fun `buildVisibleItems correctly collapses folder`() {
        val events = listOf(
            createTestEvent(1, "Event 1", "Combat"),
            createTestEvent(2, "Event 2", "Combat"),
            createTestEvent(3, "Event 3", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(
            events = events,
            folders = emptyList(),
            collapsedFolders = setOf("Combat"),
        )

        // Expected:
        // 0: FolderHeader("Combat", isExpanded=false)
        // 1: EventItem(3, null)
        assertEquals(2, visible.size)
        val header = visible[0] as ScenarioListItem.FolderHeader
        assertEquals("Combat", header.name)
        assertEquals(false, header.isExpanded)
        assertEquals(2, header.totalCount)
        assertEquals("Event 3", (visible[1] as ScenarioListItem.EventItem).item.name)
    }

    @Test
    fun `getEffectiveFolderAt resolves active folder context and boundaries accurately`() {
        val events = listOf(
            createTestEvent(1, "Event 1", null),
            createTestEvent(2, "Event 2", "F1"),
            createTestEvent(3, "Event 3", "F1"),
            createTestEvent(4, "Event 4", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(
            events = events,
            folders = emptyList(),
            collapsedFolders = emptySet(),
        )

        // 0: Event 1 -> null
        assertNull(ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 0))
        // 1: Header F1 -> null (before entering folder body)
        assertNull(ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 1))
        // 2: Event 2 -> F1
        assertEquals("F1", ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 2))
        // 3: Event 3 -> F1
        assertEquals("F1", ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 3))
        // 4: FolderEndBoundary -> F1
        assertEquals("F1", ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 4))
        // 5: Event 4 (after boundary) -> null
        assertNull(ScenarioFolderReorderHelper.getEffectiveFolderAt(visible, 5))
    }

    @Test
    fun `moveItem event downward past FolderEndBoundary drops it out of folder`() {
        val events = listOf(
            createTestEvent(1, "Event 1", "F1"),
            createTestEvent(2, "Event 2", "F1"),
            createTestEvent(3, "Event 3", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), emptySet())
        // visible:
        // 0: Header(F1)
        // 1: Event 1
        // 2: Event 2
        // 3: Boundary(F1)
        // 4: Event 3

        // Move Event 2 (index 2) down past Boundary(F1) to index 3:
        val moved = ScenarioFolderReorderHelper.moveItem(visible, fromIndex = 2, toIndex = 3)
        // moved should have Boundary at index 2, Event 2 at index 3:
        assertTrue(moved[2] is ScenarioListItem.FolderEndBoundary)
        val droppedItem = moved[3] as ScenarioListItem.EventItem
        assertEquals("Event 2", droppedItem.item.name)

        // Reconstruct events: Event 2 must now have folder = null!
        val reconstructed = ScenarioFolderReorderHelper.reconstructEvents(moved, events)
        assertEquals(3, reconstructed.size)
        assertEquals("Event 1", reconstructed[0].name)
        assertEquals("F1", reconstructed[0].folder)
        assertEquals("Event 2", reconstructed[1].name)
        assertNull(reconstructed[1].folder) // Event 2 is now ungrouped!
        assertEquals("Event 3", reconstructed[2].name)
        assertNull(reconstructed[2].folder)
    }

    @Test
    fun `moveItem ungrouped event upward past FolderEndBoundary drops it into folder at bottom`() {
        val events = listOf(
            createTestEvent(1, "Event 1", "F1"),
            createTestEvent(2, "Event 2", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), emptySet())
        // visible:
        // 0: Header(F1)
        // 1: Event 1
        // 2: Boundary(F1)
        // 3: Event 2

        // Move Event 2 (index 3) upward to index 2 (into Boundary slot):
        val moved = ScenarioFolderReorderHelper.moveItem(visible, fromIndex = 3, toIndex = 2)
        // moved: Event 2 is at index 2, Boundary is at index 3:
        assertEquals("Event 2", (moved[2] as ScenarioListItem.EventItem).item.name)
        assertTrue(moved[3] is ScenarioListItem.FolderEndBoundary)

        // Reconstruct events: Event 2 must now have folder = "F1"!
        val reconstructed = ScenarioFolderReorderHelper.reconstructEvents(moved, events)
        assertEquals(2, reconstructed.size)
        assertEquals("Event 1", reconstructed[0].name)
        assertEquals("F1", reconstructed[0].folder)
        assertEquals("Event 2", reconstructed[1].name)
        assertEquals("F1", reconstructed[1].folder) // Event 2 joined F1!
    }

    @Test
    fun `moveItem event upward past FolderHeader drops it out of folder at top`() {
        val events = listOf(
            createTestEvent(1, "Event 1", null),
            createTestEvent(2, "Event 2", "F1"),
            createTestEvent(3, "Event 3", "F1"),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), emptySet())
        // visible:
        // 0: Event 1
        // 1: Header(F1)
        // 2: Event 2
        // 3: Event 3
        // 4: Boundary(F1)

        // Move Event 2 (index 2) upward to index 1 (above Header F1):
        val moved = ScenarioFolderReorderHelper.moveItem(visible, fromIndex = 2, toIndex = 1)
        // moved: Event 2 at index 1, Header F1 at index 2
        assertEquals("Event 2", (moved[1] as ScenarioListItem.EventItem).item.name)
        assertTrue(moved[2] is ScenarioListItem.FolderHeader)

        // Reconstruct: Event 2 is now outside/above F1 (folder = null)!
        val reconstructed = ScenarioFolderReorderHelper.reconstructEvents(moved, events)
        assertEquals(3, reconstructed.size)
        assertEquals("Event 1", reconstructed[0].name)
        assertNull(reconstructed[0].folder)
        assertEquals("Event 2", reconstructed[1].name)
        assertNull(reconstructed[1].folder) // Left F1
        assertEquals("Event 3", reconstructed[2].name)
        assertEquals("F1", reconstructed[2].folder)
    }

    @Test
    fun `moveItem collapsed folder over individual event moves all its member events together`() {
        val events = listOf(
            createTestEvent(1, "Event 1", "F1"),
            createTestEvent(2, "Event 2", "F1"),
            createTestEvent(3, "Event 3", null),
        ).map { it.toUiImageEvent(false) }

        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), collapsedFolders = setOf("F1"))
        // visible:
        // 0: Header(F1) [collapsed]
        // 1: Event 3 [ungrouped]

        // Drag Header F1 (index 0) down past Event 3 (to index 1):
        val moved = ScenarioFolderReorderHelper.moveItem(visible, fromIndex = 0, toIndex = 1)
        // moved:
        // 0: Event 3
        // 1: Header(F1) [collapsed]
        assertEquals("Event 3", (moved[0] as ScenarioListItem.EventItem).item.name)
        assertTrue(moved[1] is ScenarioListItem.FolderHeader)

        // Reconstruct events: Event 3 is first, followed by ALL events of F1 together!
        val reconstructed = ScenarioFolderReorderHelper.reconstructEvents(moved, events)
        assertEquals(3, reconstructed.size)
        assertEquals("Event 3", reconstructed[0].name)
        assertNull(reconstructed[0].folder)
        assertEquals("Event 1", reconstructed[1].name)
        assertEquals("F1", reconstructed[1].folder)
        assertEquals("Event 2", reconstructed[2].name)
        assertEquals("F1", reconstructed[2].folder)
    }

    @Test
    fun `moveItem collapsed folder past expanded folder jumps the whole expanded folder`() {
        val events = listOf(
            createTestEvent(1, "Event A1", "FolderA"),
            createTestEvent(2, "Event A2", "FolderA"),
            createTestEvent(3, "Event B1", "FolderB"),
            createTestEvent(4, "Event B2", "FolderB"),
        ).map { it.toUiImageEvent(false) }

        // FolderA is expanded, FolderB is collapsed
        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), collapsedFolders = setOf("FolderB"))
        // visible:
        // 0: Header(FolderA) [expanded]
        // 1: Event A1
        // 2: Event A2
        // 3: Boundary(FolderA)
        // 4: Header(FolderB) [collapsed]

        // Drag FolderB (index 4) upward attempting to drop inside FolderA (e.g. index 2):
        val moved = ScenarioFolderReorderHelper.moveItem(visible, fromIndex = 4, toIndex = 2)

        // Rule: FolderB must jump ABOVE Header(FolderA) (index 0), NOT split FolderA!
        assertTrue(moved[0] is ScenarioListItem.FolderHeader && (moved[0] as ScenarioListItem.FolderHeader).name == "FolderB")
        assertTrue(moved[1] is ScenarioListItem.FolderHeader && (moved[1] as ScenarioListItem.FolderHeader).name == "FolderA")
        assertTrue(moved[2] is ScenarioListItem.EventItem && (moved[2] as ScenarioListItem.EventItem).item.name == "Event A1")
        assertTrue(moved[3] is ScenarioListItem.EventItem && (moved[3] as ScenarioListItem.EventItem).item.name == "Event A2")
        assertTrue(moved[4] is ScenarioListItem.FolderEndBoundary && (moved[4] as ScenarioListItem.FolderEndBoundary).folderName == "FolderA")

        // Reconstruct events: All FolderB events precede All FolderA events!
        val reconstructed = ScenarioFolderReorderHelper.reconstructEvents(moved, events)
        assertEquals(4, reconstructed.size)
        assertEquals("Event B1", reconstructed[0].name)
        assertEquals("FolderB", reconstructed[0].folder)
        assertEquals("Event B2", reconstructed[1].name)
        assertEquals("FolderB", reconstructed[1].folder)
        assertEquals("Event A1", reconstructed[2].name)
        assertEquals("FolderA", reconstructed[2].folder)
        assertEquals("Event A2", reconstructed[3].name)
        assertEquals("FolderA", reconstructed[3].folder)
    }
    @Test
    fun `collapsed folder crossing expanded header downward cannot nest`() {
        val events = listOf(createTestEvent(1, "A", "A"), createTestEvent(2, "B", "B"))
            .map { it.toUiImageEvent(false) }
        val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, emptyList(), setOf("A"))
        val moved = ScenarioFolderReorderHelper.moveItem(visible, 0, 1)
        assertEquals(listOf("folder_header_B", 2L, "folder_end_B", "folder_header_A"), moved.map { it.key })
        assertEquals(listOf("B", "A"), ScenarioFolderReorderHelper.reconstructEvents(moved, events).map { it.folder })
    }

    @Test
    fun `last event leaving folder preserves empty folder at its exact position`() {
        val source = listOf(createTestEvent(1, "Before"), createTestEvent(2, "Member", "F"), createTestEvent(3, "After"))
            .map { it.toUiImageEvent(false) }
        val visible = ScenarioFolderReorderHelper.buildVisibleItems(source, emptyList(), emptySet())
        val moved = ScenarioFolderReorderHelper.moveItem(visible, 2, 1)
        val folders = ScenarioFolderReorderHelper.reconstructFolders(moved, source)
        val events = ScenarioFolderReorderHelper.reconstructEvents(moved, source).map { it.toUiImageEvent(false) }
        assertEquals(listOf(ScenarioFolder("F", 2)), folders)
        assertEquals(moved.map { it.key }, ScenarioFolderReorderHelper.buildVisibleItems(events, folders, emptySet()).map { it.key })
    }

    @Test
    fun `all legal source and target pairs preserve boundaries events and saved positions`() {
        val events = listOf(
            createTestEvent(1, "Root before"), createTestEvent(2, "A1", "A"),
            createTestEvent(3, "A2", "A"), createTestEvent(4, "Root middle"),
            createTestEvent(5, "B1", "B"), createTestEvent(6, "Root after"),
        ).map { it.toUiImageEvent(false) }
        val folders = listOf(ScenarioFolder("Empty before", 0), ScenarioFolder("A", 1),
            ScenarioFolder("Empty middle", 3), ScenarioFolder("B", 4), ScenarioFolder("Empty after", 6))
        for (mask in 0 until (1 shl folders.size)) {
            val collapsed = folders.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.map { it.name }.toSet()
            val visible = ScenarioFolderReorderHelper.buildVisibleItems(events, folders, collapsed)
            for (from in visible.indices) for (to in visible.indices) {
                val moved = ScenarioFolderReorderHelper.moveItem(visible, from, to)
                assertRoundTrip(moved, events, collapsed)
            }
        }
    }

    @Test
    fun `seeded drag sequences on large lists survive each drop and reload`() {
        var events = (1L..1200L).map { id ->
            createTestEvent(id, "Event $id", if (id % 10L == 0L) null else "Folder ${id / 10}")
                .toUiImageEvent(false)
        }
        var folders = listOf(ScenarioFolder("Empty", 0))
        val random = kotlin.random.Random(917)
        var collapsed = events.mapNotNull { it.folder }.filterIndexed { index, _ -> index % 3 != 0 }.toSet()
        repeat(100) {
            var visible = ScenarioFolderReorderHelper.buildVisibleItems(events, folders, collapsed)
            val movable = visible.filter { it is ScenarioListItem.EventItem || it is ScenarioListItem.FolderHeader && !it.isExpanded }
            val key = movable[random.nextInt(movable.size)].key
            repeat(5) {
                val from = visible.indexOfFirst { it.key == key }
                val to = (from + if (random.nextBoolean()) 1 else -1).coerceIn(0, visible.lastIndex)
                visible = ScenarioFolderReorderHelper.moveItem(visible, from, to)
            }
            assertRoundTrip(visible, events, collapsed)
            folders = ScenarioFolderReorderHelper.reconstructFolders(visible, events)
            events = ScenarioFolderReorderHelper.reconstructEvents(visible, events).map { it.toUiImageEvent(false) }
            if (it % 10 == 0) collapsed = folders.filter { random.nextBoolean() }.map { it.name }.toSet()
        }
    }

    private fun assertRoundTrip(items: List<ScenarioListItem>, source: List<io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent>, collapsed: Set<String>) {
        var open: String? = null
        items.forEach { item ->
            when (item) {
                is ScenarioListItem.FolderHeader -> {
                    assertNull("Nested header ${item.name}", open)
                    if (item.isExpanded) open = item.name
                }
                is ScenarioListItem.FolderEndBoundary -> {
                    assertEquals(item.folderName, open)
                    open = null
                }
                is ScenarioListItem.EventItem -> Unit
            }
        }
        assertNull(open)
        val events = ScenarioFolderReorderHelper.reconstructEvents(items, source)
        assertEquals(source.size, events.size)
        assertEquals(source.map { it.event.id }.toSet(), events.map { it.id }.toSet())
        val folders = ScenarioFolderReorderHelper.reconstructFolders(items, source)
        val reloaded = ScenarioFolderReorderHelper.buildVisibleItems(events.map { it.toUiImageEvent(false) }, folders, collapsed)
        assertEquals(items.map { it.key }, reloaded.map { it.key })
    }

}
