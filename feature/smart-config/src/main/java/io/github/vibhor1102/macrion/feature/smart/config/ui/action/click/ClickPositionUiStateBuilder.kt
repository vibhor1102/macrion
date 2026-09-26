/* Copyright (C) 2026 Vibhor Goel — GPLv3 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.click

import android.content.Context
import androidx.core.content.ContextCompat
import io.github.vibhor1102.macrion.core.bitmaps.BitmapRepository
import io.github.vibhor1102.macrion.core.domain.ext.getConditionBitmap
import io.github.vibhor1102.macrion.core.domain.model.AND
import io.github.vibhor1102.macrion.core.domain.model.OR
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.condition.ScreenCondition
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.event.ScreenEvent
import io.github.vibhor1102.macrion.core.domain.model.event.TriggerEvent
import io.github.vibhor1102.macrion.core.ui.utils.createColorIndicatorDrawable
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.condition.UiScreenCondition

/** One source of truth for the target choices and explanations in both click editors. */
internal suspend fun buildClickPositionUiState(
    context: Context, event: Event, click: Click,
    availableConditions: List<UiScreenCondition>, bitmapRepository: BitmapRepository,
): ClickPositionUiState? = with(context) {
    when (event) {
        is TriggerEvent -> userSelectedState(click, forced = true)
        is ScreenEvent if click.positionType == Click.PositionType.USER_SELECTED ->
            userSelectedState(click, forced = false)
        is ScreenEvent if event.conditionOperator == OR -> ClickPositionUiState(
            positionType = Click.PositionType.ON_DETECTED_CONDITION,
            isTypeFieldVisible = true,
            isSelectorEnabled = false,
            selectorTitle = getString(R.string.field_condition_selection_title_or_operator),
            selectorDescription = getString(R.string.field_condition_selection_desc_or_operator),
            isSelectorInError = false,
            isClickOffsetVisible = true,
            isClickOffsetEnabled = true,
            clickOffsetDescription = offsetDescription(click),
        )
        is ScreenEvent if event.conditionOperator == AND -> {
            val condition = event.conditions.find { it.id == click.clickOnConditionId }
            val visualization = when (condition) {
                is ScreenCondition.Color -> createColorIndicatorDrawable(condition.color)
                is ScreenCondition.Image -> bitmapRepository.getConditionBitmap(condition)
                is ScreenCondition.Number -> ContextCompat.getDrawable(context, R.drawable.ic_number_condition)
                is ScreenCondition.Text -> ContextCompat.getDrawable(context, R.drawable.ic_text_condition)
                null -> null
            }
            ClickPositionUiState(
                positionType = Click.PositionType.ON_DETECTED_CONDITION,
                isTypeFieldVisible = true,
                isSelectorEnabled = availableConditions.isNotEmpty(),
                selectorTitle = getString(R.string.field_condition_selection_title_and_operator),
                selectorDescription = if (condition == null || visualization == null)
                    getString(R.string.field_condition_selection_desc_and_operator_not_found)
                else getString(R.string.field_condition_selection_desc_and_operator, condition.name),
                selectorVisualization = visualization,
                isSelectorInError = availableConditions.isNotEmpty() && (condition == null || visualization == null),
                isClickOffsetVisible = true,
                isClickOffsetEnabled = true,
                clickOffsetDescription = offsetDescription(click),
            )
        }
        else -> null
    }
}

private fun Context.userSelectedState(click: Click, forced: Boolean) = ClickPositionUiState(
    positionType = Click.PositionType.USER_SELECTED,
    isTypeFieldVisible = !forced,
    isSelectorEnabled = true,
    selectorTitle = getString(R.string.field_click_position_title),
    selectorDescription = click.position?.let {
        getString(R.string.field_click_position_desc, it.x, it.y)
    } ?: getString(R.string.generic_select_the_position),
    isSelectorInError = click.position == null,
    isClickOffsetEnabled = false,
    isClickOffsetVisible = !forced,
    clickOffsetDescription = offsetDescription(click),
)

private fun Context.offsetDescription(click: Click): String {
    val offset = click.clickOffset.takeIf { click.positionType == Click.PositionType.ON_DETECTED_CONDITION }
    return offset?.let { getString(R.string.field_click_offset_desc, it.x, it.y) }
        ?: getString(R.string.field_click_offset_desc_none)
}
