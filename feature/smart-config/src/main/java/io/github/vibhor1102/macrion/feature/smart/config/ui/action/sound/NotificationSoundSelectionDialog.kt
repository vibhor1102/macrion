/*
 * Copyright (C) 2026 Vibhor Goel
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
package io.github.vibhor1102.macrion.feature.smart.config.ui.action.sound

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vibhor1102.macrion.core.common.overlays.base.viewModels
import io.github.vibhor1102.macrion.core.common.overlays.dialog.OverlayDialog
import io.github.vibhor1102.macrion.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import io.github.vibhor1102.macrion.core.ui.compose.MacrionTheme
import io.github.vibhor1102.macrion.core.ui.compose.OverlayDialogShape
import io.github.vibhor1102.macrion.feature.smart.config.R
import io.github.vibhor1102.macrion.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import io.github.vibhor1102.macrion.feature.smart.config.domain.sound.NotificationSoundItem

class NotificationSoundSelectionDialog : OverlayDialog(R.style.ScenarioConfigTheme) {

    override fun tutorialMonitoringTag(): String =
        MonitoredOverlayType.NOTIFICATION_SOUND_SELECTION.name

    private val viewModel: PlaySoundViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { playSoundViewModel() },
    )

    override fun onCreateView(): ViewGroup = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { MacrionTheme { this@NotificationSoundSelectionDialog.Content() } }
    }

    override fun onDestroy() {
        viewModel.stopPreview()
        super.onDestroy()
    }

    @Composable
    private fun Content() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val ui = state ?: return

        val selectedIndex = remember(ui.sounds, ui.selectedUri) {
            ui.sounds.indexOfFirst { it.uri == ui.selectedUri }
        }
        val initialIndex = remember {
            val idx = ui.sounds.indexOfFirst { it.uri == ui.selectedUri }
            if (idx > 1) idx - 1 else if (idx >= 0) idx else 0
        }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

        LaunchedEffect(Unit) {
            if (selectedIndex >= 0) {
                val target = if (selectedIndex > 1) selectedIndex - 1 else selectedIndex
                listState.scrollToItem(target)
            }
        }

        Surface(
            shape = OverlayDialogShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.fillMaxWidth()) {
                TopBar()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(ui.sounds, key = { it.uri }) { sound ->
                        val isSelected = sound.uri == ui.selectedUri
                        val isPlaying = sound.uri == ui.playingPreviewUri
                        SoundItemRow(
                            sound = sound,
                            isSelected = isSelected,
                            isPlaying = isPlaying,
                            onSelect = {
                                viewModel.setSound(sound)
                                viewModel.playPreview(sound.uri)
                            },
                            onTogglePreview = { viewModel.togglePreview(sound.uri) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = ::finishSelection) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = null)
            }
            Text(
                text = stringResource(R.string.dialog_title_select_sound),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            FilledIconButton(onClick = ::finishSelection) {
                Icon(painterResource(R.drawable.ic_confirm), contentDescription = null)
            }
        }
    }

    @Composable
    private fun SoundItemRow(
        sound: NotificationSoundItem,
        isSelected: Boolean,
        isPlaying: Boolean,
        onSelect: () -> Unit,
        onTogglePreview: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = sound.title,
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onTogglePreview) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play_arrow
                    ),
                    contentDescription = null,
                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private fun finishSelection() {
        viewModel.stopPreview()
        super.back()
    }

    override fun back() {
        viewModel.stopPreview()
        super.back()
    }
}
