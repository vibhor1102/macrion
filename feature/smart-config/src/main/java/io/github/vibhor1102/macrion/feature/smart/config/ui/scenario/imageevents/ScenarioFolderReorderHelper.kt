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
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.event.UiImageEvent

/**
 * Items displayed in the scenario events list.
 */
sealed class ScenarioListItem {
    abstract val key: Any
    val summary: String get() = when (this) {
        is FolderHeader -> "Header($name, count=$totalCount, expanded=$isExpanded)"
        is FolderEndBoundary -> "EndBoundary($folderName)"
        is EventItem -> "Event(${item.name}, folder=$folderName)"
    }

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

internal object ReorderLog {
    fun d(msg: String) {
        try {
            android.util.Log.d("MacrionReorder", msg)
        } catch (_: Throwable) {
            println("[MacrionReorder] $msg")
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
        customFolders: List<String>,
        collapsedFolders: Set<String>,
    ): List<ScenarioListItem> {
        val result = mutableListOf<ScenarioListItem>()
        val processedFolders = mutableSetOf<String>()

        for (event in events) {
            val folder = event.folder?.trim()?.ifEmpty { null }
            if (folder == null) {
                result.add(ScenarioListItem.EventItem(event, null))
            } else {
                if (processedFolders.add(folder)) {
                    val folderEvents = events.filter { it.folder?.trim() == folder }
                    val isExpanded = folder !in collapsedFolders
                    result.add(
                        ScenarioListItem.FolderHeader(
                            name = folder,
                            totalCount = folderEvents.size,
                            enabledCount = folderEvents.count { it.event.enabledOnStart },
                            isExpanded = isExpanded,
                        )
                    )
                    if (isExpanded) {
                        for (fe in folderEvents) {
                            result.add(ScenarioListItem.EventItem(fe, folder))
                        }
                        result.add(ScenarioListItem.FolderEndBoundary(folder))
                    }
                }
            }
        }

        // Add any empty custom folders
        for (cf in customFolders) {
            if (processedFolders.add(cf)) {
                val isExpanded = cf !in collapsedFolders
                result.add(
                    ScenarioListItem.FolderHeader(
                        name = cf,
                        totalCount = 0,
                        enabledCount = 0,
                        isExpanded = isExpanded,
                    )
                )
                if (isExpanded) {
                    result.add(ScenarioListItem.FolderEndBoundary(cf))
                }
            }
        }

        ReorderLog.d("[BuildVisible] sourceEvents=${events.size}, customFolders=$customFolders, collapsed=$collapsedFolders -> ${result.size} visible items: ${result.map { it.summary }}")
        return result
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
        ReorderLog.d("[MoveItem] fromIdx=$fromIndex (${draggedItem.summary}) -> toIdx=$toIndex (${list[toIndex].summary})")

        // Rule for moving a Collapsed FolderHeader:
        // A collapsed folder header must never be placed inside an expanded folder.
        if (draggedItem is ScenarioListItem.FolderHeader && !draggedItem.isExpanded) {
            val destinationFolder = getEffectiveFolderAt(list, toIndex)
            if (destinationFolder != null) {
                // Moving downward into an expanded folder -> jump past its end boundary
                if (toIndex > fromIndex) {
                    val boundaryIndex = list.indexOfFirst {
                        it is ScenarioListItem.FolderEndBoundary && it.folderName == destinationFolder
                    }
                    if (boundaryIndex != -1) {
                        ReorderLog.d("[MoveItemRule] Collapsed folder '${draggedItem.name}' jumped to boundaryIdx=$boundaryIndex")
                        val item = list.removeAt(fromIndex)
                        list.add(boundaryIndex, item)
                        return list
                    }
                } else {
                    // Moving upward into an expanded folder -> jump above its header
                    val headerIndex = list.indexOfFirst {
                        it is ScenarioListItem.FolderHeader && it.name == destinationFolder
                    }
                    if (headerIndex != -1) {
                        ReorderLog.d("[MoveItemRule] Collapsed folder '${draggedItem.name}' jumped to headerIdx=$headerIndex")
                        val item = list.removeAt(fromIndex)
                        list.add(headerIndex, item)
                        return list
                    }
                }
            }
        }

        // Standard move for individual events or valid folder swaps
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        ReorderLog.d("[MoveItemResult] List order: ${list.map { it.summary }}")
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

        ReorderLog.d("[ReconstructEvents] Reconstructed ${result.size} events: ${result.map { "${it.name}(folder=${it.folder})" }}")
        return result
    }
}
