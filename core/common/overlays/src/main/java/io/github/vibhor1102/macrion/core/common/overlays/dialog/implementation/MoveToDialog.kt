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

import android.app.Dialog
import android.view.KeyEvent
import android.view.WindowManager

import androidx.annotation.StyleRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.base.BaseOverlay
import io.github.vibhor1102.macrion.core.common.overlays.manager.OverlayManager
import io.github.vibhor1102.macrion.core.ui.R as UiR
import io.github.vibhor1102.macrion.core.ui.compose.MacrionDialogSurface
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.utils.getDynamicColorsContext
class MoveToDialog(
    @StyleRes theme: Int,
    private val defaultValue: Int,
    private val itemCount: Int,
    private val onValueSelected: ((Int) -> Unit),
) : BaseOverlay(theme, recreateOnRotation = true) {

    private var currentValue by mutableStateOf(defaultValue.toString())
    private var requestFieldFocus by mutableStateOf(false)

    /** Tells if the dialog is visible. */
    private var isShown = false
    private var dialog: Dialog? = null

    override fun onCreate() {
        val content = ComposeView(context).apply {
            setContent {
                MacrionTheme {
                    MacrionDialogSurface {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_move_to_title),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            MoveToPositionField(
                                value = currentValue,
                                itemCount = itemCount,
                                requestFocus = requestFieldFocus,
                                onValueChanged = { value ->
                                    currentValue = value
                                },
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { back() }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                                TextButton(
                                    onClick = { validateCurrentValueAndClose() },
                                    enabled = currentValue.toEditedValue() != null,
                                ) {
                                    Text(stringResource(android.R.string.ok))
                                }
                            }
                        }
                    }
                }
            }
        }

        content.setViewTreeLifecycleOwner(this)
        content.setViewTreeSavedStateRegistryOwner(this)
        content.setViewTreeViewModelStoreOwner(this)

        dialog = ComponentDialog(context.getDynamicColorsContext(R.style.AppTheme)).apply compDialog@ {
            content.setViewTreeOnBackPressedDispatcherOwner(this)
            onBackPressedDispatcher.addCallback(this@MoveToDialog) {
                this@MoveToDialog.back()
            }
            setContentView(content)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    onBackPressedDispatcher.onBackPressed()
                    true
                } else {
                    false
                }
            }
            setOnDismissListener {
                dialog = null
                destroy()
            }
            create()
            window?.apply {
                decorView.setViewTreeLifecycleOwner(this@MoveToDialog)
                decorView.setViewTreeSavedStateRegistryOwner(this@MoveToDialog)
                decorView.setViewTreeViewModelStoreOwner(this@MoveToDialog)
                decorView.setViewTreeOnBackPressedDispatcherOwner(this@compDialog)
                setBackgroundDrawableResource(android.R.color.transparent)
                setType(OverlayManager.OVERLAY_WINDOW_TYPE)
            }
        }
    }

    override fun onStart() {
        if (isShown) return

        isShown = true
        dialog?.show()
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        requestFieldFocus = true
    }

    override fun onStop() {
        if (!isShown) return

        dialog?.hide()
        requestFieldFocus = false
        isShown = false
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }

    private fun validateCurrentValueAndClose() {
        currentValue.toEditedValue()?.let { value ->
            onValueSelected(value)
            back()
        }
    }

    private fun String.toEditedValue(): Int? =
        try {
            toInt()
        } catch (nfEx: NumberFormatException) {
            null
        }
}

@Composable
private fun MoveToPositionField(
    value: String,
    itemCount: Int,
    requestFocus: Boolean,
    onValueChanged: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    LaunchedEffect(requestFocus) {
        if (!requestFocus) return@LaunchedEffect
        focusRequester.requestFocus()
    }
    LaunchedEffect(fieldFocused) {
        if (!fieldFocused) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        keyboardController?.show()
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { candidate ->
            if (candidate.text.isEmpty() || candidate.text.toIntOrNull()?.let { it in 1..itemCount } == true) {
                fieldValue = candidate
                onValueChanged(candidate.text)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { fieldFocused = it.isFocused },
        label = { Text(stringResource(R.string.dialog_move_to_position_label)) },
        placeholder = { Text("Max: $itemCount") },
        trailingIcon = if (fieldValue.text.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChanged("") }) {
                    Icon(painterResource(UiR.drawable.ic_cancel), contentDescription = null)
                }
            }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            keyboardController?.hide()
        }),
    )
}
