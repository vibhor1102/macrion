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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import io.github.vibhor1102.macrion.core.ui.R

/**
 * A Compose-native FAB cluster controller for dialog primary/secondary actions.
 */
class FloatingActionButtonsView(val context: Context) {
    var isVisible by mutableStateOf(false)

    inner class RootShim {
        var visibility: Int
            get() = if (isVisible) View.VISIBLE else View.GONE
            set(value) { isVisible = (value == View.VISIBLE) }
    }
    val root = RootShim()

    inner class ButtonShim {
        var contentDescription: CharSequence?
            get() = primaryDescription.value
            set(value) { primaryDescription.value = value }
    }
    val primary = ButtonShim()

    private val primaryDescription = mutableStateOf<CharSequence?>(null)
    private val primaryIcon = mutableIntStateOf(R.drawable.ic_add)
    private val secondaryIcon = mutableIntStateOf(R.drawable.ic_copy)
    private val badgeText = mutableStateOf<String?>(null)
    private val secondaryVisible = mutableStateOf(false)
    var primaryModifier by mutableStateOf<@Composable () -> Modifier>({ Modifier })
    private var onPrimary: () -> Unit = {}
    private var onSecondary: () -> Unit = {}

    fun configure(
        @DrawableRes primaryIcon: Int,
        @DrawableRes secondaryIcon: Int,
        onPrimary: () -> Unit,
        onSecondary: () -> Unit,
    ) {
        this.primaryIcon.intValue = primaryIcon
        this.secondaryIcon.intValue = secondaryIcon
        this.onPrimary = onPrimary
        this.onSecondary = onSecondary
    }

    fun performPrimaryClick() { onPrimary() }
    fun performSecondaryClick() { onSecondary() }

    fun setSecondaryVisible(visible: Boolean) {
        secondaryVisible.value = visible
    }

    fun setBadge(text: String?, description: CharSequence? = null) {
        badgeText.value = text
        primaryDescription.value = description
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) return
        Box(modifier.wrapContentSize(), contentAlignment = Alignment.BottomCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (secondaryVisible.value) {
                    SmallFloatingActionButton(onClick = onSecondary) {
                        Icon(painterResource(secondaryIcon.intValue), contentDescription = null)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Box(Modifier.size(PRIMARY_CONTAINER_SIZE_DP.dp), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = onPrimary,
                        modifier = Modifier.size(56.dp).then(primaryModifier()),
                    ) {
                        Icon(
                            painterResource(primaryIcon.intValue),
                            contentDescription = primaryDescription.value?.toString(),
                        )
                    }
                    badgeText.value?.let { Badge(Modifier.align(Alignment.TopEnd).size(18.dp)) { Text(it) } }
                }
            }
        }
    }

    private companion object {
        const val PRIMARY_CONTAINER_SIZE_DP = 64
    }
}
