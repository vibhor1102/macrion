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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click.offset

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel

import io.github.vibhor1102.macrion.core.base.identifier.Identifier
import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.display.config.DisplayConfigManager
import io.github.vibhor1102.macrion.core.domain.ext.getConditionBitmap
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.ui.utils.createColorIndicatorDrawable
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ClickOffsetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bitmapRepository: BitmapRepository,
    private val editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    /** The ImageEvent being edited by the user. */
    private val editedEvent: Flow<ScreenEvent> = editionRepository.editionState.editedEventState
        .mapNotNull { event -> event.value }
        .filterIsInstance<ScreenEvent>()

    /** The ImageConditions being edited by the user. */
    private val editedImageConditions: Flow<List<ScreenCondition>> =
        editionRepository.editionState.editedEventScreenConditionsState
            .mapNotNull { it.value }

    /** The Action currently configured by the user. */
    private val workspaceClick = MutableStateFlow<Click?>(null)
    private val configuredClick = combine(
        editionRepository.editionState.editedActionState,
        workspaceClick,
    ) { action, child -> child ?: (action.value as? Click) }.filterNotNull()

    val conditionPreviews: Flow<ClickOffsetPreviewState> =
        combine(editedEvent, editedImageConditions, configuredClick) { event, conditions, click ->
            val isOrMode = event.conditionOperator == OR
            val candidates = when {
                click.positionType != Click.PositionType.ON_DETECTED_CONDITION -> emptyList()
                isOrMode -> conditions.filter { it.shouldBeDetected }.sortedBy { it.priority }
                else -> conditions.filter { it.id == click.clickOnConditionId }
            }
            ClickOffsetPreviewState(
                isOrMode = isOrMode,
                previews = candidates.map { condition ->
                    ClickOffsetConditionPreview(
                        id = condition.id,
                        name = condition.name,
                        condition = condition,
                    )
                },
            )
        }

    suspend fun loadPreviewBitmap(condition: ScreenCondition): Bitmap? = withContext(Dispatchers.IO) {
        when (condition) {
            is ScreenCondition.Color -> context.createColorIndicatorDrawable(condition.color)?.toBitmap()
            is ScreenCondition.Image -> bitmapRepository.getConditionBitmap(condition)
            is ScreenCondition.Number -> ContextCompat.getDrawable(context, R.drawable.ic_number_condition)?.toBitmap()
            is ScreenCondition.Text -> ContextCompat.getDrawable(context, R.drawable.ic_text_condition)?.toBitmap()
        }
    }

    private val initialClickOffset: Flow<Point> = configuredClick
        .map { click -> click.clickOffset ?: Point(0, 0) }
        .take(1)

    private val userClickOffset: MutableStateFlow<ClickOffsetState?> =
        MutableStateFlow(null)

    val clickOffset: Flow<ClickOffsetState> = combine(initialClickOffset, userClickOffset) { initial, user ->
        if (user == null) ClickOffsetState(initial, ClickOffsetUpdateType.INITIAL)
        else ClickOffsetState(user.offset, user.updateFrom)
    }

    fun getOffsetMaxBoundsX(): IntRange =
        displayConfigManager.displayConfig.let { displayConfig ->
            IntRange(-displayConfig.sizePx.x / 2, displayConfig.sizePx.x / 2)
        }

    fun getOffsetMaxBoundsY(): IntRange =
        displayConfigManager.displayConfig.let { displayConfig ->
            IntRange(-displayConfig.sizePx.y / 2, displayConfig.sizePx.y / 2)
        }

    fun setClickOffset(offset: Point, from: ClickOffsetUpdateType) {
        userClickOffset.value = ClickOffsetState(offset, from)
    }

    fun setClickOffsetX(offsetX: Int, from: ClickOffsetUpdateType) {
        val currentOffset = getCurrentOffset()
        userClickOffset.value = ClickOffsetState(Point(offsetX, currentOffset.y), from)
    }

    fun setClickOffsetY(offsetY: Int, from: ClickOffsetUpdateType) {
        val currentOffset = getCurrentOffset()
        userClickOffset.value = ClickOffsetState(Point(currentOffset.x, offsetY), from)
    }

    fun setWorkspaceClick(click: Click) {
        workspaceClick.value = click
    }

    fun selectedOffset(): Point? = userClickOffset.value?.offset

    fun saveChanges() {
        val clickOffset = userClickOffset.value?.offset ?: return

        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOffset = clickOffset))
        }
    }

    private fun getCurrentOffset(): Point =
        userClickOffset.value?.offset
            ?: workspaceClick.value?.clickOffset
            ?: editionRepository.editionState.getEditedAction<Click>()?.clickOffset
            ?: Point(0, 0)

}

data class ClickOffsetConditionPreview(
    val id: Identifier,
    val name: String,
    val condition: ScreenCondition,
)

data class ClickOffsetPreviewState(
    val isOrMode: Boolean = false,
    val previews: List<ClickOffsetConditionPreview> = emptyList(),
)

data class ClickOffsetState(
    val offset: Point,
    val updateFrom: ClickOffsetUpdateType,
)

enum class ClickOffsetUpdateType{
    INITIAL,
    TEXT_INPUT,
    VIEW,
}
