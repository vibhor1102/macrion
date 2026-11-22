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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent

import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import io.github.vibhor1102.macrion.core.common.permissions.ui.PermissionsHost
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RequestNotificationPermissionActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_REQUEST_ID = "starterRequestId"
        private const val STATE_HID_OVERLAYS = "hidOverlays"

        internal fun getStartIntent(context: Context, requestId: String): Intent =
            Intent(context, RequestNotificationPermissionActivity::class.java)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    @Inject lateinit var permissionController: PermissionsController
    @Inject lateinit var overlayManager: OverlayManager
    private var hidOverlays = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ownsRequest()) {
            finish()
            return
        }
        setContent {
            MacrionTheme {
                PermissionsHost(permissionController)
            }
        }

        hidOverlays = savedInstanceState?.getBoolean(STATE_HID_OVERLAYS)
            ?: !overlayManager.isOverlayStackHidden()
        if (hidOverlays) overlayManager.hideAll()
        onBackPressedDispatcher.addCallback(this) { finishActivity() }
        permissionController.startPermissionsUiFlow(
            this,
            listOf(PermissionPostNotification()),
            onAllGranted = ::finishActivity,
            onMandatoryDenied = ::finishActivity,
            resumeExisting = savedInstanceState != null,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_HID_OVERLAYS, hidOverlays)
        super.onSaveInstanceState(outState)
    }

    private fun ownsRequest(): Boolean =
        overlayManager.ownsStarterRequest(intent.getStringExtra(EXTRA_REQUEST_ID))

    private fun closeStarterOverlay() {
        if (!ownsRequest()) return
        overlayManager.navigateUp(this)
        if (hidOverlays) overlayManager.restoreVisibility()
        hidOverlays = false
    }

    private fun finishActivity() {
        if (isFinishing) return
        closeStarterOverlay()
        finish()
    }

    override fun onDestroy() {
        if (isFinishing) closeStarterOverlay()
        super.onDestroy()
    }
}
