/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent

/**
 * Items displayed in the scenario events list.
 */
sealed class ScenarioListItem {
    abstract val key: Any

    data class FolderHeader(
        val name: String,
        val totalCount: Int,
        val enabledCount: Int,
        val isExpanded: Boolean,
    ) : ScenarioListItem() {
        override val key: Any get() = "folder_header_$name"
    }

    data class FolderEndBoundary(
        val folderName: String,
    ) : ScenarioListItem() {
        override val key: Any get() = "folder_end_$folderName"
    }

    data class EventItem(
        val item: UiImageEvent,
        val folderName: String?,
    ) : ScenarioListItem() {
        override val key: Any get() = item.event.id.let {
            if (it.databaseId != 0L) it.databaseId else -requireNotNull(it.tempId)
        }
    }
}

/**
 * Pure helper logic for managing folder-aware visible items, boundary rules,
 * drag-and-drop index movements, and reconstructing scenario events for persistence.
 */
object ScenarioFolderReorderHelper {

    /**
     * Builds the visible list of items from the source events and folder state.
     * Ungrouped events appear scattered wherever they are located in [events].
     * Events belonging to a folder appear grouped under their [ScenarioListItem.FolderHeader].
     * If expanded, their items are followed by a [ScenarioListItem.FolderEndBoundary].
     */
    fun buildVisibleItems(
        events: List<UiImageEvent>,
        folders: List<ScenarioFolder>,
        collapsedFolders: Set<String>,
    ): List<ScenarioListItem> {
        val byFolder = events.groupBy { it.folder?.trim()?.ifEmpty { null } }
        val firstIndices = mutableMapOf<String, Int>()
        events.forEachIndexed { index, event ->
            event.folder?.trim()?.takeIf { it.isNotEmpty() }?.let { firstIndices.putIfAbsent(it, index) }
        }
        // Old backups contain membership only. Infer missing headers without losing their order.
        val known = folders.map { it.name }.toSet()
        val orderedFolders = (folders + firstIndices.filterKeys { it !in known }
            .map { (name, index) -> ScenarioFolder(name, index) })
            .distinctBy { it.name }
            .sortedBy { it.eventIndex }
        val result = mutableListOf<ScenarioListItem>()
        var nextFolder = 0
        fun appendFoldersThrough(index: Int) {
            while (nextFolder < orderedFolders.size && orderedFolders[nextFolder].eventIndex <= index) {
                val folder = orderedFolders[nextFolder++]
                val members = byFolder[folder.name].orEmpty()
                val expanded = folder.name !in collapsedFolders
                result.add(ScenarioListItem.FolderHeader(
                    folder.name, members.size, members.count { it.event.enabledOnStart }, expanded,
                ))
                if (expanded) {
                    members.forEach { result.add(ScenarioListItem.EventItem(it, folder.name)) }
                    result.add(ScenarioListItem.FolderEndBoundary(folder.name))
                }
            }
        }
        events.forEachIndexed { index, event ->
            appendFoldersThrough(index)
            if (event.folder.isNullOrBlank()) result.add(ScenarioListItem.EventItem(event, null))
        }
        appendFoldersThrough(Int.MAX_VALUE)
        return result
    }

    /** Capture every header, even an empty one, at its position in the full event sequence. */
    fun reconstructFolders(
        items: List<ScenarioListItem>,
        allSourceEvents: List<UiImageEvent>,
    ): List<ScenarioFolder> {
        val counts = allSourceEvents.groupingBy { it.folder?.trim() }.eachCount()
        var eventIndex = 0
        return buildList {
            items.forEach { item ->
                when (item) {
                    is ScenarioListItem.FolderHeader -> {
                        add(ScenarioFolder(item.name, eventIndex))
                        if (!item.isExpanded) eventIndex += counts[item.name] ?: 0
                    }
                    is ScenarioListItem.EventItem -> eventIndex++
                    is ScenarioListItem.FolderEndBoundary -> Unit
                }
            }
        }
    }

    /**
     * Computes the effective folder context at [targetIndex] in the list.
     * Returns null if [targetIndex] is outside any folder or after a folder boundary.
     */
    fun getEffectiveFolderAt(items: List<ScenarioListItem>, targetIndex: Int): String? {
        var activeFolder: String? = null
        for (i in 0 until targetIndex.coerceAtMost(items.size)) {
            when (val item = items[i]) {
                is ScenarioListItem.FolderHeader -> {
                    activeFolder = if (item.isExpanded) item.name else null
                }
                is ScenarioListItem.FolderEndBoundary -> {
                    activeFolder = null
                }
                is ScenarioListItem.EventItem -> { /* preserves current activeFolder */ }
            }
        }
        return activeFolder
    }

    /**
     * Moves an item from [fromIndex] to [toIndex] respecting folder boundaries and
     * collapsed folder integrity.
     */
    fun moveItem(
        items: List<ScenarioListItem>,
        fromIndex: Int,
        toIndex: Int,
    ): List<ScenarioListItem> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }

        val list = items.toMutableList()
        val draggedItem = list[fromIndex]

        // Headers can only be picked up while collapsed; end markers are targets, not handles.
        if (draggedItem is ScenarioListItem.FolderEndBoundary ||
            draggedItem is ScenarioListItem.FolderHeader && draggedItem.isExpanded) return items

        if (draggedItem is ScenarioListItem.FolderHeader) {
            // Check the INSERTION slot after removal. Checking the old target index misses
            // downward moves onto an expanded header and allows an illegal nested folder.
            list.removeAt(fromIndex)
            var destination = toIndex
            val destinationFolder = getEffectiveFolderAt(list, destination)
            if (destinationFolder != null) {
                destination = if (toIndex > fromIndex) {
                    list.indexOfFirst {
                        it is ScenarioListItem.FolderEndBoundary && it.folderName == destinationFolder
                    }.takeIf { it >= 0 }?.plus(1) ?: return items
                } else {
                    list.indexOfFirst {
                        it is ScenarioListItem.FolderHeader && it.name == destinationFolder
                    }.takeIf { it >= 0 } ?: return items
                }
            }
            list.add(destination, draggedItem)
            return list
        }

        // Standard move for individual events or valid folder swaps
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        return list
    }

    /**
     * Reconstructs the complete list of [ScreenEvent] from the visible items.
     * Events positioned between an expanded [ScenarioListItem.FolderHeader] and
     * [ScenarioListItem.FolderEndBoundary] adopt that folder's name.
     * Events positioned outside any folder adopt null (ungrouped).
     * Collapsed folders atomically emit all of their member events.
     */
    fun reconstructEvents(
        visibleItems: List<ScenarioListItem>,
        allSourceEvents: List<UiImageEvent>,
    ): List<ScreenEvent> {
        val result = mutableListOf<ScreenEvent>()
        var currentFolder: String? = null
        val seenEventIds = mutableSetOf<Long>()
        val allEventsByFolder = allSourceEvents.groupBy { it.folder?.trim()?.ifEmpty { null } }

        for (item in visibleItems) {
            when (item) {
                is ScenarioListItem.FolderHeader -> {
                    if (!item.isExpanded) {
                        // Emit all events belonging to this collapsed folder atomically
                        val folderEvents = allEventsByFolder[item.name].orEmpty()
                        for (uiEvent in folderEvents) {
                            val idKey = uiEvent.event.id.let {
                                if (it.databaseId != 0L) it.databaseId else -requireNotNull(it.tempId)
                            }
                            if (seenEventIds.add(idKey)) {
                                result.add(uiEvent.event.copy(folder = item.name))
                            }
                        }
                        currentFolder = null
                    } else {
                        currentFolder = item.name
                    }
                }
                is ScenarioListItem.FolderEndBoundary -> {
                    currentFolder = null
                }
                is ScenarioListItem.EventItem -> {
                    val idKey = item.item.event.id.let {
                        if (it.databaseId != 0L) it.databaseId else -requireNotNull(it.tempId)
                    }
                    if (seenEventIds.add(idKey)) {
                        result.add(item.item.event.copy(folder = currentFolder))
                    }
                }
            }
        }

        // Safety fallback: any event from allSourceEvents not yet added is preserved
        for (uiEvent in allSourceEvents) {
            val idKey = uiEvent.event.id.let {
                if (it.databaseId != 0L) it.databaseId else -requireNotNull(it.tempId)
            }
            if (seenEventIds.add(idKey)) {
                result.add(uiEvent.event)
            }
        }

        return result
    }
}
