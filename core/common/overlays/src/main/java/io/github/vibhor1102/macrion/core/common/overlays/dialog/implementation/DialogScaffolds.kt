/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation

import android.view.View

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import io.github.vibhor1102.macrion.core.common.overlays.R
import io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation.navbar.DialogNavigationItem


@Composable
internal fun DialogNavigation(
    items: List<DialogNavigationItem>,
    selectedItemId: Int,
    missingInputBadges: Map<Int, Boolean>,
    isPortrait: Boolean,
    onItemSelected: (Int) -> Unit,
    itemModifier: @Composable (DialogNavigationItem) -> Modifier = { Modifier },
) {
    if (isPortrait) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    modifier = itemModifier(item),
                    selected = item.id == selectedItemId,
                    onClick = { onItemSelected(item.id) },
                    icon = { NavigationItemIcon(item, missingInputBadges[item.id] == true) },
                    label = { androidx.compose.material3.Text(stringResource(item.labelRes)) },
                    alwaysShowLabel = false,
                )
            }
        }
    } else {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.weight(1f))
            items.forEach { item ->
                NavigationRailItem(
                    modifier = itemModifier(item),
                    selected = item.id == selectedItemId,
                    onClick = { onItemSelected(item.id) },
                    icon = { NavigationItemIcon(item, missingInputBadges[item.id] == true) },
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NavigationItemIcon(item: DialogNavigationItem, hasMissingInput: Boolean) {
    BadgedBox(badge = { if (hasMissingInput) Badge() }) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = stringResource(item.labelRes),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun NavBarDialogScaffold(
    topBar: View,
    persistentHeader: View,
    content: View,
    navBar: View?,
    floatingActions: View?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .heightIn(min = dimensionResource(R.dimen.bottom_sheet_min_height))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        AndroidView(factory = { topBar }, modifier = Modifier.fillMaxWidth())

        if (navBar == null) {
            AndroidView(factory = { persistentHeader }, modifier = Modifier.fillMaxWidth())
            AndroidView(
                factory = { content },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = dimensionResource(R.dimen.android_bottom_navigation_height)),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { navBar }, modifier = Modifier.fillMaxHeight())
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AndroidView(factory = { persistentHeader }, modifier = Modifier.fillMaxWidth())
                        AndroidView(factory = { content }, modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                    floatingActions?.let { actions ->
                        AndroidView(
                            factory = { actions },
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomEnd)
                                .padding(
                                    end = dimensionResource(R.dimen.margin_horizontal_default),
                                    bottom = dimensionResource(R.dimen.margin_vertical_extra_large),
                                ),
                        )
                    }
                }
            }
        }
    }
}
