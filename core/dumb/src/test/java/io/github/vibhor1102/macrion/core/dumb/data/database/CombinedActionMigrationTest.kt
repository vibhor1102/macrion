package io.github.vibhor1102.macrion.core.dumb.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), DumbDatabase::class.java)

    @Test fun existingTouchesAndDelaysSurviveMigration() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("combined-migration").path
        helper.createDatabase(path, 4).use { db ->
            db.execSQL("INSERT INTO dumb_scenario_table (id, name, repeat_count, is_repeat_infinite, max_duration_minutes, is_duration_infinite, randomize) VALUES (1, 'Scenario', 1, 0, 1, 1, 0)")
            db.execSQL("INSERT INTO dumb_action_table (id, dumb_scenario_id, name, type, priority) VALUES (1, 1, 'Zoom', 'SPLIT_ACTION', 0)")
            db.execSQL("INSERT INTO dumb_split_action_item_table (id, action_id, priority, type, from_x, from_y, to_x, to_y, duration, start_offset, wait_after_ms) VALUES (1, 1, 0, 'SWIPE', 10, 20, 30, 40, 500, 150, 250)")
        }
        helper.runMigrationsAndValidate(path, 5, true).use { db ->
            db.query("SELECT * FROM dumb_split_action_item_table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(500L, cursor.getLong(cursor.getColumnIndexOrThrow("duration")))
                assertEquals(150L, cursor.getLong(cursor.getColumnIndexOrThrow("start_offset")))
                assertEquals(250L, cursor.getLong(cursor.getColumnIndexOrThrow("wait_after_ms")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("name")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("repeat_count")))
                assertEquals(0L, cursor.getLong(cursor.getColumnIndexOrThrow("repeat_delay_ms")))
            }
        }
    }

    @Test fun existingSwipeRowsGainEmptyPathWithoutChangingPositions() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("dumb-swipe-path-migration").path
        helper.createDatabase(path, 5).use { db ->
            db.execSQL("INSERT INTO dumb_scenario_table (id, name, repeat_count, is_repeat_infinite, max_duration_minutes, is_duration_infinite, randomize) VALUES (1, 'Scenario', 1, 0, 1, 1, 0)")
            db.execSQL("INSERT INTO dumb_action_table (id, dumb_scenario_id, name, type, priority, fromX, fromY, toX, toY) VALUES (1, 1, 'Swipe', 'SWIPE', 0, 10, 20, 30, 40)")
        }
        helper.runMigrationsAndValidate(path, 6, true).use { db ->
            db.query("SELECT fromX, toY, swipe_path FROM dumb_action_table WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(10, cursor.getInt(0))
                assertEquals(40, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
        }
    }
}
