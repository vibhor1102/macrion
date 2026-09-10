/*
 * Copyright (C) 2026 Vibhor
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vibhor1102.macrion.feature.smart.config.R

@Composable
internal fun EventListRow(
    name: String,
    conditionsCount: String,
    actionsCount: String,
    @StringRes enabledTextRes: Int,
    @DrawableRes enabledIconRes: Int,
    conditionIconRes: Int,
    actionsInError: Boolean,
    showReorderHandle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reorderHandleModifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(62.dp)
            .then(
                if (accessibilityActions.isEmpty()) Modifier
                else Modifier.semantics { customActions = accessibilityActions },
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showReorderHandle) {
            DragHandle(reorderHandleModifier, isBeingDragged)
            Spacer(Modifier.width(8.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }

        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 4.dp)) {
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                EventDetail(enabledIconRes, androidx.compose.ui.res.stringResource(enabledTextRes), null, Modifier.weight(1f))
                EventDetail(
                    R.drawable.ic_click,
                    actionsCount,
                    if (actionsInError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f),
                )
                EventDetail(conditionIconRes, conditionsCount, null, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.width(16.dp))
    }
}

/** Gives the active drag a visible state without reducing its 48dp touch target. */
@Composable
private fun DragHandle(
    reorderHandleModifier: Modifier,
    isBeingDragged: Boolean,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isBeingDragged) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                shape = CircleShape,
            )
            .then(reorderHandleModifier),
        contentAlignment = Alignment.Center,
    ) {
        LegacyIcon(R.drawable.ic_reorder, Modifier.size(24.dp))
    }
}

@Composable
private fun EventDetail(
    @DrawableRes iconRes: Int,
    text: String,
    tint: Color?,
    modifier: Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LegacyIcon(iconRes, Modifier.size(16.dp), tint)
        Text(text = text, color = tint ?: Color.Unspecified, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun LegacyIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier,
    tint: Color? = null,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = modifier,
        tint = tint ?: androidx.compose.ui.graphics.Color.Unspecified,
    )
}
