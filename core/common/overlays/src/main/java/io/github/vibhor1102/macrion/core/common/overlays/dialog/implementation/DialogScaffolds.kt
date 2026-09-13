/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.core.common.overlays.dialog.implementation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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
                    label = { Text(stringResource(item.labelRes)) },
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
    topBar: @Composable () -> Unit,
    persistentHeader: @Composable () -> Unit,
    content: @Composable () -> Unit,
    navBar: @Composable () -> Unit,
    floatingActions: @Composable () -> Unit,
    isPortrait: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.bottom_sheet_min_height)),
    ) {
        if (isPortrait) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth()) { topBar() }
                    Box(modifier = Modifier.fillMaxWidth()) { persistentHeader() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = dimensionResource(R.dimen.android_bottom_navigation_height)),
                    ) {
                        content()
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = dimensionResource(R.dimen.margin_horizontal_default),
                            bottom = dimensionResource(R.dimen.dialog_create_copy_buttons_bottom_margin),
                        ),
                ) {
                    floatingActions()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    navBar()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth()) { topBar() }
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(modifier = Modifier.fillMaxHeight()) { navBar() }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.fillMaxWidth()) { persistentHeader() }
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) { content() }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = dimensionResource(R.dimen.margin_horizontal_default),
                                    bottom = dimensionResource(R.dimen.margin_vertical_extra_large),
                                ),
                        ) {
                            floatingActions()
                        }
                    }
                }
            }
        }
    }
}
