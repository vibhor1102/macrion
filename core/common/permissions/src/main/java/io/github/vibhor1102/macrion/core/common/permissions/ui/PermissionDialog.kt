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
package io.github.vibhor1102.macrion.core.common.permissions.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.permissions.PermissionUiState
import io.github.vibhor1102.macrion.core.common.permissions.PermissionsController
import io.github.vibhor1102.macrion.core.common.permissions.R
import io.github.vibhor1102.macrion.core.common.permissions.model.Permission
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionAccessibilityService
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionOverlay
import io.github.vibhor1102.macrion.core.common.permissions.model.PermissionPostNotification
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface

@Composable
fun PermissionsHost(
    permissionsController: PermissionsController,
) {
    val uiState by permissionsController.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val state = uiState) {
        is PermissionUiState.Request -> {
            PermissionDialog(
                permission = state.permission,
                onPermissionResult = { isGranted ->
                    permissionsController.onPermissionResult(isGranted)
                },
                onDismissRequest = {
                    permissionsController.onPermissionResult(state.permission.checkIfGranted(context))
                },
            )
        }
        PermissionUiState.MandatoryDenied -> {
            MandatoryPermissionDeniedDialog(
                onConfirm = permissionsController::onMandatoryDeniedDismissed,
            )
        }
        PermissionUiState.Idle -> Unit
    }
}

@Composable
fun PermissionDialog(
    permission: Permission,
    onPermissionResult: (isGranted: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val dangerousLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted || permission.isOptional) {
            onPermissionResult(isGranted)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (permission.checkIfGranted(context) || (permission.isOptional && permission.hasBeenRequestedBefore(context))) {
                    onPermissionResult(permission.checkIfGranted(context))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val state = remember(permission) { permission.toPermissionDialogUiState() }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            MacrionDialogSurface {
                PermissionDialogContent(
                    titleRes = state.titleRes,
                    descriptionRes = state.descriptionRes,
                    onRequestPermission = {
                        if (permission is Permission.Dangerous && !permission.hasBeenRequestedBefore(context)) {
                            permission.markRequested(context)
                            dangerousLauncher.launch(permission.permissionString)
                        } else {
                            permission.startRequestFlowIfNeeded(context)
                        }
                    },
                    onDismiss = {
                        onPermissionResult(permission.checkIfGranted(context))
                    },
                )
            }
        }
    }
}

@Composable
fun MandatoryPermissionDeniedDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = {
            Text(
                text = stringResource(R.string.dialog_title_permission_mandatory_denied),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.message_permission_mandatory_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

internal data class PermissionDialogUiState(
    val permission: Permission,
    @field:StringRes val titleRes: Int,
    @field:StringRes val descriptionRes: Int,
)

internal fun Permission.toPermissionDialogUiState(): PermissionDialogUiState =
    when (this) {
        is PermissionOverlay -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_overlay,
            descriptionRes = R.string.message_permission_desc_overlay,
        )

        is PermissionPostNotification -> PermissionDialogUiState(
            permission = this,
            titleRes = when (purpose) {
                PermissionPostNotification.Purpose.GENERAL -> R.string.dialog_title_permission_notification
                PermissionPostNotification.Purpose.EXTERNAL_LAUNCH_FALLBACK ->
                    R.string.dialog_title_permission_launch_fallback_notification
            },
            descriptionRes = when (purpose) {
                PermissionPostNotification.Purpose.GENERAL -> R.string.message_permission_desc_notification
                PermissionPostNotification.Purpose.EXTERNAL_LAUNCH_FALLBACK ->
                    R.string.message_permission_desc_launch_fallback_notification
            },
        )

        is PermissionAccessibilityService -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_accessibility,
            descriptionRes = R.string.message_permission_desc_accessibility,
        )
    }
