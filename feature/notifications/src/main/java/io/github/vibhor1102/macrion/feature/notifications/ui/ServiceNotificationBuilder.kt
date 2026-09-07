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
package io.github.vibhor1102.macrion.feature.notifications.ui

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.feature.notifications.R
import io.github.vibhor1102.macrion.feature.notifications.model.ServiceNotificationState


internal abstract class ServiceNotificationBuilder(
    context: Context,
    channelId: String,
) : NotificationCompat.Builder(context, channelId) {

    init {
        setColor(ContextCompat.getColor(context, R.color.macrion_icon_background))
    }

    abstract fun updateState(context: Context, state: ServiceNotificationState)
}

internal fun Context.newServiceNotificationBuilder(
    channelId: String,
    initialState: ServiceNotificationState,
    appComponentsProvider: AppComponentsProvider,
    forceLegacy: Boolean,
): ServiceNotificationBuilder {
    if (forceLegacy) return LegacyNotificationBuilder(this, channelId, initialState, appComponentsProvider)

    return try {
        CustomLayoutNotificationBuilder(this, channelId, initialState, appComponentsProvider)
            .apply { checkCustomViewsInflation(this@newServiceNotificationBuilder) }
    } catch (ex: Exception) {
        // Some devices doesn't support custom views in notification, use the regular format instead
        Log.w(TAG, "Can't create custom notification views; falling back to the legacy notification", ex)
        LegacyNotificationBuilder(this, channelId, initialState, appComponentsProvider)
    }
}

private const val TAG = "ServiceNotificationBuilder"
