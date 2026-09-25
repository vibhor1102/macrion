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
package io.github.vibhor1102.macrion.feature.dumb.config.ui.brief

import android.content.Context
import android.graphics.Point
import androidx.core.graphics.toPoint

import androidx.core.graphics.toPointF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers.Main

import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.brief.ItemBrief
import io.github.vibhor1102.macrion.core.dumb.domain.model.DumbAction
import io.github.vibhor1102.macrion.core.dumb.engine.DumbEngine
import io.github.vibhor1102.macrion.core.ui.views.itembrief.ItemBriefDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.ClickDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.PauseDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SplitDescription
import io.github.vibhor1102.macrion.core.ui.views.itembrief.renderers.SwipeDescription
import io.github.vibhor1102.macrion.feature.dumb.config.domain.DumbEditionRepository
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.DumbActionDetails
import io.github.vibhor1102.macrion.feature.dumb.config.ui.actions.copy.toDumbActionDetails

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.util.Collections
import javax.inject.Inject


class DumbScenarioBriefViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    private val dumbEditionRepository: DumbEditionRepository,
    private val dumbEngine: DumbEngine,
) : ViewModel() {


    private val briefVisualizationState: MutableStateFlow<BriefVisualizationState> =
        MutableStateFlow(BriefVisualizationState(0, false))

    val isGestureCaptureStarted: StateFlow<Boolean> = briefVisualizationState
        .map { it.gestureCaptureStarted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val dumbActionsBriefList: Flow<List<ItemBrief>> = dumbEditionRepository.editedDumbScenario
        .map { scenario ->
            scenario?.dumbActions?.map { dumbAction ->
                ItemBrief(
                    dumbAction.id ,
                    dumbAction.toDumbActionDetails(
                        context = context,
                        withPositions = false,
                    ),
                )
            }
        }
        .filterNotNull()

    fun combinableActionsFor(source: DumbAction): List<DumbActionDetails>? {
        val actions = dumbEditionRepository.editedDumbScenario.value?.dumbActions ?: return null
        if (actions.none { it.id == source.id }) return null
        val sourceCount = (source as? DumbAction.DumbSplitAction)?.subActions?.size ?: 1
        return actions.asSequence()
            .filter { it.id != source.id }
            .filter { it is DumbAction.DumbClick || it is DumbAction.DumbSwipe || it is DumbAction.DumbSplitAction }
            .filter { sourceCount + ((it as? DumbAction.DumbSplitAction)?.subActions?.size ?: 1) <= 10 }
            .map { it.toDumbActionDetails(context, withPositions = false) }
            .toList()
    }

    private val focusedAction: Flow<Pair<DumbAction?, Boolean>> =
        combine(briefVisualizationState, dumbEditionRepository.editedDumbScenario) { visualizationState, scenario ->
            val filterUpdates = visualizationState.gestureCaptureStarted
            val actionList = scenario?.dumbActions ?: return@combine null to filterUpdates
            if (visualizationState.focusedIndex !in actionList.indices) return@combine null to filterUpdates
            actionList[visualizationState.focusedIndex] to filterUpdates
        }

    val dumbActionVisualization: Flow<ItemBriefDescription?> = focusedAction
        .filter { !it.second }
        .map { (action, _) -> action?.toBriefDescription() }


    fun startGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = true)
    }

    fun endGestureCaptureState(context: Context, gesture: ItemBriefDescription) {
        val action = gesture.toDumbAction(context) ?: return
        dumbEditionRepository.addNewDumbAction(action)
    }

    fun cancelGestureCaptureState() {
        briefVisualizationState.value = briefVisualizationState.value
            .copy(gestureCaptureStarted = false)
    }

    fun setFocusedDumbActionIndex(index: Int) {
        briefVisualizationState.value = BriefVisualizationState(
            focusedIndex = index,
            gestureCaptureStarted = false,
        )
    }

    fun playAction(index: Int, onCompleted: () -> Unit) {
        val actions = dumbEditionRepository.editedDumbScenario.value?.dumbActions
        if (actions.isNullOrEmpty() || index !in actions.indices) return

        viewModelScope.launch {
            delay(500)
            dumbEngine.tryDumbAction(actions[index]) {
                viewModelScope.launch(mainDispatcher) { onCompleted() }
            }
        }
    }

    fun stopAction(): Boolean {
        if (!dumbEngine.isRunning.value) return false

        dumbEngine.stopDumbScenario()
        return true
    }

    fun createNewDumbClick(context: Context, position: Point): DumbAction.DumbClick =
        dumbEditionRepository.dumbActionBuilder.createNewDumbClick(context, position)

    fun createNewDumbSwipe(context: Context, from: Point, to: Point): DumbAction.DumbSwipe =
        dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(context, from, to)

    fun createNewDumbPause(context: Context, ): DumbAction.DumbPause =
        dumbEditionRepository.dumbActionBuilder.createNewDumbPause(context)

    fun createNewDumbZoomInOut(context: Context): DumbAction.DumbSplitAction =
        dumbEditionRepository.dumbActionBuilder.createNewDumbZoomInOut(context)

    fun combineWithNewSwipe(action: DumbAction): DumbAction.DumbSplitAction? {
        val newSwipe = dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(
            context = context,
            from = Point(-1, -1),
            to = Point(-1, -1),
        ).copy(name = "Swipe 2")
        return dumbEditionRepository.combineActionWithNew(action, newSwipe)
    }

    fun saveCombination(split: DumbAction.DumbSplitAction, sourceIds: Set<io.github.vibhor1102.macrion.core.base.identifier.Identifier>) =
        dumbEditionRepository.saveCombination(split, sourceIds)

    fun combineWithNewClick(action: DumbAction): DumbAction.DumbSplitAction? =
        dumbEditionRepository.combineActionWithNew(action,
            dumbEditionRepository.dumbActionBuilder.createNewDumbClick(context, Point(-1, -1)))

    fun combineActions(actionA: DumbAction, actionB: DumbAction): DumbAction.DumbSplitAction? =
        dumbEditionRepository.combineActions(actionA, actionB)

    fun unsplitAction(splitAction: DumbAction.DumbSplitAction) {
        dumbEditionRepository.unsplitAction(splitAction)
    }

    fun createDumbActionCopy(actionToCopy: DumbAction): DumbAction =
        dumbEditionRepository.dumbActionBuilder.createNewDumbActionFrom(actionToCopy)

    fun addNewDumbAction(dumbAction: DumbAction, index: Int) {
        dumbEditionRepository.addNewDumbAction(
            dumbAction = dumbAction,
            insertionIndex = index,
        )
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        dumbEditionRepository.updateDumbAction(dumbAction)
    }

    fun swapDumbActions(i: Int, j: Int) {
        if (i == j) return

        val actions = dumbEditionRepository.editedDumbScenario.value?.dumbActions?.toMutableList() ?: return
        if (actions.isEmpty() || i !in actions.indices || j !in actions.indices) return

        Collections.swap(actions, i, j)
        dumbEditionRepository.updateDumbActions(actions)
    }

    fun moveDumbAction(from: Int, to: Int) {
        val actions = dumbEditionRepository.editedDumbScenario.value?.dumbActions?.toMutableList() ?: return
        val movedAction = actions.removeAt(from)
        actions.add(to, movedAction)

        dumbEditionRepository.updateDumbActions(actions)
    }

    fun updateDumbActionsOrder(actionsBrief: List<ItemBrief>) =
        dumbEditionRepository.updateDumbActions(actionsBrief.map { brief -> (brief.data as DumbActionDetails).action })

    fun deleteDumbAction(index: Int) {
        val actions = dumbEditionRepository.editedDumbScenario.value?.dumbActions ?: return
        if (index !in actions.indices) return

        deleteDumbAction(actions[index])
    }

    fun deleteDumbAction(action: DumbAction) {
        dumbEditionRepository.deleteDumbAction(action)
    }

    private fun ItemBriefDescription.toDumbAction(context: Context): DumbAction? =
        when (this) {
            is ClickDescription -> createNewDumbClick(
                context = context,
                position = position?.toPoint() ?: Point(-1, -1),
            ).copy(pressDurationMs = pressDurationMs, waitBeforeMs = startOffsetMs,
                repeatCount = 1, isRepeatInfinite = false)

            is SwipeDescription -> createNewDumbSwipe(
                context = context,
                from = from?.toPoint() ?: Point(-1, -1),
                to = to?.toPoint() ?: Point(-1, -1),
            ).copy(swipeDurationMs = swipeDurationMs, waitBeforeMs = startOffsetMs, path = path,
                repeatCount = 1, isRepeatInfinite = false)

            is SplitDescription -> {
                val subDumbActions = subDescriptions.mapNotNull { it.toDumbAction(context) }
                if (subDumbActions.isNotEmpty()) {
                    dumbEditionRepository.dumbActionBuilder.createNewDumbZoomInOut(context).copy(
                        subActions = subDumbActions
                    )
                } else null
            }

            else -> null
        }

    private fun DumbAction.toBriefDescription(): ItemBriefDescription =
        when (this) {
            is DumbAction.DumbClick -> ClickDescription(
                position = position.toPointF(),
                pressDurationMs = pressDurationMs,
            )

            is DumbAction.DumbSwipe -> SwipeDescription(
                from = fromPosition.toPointF(),
                to = toPosition.toPointF(),
                swipeDurationMs = swipeDurationMs,
                path = path,
            )

            is DumbAction.DumbPause -> PauseDescription(
                pauseDurationMs = pauseDurationMs,
            )

            is DumbAction.DumbSplitAction -> SplitDescription(
                subDescriptions = subActions.map { child ->
                    when (val desc = child.toBriefDescription()) {
                        is SwipeDescription -> desc.copy(startOffsetMs = (child as DumbAction.DumbSwipe).waitBeforeMs ?: 0L)
                        is ClickDescription -> desc.copy(startOffsetMs = (child as DumbAction.DumbClick).waitBeforeMs ?: 0L)
                        else -> desc
                    }
                }
            )
        }
}

private data class BriefVisualizationState(
    val focusedIndex: Int,
    val gestureCaptureStarted: Boolean,
)
