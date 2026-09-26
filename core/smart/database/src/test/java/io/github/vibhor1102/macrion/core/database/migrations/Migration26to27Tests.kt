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

import io.github.vibhor1102.macrion.core.database.ClickDatabase
import io.github.vibhor1102.macrion.core.database.CONDITION_TABLE
import io.github.vibhor1102.macrion.core.database.EVENT_TABLE
import io.github.vibhor1102.macrion.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the AutoMigration from database v26 to v27. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration26to27Tests {

    private companion object {
        private const val OLD_DB_VERSION = 26
        private const val NEW_DB_VERSION = 27

        private const val COMPUTE_RATE_COLUMN = "computeRate"
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
            .getDatabasePath("migration-26-to-27-test").path
    }

    @Test
    fun migrate_existingCondition_computeRateDefaultsToZero() {
        val scenarioId = 1L
        val eventId = 10L
        val conditionId = 100L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(eventId, scenarioId)
            db.insertTestCondition(conditionId, eventId)
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT $COMPUTE_RATE_COLUMN FROM $CONDITION_TABLE WHERE id = $conditionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0.0, cursor.getDouble(0), 0.0001)
            }
        }
    }

    @Test
    fun migrate_existingCondition_preservesOtherColumns() {
        val scenarioId = 2L
        val eventId = 20L
        val conditionId = 200L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(eventId, scenarioId)
            db.insert(CONDITION_TABLE, 0, ContentValues().apply {
                put("id", conditionId)
                put("eventId", eventId)
                put("name", "Test Image Condition")
                put("type", "ON_IMAGE_DETECTED")
                put("priority", 1)
                put("shouldBeDetected", 1)
                put("path", "conditions/test.png")
                put("threshold", 85)
            })
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT name, type, priority, shouldBeDetected, path, threshold, $COMPUTE_RATE_COLUMN FROM $CONDITION_TABLE WHERE id = $conditionId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test Image Condition", cursor.getString(0))
                assertEquals("ON_IMAGE_DETECTED", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(1, cursor.getInt(3))
                assertEquals("conditions/test.png", cursor.getString(4))
                assertEquals(85, cursor.getInt(5))
                assertEquals(0.0, cursor.getDouble(6), 0.0001)
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

    private fun SupportSQLiteDatabase.insertTestCondition(id: Long, eventId: Long) {
        insert(CONDITION_TABLE, 0, ContentValues().apply {
            put("id", id)
            put("eventId", eventId)
            put("name", "Condition $id")
            put("type", "ON_COLOR_DETECTED")
            put("priority", 0)
            put("shouldBeDetected", 1)
            put("color_rgba", -1)
        })
    }
}
