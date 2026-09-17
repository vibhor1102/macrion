/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.feature.smart.config.data

import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.scenario.ScenarioFolder

internal data class ScenarioEventLayout(val events: List<ScreenEvent>, val folders: List<ScenarioFolder>)

/** Supply positions for membership-only backups made before folders were persisted separately. */
internal fun completeFolders(events: List<ScreenEvent>, folders: List<ScenarioFolder>): List<ScenarioFolder> {
    val known = folders.mapTo(mutableSetOf()) { it.name }
    return (folders + events.mapIndexedNotNull { index, event ->
        event.folder?.takeIf { known.add(it) }?.let { ScenarioFolder(it, index) }
    }).sortedBy { it.eventIndex }
}

/** Keep folder positions through event editor additions, deletions and membership changes. */
internal fun reconcileEventLayout(
    before: List<ScreenEvent>,
    folders: List<ScenarioFolder>,
    after: List<ScreenEvent>,
): ScenarioEventLayout {
    val updatedById = after.associateBy { it.id }
    val retained = before.filter { old -> updatedById[old.id]?.let { it.folder == old.folder } == true }
    val retainedIds = retained.mapTo(mutableSetOf()) { it.id }
    val result = retained.map { updatedById.getValue(it.id) }.toMutableList()
    var positions = completeFolders(before, folders).map { folder ->
        folder.copy(eventIndex = before.take(folder.eventIndex.coerceAtLeast(0)).count { it.id in retainedIds })
    }
    for (event in after.filter { it.id !in retainedIds }) {
        val folderIndex = positions.indexOfFirst { it.name == event.folder }
        if (folderIndex < 0) {
            event.folder?.let { positions = positions + ScenarioFolder(it, result.size) }
            result.add(event)
        } else {
            val insertion = (positions[folderIndex].eventIndex + result.count { it.folder == event.folder })
                .coerceIn(0, result.size)
            result.add(insertion, event)
            positions = positions.mapIndexed { index, folder ->
                if (index > folderIndex) folder.copy(eventIndex = folder.eventIndex + 1) else folder
            }
        }
    }
    return ScenarioEventLayout(result.mapIndexed { index, event -> event.copy(priority = index) }, positions)
}
