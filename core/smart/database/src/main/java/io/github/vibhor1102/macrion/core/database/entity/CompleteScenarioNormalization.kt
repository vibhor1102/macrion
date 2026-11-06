/* Copyright (C) 2026 Vibhor Goel */
package io.github.vibhor1102.macrion.core.database.entity

private val counterReferenceRegex = Regex("\\{([^}]+)\\}")

/** Applies the same name invariant used by database migrations to imported scenarios. */
fun CompleteScenario.normalizeUserEnteredNames(): CompleteScenario {
    val usedCounterNames = mutableSetOf<String>()
    val counterRenames = counters
        .sortedWith(compareBy<CountersEntity> { it.name != it.name.trim() }.thenBy { it.name })
        .associate { counter ->
            val base = counter.name.trim().ifEmpty { "Counter" }
            var normalized = base
            var suffix = 2
            while (!usedCounterNames.add(normalized)) normalized = "$base (${suffix++})"
            counter.name to normalized
        }

    return copy(
        scenario = scenario.copy(name = scenario.name.trim()),
        counters = counters.map { counter -> counter.copy(name = counterRenames.getValue(counter.name)) },
        events = events.map { completeEvent ->
            completeEvent.copy(
                event = completeEvent.event.copy(name = completeEvent.event.name.trim()),
                actions = completeEvent.actions.map { completeAction ->
                    val action = completeAction.action
                    completeAction.copy(action = action.copy(
                        name = action.name.trim(),
                        positionCounters = action.positionCounters.renamedBy(counterRenames),
                        fromCounters = action.fromCounters.renamedBy(counterRenames),
                        toCounters = action.toCounters.renamedBy(counterRenames),
                        counterName = action.counterName.renamedBy(counterRenames),
                        counterOperationCounterName = action.counterOperationCounterName.renamedBy(counterRenames),
                        notificationMessageText = action.notificationMessageText.renameCounterReferences(counterRenames),
                        textValue = action.textValue.renameCounterReferences(counterRenames),
                        externalActionName = action.externalActionName?.trim(),
                    ), splitItems = completeAction.splitItems.map { item -> item.copy(
                        positionCounters = item.positionCounters.renamedBy(counterRenames),
                        fromCounters = item.fromCounters.renamedBy(counterRenames),
                        toCounters = item.toCounters.renamedBy(counterRenames),
                    ) })
                },
                conditions = completeEvent.conditions.map { condition -> condition.copy(
                    name = condition.name.trim(),
                    counterName = condition.counterName.renamedBy(counterRenames),
                    counterOperationCounterName = condition.counterOperationCounterName.renamedBy(counterRenames),
                    numberCounterOperationCounterName = condition.numberCounterOperationCounterName.renamedBy(counterRenames),
                ) },
            )
        },
    )
}

private fun String?.renamedBy(renames: Map<String, String>): String? =
    this?.let { renames[it] ?: it }

private fun String?.renameCounterReferences(renames: Map<String, String>): String? =
    this?.replace(counterReferenceRegex) { match ->
        "{${renames[match.groupValues[1]] ?: match.groupValues[1]}}"
    }

private fun PositionCountersEntity?.renamedBy(renames: Map<String, String>): PositionCountersEntity? =
    this?.copy(x = x.renamedBy(renames), y = y.renamedBy(renames))
