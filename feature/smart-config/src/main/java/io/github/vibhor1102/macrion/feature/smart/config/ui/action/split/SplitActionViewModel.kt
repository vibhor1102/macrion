/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.split

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.SplitAction
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.utils.getEventConfigPreferences
import io.github.vibhor1102.macrion.feature.smart.config.utils.putClickPressDurationConfig
import io.github.vibhor1102.macrion.feature.smart.config.utils.putSwipeDurationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SplitActionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val configuredSplit = editionRepository.editionState.editedActionState
        .mapNotNull { it.value }
        .filterIsInstance<SplitAction>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000.milliseconds)

    val uiState: StateFlow<SplitActionUiState?> = combine(
        configuredSplit,
        editionRepository.editionState.editedActionState,
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedEventScreenConditionsState,
    ) { split, actionState, eventState, conditionsState ->
        split.toUiState(
            context = context,
            hasUnsavedModifications = actionState.hasChanged,
            canBeSaved = actionState.canBeSaved,
            event = eventState.value,
            availableConditions = conditionsState.value.orEmpty().filter { it.shouldBeDetected }
                .map { it.toUiScreenCondition(context, shortThreshold = true, inError = !it.isComplete()) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getEditedSplit(): SplitAction? =
        editionRepository.editionState.getEditedAction<SplitAction>()

    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<SplitAction>()?.let { split ->
            editionRepository.updateEditedAction(split.copy(name = name))
        }
    }

    fun addSwipe(): Int? {
        editionRepository.editionState.getEditedAction<SplitAction>()?.let { split ->
            if (split.subActions.size >= 10) return null
            val eventId = split.eventId
            val subIndex = split.subActions.size
            val duration = editionRepository.editedItemsBuilder.defaultValues.swipeDuration(context)
            val newSwipe = Swipe(
                id = editionRepository.editedItemsBuilder.actionsIdCreator.generateNewIdentifier(),
                eventId = eventId,
                name = context.getString(R.string.item_swipe_title) + " ${subIndex + 1}",
                from = null,
                to = null,
                swipeDuration = duration,
                priority = subIndex,
            )
            editionRepository.addSubAction(newSwipe)
            return subIndex
        }
        return null
    }

    fun addClick(): Int? {
        editionRepository.editionState.getEditedAction<SplitAction>()?.let { split ->
            if (split.subActions.size >= 10) return null
            val eventId = split.eventId
            val subIndex = split.subActions.size
            val duration = editionRepository.editedItemsBuilder.defaultValues.clickPressDuration(context)
            val newClick = Click(
                id = editionRepository.editedItemsBuilder.actionsIdCreator.generateNewIdentifier(),
                eventId = eventId,
                name = context.getString(R.string.item_click_title) + " ${subIndex + 1}",
                pressDuration = duration,
                positionType = Click.PositionType.USER_SELECTED,
                position = null,
                priority = subIndex,
            )
            editionRepository.addSubAction(newClick)
            return subIndex
        }
        return null
    }

    fun removeSubAction(index: Int) {
        editionRepository.removeSubAction(index)
    }

    fun removeSubActionByKey(key: String) {
        val index = getEditedSplit()?.subActions?.indexOfFirst { it.id.toString() == key } ?: return
        if (index >= 0) editionRepository.removeSubAction(index)
    }

    fun updateSubAction(index: Int, change: (Action) -> Action) {
        val parent = getEditedSplit() ?: return
        val child = parent.subActions.getOrNull(index) ?: return
        val updated = change(child)
        if (updated.id != child.id || updated == child) return
        editionRepository.updateEditedAction(parent.copy(
            subActions = parent.subActions.toMutableList().apply { set(index, updated) },
        ))
    }

    fun updateSubAction(key: String, change: (Action) -> Action) {
        val index = getEditedSplit()?.subActions?.indexOfFirst { it.id.toString() == key } ?: return
        if (index >= 0) updateSubAction(index, change)
    }

    fun unsplit() {
        editionRepository.editionState.getEditedAction<SplitAction>()?.let { split ->
            editionRepository.unsplitAction(split)
        }
    }

    fun save() {
        editionRepository.upsertEditedAction()
    }

    fun saveLastChildDurations() {
        val children = getEditedSplit()?.subActions ?: return
        context.getEventConfigPreferences().edit {
            children.filterIsInstance<Click>().lastOrNull()?.pressDuration?.let { putClickPressDurationConfig(it) }
            children.filterIsInstance<Swipe>().lastOrNull()?.swipeDuration?.let { putSwipeDurationConfig(it) }
        }
    }

    fun delete() {
        editionRepository.deleteEditedAction()
    }

    fun dismiss() {
        editionRepository.stopActionEdition()
    }

    private fun SplitAction.toUiState(
        context: Context,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
        event: Event?,
        availableConditions: List<UiScreenCondition>,
    ): SplitActionUiState {
        val items = subActions.mapIndexed { index, subAction ->
            when (subAction) {
                is Swipe -> {
                    val hasPos = subAction.from != null && subAction.to != null
                    val details = if (hasPos) {
                        val base = context.getString(
                            R.string.field_swipe_positions_desc,
                            subAction.from!!.x,
                            subAction.from!!.y,
                            subAction.to!!.x,
                            subAction.to!!.y,
                        ) + " • ${subAction.swipeDuration ?: 0}ms"
                        val delays = subAction.delaysSummary()
                        if (delays.isNotEmpty()) "$base ($delays)" else base
                    } else {
                        context.getString(R.string.split_action_stroke_not_configured)
                    }
                    SubActionItemUiState(
                        index = index,
                        name = subAction.name ?: "${context.getString(R.string.item_swipe_title)} ${index + 1}",
                        details = details,
                        icon = UiR.drawable.ic_swipe,
                        isComplete = SplitAction.isSubActionComplete(subAction),
                        action = subAction,
                    )
                }
                is Click -> {
                    val hasPos = subAction.position != null || subAction.positionType == Click.PositionType.ON_DETECTED_CONDITION
                    val details = if (hasPos) {
                        val base = if (subAction.positionType == Click.PositionType.ON_DETECTED_CONDITION)
                            context.getString(R.string.split_action_detected_position)
                        else "(${subAction.position!!.x}, ${subAction.position!!.y}) • ${subAction.pressDuration ?: 0}ms"
                        val delays = subAction.delaysSummary()
                        if (delays.isNotEmpty()) "$base ($delays)" else base
                    } else {
                        context.getString(R.string.split_action_stroke_not_configured)
                    }
                    SubActionItemUiState(
                        index = index,
                        name = subAction.name ?: "${context.getString(R.string.item_click_title)} ${index + 1}",
                        details = details,
                        icon = UiR.drawable.ic_click,
                        isComplete = SplitAction.isSubActionComplete(subAction),
                        action = subAction,
                    )
                }
                else -> {
                    SubActionItemUiState(
                        index = index,
                        name = subAction.name ?: "Action ${index + 1}",
                        details = "",
                        icon = UiR.drawable.ic_swipe,
                        isComplete = SplitAction.isSubActionComplete(subAction),
                        action = subAction,
                    )
                }
            }
        }

        return SplitActionUiState(
            name = name.orEmpty(),
            nameError = name.isNullOrBlank(),
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasUnsavedModifications,
            subActions = items,
            durationMs = subActions.maxOfOrNull { child ->
                when (child) {
                    is Click -> (child.waitBeforeMs ?: 0L) + (child.pressDuration ?: 0L) + (child.waitAfterMs ?: 0L)
                    is Swipe -> (child.waitBeforeMs ?: 0L) + (child.swipeDuration ?: 0L) + (child.waitAfterMs ?: 0L)
                    else -> 0L
                }
            } ?: 0L,
            canDeleteSubAction = subActions.size > 2,
            canUnsplit = editionRepository.editionState.getEditedEventActions<Action>()?.any { it.id == id } == true,
            event = event,
            availableConditions = availableConditions,
        )
    }

    private fun Swipe.delaysSummary(): String = buildString {
        val wb = waitBeforeMs
        val wa = waitAfterMs
        if (wb != null && wb > 0) append("Wait before: ${wb}ms")
        if (wa != null && wa > 0) {
            if (isNotEmpty()) append(" • ")
            append("Wait after: ${wa}ms")
        }
    }

    private fun Click.delaysSummary(): String = buildString {
        val wb = waitBeforeMs
        val wa = waitAfterMs
        if (wb != null && wb > 0) append("Wait before: ${wb}ms")
        if (wa != null && wa > 0) {
            if (isNotEmpty()) append(" • ")
            append("Wait after: ${wa}ms")
        }
    }
}
