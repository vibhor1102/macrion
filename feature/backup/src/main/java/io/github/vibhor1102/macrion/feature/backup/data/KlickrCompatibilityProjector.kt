/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.database.entity.CounterOperationValueType

import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionEntity
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType
import io.github.vibhor1102.macrion.core.dumb.data.database.DumbScenarioWithActions
import io.github.vibhor1102.macrion.feature.backup.data.base.BackupArchiveFormat

/** The Klick'r portable format targeted by compatible exports, including its verification status. */
internal data class KlickrCompatibilityProfile(
    val id: String,
    val displayName: String,
    val portableDatabaseVersion: Int,
    val crossReaderVerified: Boolean,
)

internal val CURRENT_KLICKR_COMPATIBILITY_PROFILE = KlickrCompatibilityProfile(
    id = "klickr-legacy-smart23-simple1",
    displayName = "Klick’r 4.0–4.1 legacy format (verification pending)",
    portableDatabaseVersion = 23,
    crossReaderVerified = false,
)

internal enum class KlickrCompatibilityLossReason {
    UNSUPPORTED_COMPONENT,
    CURVED_SWIPE,
    BROKEN_REFERENCE,
    MEANINGLESS_EVENT,
    MEANINGLESS_SCENARIO,
}

internal data class KlickrCompatibilityLoss(
    val reason: KlickrCompatibilityLossReason,
    val componentCount: Int,
    val scenarioId: Long,
    val scenarioName: String = "",
    val componentNames: List<String> = emptyList(),
)

internal data class KlickrCompatibilityProjection<T>(
    val value: T?,
    val losses: List<KlickrCompatibilityLoss> = emptyList(),
) {
    val isExportable: Boolean get() = value != null
}

internal data class BackupExportPlan(
    val dumbScenarios: List<DumbScenarioWithActions>,
    val smartScenarios: List<CompleteScenario>,
    val format: BackupArchiveFormat,
    val profile: KlickrCompatibilityProfile? = null,
    val losses: List<KlickrCompatibilityLoss> = emptyList(),
    val excludedScenarioCount: Int = 0,
) {
    val omittedComponentCount: Int = losses.sumOf(KlickrCompatibilityLoss::componentCount)
    val omittedCurvedSwipeCount: Int = losses.filter { it.reason == KlickrCompatibilityLossReason.CURVED_SWIPE }
        .sumOf(KlickrCompatibilityLoss::componentCount)
}

/**
 * Creates the subset that the supported Klick'r backup format can represent.
 * At database version 23, Macrion and Klick'r still share the same scenario model.
 */
internal object KlickrCompatibilityProjector {

    fun createPlan(
        dumbScenarios: List<DumbScenarioWithActions>,
        smartScenarios: List<CompleteScenario>,
    ): BackupExportPlan {
        val dumbProjections = dumbScenarios.map(::projectDumbScenario)
        val smartProjections = smartScenarios.map(::projectSmartScenario)

        return BackupExportPlan(
            dumbScenarios = dumbProjections.mapNotNull { it.value },
            smartScenarios = smartProjections.mapNotNull { it.value },
            format = BackupArchiveFormat.KLICKR_COMPATIBLE,
            profile = CURRENT_KLICKR_COMPATIBILITY_PROFILE,
            losses = dumbProjections.flatMapIndexed { index, projection ->
                projection.losses.map { it.copy(scenarioName = dumbScenarios[index].scenario.name) }
            } + smartProjections.flatMapIndexed { index, projection ->
                projection.losses.map { it.copy(scenarioName = smartScenarios[index].scenario.name) }
            },
            excludedScenarioCount = dumbProjections.count { !it.isExportable } +
                smartProjections.count { !it.isExportable },
        )
    }

    fun projectSmartScenario(scenario: CompleteScenario): KlickrCompatibilityProjection<CompleteScenario> {
        val detached = scenario.detachedCopy().let { it.copy(events = it.events.map(CompleteEventEntity::normalizeIgnoredClickTargets)) }
        val losses = mutableListOf<KlickrCompatibilityLoss>()

        val folderNames = (detached.scenario.folders.map { it.name } + detached.events.mapNotNull { it.event.folderName }).distinct()
        if (folderNames.isNotEmpty()) losses += KlickrCompatibilityLoss(
            reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
            componentCount = folderNames.size,
            scenarioId = detached.scenario.id,
            componentNames = folderNames,
        )

        // Pass 1: Filter out action types unsupported by the target Klick'r profile
        var currentEvents = detached.events.mapNotNull { event ->
            val rateLimited = event.conditions.filter { it.computeRate > 0.0 }
            if (rateLimited.isNotEmpty()) {
                // Ignoring a condition's evaluation rate can change when the event executes.
                // Omit the whole event rather than weakening its conditions or silently changing its cadence.
                losses += KlickrCompatibilityLoss(
                    reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                    componentCount = rateLimited.size,
                    scenarioId = detached.scenario.id,
                    componentNames = rateLimited.map { "${event.event.name} → ${it.name}" },
                )
                losses += KlickrCompatibilityLoss(
                    reason = KlickrCompatibilityLossReason.MEANINGLESS_EVENT,
                    componentCount = 1,
                    scenarioId = detached.scenario.id,
                    componentNames = listOf(event.event.name),
                )
                return@mapNotNull null
            }
            val curvedSwipeCount = event.actions.count { action ->
                action.action.type == ActionType.SWIPE && action.action.swipePath?.isCurved == true
            }
            if (curvedSwipeCount > 0) losses += KlickrCompatibilityLoss(
                reason = KlickrCompatibilityLossReason.CURVED_SWIPE,
                componentCount = curvedSwipeCount,
                scenarioId = detached.scenario.id,
                componentNames = event.actions.filter { it.action.type == ActionType.SWIPE && it.action.swipePath?.isCurved == true }.map { it.action.name },
            )
            val (removedActions, retainedActions) = event.actions.partition { it.action.isUnsupportedByLegacyKlickr() }
            val compatibleActions = retainedActions.map { complete ->
                // These fields are either unused by this action or already excluded above.
                complete.copy(action = complete.action.copy(
                    swipePath = null, positionCounters = null, fromCounters = null, toCounters = null,
                    counterOperationConditionId = null, counterOperationConditionPositionType = null,
                    counterOperationValueType = complete.action.counterOperationValueType.takeIf { complete.action.type == ActionType.CHANGE_COUNTER },
                ))
            }
            val unsupported = removedActions.filterNot { it.action.type == ActionType.SWIPE && it.action.swipePath?.isCurved == true }
            if (unsupported.isNotEmpty()) {
                losses += KlickrCompatibilityLoss(
                    reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                    componentCount = unsupported.size,
                    scenarioId = detached.scenario.id,
                    componentNames = unsupported.map { "${event.event.name} → ${it.action.name}" },
                )
            }
            event.copy(actions = compatibleActions)
        }

        // Pass 2: Multi-pass pruning to maintain referential integrity and prune dead components
        while (true) {
            val viableEventIds = currentEvents
                .filter { it.actions.isNotEmpty() && it.conditions.isNotEmpty() }
                .map { it.event.id }
                .toSet()

            var changed = false
            val nextEvents = mutableListOf<CompleteEventEntity>()

            for (event in currentEvents) {
                val repairedActions = mutableListOf<CompleteActionEntity>()

                for (action in event.actions) {
                    when {
                        action.action.type == ActionType.TOGGLE_EVENT && action.action.toggleAll == true ->
                            repairedActions += action.copy(eventsToggle = emptyList())

                        action.action.type == ActionType.TOGGLE_EVENT -> {
                            val survivingToggles = action.eventsToggle.filter { it.toggleEventId in viableEventIds }
                            val droppedToggles = action.eventsToggle.size - survivingToggles.size
                            if (droppedToggles > 0) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = droppedToggles,
                                    scenarioId = detached.scenario.id,
                                    componentNames = listOf("${event.event.name} → ${action.action.name}"),
                                )
                                changed = true
                            }

                            if (action.action.toggleAll != true && survivingToggles.isEmpty()) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = 1,
                                    scenarioId = detached.scenario.id,
                                    componentNames = listOf("${event.event.name} → ${action.action.name}"),
                                )
                                changed = true
                            } else {
                                repairedActions += action.copy(eventsToggle = survivingToggles)
                            }
                        }

                        action.action.type == ActionType.CLICK &&
                            action.action.clickPositionType == ClickPositionType.ON_DETECTED_CONDITION -> {
                            if (!event.hasValidClickTarget(action.action.clickPositionType, action.action.clickOnConditionId)) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = 1,
                                    scenarioId = detached.scenario.id,
                                    componentNames = listOf("${event.event.name} → ${action.action.name}"),
                                )
                                changed = true
                            } else {
                                repairedActions += action
                            }
                        }

                        else -> repairedActions += action
                    }
                }

                if (repairedActions.isEmpty() || event.conditions.isEmpty()) {
                    losses += KlickrCompatibilityLoss(
                        reason = KlickrCompatibilityLossReason.MEANINGLESS_EVENT,
                        componentCount = 1,
                        scenarioId = detached.scenario.id,
                        componentNames = listOf(event.event.name),
                    )
                    changed = true
                } else {
                    val actionsWithReindexedPriority = repairedActions.mapIndexed { index, act ->
                        if (act.action.priority == index) act
                        else act.copy(action = act.action.copy(priority = index))
                    }
                    nextEvents += event.copy(actions = actionsWithReindexedPriority)
                }
            }

            currentEvents = nextEvents
            if (!changed) break
        }

        if (currentEvents.isEmpty()) {
            losses += KlickrCompatibilityLoss(
                reason = KlickrCompatibilityLossReason.MEANINGLESS_SCENARIO,
                componentCount = 1,
                scenarioId = detached.scenario.id,
            )
            return KlickrCompatibilityProjection(value = null, losses = losses)
        }

        val pauseIds = ExportPauseIds(detached.events.flatMap { it.actions }.map { it.action.id })
        val resultScenario = detached.copy(events = currentEvents.map { event ->
            event.copy(actions = event.actions.flatMap { action ->
                if (action.action.type != ActionType.CLICK && action.action.type != ActionType.SWIPE) {
                    listOf(action)
                } else buildList {
                    fun pause(duration: Long) = CompleteActionEntity(
                        action = ActionEntity(
                            id = pauseIds.next(), eventId = action.action.eventId, name = "Pause",
                            type = ActionType.PAUSE, pauseDuration = duration,
                        ),
                        intentExtras = emptyList(), eventsToggle = emptyList(),
                    )
                    action.action.waitBeforeMs?.takeIf { it > 0 }?.let { add(pause(it)) }
                    add(action.copy(action = action.action.copy(waitBeforeMs = null, waitAfterMs = null)))
                    action.action.waitAfterMs?.takeIf { it > 0 }?.let { add(pause(it)) }
                }
            }.mapIndexed { index, action -> action.copy(action = action.action.copy(priority = index)) })
        })
        val violations = validateReferentialIntegrity(resultScenario)
        check(violations.isEmpty()) {
            "Klick'r compatibility projection produced invalid referential integrity: $violations"
        }

        return KlickrCompatibilityProjection(
            value = resultScenario,
            losses = losses,
        )
    }

    fun validateReferentialIntegrity(scenario: CompleteScenario): List<String> {
        val violations = mutableListOf<String>()
        val eventIds = scenario.events.map { it.event.id }.toSet()

        for (event in scenario.events) {
            if (event.actions.isEmpty()) {
                violations += "Event ${event.event.id} has no actions"
            }
            if (event.conditions.isEmpty()) {
                violations += "Event ${event.event.id} has no conditions"
            }
            if (event.conditions.any { it.computeRate > 0.0 }) {
                violations += "Event ${event.event.id} uses unsupported condition scan rates"
            }
            for (action in event.actions) {
                if (action.action.isUnsupportedByLegacyKlickr()) {
                    violations += "Action ${action.action.id} has unsupported type ${action.action.type}"
                }
                if (action.action.type == ActionType.TOGGLE_EVENT && action.action.toggleAll != true) {
                    if (action.eventsToggle.isEmpty()) {
                        violations += "Toggle action ${action.action.id} in event ${event.event.id} has no targets"
                    }
                    for (toggle in action.eventsToggle) {
                        if (toggle.toggleEventId !in eventIds) {
                            violations += "Toggle ${toggle.id} in action ${action.action.id} references missing event ${toggle.toggleEventId}"
                        }
                    }
                }
                if (action.action.type == ActionType.CLICK && action.action.clickPositionType == ClickPositionType.ON_DETECTED_CONDITION) {
                    val condId = action.action.clickOnConditionId
                    if (!event.hasValidClickTarget(action.action.clickPositionType, condId)) {
                        violations += "Click action ${action.action.id} references missing condition $condId"
                    }
                }
            }
        }
        return violations
    }

    fun projectDumbScenario(scenario: DumbScenarioWithActions): KlickrCompatibilityProjection<DumbScenarioWithActions> {
        val detached = scenario.detachedCopy()
        val losses = mutableListOf<KlickrCompatibilityLoss>()
        val curvedSwipeCount = detached.dumbActions.count { action ->
            action.type == io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType.SWIPE &&
                action.swipePath?.isCurved == true
        }
        if (curvedSwipeCount > 0) losses += KlickrCompatibilityLoss(
            reason = KlickrCompatibilityLossReason.CURVED_SWIPE,
            componentCount = curvedSwipeCount,
            scenarioId = detached.scenario.id,
        )
        val compatibleActions = detached.dumbActions.filterNot {
            it.type == io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType.SPLIT_ACTION ||
                (it.type == io.github.vibhor1102.macrion.core.dumb.data.database.DumbActionType.SWIPE && it.swipePath?.isCurved == true)
        }.map { it.copy(swipePath = null) }
        val removed = detached.dumbActions.size - compatibleActions.size - curvedSwipeCount
        if (removed > 0) {
            losses += KlickrCompatibilityLoss(
                reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                componentCount = removed,
                scenarioId = detached.scenario.id,
            )
        }
        if (compatibleActions.isEmpty()) {
            losses += KlickrCompatibilityLoss(
                reason = KlickrCompatibilityLossReason.MEANINGLESS_SCENARIO,
                componentCount = 1,
                scenarioId = detached.scenario.id,
            )
            return KlickrCompatibilityProjection(value = null, losses = losses)
        }
        val pauseIds = ExportPauseIds(detached.dumbActions.map { it.id })
        val timedActions = compatibleActions.flatMap { action ->
            if (action.type != DumbActionType.CLICK && action.type != DumbActionType.SWIPE) listOf(action)
            else buildList {
                fun pause(duration: Long) = DumbActionEntity(
                    id = pauseIds.next(), dumbScenarioId = action.dumbScenarioId,
                    name = "Pause", type = DumbActionType.PAUSE, pauseDuration = duration,
                )
                action.waitBeforeMs?.takeIf { it > 0 }?.let { add(pause(it)) }
                add(action.copy(waitBeforeMs = null, waitAfterMs = null))
                action.waitAfterMs?.takeIf { it > 0 }?.let { add(pause(it)) }
            }
        }.mapIndexed { index, action -> action.copy(priority = index) }
        return KlickrCompatibilityProjection(
            value = detached.copy(dumbActions = timedActions, dumbActionsWithSubActions = emptyList()),
            losses = losses,
        )
    }
}

private fun CompleteScenario.detachedCopy(): CompleteScenario = copy(
    scenario = scenario.copy(),
    events = events.map { event ->
        event.copy(
            event = event.event.copy(),
            actions = event.actions.map { action ->
                action.copy(
                    action = action.action.copy(),
                    intentExtras = action.intentExtras.map { it.copy() },
                    eventsToggle = action.eventsToggle.map { it.copy() },
                )
            },
            conditions = event.conditions.map { it.copy() },
        )
    },
    counters = counters.map { it.copy() },
)

private fun DumbScenarioWithActions.detachedCopy(): DumbScenarioWithActions = copy(
    scenario = scenario.copy(),
    dumbActions = dumbActions.map { it.copy() },
    stats = stats?.copy(),
    dumbActionsWithSubActions = dumbActionsWithSubActions.map { action ->
        action.copy(action = action.action.copy(), splitItems = action.splitItems.map { it.copy() })
    },
)

/** Pause IDs must not collide with retained or subsequently pruned source actions. */
private class ExportPauseIds(ids: List<Long>) {
    private val occupied = ids.toMutableSet()
    private var candidate = -1L

    fun next(): Long {
        while (candidate in occupied) candidate--
        return candidate.also { occupied += it }
    }
}
