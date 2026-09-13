/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.bindings.dialogs

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.ui.R

/** A Compose-native top bar for overlay dialogs with state-driven title and actions. */
class TopBarNavigationView(val context: Context) {
    private val title = mutableStateOf("")
    private val states = DialogNavigationButton.entries.associateWith {
        mutableStateOf(TopBarButtonState(it == DialogNavigationButton.DISMISS))
    }
    private val callbacks = mutableMapOf<DialogNavigationButton, () -> Unit>()

    private val buttonModifiers = DialogNavigationButton.entries.associateWith {
        mutableStateOf<@Composable () -> Modifier>({ Modifier })
    }

    fun setTitle(text: CharSequence) { title.value = text.toString() }
    fun setTitle(@StringRes text: Int) = setTitle(context.getText(text))
    fun setButtonEnabledState(type: DialogNavigationButton, enabled: Boolean) {
        update(type) { copy(enabled = enabled) }
    }
    fun setButtonVisibility(type: DialogNavigationButton, visibility: Int) =
        update(type) { copy(visible = visibility == View.VISIBLE) }
    fun setButtonClickListener(type: DialogNavigationButton, callback: () -> Unit) {
        callbacks[type] = callback
    }
    fun performButtonClick(type: DialogNavigationButton) {
        callbacks[type]?.invoke()
    }

    fun setButtonModifier(type: DialogNavigationButton, modifier: @Composable () -> Modifier) {
        buttonModifiers.getValue(type).value = modifier
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.dialog_top_bar_height)),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTopBarButton(DialogNavigationButton.DISMISS, R.drawable.ic_cancel)
                Text(
                    text = title.value,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DialogTopBarButton(DialogNavigationButton.DELETE, R.drawable.ic_delete)
                DialogTopBarButton(DialogNavigationButton.SAVE, R.drawable.ic_save_filled)
            }
        }
    }

    @Composable
    private fun DialogTopBarButton(type: DialogNavigationButton, icon: Int) {
        val state = states.getValue(type).value
        if (!state.visible) return
        val modifier = buttonModifiers.getValue(type).value()
        when (type) {
            DialogNavigationButton.DISMISS -> IconButton(
                onClick = { callbacks[type]?.invoke() },
                enabled = state.enabled,
                modifier = modifier,
            ) { Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
            DialogNavigationButton.DELETE -> FilledTonalIconButton(
                onClick = { callbacks[type]?.invoke() },
                enabled = state.enabled,
                modifier = modifier,
            ) { Icon(painterResource(icon), contentDescription = null) }
            DialogNavigationButton.SAVE -> FilledIconButton(
                onClick = { callbacks[type]?.invoke() },
                enabled = state.enabled,
                modifier = modifier,
            ) { Icon(painterResource(icon), contentDescription = null) }
        }
    }

    private fun update(type: DialogNavigationButton, change: TopBarButtonState.() -> TopBarButtonState) {
        states.getValue(type).let { it.value = it.value.change() }
    }
}

private data class TopBarButtonState(val visible: Boolean, val enabled: Boolean = true)
enum class DialogNavigationButton { DISMISS, DELETE, SAVE }
