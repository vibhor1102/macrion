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
package io.github.vibhor1102.macrion.feature.smart.config.domain.sound

import android.content.Context
import android.media.RingtoneManager
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.feature.smart.config.R
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationSoundItem(
    val title: String,
    val uri: String,
    val isDefault: Boolean = false,
)

@Singleton
class NotificationSoundRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun getNotificationSounds(): List<NotificationSoundItem> {
        val sounds = mutableListOf<NotificationSoundItem>()

        // 1. Default system notification sound
        val defaultUri = Settings.System.DEFAULT_NOTIFICATION_URI
        sounds.add(
            NotificationSoundItem(
                title = context.getString(R.string.default_notification_sound),
                uri = defaultUri.toString(),
                isDefault = true,
            )
        )

        // 2. Query available notification sounds via RingtoneManager
        try {
            val ringtoneManager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_NOTIFICATION)
            }
            val cursor = ringtoneManager.cursor
            val queriedSounds = mutableListOf<NotificationSoundItem>()
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)?.toString()
                    if (!title.isNullOrBlank() && !uri.isNullOrBlank()) {
                        queriedSounds.add(NotificationSoundItem(title = title, uri = uri))
                    }
                }
            }
            queriedSounds.sortBy { it.title.lowercase() }
            sounds.addAll(queriedSounds)
        } catch (e: Exception) {
            Log.w(TAG, "Error querying notification sounds", e)
        }

        return sounds
    }
}

private const val TAG = "NotificationSoundRepository"
