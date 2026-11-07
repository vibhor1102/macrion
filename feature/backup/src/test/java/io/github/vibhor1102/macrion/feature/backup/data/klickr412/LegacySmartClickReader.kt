/*
 * Copyright (C) 2026 Kevin Buzeau
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
package io.github.vibhor1102.macrion.feature.backup.data.klickr412

import android.util.Log
import io.github.vibhor1102.macrion.core.base.extensions.getEnum
import io.github.vibhor1102.macrion.core.base.extensions.getInt
import io.github.vibhor1102.macrion.core.base.extensions.getLong
import io.github.vibhor1102.macrion.core.base.interfaces.containsId
import io.github.vibhor1102.macrion.core.database.entity.ActionEntity
import io.github.vibhor1102.macrion.core.database.entity.ActionType
import io.github.vibhor1102.macrion.core.database.entity.ClickPositionType
import io.github.vibhor1102.macrion.core.database.entity.ConditionEntity
import kotlinx.serialization.json.JsonObject

/** Target-selection method pinned to Klick'r 4.1.2, 699bff818. See README for the extraction boundary. */
internal class LegacySmartClickReader {
    private companion object { const val TAG = "LegacySmartClickReader" }

    fun deserializeActionClick(
        jsonClick: JsonObject,
        eventConditions: List<ConditionEntity>,
        conditionsOperator: Int,
    ): ActionEntity? {
        val clickPositionType = jsonClick.getEnum<ClickPositionType>("clickPositionType", true)
            ?: return null

        return when (clickPositionType) {
            ClickPositionType.ON_DETECTED_CONDITION -> {
                val isAndConditionsOperator = conditionsOperator == 1
                val clickOnConditionId = jsonClick.getLong("clickOnConditionId", isAndConditionsOperator)
                if (isAndConditionsOperator && (clickOnConditionId == null || !eventConditions.containsId(clickOnConditionId))) {
                    Log.w(TAG, "Can't deserialize action, clickOnConditionId is not valid.")
                    return null
                }

                deserializeLegacyClick(
                    jsonClick = jsonClick,
                    positionType = clickPositionType,
                    clickOnConditionId = clickOnConditionId,
                    clickOffsetX = jsonClick.getInt("clickOffsetX"),
                    clickOffsetY = jsonClick.getInt("clickOffsetY"),
                )
            }

            ClickPositionType.USER_SELECTED ->
                deserializeLegacyClick(
                    jsonClick = jsonClick,
                    positionType = clickPositionType,
                    x = jsonClick.getInt("x", true) ?: return null,
                    y = jsonClick.getInt("y", true) ?: return null,
                )
        }
    }

    // Capture the arguments passed to upstream's finger builder, without importing its gesture engine.
    private fun deserializeLegacyClick(
        jsonClick: JsonObject, positionType: ClickPositionType,
        x: Int? = null, y: Int? = null, clickOnConditionId: Long? = null,
        clickOffsetX: Int? = null, clickOffsetY: Int? = null,
    ): ActionEntity? {
        return ActionEntity(
            id = jsonClick.getLong("id", true) ?: return null,
            eventId = jsonClick.getLong("eventId", true) ?: return null,
            name = "Parsed click", type = ActionType.CLICK, clickPositionType = positionType,
            x = x, y = y, clickOnConditionId = clickOnConditionId,
            clickOffsetX = clickOffsetX, clickOffsetY = clickOffsetY,
            pressDuration = jsonClick.getLong("pressDuration"),
        )
    }
}
