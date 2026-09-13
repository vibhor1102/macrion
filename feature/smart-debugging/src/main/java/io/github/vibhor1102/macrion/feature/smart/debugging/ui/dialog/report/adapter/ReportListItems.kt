/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.debugging.ui.dialog.report.adapter

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vibhor1102.macrion.feature.smart.debugging.R

private val reportPrimaryColor
    @Composable get() = MaterialTheme.colorScheme.onSurface

private val reportSecondaryColor
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
internal fun ReportSectionHeader(
    title: String,
    @DrawableRes iconRes: Int,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = reportPrimaryColor,
            )
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp).size(24.dp),
                colorFilter = ColorFilter.tint(reportPrimaryColor),
            )
        }
    }
}

@Composable
internal fun ReportKeyValueCard(title: String, value: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = reportPrimaryColor,
            )
            Text(
                text = value,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = reportPrimaryColor,
            )
        }
    }
}

@Composable
internal fun ReportNameValueRow(
    name: String,
    value: String,
    modifier: Modifier = Modifier,
    valueContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = reportPrimaryColor,
            )
            if (valueContent == null) {
                Text(text = value, style = MaterialTheme.typography.bodyLarge, color = reportPrimaryColor)
            } else {
                valueContent()
            }
        }
        HorizontalDivider()
    }
}

@Composable
internal fun ReportIconTransition(
    @DrawableRes startIcon: Int?,
    @DrawableRes endIcon: Int,
    separator: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        startIcon?.let {
            ReportStateIcon(it)
            Text(separator, style = MaterialTheme.typography.bodyLarge, color = reportPrimaryColor)
        }
        ReportStateIcon(endIcon)
    }
}

@Composable
private fun ReportStateIcon(@DrawableRes iconRes: Int) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(reportPrimaryColor),
    )
}

@Composable
internal fun ReportActivityRow(name: String, count: String, reached: Boolean) {
    ReportNameValueRow(
        name = name,
        value = count,
        modifier = Modifier.alpha(if (reached) 1f else 0.6f),
    )
}

@Composable
internal fun ReportTriggerConditionCard(
    name: String,
    description: String,
    @DrawableRes iconRes: Int,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(reportSecondaryColor),
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = reportPrimaryColor,
                )
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = reportSecondaryColor,
                )
            }
        }
    }
}
