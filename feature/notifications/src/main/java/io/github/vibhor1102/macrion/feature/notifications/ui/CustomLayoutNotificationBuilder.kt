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

import android.app.Notification
import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import io.github.vibhor1102.macrion.core.base.data.AppComponentsProvider
import io.github.vibhor1102.macrion.feature.notifications.R
import io.github.vibhor1102.macrion.core.ui.utils.notificationIconResId
import io.github.vibhor1102.macrion.feature.notifications.model.ServiceNotificationAction
import io.github.vibhor1102.macrion.feature.notifications.model.ServiceNotificationState
import io.github.vibhor1102.macrion.feature.notifications.model.getPendingIntent

internal class CustomLayoutNotificationBuilder(
    context: Context,
    channelId: String,
    initialState: ServiceNotificationState,
    private val appComponentsProvider: AppComponentsProvider,
) : ServiceNotificationBuilder(context, channelId) {

    init {
        setSmallIcon(notificationIconResId())
        setCategory(Notification.CATEGORY_SERVICE)
        setOngoing(true)
        setLocalOnly(true)
        setStyle(NotificationCompat.DecoratedCustomViewStyle())
        setContentIntent(ServiceNotificationAction.Config.getPendingIntent(context, appComponentsProvider))

        updateState(context, initialState)
    }

    fun checkCustomViewsInflation(context: Context) {
        val parent = FrameLayout(context)
        contentView?.apply(context, parent)
        bigContentView?.apply(context, parent)
    }

    override fun updateState(context: Context, state: ServiceNotificationState) {
        setCustomContentView(getCustomContentView(context, state))
        setCustomBigContentView(getCustomBigContentView(context, state))
    }

    private fun getCustomContentView(context: Context, state: ServiceNotificationState): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_service).apply {
            setTextViewText(
                R.id.text_scenario_name,
                context.getString(R.string.notification_title, "\n${state.scenarioName}")
            )

            addAction(
                context,
                R.id.button_play_pause,
                if (state.isScenarioRunning) ServiceNotificationAction.Pause else ServiceNotificationAction.Play
            )
            addAction(
                context,
                R.id.button_show_hide,
                if (state.isMenuVisible) ServiceNotificationAction.Hide else ServiceNotificationAction.Show
            )
        }

    private fun getCustomBigContentView(context: Context, state: ServiceNotificationState): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_service_big).apply {
            setTextViewText(R.id.text_scenario_name, state.scenarioName)

            addAction(
                context,
                R.id.button_play_pause,
                if (state.isScenarioRunning) ServiceNotificationAction.Pause else ServiceNotificationAction.Play
            )
            addAction(
                context,
                R.id.button_show_hide,
                if (state.isMenuVisible) ServiceNotificationAction.Hide else ServiceNotificationAction.Show
            )
            addAction(context, R.id.button_switch, ServiceNotificationAction.Switch)
            addAction(context, R.id.button_exit, ServiceNotificationAction.Stop)
        }

    private fun RemoteViews.addAction(context: Context, @IdRes viewId: Int, action: ServiceNotificationAction) {
        setImageViewResource(viewId, action.iconRes)
        setOnClickPendingIntent(viewId, action.getPendingIntent(context, appComponentsProvider))
    }
}

