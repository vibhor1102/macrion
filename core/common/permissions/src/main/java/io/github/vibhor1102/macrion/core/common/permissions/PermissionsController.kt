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
package io.github.vibhor1102.macrion.core.common.permissions

import android.content.Context
import android.util.Log
import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.github.vibhor1102.macrion.core.common.permissions.model.Permission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed interface PermissionUiState {
    data object Idle : PermissionUiState
    data class Request(val permission: Permission) : PermissionUiState
    data object MandatoryDenied : PermissionUiState
}

@ActivityRetainedScoped
class PermissionsController @Inject constructor() {

    private val permissionsRequestedLeft: MutableSet<Permission> = mutableSetOf()

    private val _uiState: MutableStateFlow<PermissionUiState> = MutableStateFlow(PermissionUiState.Idle)
    val uiState: StateFlow<PermissionUiState> = _uiState

    private var currentContext: Context? = null
    private var allGrantedCallback: (() -> Unit)? = null
    private var mandatoryDeniedCallback: (() -> Unit)? = null

    fun startPermissionsUiFlow(
        activity: Context,
        permissions: List<Permission>,
        onAllGranted: () -> Unit,
        onMandatoryDenied: (() -> Unit)? = null,
    ) {
        if (permissions.isEmpty()) return

        permissionsRequestedLeft.clear()
        allGrantedCallback = onAllGranted
        mandatoryDeniedCallback = onMandatoryDenied
        currentContext = activity

        permissions.forEach { permission ->
            if (!permission.checkIfGranted(activity)) permissionsRequestedLeft.add(permission)
        }

        Log.i(TAG, "Requesting missing permissions $permissions")

        handleNextPermission(activity)
    }

    private fun handleNextPermission(context: Context) {
        // All granted ? We are good
        if (permissionsRequestedLeft.isEmpty()) {
            Log.i(TAG, "All permission are granted !")
            notifyAllGranted()
            return
        }

        // Take the next permission on the list
        val nextPermission = permissionsRequestedLeft.popFirst()

        // This permission is optional and has already been requested, skip it
        if (nextPermission.isOptionalAndRequestedBefore(context)) {
            Log.d(TAG, "Skipping already requested permission $nextPermission")
            handleNextPermission(context)
            return
        }

        // Show the dialog and handle the result
        Log.i(TAG, "show permission dialog for $nextPermission")
        _uiState.value = PermissionUiState.Request(nextPermission)
    }

    fun onPermissionResult(isGranted: Boolean) {
        val currentRequest = _uiState.value as? PermissionUiState.Request ?: return
        val currentPermission = currentRequest.permission
        Log.i(TAG, "onPermissionResult: $currentPermission isGranted=$isGranted")

        val context = currentContext
        if (isGranted || currentPermission.isOptional) {
            _uiState.value = PermissionUiState.Idle
            if (context != null) {
                handleNextPermission(context)
            } else {
                notifyAllGranted()
            }
        } else {
            _uiState.value = PermissionUiState.MandatoryDenied
        }
    }

    fun onMandatoryDeniedDismissed() {
        notifyMandatoryDenied()
    }

    private fun notifyAllGranted() {
        _uiState.value = PermissionUiState.Idle
        allGrantedCallback?.invoke()
        clear()
    }

    private fun notifyMandatoryDenied() {
        _uiState.value = PermissionUiState.Idle
        mandatoryDeniedCallback?.invoke()
        clear()
    }

    private fun clear() {
        allGrantedCallback = null
        mandatoryDeniedCallback = null
        currentContext = null
        _uiState.value = PermissionUiState.Idle
        permissionsRequestedLeft.clear()
    }

    private fun Permission.isOptionalAndRequestedBefore(context: Context) =
        isOptional && hasBeenRequestedBefore(context)

    private fun MutableSet<Permission>.popFirst(): Permission =
        first().also(::remove)
}

private const val TAG = "PermissionsController"
