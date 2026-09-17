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
package io.github.vibhor1102.macrion.core.database.migrations

import android.content.ContentValues
import android.content.Context
import android.os.Build

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import io.github.vibhor1102.macrion.core.database.ACTION_TABLE
import io.github.vibhor1102.macrion.core.database.ClickDatabase
import io.github.vibhor1102.macrion.core.database.EVENT_TABLE
import io.github.vibhor1102.macrion.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the AutoMigration from database v28 to v29. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration28to29Tests {

    private companion object {
        private const val OLD_DB_VERSION = 28
        private const val NEW_DB_VERSION = 29

        private const val SOUND_URI_COLUMN = "sound_uri"
        private const val SOUND_TITLE_COLUMN = "sound_title"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    private lateinit var dbPath: String

    @Before
    fun setUp() {
        dbPath = ApplicationProvider
            .getApplicationContext<Context>()
            .getDatabasePath("migration-28-to-29-test").path
    }

    @Test
    fun migrate_existingAction_soundColumnsDefaultToNull() {
        val scenarioId = 1L
        val eventId = 10L
        val actionId = 100L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(eventId, scenarioId)
            db.insert(ACTION_TABLE, 0, ContentValues().apply {
                put("id", actionId)
                put("eventId", eventId)
                put("priority", 0)
                put("name", "Test Pause Action")
                put("type", "PAUSE")
                put("pauseDuration", 500L)
            })
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT name, type, pauseDuration, $SOUND_URI_COLUMN, $SOUND_TITLE_COLUMN FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test Pause Action", cursor.getString(0))
                assertEquals("PAUSE", cursor.getString(1))
                assertEquals(500L, cursor.getLong(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
            }
        }
    }

    @Test
    fun migrate_allowsInsertingPlaySoundAction() {
        val scenarioId = 2L
        val eventId = 20L
        val actionId = 200L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(eventId, scenarioId)
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.insert(ACTION_TABLE, 0, ContentValues().apply {
                put("id", actionId)
                put("eventId", eventId)
                put("priority", 1)
                put("name", "Alert Tone")
                put("type", "PLAY_SOUND")
                put(SOUND_URI_COLUMN, "content://settings/system/notification_sound")
                put(SOUND_TITLE_COLUMN, "Chime")
            })

            db.query("SELECT name, type, $SOUND_URI_COLUMN, $SOUND_TITLE_COLUMN FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Alert Tone", cursor.getString(0))
                assertEquals("PLAY_SOUND", cursor.getString(1))
                assertEquals("content://settings/system/notification_sound", cursor.getString(2))
                assertEquals("Chime", cursor.getString(3))
            }
        }
    }

    // ---- Helpers ----

    private fun SupportSQLiteDatabase.insertTestScenario(id: Long) {
        insert(SCENARIO_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("name", "Scenario $id")
            put("detection_quality", 1200)
            put("compute_rate", 0.0)
            put("randomize", 0)
            put("keep_screen_on", 0)
        })
    }

    private fun SupportSQLiteDatabase.insertTestEvent(id: Long, scenarioId: Long) {
        insert(EVENT_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("scenario_id", scenarioId)
            put("name", "Event $id")
            put("operator", 0)
            put("priority", 0)
            put("enabled_on_start", 1)
            put("type", "IMAGE_EVENT")
        })
    }
}
