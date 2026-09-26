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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.feature.smart.config.R


@DrawableRes
internal fun getToggleEventIconRes(): Int = R.drawable.ic_toggle_event

internal fun ToggleEvent.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_toggle_event_details_error)

    toggleAll -> when (toggleAllType) {
        ToggleEvent.ToggleType.ENABLE -> context.getString(R.string.item_toggle_event_details_enable_all)
        ToggleEvent.ToggleType.TOGGLE -> context.getString(R.string.item_toggle_event_details_invert_all)
        ToggleEvent.ToggleType.DISABLE -> context.getString(R.string.item_toggle_event_details_disable_all)
        null -> throw IllegalArgumentException("Invalid toggle event type")
    }

    else -> context.resources.getQuantityString(
        R.plurals.item_toggle_event_details_manual,
        eventToggles.size,
        eventToggles.size,
    )
}
