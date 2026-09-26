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
package io.github.vibhor1102.macrion.feature.notifications

import io.github.vibhor1102.macrion.feature.notifications.model.ServiceNotificationAction

interface ServiceNotificationListener {
    fun onPlay(): Unit?
    fun onPause(): Unit?
    fun onShow(): Unit?
    fun onHide(): Unit?
    fun onStop(): Unit?
    fun onSwitch(): Unit?
    fun onDismiss(): Unit?
}

internal fun ServiceNotificationListener.notifyAction(action: ServiceNotificationAction) =
    when (action) {
        ServiceNotificationAction.Play -> onPlay()
        ServiceNotificationAction.Pause -> onPause()
        ServiceNotificationAction.Show -> onShow()
        ServiceNotificationAction.Hide -> onHide()
        ServiceNotificationAction.Switch -> onSwitch()
        ServiceNotificationAction.Stop -> onStop()
        ServiceNotificationAction.Dismiss -> onDismiss()
        ServiceNotificationAction.Config -> Unit
    }
