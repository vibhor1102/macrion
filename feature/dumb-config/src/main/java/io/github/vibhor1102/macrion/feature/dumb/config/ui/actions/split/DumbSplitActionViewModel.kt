/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.split

import android.content.Context
import android.graphics.Point
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.ui.utils.formatDuration
import io.github.vibhor1102.macrion.feature.dumb.config.R
import io.github.vibhor1102.macrion.feature.dumb.config.domain.DumbEditionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class DumbSplitActionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val _editedDumbSplit: MutableStateFlow<DumbAction.DumbSplitAction?> = MutableStateFlow(null)
    private var initialSplit: DumbAction.DumbSplitAction? = null

    val uiState: StateFlow<DumbSplitActionUiState?> = _editedDumbSplit
        .map { split ->
            split?.toUiState(context)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEditedDumbSplit(split: DumbAction.DumbSplitAction) {
        if (initialSplit != null) return
        initialSplit = split.copy()
        _editedDumbSplit.value = split.copy()
    }

    fun getEditedDumbSplit(): DumbAction.DumbSplitAction? = _editedDumbSplit.value

    fun hasUnsavedModifications(): Boolean =
        _editedDumbSplit.value != initialSplit

    fun setName(name: String) {
        _editedDumbSplit.value = _editedDumbSplit.value?.copy(name = name)
    }

    fun setRepeatCount(repeatCount: Int) {
        _editedDumbSplit.value = _editedDumbSplit.value?.copy(repeatCount = repeatCount)
    }

    fun toggleInfiniteRepeat() {
        val current = _editedDumbSplit.value ?: return
        _editedDumbSplit.value = current.copy(isRepeatInfinite = !current.isRepeatInfinite)
    }

    fun setRepeatDelay(delayMs: Long) {
        _editedDumbSplit.value = _editedDumbSplit.value?.copy(repeatDelayMs = delayMs)
    }

    fun setWaitBeforeMs(waitBefore: Long?) {
        _editedDumbSplit.value = _editedDumbSplit.value?.copy(waitBeforeMs = waitBefore)
    }

    fun setWaitAfterMs(waitAfter: Long?) {
        _editedDumbSplit.value = _editedDumbSplit.value?.copy(waitAfterMs = waitAfter)
    }

    fun addSwipe() {
        val current = _editedDumbSplit.value ?: return
        if (current.subActions.size >= 10) return
        val subIndex = current.subActions.size
        val newSwipe = dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(
            context = context,
            from = Point(-1, -1),
            to = Point(-1, -1),
        ).copy(
            name = "${context.getString(R.string.item_title_dumb_swipe)} ${subIndex + 1}",
            priority = subIndex,
        )
        _editedDumbSplit.value = current.copy(
            subActions = current.subActions + newSwipe
        )
    }

    fun addClick() {
        val current = _editedDumbSplit.value ?: return
        if (current.subActions.size >= 10) return
        val subIndex = current.subActions.size
        val newClick = dumbEditionRepository.dumbActionBuilder.createNewDumbClick(
            context = context,
            position = Point(-1, -1),
        ).copy(
            name = "${context.getString(R.string.item_title_dumb_click)} ${subIndex + 1}",
            priority = subIndex,
        )
        _editedDumbSplit.value = current.copy(
            subActions = current.subActions + newClick
        )
    }

    fun removeSubAction(index: Int) {
        val current = _editedDumbSplit.value ?: return
        if (current.subActions.size <= 2) return
        val list = current.subActions.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            val reindexed = list.mapIndexed { idx, act ->
                when (act) {
                    is DumbAction.DumbSwipe -> act.copy(priority = idx)
                    is DumbAction.DumbClick -> act.copy(priority = idx)
                    is DumbAction.DumbPause -> act.copy(priority = idx)
                    is DumbAction.DumbSplitAction -> act.copy(priority = idx)
                }
            }
            _editedDumbSplit.value = current.copy(subActions = reindexed)
        }
    }

    fun updateSubAction(index: Int, updated: DumbAction) {
        val current = _editedDumbSplit.value ?: return
        val list = current.subActions.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _editedDumbSplit.value = current.copy(subActions = list)
        }
    }

    private fun DumbAction.DumbSplitAction.toUiState(context: Context): DumbSplitActionUiState {
        val items = subActions.mapIndexed { index, subAction ->
            when (subAction) {
                is DumbAction.DumbSwipe -> {
                    val hasPos = subAction.fromPosition.x >= 0 && subAction.fromPosition.y >= 0 &&
                        subAction.toPosition.x >= 0 && subAction.toPosition.y >= 0
                    val details = if (hasPos) {
                        val base = context.getString(
                            R.string.item_desc_dumb_swipe_details,
                            formatDuration(subAction.swipeDurationMs),
                            subAction.fromPosition.x,
                            subAction.fromPosition.y,
                            subAction.toPosition.x,
                            subAction.toPosition.y,
                        )
                        val delays = subAction.delaysSummary()
                        if (delays.isNotEmpty()) "$base ($delays)" else base
                    } else {
                        context.getString(R.string.split_action_position_not_set)
                    }
                    DumbSubActionItemUiState(
                        index = index,
                        name = subAction.name.ifBlank { "${context.getString(R.string.item_title_dumb_swipe)} ${index + 1}" },
                        details = details,
                        icon = R.drawable.ic_swipe,
                        isComplete = subAction.isValid() && io.github.vibhor1102.macrion.core.base.gesture.isCombinedTouchTimingValid(
                            subAction.swipeDurationMs, subAction.waitBeforeMs, subAction.waitAfterMs),
                        action = subAction,
                    )
                }
                is DumbAction.DumbClick -> {
                    val hasPos = subAction.position.x >= 0 && subAction.position.y >= 0
                    val details = if (hasPos) {
                        val base = context.getString(
                            R.string.item_desc_dumb_click_details,
                            formatDuration(subAction.pressDurationMs),
                            subAction.position.x,
                            subAction.position.y,
                        )
                        val delays = subAction.delaysSummary()
                        if (delays.isNotEmpty()) "$base ($delays)" else base
                    } else {
                        context.getString(R.string.split_action_position_not_set)
                    }
                    DumbSubActionItemUiState(
                        index = index,
                        name = subAction.name.ifBlank { "${context.getString(R.string.item_title_dumb_click)} ${index + 1}" },
                        details = details,
                        icon = R.drawable.ic_click,
                        isComplete = subAction.isValid() && io.github.vibhor1102.macrion.core.base.gesture.isCombinedTouchTimingValid(
                            subAction.pressDurationMs, subAction.waitBeforeMs, subAction.waitAfterMs),
                        action = subAction,
                    )
                }
                else -> {
                    DumbSubActionItemUiState(
                        index = index,
                        name = subAction.name.orEmpty().ifBlank { "Action ${index + 1}" },
                        details = "",
                        icon = R.drawable.ic_swipe,
                        isComplete = subAction.isValid(),
                        action = subAction,
                    )
                }
            }
        }

        return DumbSplitActionUiState(
            name = name,
            nameError = name.isBlank(),
            repeatCount = repeatCount.toString(),
            repeatCountError = repeatCount <= 0,
            repeatDelay = repeatDelayMs.toString(),
            repeatDelayError = !isRepeatDelayValid(),
            isRepeatInfinite = isRepeatInfinite,
            waitBefore = waitBeforeMs?.toString().orEmpty(),
            waitAfter = waitAfterMs?.toString().orEmpty(),
            canBeSaved = isValid(),
            subActions = items,
            durationMs = subActions.maxOfOrNull { child ->
                when (child) {
                    is DumbAction.DumbClick -> (child.waitBeforeMs ?: 0L) + child.pressDurationMs + (child.waitAfterMs ?: 0L)
                    is DumbAction.DumbSwipe -> (child.waitBeforeMs ?: 0L) + child.swipeDurationMs + (child.waitAfterMs ?: 0L)
                    else -> 0L
                }
            } ?: 0L,
            canDeleteSubAction = subActions.size > 2,
            canUnsplit = dumbEditionRepository.editedDumbScenario.value?.dumbActions?.any { it.id == id } == true,
        )
    }

    private fun DumbAction.DumbSwipe.delaysSummary(): String = buildString {
        val wb = waitBeforeMs
        val wa = waitAfterMs
        if (wb != null && wb > 0) append("Wait before: ${wb}ms")
        if (wa != null && wa > 0) {
            if (isNotEmpty()) append(" • ")
            append("Wait after: ${wa}ms")
        }
    }

    private fun DumbAction.DumbClick.delaysSummary(): String = buildString {
        val wb = waitBeforeMs
        val wa = waitAfterMs
        if (wb != null && wb > 0) append("Wait before: ${wb}ms")
        if (wa != null && wa > 0) {
            if (isNotEmpty()) append(" • ")
            append("Wait after: ${wa}ms")
        }
    }
}
