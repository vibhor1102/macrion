/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.toUiScreenCondition
import io.github.vibhor1102.macrion.feature.smart.config.utils.getEventConfigPreferences
import io.github.vibhor1102.macrion.feature.smart.config.utils.putClickPressDurationConfig

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class ClickViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()

    /** The action being configured by the user. */
    private val configuredClick = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Click>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000.milliseconds)

    val uiState: StateFlow<ClickUiState?> = combine(
        configuredClick,
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedActionState,
        editionRepository.editionState.editedEventScreenConditionsState,
    ) { click, event, actionState, conditionsState ->

        val evt = event.value ?: return@combine null
        click.toDialogUiState(
            context = context,
            event = evt,
            hasUnsavedModifications = actionState.hasChanged,
            canBeSaved = actionState.canBeSaved,
            availableConditions = conditionsState.value
                ?.filter { it.shouldBeDetected }
                ?.map { it.toUiScreenCondition(context = context, shortThreshold = true, inError = !it.isComplete()) }
                ?: emptyList()
            )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun getEditedClick(): Click? =
        editionRepository.editionState.getEditedAction<Click>()

    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(name = "" + name))
        }
    }

    /** Set if this click should be made on the detected condition. */
    fun setClickOnCondition(newType: Click.PositionType) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(positionType = newType))
        }
    }

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(position = position))
        }
    }

    /**
     * Set the press duration of the click.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPressDuration(durationMs: Long?) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(pressDuration = durationMs))
        }
    }

    fun setWaitBeforeMs(waitBeforeMs: Long?) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(waitBeforeMs = waitBeforeMs))
        }
    }

    fun setWaitAfterMs(waitAfterMs: Long?) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(waitAfterMs = waitAfterMs))
        }
    }

    /** Set the condition to click on when the events conditions are fulfilled. */
    fun setConditionToBeClicked(condition: ScreenCondition) {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOnConditionId = condition.id))
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            sharedPreferences.edit { putClickPressDurationConfig(click.pressDuration ?: 0) }
        }
    }


    private suspend fun Click.toDialogUiState(
        context: Context,
        event: Event,
        availableConditions: List<UiScreenCondition>,
        hasUnsavedModifications: Boolean,
        canBeSaved: Boolean,
    ) : ClickUiState {
        val positionState = buildClickPositionUiState(context, event, this,
            availableConditions, bitmapRepository)

        return ClickUiState(
            canBeSaved = canBeSaved,
            hasUnsavedModifications = hasUnsavedModifications,
            name = name,
            nameError = name?.isEmpty() ?: true,
            pressDuration = pressDuration?.toString() ?: "1",
            pressDurationError = (pressDuration ?: -1) <= 0,
            positionState = positionState,
            availableConditions = availableConditions,
            waitBeforeMs = waitBeforeMs?.toString(),
            waitAfterMs = waitAfterMs?.toString(),
        )
    }

}
