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
import io.github.vibhor1102.macrion.core.database.EVENT_TABLE
import io.github.vibhor1102.macrion.core.database.SCENARIO_TABLE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the AutoMigration from database v27 to v28. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration27to28Tests {

    private companion object {
        private const val OLD_DB_VERSION = 27
        private const val NEW_DB_VERSION = 28

        private const val FOLDER_NAME_COLUMN = "folder_name"
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
            .getDatabasePath("migration-27-to-28-test").path
    }

    @Test
    fun migrate_existingEvent_folderNameDefaultsToNull() {
        val scenarioId = 1L
        val eventId = 10L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insertTestEvent(eventId, scenarioId)
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT $FOLDER_NAME_COLUMN FROM $EVENT_TABLE WHERE id = $eventId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrate_existingEvent_preservesOtherColumns() {
        val scenarioId = 2L
        val eventId = 20L

        helper.createDatabase(dbPath, OLD_DB_VERSION).use { db ->
            db.insertTestScenario(scenarioId)
            db.insert(EVENT_TABLE, 0, ContentValues().apply {
                put("id", eventId)
                put("scenario_id", scenarioId)
                put("name", "Test Screen Event")
                put("operator", 1)
                put("priority", 5)
                put("enabled_on_start", 1)
                put("type", "IMAGE_EVENT")
            })
        }

        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT name, operator, priority, enabled_on_start, type, $FOLDER_NAME_COLUMN FROM $EVENT_TABLE WHERE id = $eventId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test Screen Event", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertEquals(5, cursor.getInt(2))
                assertEquals(1, cursor.getInt(3))
                assertEquals("IMAGE_EVENT", cursor.getString(4))
                assertTrue(cursor.isNull(5))
            }
        }
    }

    @Test
    fun migrate_scenarioFoldersDefaultToEmptyAndCanStorePositions() {
        helper.createDatabase(dbPath, OLD_DB_VERSION).use { it.insertTestScenario(3) }
        helper.runMigrationsAndValidate(dbPath, NEW_DB_VERSION, true).use { db ->
            db.query("SELECT folders FROM $SCENARIO_TABLE WHERE id = 3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("[]", cursor.getString(0))
            }
            val converter = io.github.vibhor1102.macrion.core.database.entity.ScenarioFoldersConverter()
            val folders = listOf(io.github.vibhor1102.macrion.core.database.entity.ScenarioFolderEntity("Empty", 0))
            db.execSQL("UPDATE $SCENARIO_TABLE SET folders = ? WHERE id = 3", arrayOf(converter.encode(folders)))
            db.query("SELECT folders FROM $SCENARIO_TABLE WHERE id = 3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(folders, converter.decode(cursor.getString(0)))
            }
        }
    }

    @Test
    fun emptyFoldersSurviveDaoSaveAndDatabaseReopen() = kotlinx.coroutines.runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "folder-persistence-test"
        context.deleteDatabase(databaseName)
        val folders = listOf(
            io.github.vibhor1102.macrion.core.database.entity.ScenarioFolderEntity("First", 0),
            io.github.vibhor1102.macrion.core.database.entity.ScenarioFolderEntity("Second", 0),
        )
        fun open() = androidx.room.Room.databaseBuilder(context, ClickDatabase::class.java, databaseName).build()
        try {
            val first = open()
            try {
                val scenario = io.github.vibhor1102.macrion.core.database.entity.ScenarioEntity(9, "Test", 600)
                first.scenarioDao().add(scenario)
                first.scenarioDao().update(scenario.copy(folders = folders))
            } finally {
                first.close()
            }
            val reopened = open()
            try {
                assertEquals(folders, reopened.scenarioDao().getScenario(9)?.scenario?.folders)
                assertEquals(folders, reopened.scenarioDao().getCompleteScenario(9)?.scenario?.folders)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
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
