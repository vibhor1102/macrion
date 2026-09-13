/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.tutorial.ui.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.feature.tutorial.R
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategoryUiItems
import io.github.vibhor1102.macrion.feature.tutorial.domain.model.TutorialCategoryUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun TutorialListScreen(
    uiStateFlow: StateFlow<TutorialCategoryUiState>,
    onItemClicked: (TutorialCategoryUiItems.Item) -> Unit,
) {
    val uiState by uiStateFlow.collectAsStateWithLifecycle()

    when (val state = uiState) {
        TutorialCategoryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is TutorialCategoryUiState.Loaded -> LazyColumn(Modifier.fillMaxSize()) {
            items(state.items) { item ->
                when (item) {
                    is TutorialCategoryUiItems.Header -> TutorialCategoryHeader(item)
                    is TutorialCategoryUiItems.Item -> TutorialItemCard(item) { onItemClicked(item) }
                    TutorialCategoryUiItems.SectionDivider -> HorizontalDivider(
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialCategoryHeader(item: TutorialCategoryUiItems.Header) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(2.dp, colorResource(R.color.tutorial_header_card_border), MaterialTheme.shapes.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 16.dp).size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(item.categoryNameRes),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                )
            }
        }
        Text(
            text = stringResource(item.descriptionRes),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TutorialItemCard(item: TutorialCategoryUiItems.Item, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (item is TutorialCategoryUiItems.Item.Tutorial && item.tutorialCompleted) {
                Image(
                    painter = painterResource(item.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                Icon(
                    painter = painterResource(item.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(item.nameRes), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@DrawableRes
private fun TutorialCategoryUiItems.Item.iconRes(): Int = when (this) {
    is TutorialCategoryUiItems.Item.Category -> iconRes
    is TutorialCategoryUiItems.Item.Tutorial ->
        if (tutorialCompleted) R.drawable.ic_tutorial_completed else R.drawable.ic_tutorial_not_completed
    is TutorialCategoryUiItems.Item.Slideshow -> R.drawable.ic_tutorial_slideshow
}
