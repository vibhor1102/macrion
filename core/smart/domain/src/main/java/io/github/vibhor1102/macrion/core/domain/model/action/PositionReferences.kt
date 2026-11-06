package io.github.vibhor1102.macrion.core.domain.model.action

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.domain.model.counter.CounterOperationValue

fun Action.positionCounterNames(): List<String> = when (this) {
    is Click -> positionCounters?.names().orEmpty()
    is Swipe -> fromCounters?.names().orEmpty() + toCounters?.names().orEmpty()
    is SplitAction -> subActions.flatMap { it.positionCounterNames() }
    else -> emptyList()
}

fun Action.renamePositionCounter(old: String, new: String): Action = when (this) {
    is Click -> copy(positionCounters = positionCounters?.renamed(old, new))
    is Swipe -> copy(fromCounters = fromCounters?.renamed(old, new), toCounters = toCounters?.renamed(old, new))
    is SplitAction -> copy(subActions = subActions.map { it.renamePositionCounter(old, new) })
    else -> this
}

fun Action.referencedPositionConditions(): List<Identifier> = when (this) {
    is Click -> listOfNotNull(clickOnConditionId.takeIf { positionType == Click.PositionType.ON_DETECTED_CONDITION })
    is ChangeCounter -> listOfNotNull((operationValue as? CounterOperationValue.ConditionPosition)?.conditionId)
    is SplitAction -> subActions.flatMap { it.referencedPositionConditions() }
    else -> emptyList()
}

fun Action.replacePositionCondition(old: Identifier, replacement: Identifier): Action = when (this) {
    is Click -> if (clickOnConditionId == old) copy(clickOnConditionId = replacement) else this
    is ChangeCounter -> (operationValue as? CounterOperationValue.ConditionPosition)?.let {
        if (it.conditionId == old) copy(operationValue = it.copy(conditionId = replacement)) else this
    } ?: this
    is SplitAction -> copy(subActions = subActions.map { it.replacePositionCondition(old, replacement) })
    else -> this
}
