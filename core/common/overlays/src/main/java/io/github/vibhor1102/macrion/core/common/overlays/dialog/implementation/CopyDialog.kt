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
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation

import android.view.ViewGroup

import androidx.annotation.StyleRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow

import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.ui.R as UiR

abstract class CopyDialog(
    @StyleRes theme: Int,
) : OverlayDialog(theme) {

    /** The resource id for the dialog title. */
    protected abstract val titleRes: Int
    /** The resource id for the search hint text. */
    protected abstract val searchHintRes: Int
    /** The resource id for the text displayed when there is nothing to copy. */
    protected abstract val emptyRes: Int

    abstract override fun onCreateView(): ViewGroup

    abstract fun onSearchQueryChanged(newText: String?)
    abstract fun onCopyClicked()
}

@Composable
fun CopySearchTopBar(
    @androidx.annotation.StringRes titleRes: Int,
    @androidx.annotation.StringRes searchHintRes: Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onCopy: () -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val foreground = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(searching) {
        if (searching) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .shadow(3.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searching) {
            Icon(
                painter = painterResource(UiR.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp),
                tint = foreground,
            )
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChanged(it)
                },
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp).focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = foreground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(searchHintRes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
        } else {
            IconButton(onClick = onDismiss) {
                Icon(painterResource(UiR.drawable.ic_cancel), contentDescription = null, tint = foreground)
            }
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = foreground,
                maxLines = 1,
            )
        }
        IconButton(onClick = {
            searching = !searching
            if (!searching) {
                query = ""
                onQueryChanged("")
            }
        }) {
            Icon(
                painter = painterResource(if (searching) UiR.drawable.ic_cancel else UiR.drawable.ic_search),
                contentDescription = null,
                tint = foreground,
            )
        }
        if (!searching) {
            FilledIconButton(onClick = onCopy) {
                Icon(painterResource(UiR.drawable.ic_copy), contentDescription = null)
            }
        }
    }
}
