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
package io.github.vibhor1102.macrion.core.ui.errors

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.github.vibhor1102.macrion.core.ui.R

import io.github.vibhor1102.macrion.core.ui.compose.createMacrionMessageDialog


fun Context.createNoMediaProjectionDialog(onValidated: () -> Unit): AlertDialog =
    createMacrionMessageDialog(
        title = R.string.dialog_overlay_title_warning,
        message = R.string.message_error_screen_capture_permission_dialog_not_found,
        onConfirm = {},
        onDismiss = onValidated,
    )
