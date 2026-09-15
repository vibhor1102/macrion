/*
 * Copyright (C) 2024 Kevin Buzeau
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.brief

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toPoint
import androidx.core.graphics.toPointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.domain.ext.getConditionBitmap
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.processing.domain.SmartProcessingRepository
import io.github.vibhor1102.macrion.core.processing.domain.model.DetectionState
import io.github.vibhor1102.macrion.core.settings.domain.SettingsRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.TutorialRepository
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.state.TutorialState
import io.github.vibhor1102.macrion.core.ui.utils.createColorIndicatorDrawable
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.DefaultDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.PauseDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.model.EditedListState
import io.github.vibhor1102.macrion.feature.smart.config.domain.usecase.copy.availability.IsActionCopyAvailableUseCase
import io.github.vibhor1102.macrion.feature.smart.config.ui.action.selection.ActionTypeChoice
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.UiAction
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.getIconRes
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action.toUiAction

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.util.Collections
import javax.inject.Inject


class SmartActionsBriefViewModel @Inject constructor(
    @ApplicationContext context: Context,
    isActionCopyAvailableUseCase: IsActionCopyAvailableUseCase,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    private val smartProcessingRepository: SmartProcessingRepository,
    tutorialRepository: TutorialRepository,
    settingsRepository: SettingsRepository,
) : ViewModel(), ActionConfigurator {

    private val isLegacyUiEnabled: Flow<Boolean> = settingsRepository.isLegacyActionUiEnabledFlow

    private val editedActions: Flow<EditedListState<Action>> = editionRepository.editionState.editedEventActionsState
    private val editedEvent: Flow<Event> = editionRepository.editionState.editedEventState.mapNotNull { it.value }

    private val briefVisualizationState: MutableStateFlow<BriefVisualizationState> =
        MutableStateFlow(BriefVisualizationState(0, false))

    val isGestureCaptureStarted: StateFlow<Boolean> = briefVisualizationState
        .map { it.gestureCaptureStarted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val actionBriefList: Flow<List<ItemBrief>> =
        combine(editedEvent, editedActions) { event, actions ->
            val actionList = actions.value ?: emptyList()
            actionList.mapIndexed { index, action ->
                ItemBrief(action.id, action.toUiAction(context, event, inError = !actions.itemValidity[index]) )
            }
        }

    val isTestingAction: Flow<Boolean> = smartProcessingRepository.detectionState
        .map { state -> state == DetectionState.DETECTING }

    private val focusedAction: Flow<Pair<Action?, Boolean>> =
        combine(briefVisualizationState, editedActions) { visualizationState, actions ->
            val filterUpdates = visualizationState.gestureCaptureStarted
            val actionList = actions.value ?: return@combine null to filterUpdates
            if (visualizationState.focusedIndex !in actionList.indices) return@combine null to filterUpdates
            actionList[visualizationState.focusedIndex] to filterUpdates
        }

    val actionVisualization: Flow<ItemBriefDescription?> = focusedAction
        .filter { !it.second }
        .map { (action, _) -> action?.toActionDescription(context) }

    val canCopyActions: Flow<Boolean> = isActionCopyAvailableUseCase()

    val actionTypeChoices: StateFlow<List<ActionTypeChoice>> =
        combine(canCopyActions, isLegacyUiEnabled) { canCopy, legacyEnabled ->
            buildList {
                if (!legacyEnabled && canCopy) add(ActionTypeChoice.Copy)
                add(ActionTypeChoice.Click)
                add(ActionTypeChoice.Swipe)
                add(ActionTypeChoice.Pause)
                add(ActionTypeChoice.SetText)
                add(ActionTypeChoice.System)
                add(ActionTypeChoice.ChangeCounter)
                add(ActionTypeChoice.ExternalAction)
                add(ActionTypeChoice.ToggleEvent)
                add(ActionTypeChoice.Notification)
                add(ActionTypeChoice.Intent)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isTutorialModeEnabled: Flow<Boolean> =
        tutorialRepository.tutorialState.map { it is TutorialState.Started }

    fun startGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = true)
    }

    fun endGestureCaptureState(context: Context, gesture: ItemBriefDescription) {
        val action = gesture.toAction(context) ?: return
        editionRepository.apply {
            startActionEdition(action)
            upsertEditedAction()
        }
    }

    fun cancelGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = false)
    }

    fun setFocusedActionIndex(index: Int) {
        briefVisualizationState.value = BriefVisualizationState(
            focusedIndex = index,
            gestureCaptureStarted = false,
        )
    }

    override fun getActionTypeChoices(): List<ActionTypeChoice> =
        actionTypeChoices.value

    override fun createAction(context: Context, choice: ActionTypeChoice): Action = when (choice) {
        ActionTypeChoice.Click -> editionRepository.editedItemsBuilder.createNewClick(context)
        ActionTypeChoice.Swipe -> editionRepository.editedItemsBuilder.createNewSwipe(context)
        ActionTypeChoice.Pause -> editionRepository.editedItemsBuilder.createNewPause(context)
        ActionTypeChoice.Intent -> editionRepository.editedItemsBuilder.createNewIntent(context)
        ActionTypeChoice.ToggleEvent -> editionRepository.editedItemsBuilder.createNewToggleEvent(context)
        ActionTypeChoice.ChangeCounter -> editionRepository.editedItemsBuilder.createNewChangeCounter(context)
        ActionTypeChoice.ExternalAction -> editionRepository.editedItemsBuilder.createNewExternalAction(context)
        ActionTypeChoice.Notification -> editionRepository.editedItemsBuilder.createNewNotification(context)
        ActionTypeChoice.System -> editionRepository.editedItemsBuilder.createNewSystemAction(context)
        ActionTypeChoice.SetText -> editionRepository.editedItemsBuilder.createNewSetText(context)
        ActionTypeChoice.Copy -> throw IllegalArgumentException("Unsupported action type for creation $choice")
    }

    override fun startActionEdition(action: Action) {
        editionRepository.startActionEdition(action)
    }

    override fun upsertEditedAction() {
        editionRepository.upsertEditedAction()
    }

    override fun removeEditedAction() {
        editionRepository.deleteEditedAction()
    }

    override fun dismissEditedAction() {
        editionRepository.stopActionEdition()
    }

    fun playAction(context: Context, index: Int) {
        val scenario = editionRepository.editionState.getScenario()
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList()
        if (scenario == null || actions == null || index !in actions.indices) return

        viewModelScope.launch {
            delay(500)
            smartProcessingRepository.tryAction(context, scenario, actions[index])
        }
    }

    fun stopAction(): Boolean {
        if (!smartProcessingRepository.isRunning()) return false

        smartProcessingRepository.stopDetection()
        return true
    }

    fun swapActions(i: Int, j: Int) {
        if (i == j) return

        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (actions.size <= 1 || i !in actions.indices || j !in actions.indices) return

        Collections.swap(actions, i, j)
        editionRepository.updateActionsOrder(actions)
    }

    fun moveAction(from: Int, to: Int) {
        if (from == to) return

        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (actions.size <= 1 || from !in actions.indices || to !in actions.indices) return

        val movedAction = actions.removeAt(from)
        actions.add(to, movedAction)

        editionRepository.updateActionsOrder(actions)
    }

    fun updateActionOrder(actionsBrief: List<ItemBrief>) =
        editionRepository.updateActionsOrder(actionsBrief.map { brief -> (brief.data as UiAction).action })

    fun deleteAction(index: Int) {
        val actions = editionRepository.editionState.getEditedEventActions<Action>()?.toMutableList() ?: return
        if (index !in actions.indices) return

        editionRepository.apply {
            startActionEdition(actions[index])
            deleteEditedAction()
        }
    }

    private fun ItemBriefDescription.toAction(context: Context): Action? =
        when (this) {
            is ClickDescription -> editionRepository.editedItemsBuilder.createNewClick(context)
                .copy(
                    position = position?.toPoint(),
                    pressDuration = pressDurationMs,
                    positionType = Click.PositionType.USER_SELECTED,
                )

            is SwipeDescription -> editionRepository.editedItemsBuilder.createNewSwipe(context)
                .copy(
                    from = from?.toPoint(),
                    to = to?.toPoint(),
                    swipeDuration = swipeDurationMs,
                )

            else -> null
        }

    private suspend fun Action.toActionDescription(context: Context): ItemBriefDescription = when (this) {
        is Click -> ClickDescription(
            position = position?.toPointF(),
            pressDurationMs = pressDuration ?: 1,
            imageConditionBitmap = findClickOnConditionBitmap(context),
        )

        is Swipe -> SwipeDescription(
            from = from?.toPointF(),
            to = to?.toPointF(),
            swipeDurationMs = swipeDuration ?: 1,
        )

        is Pause -> PauseDescription(
            pauseDurationMs = pauseDuration ?: 1,
        )

        else -> DefaultDescription(
            icon = ContextCompat.getDrawable(context, getIconRes())
        )
    }

    private suspend fun Click.findClickOnConditionBitmap(context: Context): Bitmap? {
        if (positionType != Click.PositionType.ON_DETECTED_CONDITION) return null

        return editionRepository.editionState.getEditedEventConditions<ScreenCondition>()
            ?.find { it.id == clickOnConditionId }
            ?.let { condition ->
                when (condition) {
                    is ScreenCondition.Color -> context.createColorIndicatorDrawable(condition.color)?.toBitmap()
                    is ScreenCondition.Image -> bitmapRepository.getConditionBitmap(condition)
                    is ScreenCondition.Number -> ContextCompat.getDrawable(context, R.drawable.ic_number_condition)?.toBitmap()
                    is ScreenCondition.Text -> ContextCompat.getDrawable(context, R.drawable.ic_text_condition)?.toBitmap()
                }
            }
    }
}

private data class BriefVisualizationState(
    val focusedIndex: Int,
    val gestureCaptureStarted: Boolean,
)
