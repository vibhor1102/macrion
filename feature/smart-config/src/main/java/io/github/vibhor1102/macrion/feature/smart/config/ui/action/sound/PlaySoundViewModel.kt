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

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.domain.model.action.PlaySound
import io.github.vibhor1102.macrion.feature.smart.config.domain.EditionRepository
import io.github.vibhor1102.macrion.feature.smart.config.domain.sound.NotificationSoundItem
import io.github.vibhor1102.macrion.feature.smart.config.domain.sound.NotificationSoundRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
class PlaySoundViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val editionRepository: EditionRepository,
    private val soundRepository: NotificationSoundRepository,
) : ViewModel() {

    private val configuredPlaySound = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<PlaySound>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    private val availableSounds: List<NotificationSoundItem> =
        soundRepository.getNotificationSounds()

    private val playingPreviewUri = MutableStateFlow<String?>(null)
    private var previewPlayer: MediaPlayer? = null

    val uiState: StateFlow<PlaySoundDialogUiState?> =
        combine(configuredPlaySound, editedActionHasChanged, playingPreviewUri) { sound, hasChanged, previewUri ->
            sound.toUiState(hasChanged, availableSounds, previewUri)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun hasUnsavedModifications(): Boolean =
        uiState.value?.hasUnsavedModifications == true

    fun setName(name: String) {
        updateEditedPlaySound { old -> old.copy(name = "" + name) }
    }

    fun setSound(sound: NotificationSoundItem) {
        updateEditedPlaySound { old ->
            old.copy(
                soundUri = sound.uri,
                soundTitle = sound.title,
            )
        }
    }

    fun togglePreview(uri: String) {
        if (playingPreviewUri.value == uri) {
            stopPreview()
        } else {
            startPreview(uri)
        }
    }

    private fun startPreview(uriString: String) {
        stopPreview()
        try {
            val uri = Uri.parse(uriString)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener {
                    stopPreview()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Preview MediaPlayer error: what=$what, extra=$extra")
                    stopPreview()
                    true
                }
                prepare()
                start()
            }
            previewPlayer = player
            playingPreviewUri.value = uriString
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preview sound: $uriString", e)
            stopPreview()
        }
    }

    fun stopPreview() {
        previewPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping preview player", e)
            } finally {
                player.release()
            }
        }
        previewPlayer = null
        playingPreviewUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }

    private fun updateEditedPlaySound(updater: (old: PlaySound) -> PlaySound) {
        editionRepository.editionState.getEditedAction<PlaySound>()?.let { oldPlaySound ->
            editionRepository.updateEditedAction(updater(oldPlaySound))
        }
    }
}

private const val TAG = "PlaySoundViewModel"
