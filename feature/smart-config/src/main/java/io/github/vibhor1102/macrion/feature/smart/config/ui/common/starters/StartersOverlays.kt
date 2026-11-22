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
package io.github.vibhor1102.macrion.feature.smart.config.ui.common.starters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

import io.github.vibhor1102.macrion.core.base.data.getNotificationSettingsIntent
import io.github.vibhor1102.macrion.core.base.data.getOpenWebBrowserIntent
import io.github.vibhor1102.macrion.core.base.data.getOpenWebBrowserPickerIntent
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.common.overlays.menu.implementation.ActivityStarterOverlayMenu
import java.util.UUID


internal fun newWebBrowserStarterOverlay(uri: Uri) = ActivityStarterOverlayMenu(
    intent = getOpenWebBrowserIntent(uri),
    fallbackIntent = getOpenWebBrowserPickerIntent(uri),
)

fun OverlayManager.requestScreenCapture(context: Context) {
    if (getBackStackTop() is ScreenCaptureStarterOverlay) return
    navigateTo(
        context = context,
        newOverlay = ScreenCaptureStarterOverlay(context),
        hideCurrent = true,
    )
}

internal fun OverlayManager.ownsStarterRequest(requestId: String?): Boolean {
    val starter = getBackStackTop() as? OwnedActivityStarterOverlay ?: return false
    return starter.requestId == requestId && isCurrentOverlay(starter)
}

internal open class OwnedActivityStarterOverlay(
    val requestId: String = UUID.randomUUID().toString(),
    intent: (String) -> Intent,
) : ActivityStarterOverlayMenu(intent(requestId))

private class ScreenCaptureStarterOverlay(context: Context) :
    OwnedActivityStarterOverlay(intent = { RestartMediaProjectionActivity.getStartIntent(context, it) })

fun OverlayManager.isRequestingNotificationPermission(): Boolean =
    getBackStackTop() is NotificationPermissionStarterOverlay

internal fun newNotificationPermissionStarterOverlay(context: Context): ActivityStarterOverlayMenu =
    NotificationPermissionStarterOverlay(context)

private class NotificationPermissionStarterOverlay(context: Context) :
    OwnedActivityStarterOverlay(intent = { RequestNotificationPermissionActivity.getStartIntent(context, it) })

@RequiresApi(Build.VERSION_CODES.O)
internal fun newNotificationSettingsStarterOverlay() = ActivityStarterOverlayMenu(
    intent = getNotificationSettingsIntent(),
)

internal fun newPickFolderStarterOverlay(context: Context) = OwnedActivityStarterOverlay {
    PickFolderActivity.getStartIntent(context, it)
}