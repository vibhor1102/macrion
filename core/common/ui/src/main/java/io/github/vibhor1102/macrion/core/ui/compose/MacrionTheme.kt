/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.ui.compose

import android.os.Build
import androidx.annotation.ColorRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.core.ui.R

val OverlayDialogCornerRadius = 28.dp
val OverlayDialogShape = RoundedCornerShape(topStart = OverlayDialogCornerRadius, topEnd = OverlayDialogCornerRadius)

@Composable
fun MacrionTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> macrionDarkColorScheme()
        else -> macrionLightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Provides the Material content color expected by Compose content hosted in a legacy dialog surface. */
@Composable
fun MacrionDialogSurface(content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) {
            focusManager.clearFocus()
            keyboardController?.hide()
        },
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

@Composable
private fun macrionDarkColorScheme() = darkColorScheme(
    primary = composeColor(R.color.md_theme_dark_primary),
    onPrimary = composeColor(R.color.md_theme_dark_onPrimary),
    primaryContainer = composeColor(R.color.md_theme_dark_primaryContainer),
    onPrimaryContainer = composeColor(R.color.md_theme_dark_onPrimaryContainer),
    secondary = composeColor(R.color.md_theme_dark_secondary),
    onSecondary = composeColor(R.color.md_theme_dark_onSecondary),
    secondaryContainer = composeColor(R.color.md_theme_dark_secondaryContainer),
    onSecondaryContainer = composeColor(R.color.md_theme_dark_onSecondaryContainer),
    background = composeColor(R.color.md_theme_dark_background),
    onBackground = composeColor(R.color.md_theme_dark_onBackground),
    surface = composeColor(R.color.md_theme_dark_surface),
    onSurface = composeColor(R.color.md_theme_dark_onSurface),
    surfaceVariant = composeColor(R.color.md_theme_dark_surfaceVariant),
    onSurfaceVariant = composeColor(R.color.md_theme_dark_onSurfaceVariant),
    outline = composeColor(R.color.md_theme_dark_outline),
)

@Composable
private fun macrionLightColorScheme() = lightColorScheme(
    primary = composeColor(R.color.md_theme_light_primary),
    onPrimary = composeColor(R.color.md_theme_light_onPrimary),
    primaryContainer = composeColor(R.color.md_theme_light_primaryContainer),
    onPrimaryContainer = composeColor(R.color.md_theme_light_onPrimaryContainer),
    secondary = composeColor(R.color.md_theme_light_secondary),
    onSecondary = composeColor(R.color.md_theme_light_onSecondary),
    secondaryContainer = composeColor(R.color.md_theme_light_secondaryContainer),
    onSecondaryContainer = composeColor(R.color.md_theme_light_onSecondaryContainer),
    background = composeColor(R.color.md_theme_light_background),
    onBackground = composeColor(R.color.md_theme_light_onBackground),
    surface = composeColor(R.color.md_theme_light_surface),
    onSurface = composeColor(R.color.md_theme_light_onSurface),
    surfaceVariant = composeColor(R.color.md_theme_light_surfaceVariant),
    onSurfaceVariant = composeColor(R.color.md_theme_light_onSurfaceVariant),
    outline = composeColor(R.color.md_theme_light_outline),
)

@Composable
private fun composeColor(@ColorRes color: Int): Color = colorResource(color)
