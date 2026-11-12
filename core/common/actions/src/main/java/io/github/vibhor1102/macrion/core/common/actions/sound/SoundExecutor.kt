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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SoundExecutor internal constructor(
    private val context: Context,
    private val workerDispatcher: CoroutineDispatcher?,
    private val createPlayer: () -> MediaPlayer,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, null, ::MediaPlayer)

    // MediaPlayer operations stay on one worker thread. Neither UI cleanup nor callbacks wait for preparation.
    private val workerDelegate = lazy {
        CoroutineScope(SupervisorJob() +
            (workerDispatcher ?: Executors.newSingleThreadExecutor { task ->
                Thread(task, "MacrionSound").apply { isDaemon = true }
            }.asCoroutineDispatcher()))
    }
    private val generation = AtomicLong()
    private var activePlayer: MediaPlayer? = null // Worker-confined.

    /** Wait for preparation and start(), not for the sound's completion. Only the scenario is suspended. */
    suspend fun playSound(soundUri: String) {
        currentCoroutineContext().ensureActive()
        suspendCancellableCoroutine<Unit> { continuation ->
            val request = generation.incrementAndGet()
            continuation.invokeOnCancellation {
                // Invalidate immediately, even while the worker is blocked in native preparation. A cancelled
                // request must neither start later nor stop a newer request that has already replaced it.
                if (generation.compareAndSet(request, request + 1)) releaseWhenCurrent(request + 1)
            }
            workerDelegate.value.launch {
                var candidate: MediaPlayer? = null
                try {
                    if (generation.get() != request) return@launch
                    releaseActivePlayer()
                    val player = createPlayer().also { candidate = it }
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    player.setDataSource(context, soundUri.toUri())
                    player.setOnCompletionListener { completed -> releaseWhenActive(completed) }
                    player.setOnErrorListener { failed, what, extra ->
                        Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        releaseWhenActive(failed)
                        true
                    }
                    player.prepare()
                    if (generation.get() == request) {
                        activePlayer = player
                        candidate = null
                        player.start()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to play sound: $soundUri", e)
                    releaseActivePlayer()
                } finally {
                    try {
                        candidate?.release()
                    } finally {
                        // Preserve best-effort sound failures, but never advance a stopped scenario.
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
    }

    fun stopSound() = releaseWhenCurrent(generation.incrementAndGet())

    private fun releaseWhenCurrent(request: Long) {
        if (!workerDelegate.isInitialized()) return
        workerDelegate.value.launch {
            // A late cleanup request must not stop a newer scenario's sound.
            if (generation.get() == request) releaseActivePlayer()
        }
    }

    private fun releaseWhenActive(player: MediaPlayer) {
        workerDelegate.value.launch {
            if (activePlayer === player) releaseActivePlayer()
        }
    }

    private fun releaseActivePlayer() {
        val player = activePlayer ?: return
        activePlayer = null
        player.release()
    }

    fun clear() = stopSound()
}

private const val TAG = "SoundExecutor"
