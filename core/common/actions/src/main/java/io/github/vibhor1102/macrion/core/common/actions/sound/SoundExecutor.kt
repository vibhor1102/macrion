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
package io.github.vibhor1102.macrion.core.common.actions.sound

import androidx.core.net.toUri
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SoundExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private var activePlayer: MediaPlayer? = null

    @Synchronized
    fun playSound(soundUri: String) {
        stopSound()
        try {
            val uri = soundUri.toUri()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setOnCompletionListener { mp ->
                    synchronized(this@SoundExecutor) {
                        if (activePlayer === mp) {
                            activePlayer = null
                        }
                    }
                    mp.release()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    synchronized(this@SoundExecutor) {
                        if (activePlayer === mp) {
                            activePlayer = null
                        }
                    }
                    mp.release()
                    true
                }
                prepare()
                start()
            }
            activePlayer = player
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play sound: $soundUri", e)
        }
    }

    @Synchronized
    fun stopSound() {
        activePlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping sound player", e)
            } finally {
                player.release()
            }
        }
        activePlayer = null
    }

    fun clear() {
        stopSound()
    }
}

private const val TAG = "SoundExecutor"
