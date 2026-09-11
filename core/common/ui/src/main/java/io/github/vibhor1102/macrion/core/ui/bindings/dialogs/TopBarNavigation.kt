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
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.ui.R
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme

/** A single Compose top bar with Android-only anchors for tutorial coordinates and programmatic taps. */
class TopBarNavigationView(context: Context) : FrameLayout(context) {
    val root: View get() = this
    val buttonDismiss = createAnchor(DialogNavigationButton.DISMISS, Gravity.START or Gravity.CENTER_VERTICAL, 8)
    val buttonDelete = createAnchor(DialogNavigationButton.DELETE, Gravity.END or Gravity.CENTER_VERTICAL, 64)
    val buttonSave = createAnchor(DialogNavigationButton.SAVE, Gravity.END or Gravity.CENTER_VERTICAL, 8)

    private val title = mutableStateOf("")
    private val states = DialogNavigationButton.entries.associateWith {
        mutableStateOf(TopBarButtonState(it == DialogNavigationButton.DISMISS))
    }
    private val callbacks = mutableMapOf<DialogNavigationButton, () -> Unit>()

    init {
        elevation = 3 * resources.displayMetrics.density
        addView(
            ComposeView(context).apply { setContent { MacrionTheme { DialogTopBarContent() } } },
            LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.dialog_top_bar_height)),
        )
    }

    fun setTitle(text: CharSequence) { title.value = text.toString() }
    fun setTitle(@StringRes text: Int) = setTitle(context.getText(text))
    fun setButtonEnabledState(type: DialogNavigationButton, enabled: Boolean) {
        buttonFor(type).isEnabled = enabled
        update(type) { copy(enabled = enabled) }
    }
    fun setButtonVisibility(type: DialogNavigationButton, visibility: Int) = update(type) { copy(visible = visibility == View.VISIBLE) }
    fun setButtonClickListener(type: DialogNavigationButton, callback: () -> Unit) { callbacks[type] = callback }

    @androidx.compose.runtime.Composable
    private fun DialogTopBarContent() {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogTopBarButton(DialogNavigationButton.DISMISS, R.drawable.ic_cancel)
                Text(
                    text = title.value,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DialogTopBarButton(DialogNavigationButton.DELETE, R.drawable.ic_delete)
                DialogTopBarButton(DialogNavigationButton.SAVE, R.drawable.ic_save_filled)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DialogTopBarButton(type: DialogNavigationButton, icon: Int) {
        val state = states.getValue(type).value
        if (!state.visible) return
        when (type) {
            DialogNavigationButton.DISMISS -> IconButton(
                onClick = { callbacks[type]?.invoke() }, enabled = state.enabled,
            ) { Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
            DialogNavigationButton.DELETE -> FilledTonalIconButton(
                onClick = { callbacks[type]?.invoke() }, enabled = state.enabled,
            ) { Icon(painterResource(icon), contentDescription = null) }
            DialogNavigationButton.SAVE -> FilledIconButton(
                onClick = { callbacks[type]?.invoke() }, enabled = state.enabled,
            ) { Icon(painterResource(icon), contentDescription = null) }
        }
    }

    private fun createAnchor(type: DialogNavigationButton, gravity: Int, marginDp: Int): View =
        View(context).apply {
            visibility = INVISIBLE
            setOnClickListener { callbacks[type]?.invoke() }
            this@TopBarNavigationView.addView(this, LayoutParams(48.dpPx, 48.dpPx, gravity).apply {
                marginStart = marginDp.dpPx
                marginEnd = marginDp.dpPx
            })
        }

    private fun buttonFor(type: DialogNavigationButton): View = when (type) {
        DialogNavigationButton.DISMISS -> buttonDismiss
        DialogNavigationButton.DELETE -> buttonDelete
        DialogNavigationButton.SAVE -> buttonSave
    }

    private fun update(type: DialogNavigationButton, change: TopBarButtonState.() -> TopBarButtonState) {
        states.getValue(type).let { it.value = it.value.change() }
    }

    private val Int.dpPx get() = (this * resources.displayMetrics.density).toInt()
}

private data class TopBarButtonState(val visible: Boolean, val enabled: Boolean = true)
enum class DialogNavigationButton { DISMISS, DELETE, SAVE }
