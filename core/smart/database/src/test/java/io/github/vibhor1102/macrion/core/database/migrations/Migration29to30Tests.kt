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

/** Tests the AutoMigration from database v29 to v30. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration29to30Tests {

    private companion object {
        private const val OLD_DB_VERSION = 29
        private const val NEW_DB_VERSION = 30

        private const val SCREENSHOT_FOLDER_URI_COLUMN = "screenshot_folder_uri"
        private const val SCREENSHOT_FOLDER_NAME_COLUMN = "screenshot_folder_name"
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
            .getDatabasePath("migration-29-to-30-test").path
    }

    @Test
    fun migrate_existingAction_screenshotColumnsDefaultToNull() {
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
                put("name", "Test Sound Action")
                put("type", "PLAY_SOUND")
                put("sound_uri", "content://media/internal/audio/media/12")
                put("sound_title", "Beep")
            })
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT name, type, sound_uri, sound_title, $SCREENSHOT_FOLDER_URI_COLUMN, $SCREENSHOT_FOLDER_NAME_COLUMN FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test Sound Action", cursor.getString(0))
                assertEquals("PLAY_SOUND", cursor.getString(1))
                assertEquals("content://media/internal/audio/media/12", cursor.getString(2))
                assertEquals("Beep", cursor.getString(3))
                assertTrue(cursor.isNull(4))
                assertTrue(cursor.isNull(5))
            }
        }
    }

    @Test
    fun migrate_allowsInsertingCaptureScreenshotAction() {
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
                put("name", "Capture Screen")
                put("type", "CAPTURE_SCREENSHOT")
                put(SCREENSHOT_FOLDER_URI_COLUMN, "content://com.android.externalstorage.documents/tree/primary%3APictures%2FCustom")
                put(SCREENSHOT_FOLDER_NAME_COLUMN, "Custom")
            })

            db.query("SELECT name, type, $SCREENSHOT_FOLDER_URI_COLUMN, $SCREENSHOT_FOLDER_NAME_COLUMN FROM $ACTION_TABLE WHERE id = $actionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Capture Screen", cursor.getString(0))
                assertEquals("CAPTURE_SCREENSHOT", cursor.getString(1))
                assertEquals("content://com.android.externalstorage.documents/tree/primary%3APictures%2FCustom", cursor.getString(2))
                assertEquals("Custom", cursor.getString(3))
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
