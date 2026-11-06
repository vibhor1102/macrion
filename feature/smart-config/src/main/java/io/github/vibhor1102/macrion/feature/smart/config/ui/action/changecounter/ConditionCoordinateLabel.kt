package io.github.vibhor1102.macrion.feature.smart.config.ui.action.changecounter

import io.github.vibhor1102.macrion.core.domain.model.counter.ConditionCoordinate
import io.github.vibhor1102.macrion.feature.smart.config.R

internal fun ConditionCoordinate?.coordinateLabel(): Int = when (this) {
    ConditionCoordinate.CENTER_X, null -> R.string.coordinate_center_x
    ConditionCoordinate.CENTER_Y -> R.string.coordinate_center_y
    ConditionCoordinate.LEFT -> R.string.coordinate_left
    ConditionCoordinate.RIGHT -> R.string.coordinate_right
    ConditionCoordinate.TOP -> R.string.coordinate_top
    ConditionCoordinate.BOTTOM -> R.string.coordinate_bottom
}
