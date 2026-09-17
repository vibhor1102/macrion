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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import io.github.vibhor1102.macrion.core.domain.model.action.Action
import io.github.vibhor1102.macrion.core.domain.model.action.ChangeCounter
import io.github.vibhor1102.macrion.core.domain.model.action.Click
import io.github.vibhor1102.macrion.core.domain.model.action.Intent
import io.github.vibhor1102.macrion.core.domain.model.action.Notification
import io.github.vibhor1102.macrion.core.domain.model.action.Pause
import io.github.vibhor1102.macrion.core.domain.model.action.SetText
import io.github.vibhor1102.macrion.core.domain.model.action.Swipe
import io.github.vibhor1102.macrion.core.domain.model.action.SystemAction
import io.github.vibhor1102.macrion.core.domain.model.action.ToggleEvent
import io.github.vibhor1102.macrion.core.domain.model.event.Event
import io.github.vibhor1102.macrion.core.domain.model.action.ExternalAction
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.core.domain.model.action.CaptureScreenshot

data class UiAction(
    @param:DrawableRes val icon: Int,
    val name: String,
    val description: String,
    val action: Action,
    val haveError: Boolean,
)

internal fun Action.toUiAction(context: Context, parent: Event? = null, inError: Boolean = !isComplete()): UiAction =
    UiAction(
        action = this,
        name = name!!,
        icon = getIconRes(),
        description = getActionDescription(context, parent, inError),
        haveError = inError,
    )

@DrawableRes
internal fun Action.getIconRes(): Int = when (this) {
    is Click -> getClickIconRes()
    is Swipe -> getSwipeIconRes()
    is Pause -> getPauseIconRes()
    is Intent -> getIntentIconRes()
    is ToggleEvent -> getToggleEventIconRes()
    is ChangeCounter -> getChangeCounterIconRes()
    is ExternalAction -> getExternalActionIconRes()
    is Notification -> getNotificationIconRes()
    is SystemAction -> getSystemActionIconRes()
    is SetText -> getSetTextIconRes()
    is PlaySound -> getPlaySoundIconRes()
    is CaptureScreenshot -> getCaptureScreenshotIconRes()
}

internal fun Action.getActionDescription(context: Context, parent: Event?, inError: Boolean): String = when (this) {
    is Click -> getDescription(context, parent, inError)
    is Swipe -> getDescription(context, inError)
    is Pause -> getDescription(context, inError)
    is Intent -> getDescription(context, inError)
    is ToggleEvent -> getDescription(context, inError)
    is ChangeCounter -> getDescription(context, inError)
    is ExternalAction -> getDescription(context, inError)
    is Notification -> getDescription(context, inError)
    is SystemAction -> getDescription(context, inError)
    is SetText -> getDescription(context, inError)
    is PlaySound -> getDescription(context, inError)
    is CaptureScreenshot -> getDescription(context, inError)
}
