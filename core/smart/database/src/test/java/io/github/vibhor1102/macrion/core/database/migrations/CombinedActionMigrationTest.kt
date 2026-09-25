package io.github.vibhor1102.macrion.core.database.migrations

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vibhor1102.macrion.core.database.ClickDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class CombinedActionMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ClickDatabase::class.java)

    @Test fun existingTouchesAndDelaysSurviveMigration() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("combined-migration").path
        helper.createDatabase(path, 32).use { db ->
            db.execSQL("INSERT INTO scenario_table (id, name, detection_quality, compute_rate, randomize, keep_screen_on) VALUES (1, 'Scenario', 600, 0, 0, 0)")
            db.execSQL("INSERT INTO event_table (id, scenario_id, name, operator, priority, enabled_on_start, type) VALUES (1, 1, 'Event', 0, 0, 1, 'IMAGE_EVENT')")
            db.execSQL("INSERT INTO action_table (id, eventId, name, type, priority) VALUES (1, 1, 'Zoom', 'SPLIT_ACTION', 0)")
            db.execSQL("INSERT INTO split_action_item_table (id, action_id, priority, type, from_x, from_y, to_x, to_y, duration, start_offset, wait_after_ms) VALUES (1, 1, 0, 'SWIPE', 10, 20, 30, 40, 500, 150, 250)")
        }
        helper.runMigrationsAndValidate(path, 33, true).use { db ->
            db.query("SELECT * FROM split_action_item_table").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(500L, cursor.getLong(cursor.getColumnIndexOrThrow("duration")))
                assertEquals(150L, cursor.getLong(cursor.getColumnIndexOrThrow("start_offset")))
                assertEquals(250L, cursor.getLong(cursor.getColumnIndexOrThrow("wait_after_ms")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("name")))
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("click_position_type")))
            }
        }
    }

    @Test fun existingSwipeRowsGainEmptyPathWithoutChangingPositions() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("swipe-path-migration").path
        helper.createDatabase(path, 33).use { db ->
            db.execSQL("INSERT INTO scenario_table (id, name, detection_quality, compute_rate, randomize, keep_screen_on) VALUES (1, 'Scenario', 600, 0, 0, 0)")
            db.execSQL("INSERT INTO event_table (id, scenario_id, name, operator, priority, enabled_on_start, type) VALUES (1, 1, 'Event', 0, 0, 1, 'IMAGE_EVENT')")
            db.execSQL("INSERT INTO action_table (id, eventId, name, type, priority, fromX, fromY, toX, toY) VALUES (1, 1, 'Swipe', 'SWIPE', 0, 10, 20, 30, 40)")
        }
        helper.runMigrationsAndValidate(path, 34, true).use { db ->
            db.query("SELECT fromX, toY, swipe_path FROM action_table WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(10, cursor.getInt(0))
                assertEquals(40, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
            }
        }
    }
}
