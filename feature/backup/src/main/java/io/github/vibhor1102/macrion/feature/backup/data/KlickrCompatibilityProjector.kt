/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.backup.data

import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.CompleteActionEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteEventEntity
import io.github.vibhor1102.macrion.core.database.entity.CompleteScenario
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
    id = "klickr-d466f1620c54",
    displayName = "Klick’r 4.0.1-1-gd466f162 (verification pending)",
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
            losses = dumbProjections.flatMap { it.losses } + smartProjections.flatMap { it.losses },
            excludedScenarioCount = dumbProjections.count { !it.isExportable } +
                smartProjections.count { !it.isExportable },
        )
    }

    fun projectSmartScenario(scenario: CompleteScenario): KlickrCompatibilityProjection<CompleteScenario> {
        val detached = scenario.detachedCopy()
        val losses = mutableListOf<KlickrCompatibilityLoss>()

        // Pass 1: Filter out action types unsupported by the target Klick'r profile
        var currentEvents = detached.events.map { event ->
            val curvedSwipeCount = event.actions.count { action ->
                action.action.type == ActionType.SWIPE && action.action.swipePath?.isCurved == true
            }
            if (curvedSwipeCount > 0) losses += KlickrCompatibilityLoss(
                reason = KlickrCompatibilityLossReason.CURVED_SWIPE,
                componentCount = curvedSwipeCount,
                scenarioId = detached.scenario.id,
            )
            val compatibleActions = event.actions.filterNot { action ->
                action.action.type == ActionType.EXTERNAL_ACTION ||
                    action.action.type == ActionType.PLAY_SOUND ||
                    action.action.type == ActionType.CAPTURE_SCREENSHOT ||
                    action.action.type == ActionType.SPLIT_ACTION ||
                    (action.action.type == ActionType.SWIPE && action.action.swipePath?.isCurved == true)
            }.map { action ->
                if (action.action.type == ActionType.SWIPE) action.copy(action = action.action.copy(swipePath = null))
                else action
            }
            val removedActionCount = event.actions.size - compatibleActions.size - curvedSwipeCount
            if (removedActionCount > 0) {
                losses += KlickrCompatibilityLoss(
                    reason = KlickrCompatibilityLossReason.UNSUPPORTED_COMPONENT,
                    componentCount = removedActionCount,
                    scenarioId = detached.scenario.id,
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
            val viableConditionIds = currentEvents
                .filter { it.actions.isNotEmpty() && it.conditions.isNotEmpty() }
                .flatMap { it.conditions }
                .map { it.id }
                .toSet()

            var changed = false
            val nextEvents = mutableListOf<CompleteEventEntity>()

            for (event in currentEvents) {
                val repairedActions = mutableListOf<CompleteActionEntity>()

                for (action in event.actions) {
                    when {
                        action.action.type == ActionType.TOGGLE_EVENT -> {
                            val survivingToggles = action.eventsToggle.filter { it.toggleEventId in viableEventIds }
                            val droppedToggles = action.eventsToggle.size - survivingToggles.size
                            if (droppedToggles > 0) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = droppedToggles,
                                    scenarioId = detached.scenario.id,
                                )
                                changed = true
                            }

                            if (action.action.toggleAll != true && survivingToggles.isEmpty()) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = 1,
                                    scenarioId = detached.scenario.id,
                                )
                                changed = true
                            } else {
                                repairedActions += action.copy(eventsToggle = survivingToggles)
                            }
                        }

                        action.action.type == ActionType.CLICK &&
                            action.action.clickPositionType == ClickPositionType.ON_DETECTED_CONDITION -> {
                            val condId = action.action.clickOnConditionId
                            if (condId == null || condId !in viableConditionIds) {
                                losses += KlickrCompatibilityLoss(
                                    reason = KlickrCompatibilityLossReason.BROKEN_REFERENCE,
                                    componentCount = 1,
                                    scenarioId = detached.scenario.id,
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

        val resultScenario = detached.copy(events = currentEvents)
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
        val conditionIds = scenario.events.flatMap { it.conditions }.map { it.id }.toSet()

        for (event in scenario.events) {
            if (event.actions.isEmpty()) {
                violations += "Event ${event.event.id} has no actions"
            }
            if (event.conditions.isEmpty()) {
                violations += "Event ${event.event.id} has no conditions"
            }
            for (action in event.actions) {
                if (action.action.type == ActionType.EXTERNAL_ACTION ||
                    action.action.type == ActionType.PLAY_SOUND ||
                    action.action.type == ActionType.CAPTURE_SCREENSHOT
                ) {
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
                    if (condId == null || condId !in conditionIds) {
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
        return KlickrCompatibilityProjection(
            value = detached.copy(dumbActions = compatibleActions, dumbActionsWithSubActions = emptyList()),
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
